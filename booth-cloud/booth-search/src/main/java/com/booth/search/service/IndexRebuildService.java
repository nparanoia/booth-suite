package com.booth.search.service;

import com.booth.api.product.ProductSearchClient;
import com.booth.api.product.dto.ProductSearchExportDTO;
import com.booth.common.core.exception.BizException;
import com.booth.search.error.SearchErrorCode;
import com.booth.search.support.SearchIndexNames;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * 先记下正在重建的新索引，再按游标导入。这段时间里的增量会同时写旧索引和新索引。
 * 别名切换是一次请求，搜索在切换前后都读得到 product 别名。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IndexRebuildService {

    private static final int PAGE = 200;

    private final StringRedisTemplate redis;
    private final ProductIndexService indexService;
    private final ProductSearchClient productSearchClient;

    public void rebuild() {
        Boolean locked = redis.opsForValue().setIfAbsent(SearchIndexNames.REBUILD_LOCK, "1", Duration.ofMinutes(10));
        if (!Boolean.TRUE.equals(locked)) {
            throw new BizException(SearchErrorCode.REBUILD_BUSY);
        }
        String created = null;
        try {
            created = indexService.createIndex();
            redis.opsForValue().set(SearchIndexNames.REBUILD_INDEX, created);
            importAll(created);
            indexService.switchAlias(created);
            log.info("商品索引已切换到 {}", created);
            created = null;
        } catch (RuntimeException e) {
            if (created != null) {
                indexService.deleteIndexQuietly(created);
            }
            throw e;
        } finally {
            redis.delete(SearchIndexNames.REBUILD_INDEX);
            redis.delete(SearchIndexNames.REBUILD_LOCK);
        }
    }

    private void importAll(String index) {
        long afterId = 0;
        while (true) {
            ProductSearchExportDTO page = productSearchClient.export(afterId, PAGE).dataOrThrow();
            if (page.docs() != null && !page.docs().isEmpty()) {
                indexService.writeAll(index, page.docs());
            }
            redis.expire(SearchIndexNames.REBUILD_LOCK, Duration.ofMinutes(10));
            if (!page.hasMore()) {
                return;
            }
            afterId = page.nextAfterId();
        }
    }
}
