local available = redis.call('get', KEYS[1])
local locked = redis.call('get', KEYS[2])
if not available or not locked then
    return -2
end

local quantity = tonumber(ARGV[1])
if tonumber(locked) < quantity then
    return -3
end

redis.call('incrby', KEYS[1], quantity)
redis.call('decrby', KEYS[2], quantity)
return 1
