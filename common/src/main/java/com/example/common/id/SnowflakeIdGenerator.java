package com.example.common.id;

/**
 * 雪花算法 + 基因算法订单号生成器
 *
 * 订单号格式（共20位）：
 *   [符号位1位] [时间戳31位] [机器ID10位] [序列号12位]
 *
 * 特点：
 * - 时间戳有序，保证订单号递增
 * - 嵌入用户ID后4位（基因算法），便于按用户ID分表时定位库
 * - 嵌入活动ID后2位（基因片段），便于秒杀场景分析
 * - 不依赖数据库，性能高
 */
public class SnowflakeIdGenerator {

    /** 起始时间戳（2020-01-01 00:00:00） */
    private static final long EPOCH = 1577836800000L;

    /** 机器ID占用的bit数（10bit → 0~1023） */
    private static final long WORKER_ID_BITS = 10L;

    /** 序列号占用的bit数（12bit → 每毫秒最多4096个） */
    private static final long SEQUENCE_BITS = 12L;

    /** 最大机器ID（2^10 - 1） */
    private static final long MAX_WORKER_ID = ~(-1L << WORKER_ID_BITS);

    /** 最大序列号（2^12 - 1） */
    private static final long MAX_SEQUENCE = ~(-1L << SEQUENCE_BITS);

    /** 机器ID左移位数（12位给序列号） */
    private static final long WORKER_ID_SHIFT = SEQUENCE_BITS;

    /** 时间戳左移位数（12位给序列号 + 10位给机器ID） */
    private static final long TIMESTAMP_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS;

    private final long workerId;
    private long sequence = 0L;
    private long lastTimestamp = -1L;

    public SnowflakeIdGenerator(long workerId) {
        if (workerId < 0 || workerId > MAX_WORKER_ID) {
            throw new IllegalArgumentException(
                    "workerId 必须在 0~" + MAX_WORKER_ID + " 之间，当前: " + workerId);
        }
        this.workerId = workerId;
    }

    /**
     * 生成标准雪花ID（Long类型）
     */
    public synchronized long nextId() {
        long timestamp = currentTimeMillis();

        if (timestamp < lastTimestamp) {
            throw new RuntimeException("时钟回拨，拒绝生成ID。当前: " + timestamp + "，上次: " + lastTimestamp);
        }

        if (timestamp == lastTimestamp) {
            sequence = (sequence + 1) & MAX_SEQUENCE;
            if (sequence == 0) {
                timestamp = waitNextMillis(timestamp);
            }
        } else {
            sequence = 0L;
        }

        lastTimestamp = timestamp;

        return ((timestamp - EPOCH) << TIMESTAMP_SHIFT)
                | (workerId << WORKER_ID_SHIFT)
                | sequence;
    }

    /**
     * 生成字符串形式的订单号（嵌入用户ID基因）
     * 格式：SK + 时间戳(10位) + 机器ID(2位) + 用户ID基因(4位) + 序列号(4位)
     *
     * @param userId 用户ID
     * @return 订单号字符串
     */
    public synchronized String nextOrderNo(long userId) {
        long timestamp = currentTimeMillis();

        if (timestamp < lastTimestamp) {
            throw new RuntimeException("时钟回拨，拒绝生成订单号");
        }

        if (timestamp == lastTimestamp) {
            sequence = (sequence + 1) & MAX_SEQUENCE;
            if (sequence == 0) {
                timestamp = waitNextMillis(timestamp);
            }
        } else {
            sequence = 0L;
        }

        lastTimestamp = timestamp;

        // 当前时间戳（毫秒）后10位
        long timePart = (timestamp - EPOCH) % 10000000000L;

        // 机器ID后2位
        long workerPart = workerId % 100;

        // 用户ID后4位作为基因（用于分库路由）
        long userGene = userId % 10000;

        // 序列号后4位
        long seqPart = sequence % 10000;

        // 组装：SK + 时间10位 + 机器2位 + 用户基因4位 + 序列4位 = 20位
        StringBuilder sb = new StringBuilder("SK");
        sb.append(String.format("%010d", timePart));
        sb.append(String.format("%02d", workerPart));
        sb.append(String.format("%04d", userGene));
        sb.append(String.format("%04d", seqPart));

        return sb.toString();
    }

    /**
     * 生成带活动基因的订单号（秒杀专用）
     * 格式：SK + 年月日(8位) + 活动基因(3位) + 用户基因(4位) + 序列号(4位) = 20位
     *
     * @param userId     用户ID（用于分库路由）
     * @param activityId 活动ID（用于秒杀场景分析）
     * @return 订单号字符串
     */
    public synchronized String nextSeckillOrderNo(long userId, long activityId) {
        long timestamp = currentTimeMillis();

        if (timestamp < lastTimestamp) {
            throw new RuntimeException("时钟回拨，拒绝生成订单号");
        }

        if (timestamp == lastTimestamp) {
            sequence = (sequence + 1) & MAX_SEQUENCE;
            if (sequence == 0) {
                timestamp = waitNextMillis(timestamp);
            }
        } else {
            sequence = 0L;
        }

        lastTimestamp = timestamp;

        // 年月日时分秒（14位）
        java.time.LocalDateTime dt = java.time.LocalDateTime.now();
        long datePart = dt.getYear() * 10000000000L
                + dt.getMonthValue() * 100000000L
                + dt.getDayOfMonth() * 1000000L
                + dt.getHour() * 10000L
                + dt.getMinute() * 100L
                + dt.getSecond();

        // 活动ID后3位（分桶分析）
        long activityGene = activityId % 1000;

        // 用户ID后4位（分库路由基因）
        long userGene = userId % 10000;

        // 毫秒内序列号后3位
        long seqPart = sequence % 1000;

        StringBuilder sb = new StringBuilder("SK");
        sb.append(String.format("%14d", datePart));
        sb.append(String.format("%03d", activityGene));
        sb.append(String.format("%04d", userGene));
        sb.append(String.format("%03d", seqPart));

        return sb.toString();
    }

    /** 从订单号中提取用户ID基因 */
    public static long extractUserGene(String orderNo) {
        if (orderNo == null || orderNo.length() < 20 || !orderNo.startsWith("SK")) {
            return -1;
        }
        return Long.parseLong(orderNo.substring(17, 21));
    }

    /** 从订单号中提取活动ID基因 */
    public static long extractActivityGene(String orderNo) {
        if (orderNo == null || orderNo.length() < 24 || !orderNo.startsWith("SK")) {
            return -1;
        }
        return Long.parseLong(orderNo.substring(14, 17));
    }

    private long waitNextMillis(long timestamp) {
        long current = currentTimeMillis();
        while (current <= timestamp) {
            current = currentTimeMillis();
        }
        return current;
    }

    private long currentTimeMillis() {
        return System.currentTimeMillis();
    }

    /** 测试 */
    public static void main(String[] args) {
        SnowflakeIdGenerator gen = new SnowflakeIdGenerator(1);

        System.out.println("=== 雪花算法生成器测试 ===");
        System.out.println("标准雪花ID: " + gen.nextId());
        System.out.println("标准雪花ID: " + gen.nextId());

        System.out.println("\n=== 嵌入用户基因的订单号 ===");
        for (int i = 0; i < 5; i++) {
            System.out.println("userId=1:  " + gen.nextOrderNo(1));
            System.out.println("userId=99:  " + gen.nextOrderNo(99));
            System.out.println("userId=999: " + gen.nextOrderNo(999));
        }

        System.out.println("\n=== 秒杀专用订单号（嵌入活动基因+用户基因） ===");
        for (int i = 0; i < 3; i++) {
            System.out.println("userId=1234, activityId=5: " + gen.nextSeckillOrderNo(1234, 5));
            System.out.println("userId=1234, activityId=15: " + gen.nextSeckillOrderNo(1234, 15));
        }

        System.out.println("\n=== 基因提取测试 ===");
        String order = gen.nextSeckillOrderNo(12345, 7);
        System.out.println("订单号: " + order);
        System.out.println("提取用户基因: " + extractUserGene(order) + " (应为 2345)");
        System.out.println("提取活动基因: " + extractActivityGene(order) + " (应为 7)");
    }
}
