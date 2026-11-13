package com.booth.trade.support;

import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SeckillScripts {

    private final StringRedisTemplate redis;
    private final DefaultRedisScript<Long> reserveScript;
    private final DefaultRedisScript<Long> revertScript;

    public SeckillScripts(StringRedisTemplate redis) {
        this.redis = redis;
        this.reserveScript = script("lua/seckill_reserve.lua");
        this.revertScript = script("lua/seckill_revert.lua");
    }

    /** 1 成功，-1 未预热，-2 不在场次内，-3 超过限购，-4 库存不足 */
    public long reserve(long seckillSkuId, long memberId, int quantity, long ttlSeconds) {
        Long code = redis.execute(reserveScript, List.of(
                SeckillKeys.stock(seckillSkuId), SeckillKeys.info(seckillSkuId), SeckillKeys.bought(seckillSkuId)),
                String.valueOf(memberId), String.valueOf(quantity), String.valueOf(Math.max(ttlSeconds, 60)));
        return code == null ? -1 : code;
    }

    /** 返回 false 表示这个 token 已经归还过。 */
    public boolean revert(long seckillSkuId, long memberId, int quantity, String token, long ttlSeconds) {
        Long code = redis.execute(revertScript, List.of(
                SeckillKeys.stock(seckillSkuId), SeckillKeys.bought(seckillSkuId), SeckillKeys.revert(token)),
                String.valueOf(memberId), String.valueOf(quantity), String.valueOf(Math.max(ttlSeconds, 60)));
        return code != null && code == 1;
    }

    private static DefaultRedisScript<Long> script(String path) {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setLocation(new ClassPathResource(path));
        script.setResultType(Long.class);
        return script;
    }
}
