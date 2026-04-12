-- ======================================================
-- 秒杀商品数据初始化脚本
-- 执行前请确保数据库已初始化完成
-- ======================================================

USE flash_sale_product;

-- 批量插入秒杀商品
INSERT INTO t_product (name, description, price, stock, status) VALUES
('iPhone 15 Pro Max 256GB', 'Apple iPhone 15 Pro Max 256GB 深空黑钛金属 A17 Pro芯片', 9999.00, 50, 1),
('MacBook Pro M3 14英寸', 'MacBook Pro 14英寸 M3芯片 16GB+512GB 深空灰', 15999.00, 20, 1),
('AirPods Pro 2 主动降噪', 'AirPods Pro 2代 支持主动降噪 自适应音频', 1999.00, 100, 1),
('Apple Watch Series 9', 'Apple Watch Series 9 45mm GPS版 铝金属表壳', 2999.00, 30, 1),
('iPad Pro 12.9英寸 M2', 'iPad Pro 12.9英寸 M2芯片 256GB WiFi版', 10999.00, 25, 1),
('Sony WH-1000XM5 降噪耳机', '索尼 WH-1000XM5 头戴式无线降噪耳机', 2499.00, 80, 1),
('Nintendo Switch OLED', 'Nintendo Switch OLED版 白色主机 配原装底座', 2599.00, 40, 1),
('DJI Mini 3 Pro 无人机', '大疆 DJI Mini 3 Pro 带屏遥控器版 畅飞套装', 5999.00, 15, 1),
('戴森 V15 吸尘器', '戴森 V15 Detect Absolute 无绳吸尘器', 5499.00, 35, 1),
('小米 14 Ultra 影像旗舰', '小米 14 Ultra 16GB+512GB 徕卡影像', 6999.00, 60, 1),
('GoPro Hero 12 Black', 'GoPro Hero 12 Black 运动相机 防水防抖', 3699.00, 45, 1),
('飞利浦电动牙刷旗舰款', '飞利浦 Sonicare Diamond Clean Smart 钻石亮白智能牙刷', 1099.00, 120, 1);

-- 查看插入结果
SELECT id, name, price, stock, status FROM t_product ORDER BY id DESC LIMIT 15;
