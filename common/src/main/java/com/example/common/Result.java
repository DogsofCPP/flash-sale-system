package com.example.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Result<T> {

    /** 状态码 */
    private int code;

    /** 消息 */
    private String message;

    /** 数据 */
    private T data;

    // ===================== 成功 =====================

    public static <T> Result<T> success() {
        return new Result<>(200, "success", null);
    }

    public static <T> Result<T> success(T data) {
        return new Result<>(200, "success", data);
    }

    public static <T> Result<T> success(String message, T data) {
        return new Result<>(200, message, data);
    }

    // ===================== 失败 =====================

    public static <T> Result<T> error(int code, String message) {
        return new Result<>(code, message, null);
    }

    public static <T> Result<T> error(String message) {
        return new Result<>(500, message, null);
    }

    // ===================== 业务错误码 =====================

    public static <T> Result<T> paramError() {
        return new Result<>(400, "参数错误", null);
    }

    public static <T> Result<T> unauthorized() {
        return new Result<>(401, "未授权", null);
    }

    public static <T> Result<T> forbidden() {
        return new Result<>(403, "禁止访问", null);
    }

    public static <T> Result<T> notFound() {
        return new Result<>(404, "资源不存在", null);
    }

    public static <T> Result<T> rateLimit() {
        return new Result<>(429, "请求过于频繁", null);
    }

    // 业务错误码
    public static <T> Result<T> activityNotFound() {
        return new Result<>(1001, "活动不存在", null);
    }

    public static <T> Result<T> activityNotStarted() {
        return new Result<>(1002, "活动未开始", null);
    }

    public static <T> Result<T> activityEnded() {
        return new Result<>(1003, "活动已结束", null);
    }

    public static <T> Result<T> stockNotEnough() {
        return new Result<>(1004, "库存不足", null);
    }

    public static <T> Result<T> limitExceeded() {
        return new Result<>(1005, "购买数量超限", null);
    }

    public static <T> Result<T> duplicateOrder() {
        return new Result<>(1006, "重复购买", null);
    }

    public static <T> Result<T> orderCreateFailed() {
        return new Result<>(1007, "订单创建失败", null);
    }

    public static <T> Result<T> orderNotFound() {
        return new Result<>(1008, "订单不存在", null);
    }

    public static <T> Result<T> orderTimeout() {
        return new Result<>(1009, "订单已超时", null);
    }

    public static <T> Result<T> orderStatusError() {
        return new Result<>(1010, "订单状态异常", null);
    }
}
