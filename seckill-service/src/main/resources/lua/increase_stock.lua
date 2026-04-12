local stock_key = KEYS[1]
local stock = redis.call('GET', stock_key)
if not stock then
    return -1  -- 库存不存在
end
return redis.call('INCRBY', stock_key, tonumber(ARGV[1]))
