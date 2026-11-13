package com.booth.trade.service;

import com.booth.common.core.exception.BizException;
import com.booth.trade.error.TradeErrorCode;
import com.booth.trade.support.SeckillKeys;
import com.booth.trade.support.SeckillScripts;
import com.booth.trade.vo.SeckillResultVO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Redis 里的预占、放弃标记和归还。归还按 token 只生效一次。
 * 确认超时不能归还：消息可能已经进了队列，这时加回库存会和消费者各算各的。
 */
@Service
@RequiredArgsConstructor
public class SeckillReservationService {

    static final String QUEUED = "QUEUED";
    static final String SUCCESS = "SUCCESS";
    static final String FAILED = "FAILED";

    private final StringRedisTemplate redis;
    private final SeckillScripts scripts;

    public long ttlSeconds(long seckillSkuId) {
        Long ttl = redis.getExpire(SeckillKeys.stock(seckillSkuId));
        if (ttl == null || ttl < 60) {
            return Duration.ofDays(2).toSeconds();
        }
        return ttl;
    }

    public long reserve(long seckillSkuId, long memberId, int quantity) {
        return scripts.reserve(seckillSkuId, memberId, quantity, ttlSeconds(seckillSkuId));
    }

    public void queue(String requestId, long memberId, long seckillSkuId, int quantity, String orderSn) {
        write(requestId, memberId, seckillSkuId, quantity, QUEUED, orderSn, null);
    }

    public void succeed(String requestId, String orderSn) {
        String key = SeckillKeys.result(requestId);
        redis.opsForHash().put(key, "status", SUCCESS);
        redis.opsForHash().put(key, "orderSn", orderSn);
        redis.opsForHash().delete(key, "reason");
    }

    public void fail(String requestId, String reason) {
        String key = SeckillKeys.result(requestId);
        redis.opsForHash().put(key, "status", FAILED);
        if (reason != null) {
            redis.opsForHash().put(key, "reason", reason);
        }
    }

    public boolean aborted(String requestId) {
        return Boolean.TRUE.equals(redis.hasKey(SeckillKeys.abort(requestId)));
    }

    public void holdPlacing(String requestId) {
        redis.opsForValue().set(SeckillKeys.placing(requestId), "1", Duration.ofSeconds(30));
    }

    public boolean placing(String requestId) {
        return Boolean.TRUE.equals(redis.hasKey(SeckillKeys.placing(requestId)));
    }

    public void endPlacing(String requestId) {
        redis.delete(SeckillKeys.placing(requestId));
    }

    /** 先写放弃标记，再归还。晚到的消费者看到标记就不再下单。 */
    public void abortAndRevert(String requestId, long memberId, long seckillSkuId, int quantity) {
        long ttl = ttlSeconds(seckillSkuId);
        redis.opsForValue().setIfAbsent(SeckillKeys.abort(requestId), "1", Duration.ofSeconds(ttl));
        scripts.revert(seckillSkuId, memberId, quantity, requestId, ttl);
        fail(requestId, "抢购请求已放弃");
    }

    public void revertRequest(String requestId, long memberId, long seckillSkuId, int quantity) {
        scripts.revert(seckillSkuId, memberId, quantity, requestId, ttlSeconds(seckillSkuId));
    }

    public void revertClosed(String orderSn, long memberId, long seckillSkuId, int quantity) {
        scripts.revert(seckillSkuId, memberId, quantity, "close:" + orderSn, ttlSeconds(seckillSkuId));
    }

    public SeckillResultVO result(Long memberId, String requestId) {
        Map<Object, Object> row = redis.opsForHash().entries(SeckillKeys.result(requestId));
        BizException.throwIf(row.isEmpty() || !String.valueOf(memberId).equals(String.valueOf(row.get("memberId"))),
                TradeErrorCode.SECKILL_NOT_FOUND);
        return new SeckillResultVO(string(row.get("status")), string(row.get("orderSn")), string(row.get("reason")));
    }

    public String status(String requestId) {
        Object value = redis.opsForHash().get(SeckillKeys.result(requestId), "status");
        return value == null ? null : value.toString();
    }

    private void write(String requestId, long memberId, long seckillSkuId, int quantity, String status, String orderSn, String reason) {
        Map<String, String> fields = new HashMap<>();
        fields.put("status", status);
        fields.put("memberId", String.valueOf(memberId));
        fields.put("seckillSkuId", String.valueOf(seckillSkuId));
        fields.put("quantity", String.valueOf(quantity));
        fields.put("orderSn", orderSn);
        if (reason != null) {
            fields.put("reason", reason);
        }
        String key = SeckillKeys.result(requestId);
        redis.opsForHash().putAll(key, fields);
        redis.expire(key, Duration.ofSeconds(ttlSeconds(seckillSkuId)));
    }

    private static String string(Object value) {
        return value == null ? null : value.toString();
    }
}
