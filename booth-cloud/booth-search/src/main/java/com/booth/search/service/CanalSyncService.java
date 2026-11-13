package com.booth.search.service;

import com.booth.api.product.ProductSearchClient;
import com.booth.api.product.dto.ProductIdQuery;
import com.booth.api.product.dto.ProductSearchDocDTO;
import com.booth.search.support.CanalMessageParser;
import com.booth.search.support.CanalMessageParser.CanalChange;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * 变更消息只用来发现受影响的商品，文档内容重新向商品服务查询。重复投递结果一致。
 */
@Service
@RequiredArgsConstructor
public class CanalSyncService {

    private static final int BATCH = 200;

    private final ProductSearchClient productSearchClient;
    private final ProductIndexService indexService;

    public void accept(String body) {
        if (body == null || body.isBlank()) {
            return;
        }
        for (CanalChange change : CanalMessageParser.parse(body)) {
            LinkedHashSet<Long> productIds = new LinkedHashSet<>(change.productIds());
            if (!change.brandIds().isEmpty()) {
                productIds.addAll(productSearchClient.ids(new ProductIdQuery(change.brandIds(), List.of())).dataOrThrow());
            }
            if (!change.categoryIds().isEmpty()) {
                productIds.addAll(productSearchClient.ids(new ProductIdQuery(List.of(), change.categoryIds())).dataOrThrow());
            }
            refresh(productIds);
        }
    }

    private void refresh(LinkedHashSet<Long> productIds) {
        List<Long> ids = new ArrayList<>(productIds);
        for (int from = 0; from < ids.size(); from += BATCH) {
            List<Long> batch = ids.subList(from, Math.min(from + BATCH, ids.size()));
            List<ProductSearchDocDTO> docs = productSearchClient.docs(batch).dataOrThrow();
            indexService.apply(docs);
        }
    }
}
