package com.booth.trade.service;

import com.booth.api.product.StockInnerClient;
import com.booth.api.product.dto.OrderSnRequest;
import com.booth.api.product.dto.StockItemDTO;
import com.booth.api.product.dto.StockReleaseRequest;
import com.booth.api.user.IntegrationInnerClient;
import com.booth.api.user.dto.IntegrationChangeRequest;
import com.booth.trade.entity.TradeOrder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 订单消息的副作用。库存、积分、优惠券各自幂等，消息重复投递不会重复生效。
 */
@Service
@RequiredArgsConstructor
public class OrderEventHandler {

    private final OrderLifecycleService lifecycleService;
    private final StockInnerClient stockInnerClient;
    private final IntegrationInnerClient integrationInnerClient;
    private final CouponService couponService;
    private final SeckillRestoreService seckillRestoreService;

    public void onPaid(String orderSn) {
        stockInnerClient.deduct(new OrderSnRequest(orderSn)).dataOrThrow();
        couponService.markUsed(orderSn);
    }

    public void onClosed(String orderSn) {
        TradeOrder order = lifecycleService.find(orderSn);
        if (order == null) {
            return;
        }
        List<StockItemDTO> items = lifecycleService.items(order.getId()).stream()
                .map(item -> new StockItemDTO(item.getSkuId(), item.getQuantity())).toList();
        stockInnerClient.release(new StockReleaseRequest(orderSn, items)).dataOrThrow();
        couponService.unlock(orderSn);
        if (order.getUseIntegration() != null && order.getUseIntegration() > 0) {
            integrationInnerClient.refund(order.getMemberId(), new IntegrationChangeRequest(orderSn, order.getUseIntegration()))
                    .dataOrThrow();
        }
        seckillRestoreService.restore(orderSn);
    }

    public void onCompleted(String orderSn) {
        TradeOrder order = lifecycleService.find(orderSn);
        if (order == null || order.getRewardIntegration() == null || order.getRewardIntegration() <= 0) {
            return;
        }
        integrationInnerClient.reward(order.getMemberId(), new IntegrationChangeRequest(orderSn, order.getRewardIntegration()))
                .dataOrThrow();
    }

    public void onTimeout(String orderSn) {
        lifecycleService.closeTimeout(orderSn);
    }
}
