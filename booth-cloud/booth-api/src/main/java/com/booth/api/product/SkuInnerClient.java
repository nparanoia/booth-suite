package com.booth.api.product;

import com.booth.api.product.dto.SkuInfoDTO;
import com.booth.common.core.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient(name = "booth-product", contextId = "skuInnerClient", path = "/inner/skus")
public interface SkuInnerClient {

    /**
     * 不存在的 SKU 不会出现在结果里，调用方按 skuId 对比判断。
     */
    @PostMapping("/batch")
    Result<List<SkuInfoDTO>> listByIds(@RequestBody List<Long> skuIds);
}
