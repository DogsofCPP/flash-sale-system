package com.example.userservice.mapper;

import com.example.userservice.domain.SeckillProduct;
import com.example.userservice.datasource.TargetDataSource;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface SeckillProductMapper {

    @TargetDataSource("master")
    @Insert("INSERT INTO seckill_product (activity_id, product_id, seckill_price, stock, limit_per_user, created_at, updated_at) " +
            "VALUES (#{activityId}, #{productId}, #{seckillPrice}, #{stock}, #{limitPerUser}, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(SeckillProduct seckillProduct);

    @TargetDataSource("slave")
    @Select("SELECT * FROM seckill_product WHERE id = #{id}")
    SeckillProduct findById(Long id);

    @TargetDataSource("slave")
    @Select("SELECT * FROM seckill_product WHERE activity_id = #{activityId}")
    List<SeckillProduct> findByActivityId(Long activityId);

    @TargetDataSource("slave")
    @Select("SELECT * FROM seckill_product WHERE activity_id = #{activityId} AND product_id = #{productId}")
    SeckillProduct findByActivityAndProduct(@Param("activityId") Long activityId, @Param("productId") Long productId);

    @TargetDataSource("master")
    @Update("UPDATE seckill_product SET stock = stock - 1, updated_at = NOW() " +
            "WHERE activity_id = #{activityId} AND product_id = #{productId} AND stock > 0")
    int decreaseStock(@Param("activityId") Long activityId, @Param("productId") Long productId);

    @TargetDataSource("master")
    @Update("UPDATE seckill_product SET stock = stock + #{count}, updated_at = NOW() " +
            "WHERE activity_id = #{activityId} AND product_id = #{productId}")
    int increaseStock(@Param("activityId") Long activityId, @Param("productId") Long productId, @Param("count") Integer count);

    @TargetDataSource("master")
    @Update("UPDATE seckill_product SET stock = #{stock}, updated_at = NOW() " +
            "WHERE activity_id = #{activityId} AND product_id = #{productId}")
    int updateStock(@Param("activityId") Long activityId, @Param("productId") Long productId, @Param("stock") Integer stock);
}
