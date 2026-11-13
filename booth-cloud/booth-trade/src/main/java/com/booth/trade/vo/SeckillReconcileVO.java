package com.booth.trade.vo;

/**
 * redisSold、dbSold、dbReserved、openOrderQuantity 都按还占着库存的件数算。
 * 已关闭的秒杀单库存已经归还，不能再加进合计。dbSold 来自数据库的初始库存减剩余库存，不依赖 Redis。
 */
public record SeckillReconcileVO(long redisSold, long dbSold, long dbReserved, long openOrderQuantity, boolean matched) {
}
