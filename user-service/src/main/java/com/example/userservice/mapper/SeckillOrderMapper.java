package com.example.userservice.mapper;

import com.example.userservice.domain.SeckillOrder;
import com.example.userservice.datasource.DataSource;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface SeckillOrderMapper {

    @DataSource("master")
    @Insert("INSERT INTO seckill_order (user_id, activity_id, product_id, order_no, status, created_at, updated_at) " +
            "VALUES (#{userId}, #{activityId}, #{productId}, #{orderNo}, #{status}, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(SeckillOrder seckillOrder);

    @DataSource("slave")
    @Select("SELECT * FROM seckill_order WHERE id = #{id}")
    SeckillOrder findById(Long id);

    @DataSource("slave")
    @Select("SELECT * FROM seckill_order WHERE user_id = #{userId} AND activity_id = #{activityId} AND product_id = #{productId}")
    SeckillOrder findByUserActivityProduct(@Param("userId") Long userId, @Param("activityId") Long activityId, @Param("productId") Long productId);

    @DataSource("slave")
    @Select("SELECT * FROM seckill_order WHERE user_id = #{userId} ORDER BY created_at DESC")
    List<SeckillOrder> findByUserId(Long userId);

    @DataSource("master")
    @Update("UPDATE seckill_order SET status = #{status}, updated_at = NOW() WHERE id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);

    @DataSource("master")
    @Update("UPDATE seckill_order SET status = 1, order_no = #{orderNo}, updated_at = NOW() " +
            "WHERE user_id = #{userId} AND activity_id = #{activityId} AND product_id = #{productId} AND status = 0")
    int updateStatusToSuccess(@Param("userId") Long userId, @Param("activityId") Long activityId, @Param("productId") Long productId, @Param("orderNo") String orderNo);
}
