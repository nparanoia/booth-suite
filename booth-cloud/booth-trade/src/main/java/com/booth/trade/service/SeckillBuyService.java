package com.booth.trade.service;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.booth.api.user.MemberInnerClient;
import com.booth.common.core.exception.BizException;
import com.booth.trade.entity.LocalMessage;
import com.booth.trade.entity.SeckillActivity;
import com.booth.trade.entity.SeckillSession;
import com.booth.trade.entity.SeckillSku;
import com.booth.trade.enums.LocalMessageStatus;
import com.booth.trade.enums.ReleaseStatus;
import com.booth.trade.error.SeckillBusyException;
import com.booth.trade.error.TradeErrorCode;
import com.booth.trade.support.SeckillKeys;
import com.booth.trade.support.TradeMetrics;
import com.booth.trade.support.SeckillMq;
import com.booth.trade.support.SeckillOrderMessage;
import com.booth.trade.support.SeckillWindow;
import com.booth.trade.vo.SeckillResultVO;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SeckillBuyService {

    private final SeckillAdminService adminService;
    private final SeckillReservationService reservationService;
    private final LocalMessageService localMessageService;
    private final MemberInnerClient memberInnerClient;
    private final StringRedisTemplate redis;
    private final TradeMetrics tradeMetrics;

    public String path(Long memberId, Long seckillSkuId) {
        requireOpen(seckillSkuId);
        String path = UUID.randomUUID().toString().replace("-", "");
        redis.opsForValue().set(SeckillKeys.path(memberId, seckillSkuId), path, Duration.ofSeconds(60));
        return path;
    }

    /**
     * 第一个参数是秒杀商品 ID，热点限流按它计数。
     * 只在路由明确失败时归还库存。确认超时说明结果未知，留给重投，不能加回库存。
     */
    @com.alibaba.csp.sentinel.annotation.SentinelResource(value = "seckillBuy", blockHandler = "blocked")
    public String buy(Long seckillSkuId, Long memberId, String path, int quantity, Long addressId) {
        String removed = redis.opsForValue().getAndDelete(SeckillKeys.path(memberId, seckillSkuId));
        BizException.throwIf(removed == null || !removed.equals(path), TradeErrorCode.SECKILL_PATH_INVALID);
        SeckillSku sku = requireOpen(seckillSkuId);
        BizException.throwIf(quantity > sku.getPerLimit(), TradeErrorCode.SECKILL_LIMIT);
        memberInnerClient.getAddress(memberId, addressId).dataOrThrow();

        long code = reservationService.reserve(seckillSkuId, memberId, quantity);
        if (code == -2 || code == -1) {
            throw new BizException(TradeErrorCode.SECKILL_NOT_STARTED);
        }
        if (code == -3) {
            throw new BizException(TradeErrorCode.SECKILL_LIMIT);
        }
        if (code != 1) {
            throw new BizException(TradeErrorCode.SECKILL_SOLD_OUT);
        }

        String requestId = IdWorker.getIdStr();
        long orderId = IdWorker.getId();
        String orderSn = LocalDate.now(SeckillWindow.ZONE).format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE) + orderId;
        reservationService.queue(requestId, memberId, seckillSkuId, quantity, orderSn);
        SeckillOrderMessage message = new SeckillOrderMessage(requestId, orderSn, orderId, memberId, seckillSkuId, quantity, addressId);
        try {
            String messageId = localMessageService.save(SeckillMq.BIZ_ORDER, requestId, SeckillMq.EXCHANGE,
                    SeckillMq.ROUTING_KEY, message, 0);
            if (unrouted(messageId)) {
                reservationService.abortAndRevert(requestId, memberId, seckillSkuId, quantity);
                throw new BizException(TradeErrorCode.SECKILL_QUEUE_FAILED);
            }
        } catch (RuntimeException e) {
            if (!(e instanceof BizException biz) || biz.getErrorCode() != TradeErrorCode.SECKILL_QUEUE_FAILED) {
                reservationService.abortAndRevert(requestId, memberId, seckillSkuId, quantity);
            }
            throw e;
        }
        return requestId;
    }

    public String blocked(Long seckillSkuId, Long memberId, String path, int quantity, Long addressId, BlockException ex) {
        tradeMetrics.seckillLimited();
        throw new SeckillBusyException();
    }

    public SeckillResultVO result(Long memberId, String requestId) {
        return reservationService.result(memberId, requestId);
    }

    /** 只有被退回才能确定没进队列。重试耗尽和等待超时都可能已经送达，调用方不能归还库存。 */
    private boolean unrouted(String messageId) {
        try {
            for (int i = 0; i < 20; i++) {
                LocalMessage row = localMessageService.findByMessageId(messageId);
                if (row != null && row.getStatus() == LocalMessageStatus.CONFIRMED) {
                    return false;
                }
                if (row != null && row.getStatus() == LocalMessageStatus.ROUTE_FAILED) {
                    return true;
                }
                if (row != null && row.getStatus() == LocalMessageStatus.FAILED) {
                    return false;
                }
                Thread.sleep(100);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return false;
    }

    private SeckillSku requireOpen(Long seckillSkuId) {
        SeckillSku sku = adminService.requireSku(seckillSkuId);
        SeckillActivity activity = adminService.requireActivity(sku.getActivityId());
        SeckillSession session = adminService.requireSession(sku.getSessionId());
        LocalDate today = LocalDate.now(SeckillWindow.ZONE);
        boolean open = activity.getStatus() == ReleaseStatus.RELEASED
                && SeckillWindow.covers(activity, today)
                && Boolean.TRUE.equals(session.getStatus())
                && SeckillWindow.openNow(session);
        BizException.throwIf(!open, TradeErrorCode.SECKILL_NOT_STARTED);
        return sku;
    }
}
