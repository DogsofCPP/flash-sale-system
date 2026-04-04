package com.example.orderservice.mapper;

import com.example.orderservice.domain.Order;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface OrderMapper {

    @Select("SELECT * FROM t_order WHERE id = #{id}")
    Order findById(Long id);

    @Select("SELECT * FROM t_order WHERE order_no = #{orderNo}")
    Order findByOrderNo(String orderNo);

    @Select("SELECT * FROM t_order WHERE user_id = #{userId}")
    List<Order> findByUserId(Long userId);

    @Select("SELECT * FROM t_order WHERE user_id = #{userId} AND status = #{status}")
    List<Order> findByUserIdAndStatus(Long userId, Integer status);

    @Select("SELECT * FROM t_order WHERE status = #{status} AND expire_time < NOW()")
    List<Order> findExpiredOrders(Integer status);

    @Insert("INSERT INTO t_order(order_no, user_id, product_id, activity_id, product_name, " +
            "product_price, quantity, total_amount, status, expire_time, created_at) " +
            "VALUES(#{orderNo}, #{userId}, #{productId}, #{activityId}, #{productName}, " +
            "#{productPrice}, #{quantity}, #{totalAmount}, #{status}, #{expireTime}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Order order);

    @Update("UPDATE t_order SET status=#{status}, updated_at=NOW() WHERE id=#{id}")
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);

    @Update("UPDATE t_order SET status=2, cancel_time=NOW(), updated_at=NOW() WHERE order_no=#{orderNo} AND status=0")
    int cancelOrder(@Param("orderNo") String orderNo);

    /** 支付成功：状态0→1 */
    @Update("UPDATE t_order SET status=1, pay_time=NOW(), updated_at=NOW() WHERE order_no=#{orderNo} AND status=0")
    int payOrder(@Param("orderNo") String orderNo);

    /** 退款成功：状态1→3 */
    @Update("UPDATE t_order SET status=3, updated_at=NOW() WHERE order_no=#{orderNo} AND status=1")
    int refundOrder(@Param("orderNo") String orderNo, @Param("reason") String reason);

    /** 标记超时 */
    @Update("UPDATE t_order SET status=4, updated_at=NOW() WHERE order_no=#{orderNo} AND status=0")
    int markExpired(@Param("orderNo") String orderNo);

    /** 查询待支付订单（用于定时扫描） */
    @Select("SELECT * FROM t_order WHERE status = 0 AND expire_time < NOW()")
    List<Order> findPendingOrders();
}