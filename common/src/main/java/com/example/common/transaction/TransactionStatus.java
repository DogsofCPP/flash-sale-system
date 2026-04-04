package com.example.common.transaction;

/**
 * 事务状态常量
 */
public class TransactionStatus {

    // 全局事务状态
    public static final int PENDING = 0;         // 待处理
    public static final int TRYING = 1;           // Try阶段
    public static final int CONFIRMED = 2;       // 已确认
    public static final int ROLLING_BACK = 3;     // 回滚中
    public static final int ROLLED_BACK = 4;      // 已回滚
    public static final int COMMITTED = 5;        // 已提交
    public static final int FAILED = 6;          // 失败

    // 分支事务状态
    public static final int BRANCH_PENDING = 0;
    public static final int BRANCH_SUCCESS = 1;
    public static final int BRANCH_FAILED = 2;

    // 消息事务状态
    public static final int MSG_PREPARED = 0;
    public static final int MSG_COMMITTED = 1;
    public static final int MSG_ROLLED_BACK = 2;
}