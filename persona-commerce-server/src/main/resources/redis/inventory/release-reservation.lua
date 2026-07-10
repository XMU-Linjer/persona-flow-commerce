local quantity = redis.call('hget', KEYS[3], ARGV[1])
if not quantity then
    return 0
end

local available = redis.call('get', KEYS[1])
local locked = redis.call('get', KEYS[2])
if not available or not locked then
    return -2
end

quantity = tonumber(quantity)
if tonumber(locked) < quantity then
    return -3
end

redis.call('incrby', KEYS[1], quantity)
redis.call('decrby', KEYS[2], quantity)
redis.call('hdel', KEYS[3], ARGV[1])
return 1
