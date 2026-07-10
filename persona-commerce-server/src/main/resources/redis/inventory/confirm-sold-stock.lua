local locked = redis.call('get', KEYS[1])
local sold = redis.call('get', KEYS[2])
if not locked or not sold then
    return -2
end

local quantity = tonumber(ARGV[1])
if tonumber(locked) < quantity then
    return -3
end

redis.call('decrby', KEYS[1], quantity)
redis.call('incrby', KEYS[2], quantity)
return 1
