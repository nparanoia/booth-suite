package com.booth.trade.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.booth.trade.entity.SeckillOrder;
import com.booth.trade.enums.SeckillRecordStatus;
import com.booth.trade.mapper.SeckillOrderMapper;
import com.booth.trade.mapper.SeckillSkuMapper;
import com.booth.trade.entity.SeckillSku;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 关单后归还活动库存。数据库用状态 CAS，Redis 用关闭令牌，两边重试都不会加两次。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SeckillRestoreService {

    private final SeckillOrderMapper seckillOrderMapper;
    private final SeckillSkuMapper seckillSkuMapper;
    private final SeckillReservationService reservationService;
    private final TransactionTemplate transactionTemplate;

    public void restore(String orderSn) {
        SeckillOrder row = transactionTemplate.execute(status -> close(orderSn));
        if (row == null) {
            return;
        }
        reservationService.revertClosed(orderSn, row.getMemberId(), row.getSeckillSkuId(), row.getQuantity());
    }

    private SeckillOrder close(String orderSn) {
        SeckillOrder row = seckillOrderMapper.selectOne(Wrappers.<SeckillOrder>lambdaQuery()
                .eq(SeckillOrder::getOrderSn, orderSn));
        if (row == null) {
            return null;
        }
        int closed = seckillOrderMapper.update(null, Wrappers.<SeckillOrder>lambdaUpdate()
                .set(SeckillOrder::getStatus, SeckillRecordStatus.CLOSED)
                .setSql("active_member = null")
                .eq(SeckillOrder::getId, row.getId())
                .eq(SeckillOrder::getStatus, SeckillRecordStatus.ACTIVE));
        if (closed == 1) {
            seckillSkuMapper.update(null, Wrappers.<SeckillSku>lambdaUpdate()
                    .setSql("seckill_stock = seckill_stock + " + row.getQuantity())
                    .eq(SeckillSku::getId, row.getSeckillSkuId()));
            log.info("归还秒杀活动库存，orderSn={}，件数={}", orderSn, row.getQuantity());
        }
        return row;
    }
}
