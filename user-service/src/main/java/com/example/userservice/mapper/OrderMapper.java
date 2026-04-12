package com.example.userservice.mapper;

import com.example.userservice.domain.Order;
import com.example.userservice.datasource.DataSource;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface OrderMapper {

    @DataSource("master")
    @Insert("INSERT INTO t_order (order_no, user_id, product_id, product_name, product_price, " +
            "quantity, total_amount, status, created_at, updated_at, expire_time) " +
            "VALUES (#{orderNo}, #{userId}, #{productId}, #{productName}, #{productPrice}, " +
            "#{quantity}, #{totalAmount}, #{status}, #{createdAt}, #{updatedAt}, #{expireTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Order order);

    @DataSource("slave")
    @Select("SELECT * FROM t_order WHERE id = #{id}")
    Order findById(Long id);

    @DataSource("slave")
    @Select("SELECT * FROM t_order WHERE order_no = #{orderNo}")
    Order findByOrderNo(String orderNo);

    @DataSource("slave")
    @Select("SELECT * FROM t_order WHERE user_id = #{userId} ORDER BY created_at DESC")
    List<Order> findByUserId(Long userId);

    @DataSource("slave")
    @Select("SELECT * FROM t_order WHERE status = #{status} AND expire_time < NOW()")
    List<Order> findExpiredOrders(@Param("status") Integer status);

    @DataSource("master")
    @Update("UPDATE t_order SET status = #{status}, updated_at = NOW() WHERE id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);

    @DataSource("master")
    @Update("UPDATE t_order SET status = 2, updated_at = NOW() WHERE id = #{id} AND status = 0")
    int cancelOrder(Long id);
}
