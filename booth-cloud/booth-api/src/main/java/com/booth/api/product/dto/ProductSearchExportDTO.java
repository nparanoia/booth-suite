package com.booth.api.product.dto;

import java.util.List;

/**
 * 按商品 ID 往后翻的一页上架商品。nextAfterId 是本页最后一条的 ID，没有数据时等于请求里的 afterId。
 */
public record ProductSearchExportDTO(List<ProductSearchDocDTO> docs, long nextAfterId, boolean hasMore) {
}
