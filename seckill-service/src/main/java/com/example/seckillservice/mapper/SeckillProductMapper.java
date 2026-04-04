package com.example.seckillservice.mapper;

import com.example.seckillservice.domain.SeckillProduct;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface SeckillProductMapper {

    @Select("SELECT * FROM seckill_product WHERE id = #{id}")
    SeckillProduct findById(Long id);

    @Select("SELECT * FROM seckill_product WHERE activity_id = #{activityId}")
    List<SeckillProduct> findByActivityId(Long activityId);

    @Select("SELECT * FROM seckill_product WHERE activity_id = #{activityId} AND product_id = #{productId}")
    SeckillProduct findByActivityAndProduct(@Param("activityId") Long activityId, @Param("productId") Long productId);

    @Update("UPDATE seckill_product SET stock = #{stock} WHERE activity_id = #{activityId} AND product_id = #{productId}")
    int updateStock(@Param("activityId") Long activityId, @Param("productId") Long productId, @Param("stock") Integer stock);

    @Insert("INSERT INTO seckill_product(activity_id, product_id, seckill_price, stock, limit_per_user) " +
            "VALUES(#{activityId}, #{productId}, #{seckillPrice}, #{stock}, #{limitPerUser})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(SeckillProduct product);
}
