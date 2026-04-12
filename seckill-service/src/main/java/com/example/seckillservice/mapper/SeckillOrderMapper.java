package com.example.seckillservice.mapper;

import com.example.seckillservice.domain.SeckillOrder;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface SeckillOrderMapper {

    @Select("SELECT * FROM seckill_order WHERE id = #{id}")
    SeckillOrder findById(Long id);

    @Select("SELECT * FROM seckill_order WHERE user_id = #{userId} AND activity_id = #{activityId}")
    SeckillOrder findByUserAndActivity(@Param("userId") Long userId, @Param("activityId") Long activityId);

    @Select("SELECT * FROM seckill_order WHERE user_id = #{userId}")
    List<SeckillOrder> findByUserId(Long userId);

    @Insert("INSERT INTO seckill_order(user_id, activity_id, product_id, order_no, status) " +
            "VALUES(#{userId}, #{activityId}, #{productId}, #{orderNo}, #{status})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(SeckillOrder order);

    @Update("UPDATE seckill_order SET status=#{status}, order_no=#{orderNo}, updated_at=NOW() WHERE id=#{id}")
    int updateStatusToSuccess(@Param("id") Long id, @Param("status") Integer status, @Param("orderNo") String orderNo);

    @Update("UPDATE seckill_order SET status=2, fail_reason=#{reason}, updated_at=NOW() WHERE id=#{id}")
    int updateStatusToFailed(@Param("id") Long id, @Param("reason") String reason);
}
