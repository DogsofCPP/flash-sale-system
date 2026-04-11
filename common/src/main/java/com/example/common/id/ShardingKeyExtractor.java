package com.example.common.id;

/**
 * ShardingSphere-JDBC 分片键提取工具
 *
 * 配合 ShardingSphere 的 ShardingKeyGenerator 使用
 * 按用户ID后4位分库（4个库），按活动ID后2位分表（100张表）
 */
public class ShardingKeyExtractor {

    // ======== 按用户ID分库（用户服务、订单服务） ========

    /**
     * 根据用户ID计算分库序号（0~3，对应 ds_0 ~ ds_3）
     * 使用后4位取模，4个库
     */
    public static int getDatabaseShardingIndex(long userId) {
        return (int) (Math.abs(userId % 4));
    }

    /**
     * 根据用户ID获取分库名称
     */
    public static String getDatabaseName(long userId) {
        return "ds_" + getDatabaseShardingIndex(userId);
    }

    // ======== 按订单号分库（订单服务） ========

    /**
     * 根据订单号中的用户基因提取分库序号
     * 订单号第17~20位为用户基因
     */
    public static int getDatabaseShardingByOrderNo(String orderNo) {
        if (orderNo == null || orderNo.length() < 21) {
            return 0;
        }
        long userGene = Long.parseLong(orderNo.substring(17, 21));
        return (int) (userGene % 4);
    }

    // ======== 按活动ID分表（秒杀服务） ========

    /**
     * 根据活动ID计算分表序号（0~99，对应 t_order_0 ~ t_order_99）
     * 使用后2位取模，100张表
     */
    public static int getActivityTableShardingIndex(long activityId) {
        return (int) (Math.abs(activityId % 100));
    }

    /**
     * 根据用户ID和活动ID联合分表
     * activityId后2位决定表序号
     */
    public static int getSecKillTableShardingIndex(long activityId, long userId) {
        long combined = (activityId % 100) * 10 + (userId % 10);
        return (int) (combined % 100);
    }

    // ======== 订单号分表（订单表按年分表） ========

    /**
     * 根据订单号中的日期基因提取分表序号
     * 订单号第3~16位为日期时间（14位）
     */
    public static int getOrderTableShardingByOrderNo(String orderNo) {
        if (orderNo == null || orderNo.length() < 20) {
            return 0;
        }
        // 取年月日的最后两位作为分表依据
        // SK + 14位日期时间(yyyyMMddHHmmss) + ...
        // 例如: SK20260101150000...
        String dateStr = orderNo.substring(2, 10); // yyyyMMdd
        return (int) (Long.parseLong(dateStr) % 100);
    }

    // ======== 测试 ========

    public static void main(String[] args) {
        System.out.println("=== 分库测试（用户ID） ===");
        for (long userId : new long[]{1, 2, 3, 4, 5, 100, 999, 10000, 65535}) {
            System.out.printf("userId=%-6d → 数据库: %-4s (序号: %d)%n",
                    userId, getDatabaseName(userId), getDatabaseShardingIndex(userId));
        }

        System.out.println("\n=== 秒杀表分片（活动ID） ===");
        for (long activityId : new long[]{1, 2, 3, 10, 50, 99, 100, 101, 199}) {
            System.out.printf("activityId=%-4d → 表序号: %d%n",
                    activityId, getActivityTableShardingIndex(activityId));
        }

        System.out.println("\n=== 订单号基因提取 ===");
        String testOrderNo = "SK202601011500001200345678";
        System.out.println("订单号: " + testOrderNo);
        System.out.println("用户基因: " + extractUserGene(testOrderNo));
        System.out.println("数据库: " + getDatabaseShardingByOrderNo(testOrderNo));
        System.out.println("订单表: " + getOrderTableShardingByOrderNo(testOrderNo));
    }

    private static long extractUserGene(String orderNo) {
        if (orderNo == null || orderNo.length() < 21) {
            return -1;
        }
        return Long.parseLong(orderNo.substring(17, 21));
    }
}
