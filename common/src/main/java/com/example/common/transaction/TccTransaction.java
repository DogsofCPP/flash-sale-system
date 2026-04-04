package com.example.common.transaction;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * TCC事务协调器日志
 * 记录每个全局事务的完整执行轨迹
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TccTransaction implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String globalTxId;
    private String bizType;
    private String bizNo;
    private Long userId;

    /** 事务状态：0-PENDING 1-TRYPING 2-CONFIRMED 3-ROLLING_BACK 4-ROLLED_BACK 5-COMMITTED 6-FAILED */
    private int status;

    /** Try阶段结果 */
    private int tryResult;     // 0-pending 1-success 2-failed
    private String tryMsg;

    /** Confirm阶段结果 */
    private int confirmResult; // 0-pending 1-success 2-failed
    private String confirmMsg;

    /** Cancel阶段结果 */
    private int cancelResult;  // 0-pending 1-success 2-failed
    private String cancelMsg;

    /** 各分支事务状态（JSON字符串） */
    private String branchStatus;

    /** 活动ID（秒杀场景） */
    private Long activityId;

    /** 商品ID（秒杀场景） */
    private Long productId;

    /** 扣减数量（秒杀场景） */
    private Integer quantity;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;

    /** 完成时间 */
    private LocalDateTime finishedAt;

    /** 超时时间 */
    private LocalDateTime expireAt;

    /** 重试次数 */
    private int retryCount;

    public TccTransaction(String globalTxId, String bizType, String bizNo, Long userId) {
        this.globalTxId = globalTxId;
        this.bizType = bizType;
        this.bizNo = bizNo;
        this.userId = userId;
        this.status = 0;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.expireAt = LocalDateTime.now().plusMinutes(30);
        this.retryCount = 0;
    }

    public void startTry() {
        this.status = 1;
        this.updatedAt = LocalDateTime.now();
    }

    public void trySuccess() {
        this.tryResult = 1;
        this.updatedAt = LocalDateTime.now();
    }

    public void tryFailed(String msg) {
        this.tryResult = 2;
        this.tryMsg = msg;
        this.updatedAt = LocalDateTime.now();
    }

    public void startConfirm() {
        this.status = 2;
        this.updatedAt = LocalDateTime.now();
    }

    public void confirmSuccess() {
        this.confirmResult = 1;
        this.status = 5;
        this.finishedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void confirmFailed(String msg) {
        this.confirmResult = 2;
        this.confirmMsg = msg;
        this.status = 4;
        this.updatedAt = LocalDateTime.now();
    }

    public void startRollback() {
        this.status = 3;
        this.updatedAt = LocalDateTime.now();
    }

    public void rollbackSuccess() {
        this.cancelResult = 1;
        this.status = 4;
        this.finishedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void rollbackFailed(String msg) {
        this.cancelResult = 2;
        this.cancelMsg = msg;
        this.retryCount++;
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expireAt);
    }
}