package com.example.userservice.mapper;

import com.example.userservice.domain.SeckillActivity;
import com.example.userservice.datasource.TargetDataSource;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface SeckillActivityMapper {

    @TargetDataSource("master")
    @Insert("INSERT INTO seckill_activity (name, start_time, end_time, status, created_at, updated_at) " +
            "VALUES (#{name}, #{startTime}, #{endTime}, #{status}, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(SeckillActivity activity);

    @TargetDataSource("slave")
    @Select("SELECT * FROM seckill_activity WHERE id = #{id}")
    SeckillActivity findById(Long id);

    @TargetDataSource("slave")
    @Select("SELECT * FROM seckill_activity ORDER BY created_at DESC")
    List<SeckillActivity> findAll();

    @TargetDataSource("slave")
    @Select("SELECT * FROM seckill_activity WHERE status = 1 AND start_time <= NOW() AND end_time > NOW()")
    List<SeckillActivity> findActiveActivities();

    @TargetDataSource("master")
    @Update("UPDATE seckill_activity SET status = #{status}, updated_at = NOW() WHERE id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);

    @TargetDataSource("master")
    @Update("UPDATE seckill_activity SET status = 2, updated_at = NOW() WHERE id = #{id} AND end_time <= NOW()")
    int updateStatusToEnded(Long id);
}
