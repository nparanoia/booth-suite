package com.booth.api.product;

import com.booth.api.product.dto.OrderSnRequest;
import com.booth.api.product.dto.StockLockRequest;
import com.booth.api.product.dto.StockReleaseRequest;
import com.booth.common.core.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 三个接口都以 orderSn 幂等。锁定、扣减、释放必须能重复调用。
 */
@FeignClient(name = "booth-product", contextId = "stockInnerClient", path = "/inner/stock")
public interface StockInnerClient {

    @PostMapping("/lock")
    Result<Void> lock(@RequestBody StockLockRequest request);

    @PostMapping("/deduct")
    Result<Void> deduct(@RequestBody OrderSnRequest request);

    @PostMapping("/release")
    Result<Void> release(@RequestBody StockReleaseRequest request);
}
