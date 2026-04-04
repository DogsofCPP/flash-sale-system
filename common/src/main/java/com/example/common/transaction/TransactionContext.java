package com.example.common.transaction;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 分布式事务上下文
 * 在整个事务链路中传递，包含事务ID、分支事务信息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransactionContext implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 全局事务ID */
    private String globalTxId;

    /** 分支事务ID */
    private String branchTxId;

    /** 事务类型：STOCK_TCC, ORDER_TCC, PAYMENT */
    private String txType;

    /** 参与者服务名 */
    private String participant;

    /** 事务状态：TRYING, CONFIRMING, CANCELLING, COMMITTED, ROLLED_BACK */
    private String status;

    /** 业务数据（Try阶段保存，用于Confirm/Cancel） */
    private Map<String, Object> businessData;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;

    /** 重试次数 */
    private int retryCount;

    /** 最大重试次数 */
    private int maxRetries;

    public TransactionContext(String globalTxId, String txType, String participant) {
        this.globalTxId = globalTxId;
        this.branchTxId = java.util.UUID.randomUUID().toString();
        this.txType = txType;
        this.participant = participant;
        this.status = "TRYING";
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.retryCount = 0;
        this.maxRetries = 3;
    }

    public void markConfirming() {
        this.status = "CONFIRMING";
        this.updatedAt = LocalDateTime.now();
    }

    public void markCancelling() {
        this.status = "CANCELLING";
        this.updatedAt = LocalDateTime.now();
    }

    public void markCommitted() {
        this.status = "COMMITTED";
        this.updatedAt = LocalDateTime.now();
    }

    public void markRolledBack() {
        this.status = "ROLLED_BACK";
        this.updatedAt = LocalDateTime.now();
    }

    public boolean canRetry() {
        return retryCount < maxRetries;
    }

    public void incrementRetry() {
        this.retryCount++;
        this.updatedAt = LocalDateTime.now();
    }
}