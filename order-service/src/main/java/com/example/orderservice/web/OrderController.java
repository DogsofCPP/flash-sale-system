package com.example.orderservice.web;

import com.example.common.Result;
import com.example.orderservice.domain.Order;
import com.example.orderservice.service.OrderService;
import com.example.orderservice.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private PaymentService paymentService;

    // ======== 查询订单 ========

    @GetMapping("/{orderNo}")
    public Result<Order> getOrder(@PathVariable String orderNo) {
        Order order = orderService.getOrderByOrderNo(orderNo);
        if (order == null) {
            return Result.orderNotFound();
        }
        return Result.success(order);
    }

    @GetMapping
    public Result<List<Order>> list(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Integer status) {
        List<Order> orders;
        if (userId != null && status != null) {
            orders = orderService.getOrdersByUserIdAndStatus(userId, status);
        } else if (userId != null) {
            orders = orderService.getOrdersByUserId(userId);
        } else {
            return Result.error(400, "userId参数不能为空");
        }
        return Result.success(orders);
    }

    // ======== 取消订单 ========

    @PostMapping("/{orderNo}/cancel")
    public Result<Void> cancel(
            @PathVariable String orderNo,
            @RequestParam(required = false) Long userId) {
        boolean ok = paymentService.cancelOrder(orderNo, userId);
        if (ok) {
            return Result.success("订单已取消", null);
        }
        return Result.orderStatusError();
    }

    // ======== 订单支付 ========

    /**
     * 支付订单（模拟）
     *
     * @param orderNo  订单号
     * @param payToken 支付令牌（模拟）
     * @return 支付结果
     */
    @PostMapping("/{orderNo}/pay")
    public Result<String> pay(
            @PathVariable String orderNo,
            @RequestParam(required = false) String payToken) {

        if (payToken == null || payToken.trim().isEmpty()) {
            return Result.error(400, "支付令牌不能为空");
        }

        boolean ok = paymentService.payOrder(orderNo, payToken);
        if (ok) {
            Order order = paymentService.getOrderByOrderNo(orderNo);
            return Result.success(order);
        }
        return Result.error(500, "支付失败，订单可能已超时或状态不正确");
    }

    // ======== 订单退款 ========

    /**
     * 退款订单
     *
     * @param orderNo 订单号
     * @param reason  退款原因
     * @return 退款结果
     */
    @PostMapping("/{orderNo}/refund")
    public Result<Void> refund(
            @PathVariable String orderNo,
            @RequestParam(required = false) String reason) {
        if (reason == null) reason = "用户申请退款";
        boolean ok = paymentService.refundOrder(orderNo, reason);
        if (ok) {
            return Result.success("退款申请已提交", null);
        }
        return Result.orderStatusError();
    }

    // ======== 支付结果查询 ========

    /**
     * 查询支付结果（轮询接口）
     */
    @GetMapping("/{orderNo}/status")
    public Result<String> getPayStatus(@PathVariable String orderNo) {
        Order order = paymentService.getOrderByOrderNo(orderNo);
        if (order == null) {
            return Result.orderNotFound();
        }
        String statusDesc = switch (order.getStatus()) {
            case 0 -> "待支付";
            case 1 -> "已支付";
            case 2 -> "已取消";
            case 3 -> "已退款";
            case 4 -> "已超时";
            default -> "未知状态";
        };
        return Result.success(statusDesc);
    }

    // ======== 用户订单列表（支付服务） ========

    @GetMapping("/user/{userId}")
    public Result<List<Order>> userOrders(@PathVariable Long userId) {
        List<Order> orders = paymentService.getUserOrders(userId);
        return Result.success(orders);
    }

    @GetMapping("/pending")
    public Result<List<Order>> pendingOrders() {
        List<Order> orders = paymentService.getPendingOrders();
        return Result.success(orders);
    }
}
