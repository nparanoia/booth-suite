package com.booth.trade.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.booth.trade.entity.SeckillOrder;
import com.booth.trade.entity.SeckillSku;
import com.booth.trade.entity.TradeOrder;
import com.booth.trade.enums.OrderStatus;
import com.booth.trade.enums.SeckillRecordStatus;
import com.booth.trade.mapper.SeckillOrderMapper;
import com.booth.trade.mapper.SeckillSkuMapper;
import com.booth.trade.support.SeckillKeys;
import com.booth.trade.support.TradeMetrics;
import com.booth.trade.vo.SeckillReconcileVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SeckillReconcileService {

    private final SeckillOrderMapper seckillOrderMapper;
    private final SeckillSkuMapper seckillSkuMapper;
    private final OrderLifecycleService orderLifecycleService;
    private final StringRedisTemplate redis;
    private final TradeMetrics tradeMetrics;

    public SeckillReconcileVO reconcile(Long seckillSkuId) {
        SeckillSku sku = seckillSkuMapper.selectById(seckillSkuId);
        long dbSold = sku == null ? 0 : (long) sku.getSeckillStockInitial() - sku.getSeckillStock();
        List<SeckillOrder> active = seckillOrderMapper.selectList(Wrappers.<SeckillOrder>lambdaQuery()
                .eq(SeckillOrder::getSeckillSkuId, seckillSkuId)
                .eq(SeckillOrder::getStatus, SeckillRecordStatus.ACTIVE));
        long dbReserved = active.stream().mapToLong(SeckillOrder::getQuantity).sum();
        long openOrderQuantity = 0;
        long closing = 0;
        for (SeckillOrder record : active) {
            TradeOrder order = orderLifecycleService.find(record.getOrderSn());
            if (order != null && order.getStatus() == OrderStatus.CLOSED) {
                closing += record.getQuantity();
            } else if (order != null) {
                openOrderQuantity += record.getQuantity();
            }
        }
        String initial = hash(seckillSkuId, "initial");
        String remain = redis.opsForValue().get(SeckillKeys.stock(seckillSkuId));
        boolean warmed = initial != null && remain != null;
        long redisSold = warmed ? Long.parseLong(initial) - Long.parseLong(remain) : 0;
        boolean stockMatched = warmed ? redisSold == dbSold && dbSold == dbReserved : dbSold == dbReserved;
        boolean matched = stockMatched && openOrderQuantity == dbReserved;
        boolean settling = stockMatched && closing > 0 && dbReserved - openOrderQuantity == closing;
        if (settling) {
            log.info("秒杀关单正在归还，seckillSkuId={}，尚未归还={}", seckillSkuId, closing);
        } else if (!matched) {
            tradeMetrics.reconcileDiff();
            log.error("秒杀对账不一致，seckillSkuId={}，Redis已售={}，数据库已售={}，有效秒杀单={}，未关闭订单={}",
                    seckillSkuId, redisSold, dbSold, dbReserved, openOrderQuantity);
        }
        return new SeckillReconcileVO(redisSold, dbSold, dbReserved, openOrderQuantity, matched);
    }

    private String hash(Long seckillSkuId, String field) {
        Object value = redis.opsForHash().get(SeckillKeys.info(seckillSkuId), field);
        return value == null ? null : value.toString();
    }
}
