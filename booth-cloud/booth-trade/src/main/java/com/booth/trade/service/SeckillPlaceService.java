package com.booth.trade.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.booth.api.product.SkuInnerClient;
import com.booth.api.product.StockInnerClient;
import com.booth.api.product.dto.SkuInfoDTO;
import com.booth.api.product.dto.StockItemDTO;
import com.booth.api.product.dto.StockLockRequest;
import com.booth.api.product.dto.StockReleaseRequest;
import com.booth.api.user.MemberInnerClient;
import com.booth.api.user.dto.MemberAddressDTO;
import com.booth.api.user.dto.MemberBriefDTO;
import com.booth.common.core.exception.BizException;
import com.booth.trade.entity.SeckillActivity;
import com.booth.trade.entity.SeckillOrder;
import com.booth.trade.entity.SeckillSku;
import com.booth.trade.enums.ReleaseStatus;
import com.booth.trade.enums.SeckillRecordStatus;
import com.booth.trade.error.TradeErrorCode;
import com.booth.trade.mapper.SeckillOrderMapper;
import com.booth.trade.support.SeckillOrderMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 幂等依据是订单号，以及每个会员在同一秒杀商品上只能有一笔有效单。
 * 业务失败归还 Redis；系统异常不归还，消息重试，最后进死信，由对账看差额。
 * 同一个订单号已经落库时不能再释放实物库存，否则会把成功那一单的锁定放掉。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SeckillPlaceService {

    private final SeckillAdminService adminService;
    private final SeckillOrderStore orderStore;
    private final SeckillOrderMapper seckillOrderMapper;
    private final SeckillReservationService reservationService;
    private final MemberInnerClient memberInnerClient;
    private final SkuInnerClient skuInnerClient;
    private final StockInnerClient stockInnerClient;

    public void handle(SeckillOrderMessage message) {
        String status = reservationService.status(message.requestId());
        if (SeckillReservationService.SUCCESS.equals(status) || SeckillReservationService.FAILED.equals(status)) {
            return;
        }
        if (findByOrderSn(message.orderSn()) != null) {
            reservationService.succeed(message.requestId(), message.orderSn());
            return;
        }
        if (reservationService.aborted(message.requestId())) {
            reservationService.fail(message.requestId(), "抢购请求已放弃");
            return;
        }
        SeckillOrder active = findActive(message.seckillSkuId(), message.memberId());
        if (active != null) {
            failBusiness(message, false, "已经有一笔未关闭的秒杀订单");
            return;
        }

        reservationService.holdPlacing(message.requestId());
        try {
            place(message);
        } finally {
            reservationService.endPlacing(message.requestId());
        }
    }

    private void place(SeckillOrderMessage message) {
        boolean stockAttempted = false;
        try {
            SeckillSku seckill = adminService.requireSku(message.seckillSkuId());
            SeckillActivity activity = adminService.requireActivity(seckill.getActivityId());
            BizException.throwIf(activity.getStatus() != ReleaseStatus.RELEASED, TradeErrorCode.SECKILL_NOT_STARTED);
            BizException.throwIf(message.quantity() > seckill.getPerLimit(), TradeErrorCode.SECKILL_LIMIT);
            List<SkuInfoDTO> skus = skuInnerClient.listByIds(List.of(seckill.getSkuId())).dataOrThrow();
            BizException.throwIf(skus.isEmpty(), TradeErrorCode.SKU_NOT_FOUND);
            SkuInfoDTO sku = skus.get(0);
            BizException.throwIf(!sku.onSale(), TradeErrorCode.SKU_OFF_SHELF);
            MemberBriefDTO member = memberInnerClient.getBrief(message.memberId()).dataOrThrow();
            MemberAddressDTO address = memberInnerClient.getAddress(message.memberId(), message.addressId()).dataOrThrow();
            stockAttempted = true;
            stockInnerClient.lock(new StockLockRequest(message.orderSn(),
                    List.of(new StockItemDTO(seckill.getSkuId(), message.quantity())))).dataOrThrow();
            if (stopBecauseAborted(message, true)) {
                return;
            }
            orderStore.save(message, member, address, seckill, sku);
            reservationService.succeed(message.requestId(), message.orderSn());
        } catch (BizException e) {
            failBusiness(message, stockAttempted && findByOrderSn(message.orderSn()) == null, e.getMessage());
        } catch (DuplicateKeyException e) {
            if (findByOrderSn(message.orderSn()) != null) {
                reservationService.succeed(message.requestId(), message.orderSn());
                return;
            }
            failBusiness(message, stockAttempted, "已经有一笔未关闭的秒杀订单");
        }
    }

    /** 落单前再看一次放弃标记。已经放弃就不再创建订单。两边都可以归还，令牌保证只加一次。 */
    private boolean stopBecauseAborted(SeckillOrderMessage message, boolean releaseStock) {
        if (!reservationService.aborted(message.requestId())) {
            return false;
        }
        if (findByOrderSn(message.orderSn()) != null) {
            reservationService.succeed(message.requestId(), message.orderSn());
            return true;
        }
        if (releaseStock) {
            release(message);
        }
        reservationService.revertRequest(message.requestId(), message.memberId(), message.seckillSkuId(), message.quantity());
        reservationService.fail(message.requestId(), "抢购请求已放弃");
        return true;
    }

    private void failBusiness(SeckillOrderMessage message, boolean releaseStock, String reason) {
        if (releaseStock) {
            release(message);
        }
        reservationService.revertRequest(message.requestId(), message.memberId(), message.seckillSkuId(), message.quantity());
        reservationService.fail(message.requestId(), reason);
        log.info("秒杀下单失败，requestId={}，原因={}", message.requestId(), reason);
    }

    private void release(SeckillOrderMessage message) {
        try {
            SeckillSku seckill = adminService.requireSku(message.seckillSkuId());
            stockInnerClient.release(new StockReleaseRequest(message.orderSn(),
                    List.of(new StockItemDTO(seckill.getSkuId(), message.quantity())))).dataOrThrow();
        } catch (RuntimeException e) {
            log.error("秒杀失败后释放实物库存失败，orderSn={}", message.orderSn(), e);
        }
    }

    private SeckillOrder findByOrderSn(String orderSn) {
        return seckillOrderMapper.selectOne(Wrappers.<SeckillOrder>lambdaQuery().eq(SeckillOrder::getOrderSn, orderSn));
    }

    private SeckillOrder findActive(long seckillSkuId, long memberId) {
        return seckillOrderMapper.selectOne(Wrappers.<SeckillOrder>lambdaQuery()
                .eq(SeckillOrder::getSeckillSkuId, seckillSkuId)
                .eq(SeckillOrder::getActiveMember, memberId)
                .eq(SeckillOrder::getStatus, SeckillRecordStatus.ACTIVE));
    }
}
