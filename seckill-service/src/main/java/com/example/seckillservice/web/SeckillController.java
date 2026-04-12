package com.example.seckillservice.web;

import com.example.common.Result;
import com.example.common.SeckillRequest;
import com.example.common.SeckillResponse;
import com.example.seckillservice.config.RateLimiterConfig;
import com.example.seckillservice.domain.*;
import com.example.seckillservice.service.SeckillService;
import com.example.seckillservice.transaction.TransactionalSeckillService;
import com.example.seckillservice.transaction.TccTransactionCoordinator;
import com.example.common.transaction.TccTransaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/seckill")
public class SeckillController {

    @Autowired
    private SeckillService seckillService;

    @Autowired
    private TransactionalSeckillService transactionalSeckillService;

    @Autowired
    private TccTransactionCoordinator tccCoordinator;

    @Autowired
    private RateLimiterConfig rateLimiterConfig;

    // ========== 活动管理 ==========

    @GetMapping("/activities")
    public Result<List<SeckillActivity>> getAllActivities() {
        return Result.success(seckillService.getAllActivities());
    }

    @GetMapping("/activities/active")
    public Result<List<SeckillActivity>> getActiveActivities() {
        return Result.success(seckillService.getActiveActivities());
    }

    @GetMapping("/activities/{id}")
    public Result<SeckillActivity> getActivity(@PathVariable("id") Long id) {
        SeckillActivity activity = seckillService.getActivity(id);
        if (activity == null) {
            return Result.activityNotFound();
        }
        return Result.success(activity);
    }

    @GetMapping("/activities/{id}/products")
    public Result<List<SeckillProduct>> getActivityProducts(@PathVariable("id") Long id) {
        return Result.success(seckillService.getActivityProducts(id));
    }

    // ========== 库存管理 ==========

    @PostMapping("/activities/{activityId}/products/{productId}/init-stock")
    public Result<Void> initStock(@PathVariable("activityId") Long activityId, @PathVariable("productId") Long productId) {
        seckillService.initSeckillStock(activityId, productId);
        return Result.success("库存初始化成功", null);
    }

    @PostMapping("/activities/init-all-stock")
    public Result<Void> initAllStock() {
        seckillService.initAllSeckillStock();
        return Result.success("所有活动库存初始化成功", null);
    }

    // ========== 秒杀下单（普通模式）==========

    @PostMapping("/seckill")
    public Result<SeckillResponse> seckill(@RequestBody SeckillRequest request) {

        // 限流
        if (!rateLimiterConfig.tryAcquire("seckill:" + request.getActivityId(), 1, 100,
                java.util.concurrent.TimeUnit.MILLISECONDS)) {
            return Result.rateLimit();
        }

        try {
            SeckillResponse response = seckillService.seckill(request.getUserId(), request);
            return Result.success("秒杀成功", response);
        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        } catch (Exception e) {
            return Result.orderCreateFailed();
        }
    }

    // ========== 秒杀下单（分布式事务模式）==========

    /**
     * 基于TCC分布式事务的秒杀下单
     *
     * 使用TCC模式保障数据一致性：
     * - Try: Redis预扣库存
     * - Confirm: 发送Kafka消息创建订单
     * - Cancel: 回滚Redis库存
     */
    @PostMapping("/seckill/transactional")
    public Result<SeckillResponse> seckillWithTransaction(@RequestBody SeckillRequest request) {

        // 限流
        if (!rateLimiterConfig.tryAcquire("tx_seckill:" + request.getActivityId(), 1, 100,
                java.util.concurrent.TimeUnit.MILLISECONDS)) {
            return Result.rateLimit();
        }

        try {
            SeckillResponse response = transactionalSeckillService.seckillWithTransaction(request.getUserId(), request);
            return Result.success("秒杀成功（分布式事务）", response);
        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        } catch (Exception e) {
            return Result.orderCreateFailed();
        }
    }

    // ========== 秒杀订单 ==========

    @GetMapping("/orders/user/{userId}")
    public Result<List<SeckillOrder>> getUserOrders(@PathVariable("userId") Long userId) {
        return Result.success(seckillService.getUserOrders(userId));
    }

    @GetMapping("/orders/user/{userId}/transactional")
    public Result<List<SeckillOrder>> getUserOrdersTransactional(@PathVariable("userId") Long userId) {
        return Result.success(transactionalSeckillService.getUserOrders(userId));
    }

    // ========== TCC事务查询（运维/调试用）==========

    @GetMapping("/transaction/{globalTxId}")
    public Result<TccTransaction> getTransaction(@PathVariable("globalTxId") String globalTxId) {
        TccTransaction tx = tccCoordinator.getTransaction(globalTxId);
        if (tx == null) {
            return Result.error(404, "事务不存在或已过期");
        }
        return Result.success(tx);
    }

    @PostMapping("/transaction/{globalTxId}/cancel")
    public Result<Void> cancelTransaction(
            @PathVariable("globalTxId") String globalTxId,
            @RequestParam(required = false) String reason) {
        if (reason == null) reason = "用户主动取消";
        boolean ok = tccCoordinator.cancelPhase(globalTxId, reason);
        if (ok) {
            return Result.success("事务已取消", null);
        }
        return Result.error(500, "取消失败，事务可能已结束");
    }

    @GetMapping("/transaction/{globalTxId}/status")
    public Result<String> getTransactionStatus(@PathVariable("globalTxId") String globalTxId) {
        TccTransaction tx = tccCoordinator.getTransaction(globalTxId);
        if (tx == null) {
            return Result.success("NOT_FOUND");
        }
        return Result.success(tx.getStatus() + "");
    }
}