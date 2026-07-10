local reserved = redis.call('hget', KEYS[3], ARGV[1])
if reserved then
    if tonumber(reserved) == tonumber(ARGV[2]) then
        return 2
    end
    return -4
end

local available = redis.call('get', KEYS[1])
local locked = redis.call('get', KEYS[2])
if not available or not locked then
    return -2
end

local quantity = tonumber(ARGV[2])
if tonumber(available) < quantity then
    return -1
end

redis.call('decrby', KEYS[1], quantity)
redis.call('incrby', KEYS[2], quantity)
redis.call('hset', KEYS[3], ARGV[1], quantity)
redis.call('hsetnx', KEYS[3], '__createdAt', ARGV[3])
return 1
