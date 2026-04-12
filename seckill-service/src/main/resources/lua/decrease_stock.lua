local stock_key = KEYS[1]
local user_key = KEYS[2]
local quantity = tonumber(ARGV[1])
local limit = tonumber(ARGV[2])

-- 检查用户购买限制
local user_bought = redis.call('GET', user_key)
if user_bought and tonumber(user_bought) >= limit then
    return -1  -- 超限
end

-- 检查库存
local stock = redis.call('GET', stock_key)
if not stock then
    return -2  -- 库存不存在
end
if tonumber(stock) < quantity then
    return -3  -- 库存不足
end

-- 扣减库存
redis.call('DECRBY', stock_key, quantity)
redis.call('INCR', user_key)

return tonumber(stock) - quantity
