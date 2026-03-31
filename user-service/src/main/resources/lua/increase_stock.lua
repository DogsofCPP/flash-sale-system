-- Lua 脚本：原子增加库存（用于回滚）
-- 返回值：增加后的库存

local key = KEYS[1]
local quantity = tonumber(ARGV[1])

-- 检查 key 是否存在
local exists = redis.call('EXISTS', key)
if exists == 0 then
    return -1
end

-- 获取当前库存并增加
local current = tonumber(redis.call('GET', key))
if current == nil then
    return -1
end

local newStock = current + quantity
redis.call('SET', key, newStock)

return newStock
