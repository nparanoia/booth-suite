package com.booth.product.controller.inner;

import com.booth.api.product.dto.OrderSnRequest;
import com.booth.api.product.dto.StockLockRequest;
import com.booth.api.product.dto.StockReleaseRequest;
import com.booth.common.core.result.Result;
import com.booth.product.service.StockInnerService;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 实现 {@link com.booth.api.product.StockInnerClient}。
 */
@Hidden
@RestController
@RequestMapping("/inner/stock")
@RequiredArgsConstructor
public class StockInnerController {

    private final StockInnerService stockInnerService;

    @PostMapping("/lock")
    public Result<Void> lock(@Valid @RequestBody StockLockRequest request) {
        return onceMoreOnDuplicate(() -> stockInnerService.lock(request));
    }

    @PostMapping("/deduct")
    public Result<Void> deduct(@Valid @RequestBody OrderSnRequest request) {
        stockInnerService.deduct(request.orderSn());
        return Result.ok();
    }

    @PostMapping("/release")
    public Result<Void> release(@Valid @RequestBody StockReleaseRequest request) {
        return onceMoreOnDuplicate(() -> stockInnerService.release(request));
    }

    /**
     * 并发的第二次请求会在插入流水时撞上唯一键，事务回滚后再执行一次即可命中幂等分支。
     * 捕获必须放在事务方法外面，否则事务会被标记为只能回滚。
     */
    private Result<Void> onceMoreOnDuplicate(Runnable action) {
        try {
            action.run();
        } catch (DuplicateKeyException e) {
            action.run();
        }
        return Result.ok();
    }
}
