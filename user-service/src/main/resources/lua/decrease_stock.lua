-- Lua 脚本：原子扣减库存
-- 返回值：剩余库存（>=0表示成功，-1表示库存不足，-2表示key不存在）

local key = KEYS[1]
local quantity = tonumber(ARGV[1])

-- 检查 key 是否存在
local exists = redis.call('EXISTS', key)
if exists == 0 then
    return -2
end

-- 获取当前库存
local stock = tonumber(redis.call('GET', key))
if stock == nil then
    return -2
end

-- 检查库存是否足够
if stock < quantity then
    return -1
end

-- 扣减库存
local newStock = stock - quantity
redis.call('SET', key, newStock)

return newStock
