package com.booth.search.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.VersionType;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem;
import co.elastic.clients.elasticsearch.indices.GetAliasResponse;
import com.booth.api.product.dto.ProductSearchDocDTO;
import com.booth.search.support.SearchIndexNames;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 索引对外只用别名 product。写入带 external_gte，版本更小的旧消息会被拒绝。
 * 重建进行中时，增量同时写别名和新索引的真实名字。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductIndexService {

    private final ElasticsearchClient client;
    private final StringRedisTemplate redis;

    @PostConstruct
    public void ensureAlias() {
        if (aliasExists()) {
            return;
        }
        String index = createIndex();
        io(() -> client.indices().updateAliases(request -> request.actions(action ->
                action.add(add -> add.index(index).alias(SearchIndexNames.ALIAS)))));
        log.info("已创建商品索引 {}，别名 {}", index, SearchIndexNames.ALIAS);
    }

    public String createIndex() {
        String name = SearchIndexNames.newIndex();
        io(() -> {
            try (InputStream mapping = new ClassPathResource("es/product-mapping.json").getInputStream()) {
                client.indices().create(request -> request.index(name).withJson(mapping));
            }
            return null;
        });
        return name;
    }

    public void apply(List<ProductSearchDocDTO> docs) {
        if (docs == null || docs.isEmpty()) {
            return;
        }
        for (String index : targets()) {
            writeAll(index, docs);
        }
    }

    public void writeAll(String index, List<ProductSearchDocDTO> docs) {
        List<ProductSearchDocDTO> upserts = docs.stream().filter(ProductSearchDocDTO::onShelf).toList();
        List<ProductSearchDocDTO> removals = docs.stream().filter(doc -> !doc.onShelf()).toList();
        if (!upserts.isEmpty()) {
            bulkIndex(index, upserts);
        }
        for (ProductSearchDocDTO removal : removals) {
            delete(index, removal.id(), removal.version());
        }
    }

    public void switchAlias(String newIndex) {
        Set<String> previous = aliasIndices();
        io(() -> client.indices().updateAliases(request -> {
            for (String old : previous) {
                if (!old.equals(newIndex)) {
                    request.actions(action -> action.remove(remove -> remove.index(old).alias(SearchIndexNames.ALIAS)));
                }
            }
            request.actions(action -> action.add(add -> add.index(newIndex).alias(SearchIndexNames.ALIAS)));
            return request;
        }));
        for (String old : previous) {
            if (!old.equals(newIndex)) {
                deleteIndexQuietly(old);
            }
        }
    }

    public void deleteIndexQuietly(String index) {
        try {
            io(() -> client.indices().delete(request -> request.index(index)));
        } catch (RuntimeException e) {
            log.warn("删除索引失败，index={}", index, e);
        }
    }

    private void bulkIndex(String index, List<ProductSearchDocDTO> docs) {
        BulkResponse response = io(() -> client.bulk(request -> {
            for (ProductSearchDocDTO doc : docs) {
                request.operations(op -> op.index(item -> item
                        .index(index)
                        .id(String.valueOf(doc.id()))
                        .document(ProductIndexDoc.from(doc))
                        .version(doc.version())
                        .versionType(VersionType.ExternalGte)));
            }
            return request;
        }));
        if (!response.errors()) {
            return;
        }
        for (BulkResponseItem item : response.items()) {
            if (item.error() == null || "version_conflict_engine_exception".equals(item.error().type())) {
                continue;
            }
            if ("index_not_found_exception".equals(item.error().type()) && !SearchIndexNames.ALIAS.equals(index)) {
                log.info("重建索引已取消，跳过写入 {}", index);
                return;
            }
            throw new IllegalStateException("写入商品索引失败：" + item.error().reason());
        }
    }

    private void delete(String index, Long id, long version) {
        try {
            io(() -> client.delete(request -> request
                    .index(index)
                    .id(String.valueOf(id))
                    .version(version)
                    .versionType(VersionType.ExternalGte)));
        } catch (ElasticsearchException e) {
            if (e.status() == 409) {
                log.info("忽略旧的商品删除，id={}", id);
                return;
            }
            if (e.status() == 404 && !SearchIndexNames.ALIAS.equals(index)) {
                log.info("重建索引已取消，跳过删除 {}", index);
                return;
            }
            throw e;
        }
    }

    private List<String> targets() {
        LinkedHashSet<String> names = new LinkedHashSet<>();
        names.add(SearchIndexNames.ALIAS);
        String rebuilding = redis.opsForValue().get(SearchIndexNames.REBUILD_INDEX);
        if (StringUtils.hasText(rebuilding)) {
            names.add(rebuilding);
        }
        return new ArrayList<>(names);
    }

    private boolean aliasExists() {
        return io(() -> client.indices().existsAlias(request -> request.name(SearchIndexNames.ALIAS))).value();
    }

    private Set<String> aliasIndices() {
        if (!aliasExists()) {
            return Set.of();
        }
        GetAliasResponse response = io(() -> client.indices().getAlias(request -> request.name(SearchIndexNames.ALIAS)));
        return response.result().keySet();
    }

    private <T> T io(IoCall<T> call) {
        try {
            return call.run();
        } catch (IOException e) {
            throw new IllegalStateException("搜索引擎访问失败", e);
        }
    }

    @FunctionalInterface
    private interface IoCall<T> {
        T run() throws IOException;
    }
}
