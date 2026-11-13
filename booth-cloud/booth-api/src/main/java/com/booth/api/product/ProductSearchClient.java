package com.booth.api.product;

import com.booth.api.product.dto.ProductIdQuery;
import com.booth.api.product.dto.ProductSearchDocDTO;
import com.booth.api.product.dto.ProductSearchExportDTO;
import com.booth.common.core.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "booth-product", contextId = "productSearchClient", path = "/inner/products")
public interface ProductSearchClient {

    /**
     * 只返回上架商品的完整文档。未上架、已删除的 ID 带回 onShelf=false，调用方据此删除索引。
     * 数据库里根本没有的 ID 不会出现。
     */
    @PostMapping("/search-docs")
    Result<List<ProductSearchDocDTO>> docs(@RequestBody List<Long> ids);

    @GetMapping("/search-docs/export")
    Result<ProductSearchExportDTO> export(@RequestParam("afterId") long afterId, @RequestParam("size") int size);

    @PostMapping("/search-docs/ids")
    Result<List<Long>> ids(@RequestBody ProductIdQuery query);
}
