package com.example.common.transaction;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 事务消息（用于可靠消息 + TCC模式）
 * 包含事务协调所需的所有信息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransactionMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 全局事务ID */
    private String globalTxId;

    /** 分支事务ID */
    private String branchTxId;

    /** 事务类型 */
    private String txType;

    /** 业务类型：SECKILL_ORDER, PAYMENT, STOCK_DEDUCT */
    private String bizType;

    /** 业务单号（如订单号） */
    private String bizNo;

    /** 用户ID */
    private Long userId;

    /** 活动ID */
    private Long activityId;

    /** 商品ID */
    private Long productId;

    /** 扣减数量 */
    private Integer quantity;

    /** 金额 */
    private BigDecimal amount;

    /** 事务状态：PREPARED, COMMITTED, ROLLED_BACK */
    private int status;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;

    /** 确认时间 */
    private LocalDateTime confirmedAt;

    /** 回滚时间 */
    private LocalDateTime rolledBackAt;

    /** 重试次数 */
    private int retryCount;

    /** 错误信息 */
    private String errorMsg;

    public TransactionMessage(String globalTxId, String bizType, String bizNo, Long userId) {
        this.globalTxId = globalTxId;
        this.branchTxId = java.util.UUID.randomUUID().toString().replace("-", "");
        this.bizType = bizType;
        this.bizNo = bizNo;
        this.userId = userId;
        this.status = 0; // PREPARED
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.retryCount = 0;
    }

    public void commit() {
        this.status = 1;
        this.confirmedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void rollback(String reason) {
        this.status = 2;
        this.rolledBackAt = LocalDateTime.now();
        this.errorMsg = reason;
        this.updatedAt = LocalDateTime.now();
    }

    public boolean canRetry() {
        return this.retryCount < 3;
    }
}