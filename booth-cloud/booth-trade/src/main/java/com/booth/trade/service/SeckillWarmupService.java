package com.booth.trade.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.booth.trade.entity.SeckillActivity;
import com.booth.trade.entity.SeckillSession;
import com.booth.trade.entity.SeckillSku;
import com.booth.trade.enums.ReleaseStatus;
import com.booth.trade.mapper.SeckillActivityMapper;
import com.booth.trade.mapper.SeckillSessionMapper;
import com.booth.trade.mapper.SeckillSkuMapper;
import com.booth.trade.support.SeckillKeys;
import com.booth.trade.support.SeckillWindow;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

/**
 * 库存用 SET NX 写入。已经预热过的 key 只刷新价格、限购和场次时间，不覆盖剩余库存。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SeckillWarmupService {

    private final SeckillActivityMapper activityMapper;
    private final SeckillSessionMapper sessionMapper;
    private final SeckillSkuMapper skuMapper;
    private final StringRedisTemplate redis;

    public void warmupDue() {
        List<SeckillActivity> activities = activityMapper.selectList(Wrappers.<SeckillActivity>lambdaQuery()
                .eq(SeckillActivity::getStatus, ReleaseStatus.RELEASED));
        for (SeckillActivity activity : activities) {
            List<SeckillSku> skus = skuMapper.selectList(Wrappers.<SeckillSku>lambdaQuery()
                    .eq(SeckillSku::getActivityId, activity.getId()));
            for (SeckillSku sku : skus) {
                SeckillSession session = sessionMapper.selectById(sku.getSessionId());
                if (session != null && SeckillWindow.warmupDue(activity, session)) {
                    warm(activity, session, sku);
                }
            }
        }
    }

    private void warm(SeckillActivity activity, SeckillSession session, SeckillSku sku) {
        Duration ttl = SeckillWindow.ttl(activity, session);
        String stockKey = SeckillKeys.stock(sku.getId());
        Boolean created = redis.opsForValue().setIfAbsent(stockKey, String.valueOf(sku.getSeckillStock()), ttl);
        String infoKey = SeckillKeys.info(sku.getId());
        if (Boolean.TRUE.equals(created)) {
            redis.opsForHash().putIfAbsent(infoKey, "initial", String.valueOf(sku.getSeckillStock()));
            log.info("预热秒杀库存，seckillSkuId={}，库存={}", sku.getId(), sku.getSeckillStock());
        }
        redis.opsForHash().put(infoKey, "price", sku.getSeckillPrice().toPlainString());
        redis.opsForHash().put(infoKey, "limit", String.valueOf(sku.getPerLimit()));
        redis.opsForHash().put(infoKey, "skuId", String.valueOf(sku.getSkuId()));
        redis.opsForHash().put(infoKey, "start", String.valueOf(SeckillWindow.startMillis(session.getStartTime())));
        redis.opsForHash().put(infoKey, "end", String.valueOf(SeckillWindow.startMillis(session.getEndTime())));
        redis.expire(infoKey, ttl);
        redis.expire(stockKey, ttl);
        String boughtKey = SeckillKeys.bought(sku.getId());
        redis.opsForHash().putIfAbsent(boughtKey, "_", "0");
        redis.expire(boughtKey, ttl);
    }
}
