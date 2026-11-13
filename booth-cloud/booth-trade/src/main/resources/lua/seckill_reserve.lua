-- KEYS: stock, info, bought
-- ARGV: memberId, quantity, ttlSeconds
local stock = tonumber(redis.call('GET', KEYS[1]))
if not stock then
    return -1
end
local startAt = tonumber(redis.call('HGET', KEYS[2], 'start'))
local endAt = tonumber(redis.call('HGET', KEYS[2], 'end'))
local limit = tonumber(redis.call('HGET', KEYS[2], 'limit'))
if not startAt or not endAt or not limit then
    return -1
end
local nowParts = redis.call('TIME')
local now = tonumber(nowParts[1]) * 1000 + math.floor(tonumber(nowParts[2]) / 1000)
if now < startAt or now > endAt then
    return -2
end
local qty = tonumber(ARGV[2])
if not qty or qty < 1 then
    return -4
end
local bought = tonumber(redis.call('HGET', KEYS[3], ARGV[1])) or 0
if bought + qty > limit then
    return -3
end
if stock < qty then
    return -4
end
redis.call('DECRBY', KEYS[1], qty)
redis.call('HINCRBY', KEYS[3], ARGV[1], qty)
redis.call('EXPIRE', KEYS[1], ARGV[3])
redis.call('EXPIRE', KEYS[2], ARGV[3])
redis.call('EXPIRE', KEYS[3], ARGV[3])
return 1
