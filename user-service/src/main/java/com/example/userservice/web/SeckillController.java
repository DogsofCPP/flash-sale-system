package com.example.userservice.web;

import com.example.userservice.config.RateLimiterConfig;
import com.example.userservice.domain.Order;
import com.example.userservice.domain.SeckillActivity;
import com.example.userservice.domain.SeckillProduct;
import com.example.userservice.service.OrderService;
import com.example.userservice.service.SeckillService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/seckill")
public class SeckillController {

    @Autowired
    private SeckillService seckillService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private RateLimiterConfig rateLimiterConfig;

    @GetMapping("/activities")
    public ResponseEntity<List<SeckillActivity>> getAllActivities() {
        return ResponseEntity.ok(seckillService.getAllActivities());
    }

    @GetMapping("/activities/active")
    public ResponseEntity<List<SeckillActivity>> getActiveActivities() {
        return ResponseEntity.ok(seckillService.getActiveActivities());
    }

    @GetMapping("/activities/{id}")
    public ResponseEntity<SeckillActivity> getActivity(@PathVariable Long id) {
        return ResponseEntity.ok(seckillService.getActivity(id));
    }

    @GetMapping("/activities/{activityId}/products")
    public ResponseEntity<List<SeckillProduct>> getActivityProducts(@PathVariable Long activityId) {
        return ResponseEntity.ok(seckillService.getActivityProducts(activityId));
    }

    @PostMapping("/activities/{activityId}/products/{productId}/init-stock")
    public ResponseEntity<Map<String, Object>> initStock(@PathVariable Long activityId, @PathVariable Long productId) {
        seckillService.initSeckillStock(activityId, productId);
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "库存初始化成功");
        return ResponseEntity.ok(result);
    }

    @PostMapping("/activities/init-all-stock")
    public ResponseEntity<Map<String, Object>> initAllStock() {
        seckillService.initAllSeckillStock();
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "所有活动库存初始化成功");
        return ResponseEntity.ok(result);
    }

    @PostMapping("/order")
    public ResponseEntity<Map<String, Object>> seckill(
            @RequestParam Long userId,
            @RequestParam Long activityId,
            @RequestParam Long productId) {

        Map<String, Object> result = new HashMap<>();

        if (!rateLimiterConfig.tryAcquire("seckill:" + activityId, 1, 100, java.util.concurrent.TimeUnit.MILLISECONDS)) {
            result.put("success", false);
            result.put("message", "系统繁忙，请稍后重试");
            return ResponseEntity.ok(result);
        }

        try {
            String orderNo = seckillService.seckill(userId, activityId, productId);
            result.put("success", true);
            result.put("message", "秒杀成功");
            result.put("orderNo", orderNo);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }

        return ResponseEntity.ok(result);
    }

    @GetMapping("/order/{orderNo}")
    public ResponseEntity<Order> getOrder(@PathVariable String orderNo) {
        return ResponseEntity.ok(orderService.getOrderByOrderNo(orderNo));
    }

    @GetMapping("/orders/user/{userId}")
    public ResponseEntity<List<Order>> getUserOrders(@PathVariable Long userId) {
        return ResponseEntity.ok(orderService.getOrdersByUserId(userId));
    }

    @PostMapping("/order/{orderNo}/cancel")
    public ResponseEntity<Map<String, Object>> cancelOrder(@PathVariable String orderNo) {
        boolean success = orderService.cancelOrder(orderNo);
        Map<String, Object> result = new HashMap<>();
        result.put("success", success);
        result.put("message", success ? "订单已取消" : "取消失败");
        return ResponseEntity.ok(result);
    }
}
