package com.booth.product.task;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.booth.api.product.dto.StockItemDTO;
import com.booth.api.product.dto.StockReleaseRequest;
import com.booth.api.trade.OrderInnerClient;
import com.booth.api.trade.dto.OrderStatusDTO;
import com.booth.product.entity.StockLockRecord;
import com.booth.product.enums.StockLockStatus;
import com.booth.product.mapper.StockLockRecordMapper;
import com.booth.product.service.StockInnerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 延迟消息丢失时的兜底：锁定超过支付超时再加 10 分钟仍未结束的记录，向交易服务核对订单状态。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StockReconcileTask {

    private static final int PAID_MIN = 1;
    private static final int PAID_MAX = 3;
    private static final int CLOSED = 4;

    private final StockLockRecordMapper lockRecordMapper;
    private final StockInnerService stockInnerService;
    private final OrderInnerClient orderInnerClient;

    @Value("${booth.order.pay-timeout:24h}")
    private Duration payTimeout;

    @Scheduled(fixedDelay = 300_000, initialDelay = 60_000)
    public void reconcile() {
        LocalDateTime deadline = LocalDateTime.now().minus(payTimeout).minusMinutes(10);
        List<StockLockRecord> stuck = lockRecordMapper.selectList(Wrappers.<StockLockRecord>lambdaQuery()
                .eq(StockLockRecord::getStatus, StockLockStatus.LOCKED)
                .lt(StockLockRecord::getCreateTime, deadline)
                .last("LIMIT 200"));
        Map<String, List<StockLockRecord>> byOrder = stuck.stream().collect(Collectors.groupingBy(StockLockRecord::getOrderSn));
        for (Map.Entry<String, List<StockLockRecord>> entry : byOrder.entrySet()) {
            try {
                reconcileOrder(entry.getKey(), entry.getValue());
            } catch (RuntimeException e) {
                log.warn("库存对账失败，orderSn={}", entry.getKey(), e);
            }
        }
    }

    private void reconcileOrder(String orderSn, List<StockLockRecord> records) {
        OrderStatusDTO status = orderInnerClient.status(orderSn).dataOrThrow();
        if (!status.exists() || (status.status() != null && status.status() == CLOSED)) {
            List<StockItemDTO> items = records.stream().map(record -> new StockItemDTO(record.getSkuId(), record.getQuantity())).toList();
            stockInnerService.release(new StockReleaseRequest(orderSn, items));
            log.info("对账释放锁定库存，orderSn={}", orderSn);
            return;
        }
        if (status.status() != null && status.status() >= PAID_MIN && status.status() <= PAID_MAX) {
            stockInnerService.deduct(orderSn);
            log.info("对账扣减锁定库存，orderSn={}", orderSn);
        }
    }
}
