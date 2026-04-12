package com.example.seckillservice.mapper;

import com.example.seckillservice.domain.SeckillActivity;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface SeckillActivityMapper {

    @Select("SELECT * FROM seckill_activity WHERE id = #{id}")
    SeckillActivity findById(Long id);

    @Select("SELECT * FROM seckill_activity")
    List<SeckillActivity> findAll();

    @Select("SELECT * FROM seckill_activity WHERE status = 1 OR (status = 0 AND start_time <= NOW())")
    List<SeckillActivity> findActiveActivities();

    @Insert("INSERT INTO seckill_activity(name, start_time, end_time, status) " +
            "VALUES(#{name}, #{startTime}, #{endTime}, #{status})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(SeckillActivity activity);

    @Update("UPDATE seckill_activity SET status=#{status} WHERE id=#{id}")
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);
}
