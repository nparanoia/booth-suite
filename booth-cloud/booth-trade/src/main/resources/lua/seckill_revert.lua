-- 同一个 token 只归还一次。关单重试、消息重放都不能把库存加两次。
-- KEYS: stock, bought, revertToken
-- ARGV: memberId, quantity, ttlSeconds
local ok = redis.call('SET', KEYS[3], '1', 'NX', 'EX', ARGV[3])
if not ok then
    return 0
end
redis.call('INCRBY', KEYS[1], ARGV[2])
local left = redis.call('HINCRBY', KEYS[2], ARGV[1], -tonumber(ARGV[2]))
if left < 0 then
    redis.call('HSET', KEYS[2], ARGV[1], '0')
end
redis.call('EXPIRE', KEYS[1], ARGV[3])
redis.call('EXPIRE', KEYS[2], ARGV[3])
return 1
