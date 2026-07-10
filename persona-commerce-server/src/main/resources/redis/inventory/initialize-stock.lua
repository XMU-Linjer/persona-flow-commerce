if redis.call('exists', KEYS[1]) == 1
        and redis.call('exists', KEYS[2]) == 1
        and redis.call('exists', KEYS[3]) == 1 then
    return 0
end

redis.call('set', KEYS[1], ARGV[1])
redis.call('set', KEYS[2], ARGV[2])
redis.call('set', KEYS[3], ARGV[3])
return 1
