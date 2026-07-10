redis.call('set', KEYS[1], ARGV[1])
redis.call('set', KEYS[2], ARGV[2])
redis.call('set', KEYS[3], ARGV[3])
return 1
