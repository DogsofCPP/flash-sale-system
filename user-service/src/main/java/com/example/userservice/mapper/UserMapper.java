package com.example.userservice.mapper;

import com.example.userservice.domain.User;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserMapper {

    @Select("SELECT id, username, password_hash, phone, status FROM t_user WHERE username = #{username}")
    User findByUsername(String username);

    @Select("SELECT id, username, password_hash, phone, status FROM t_user WHERE id = #{id}")
    User findById(Long id);

    @Insert("INSERT INTO t_user(username, password_hash, phone, status, created_at, updated_at) " +
            "VALUES(#{username}, #{passwordHash}, #{phone}, #{status}, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(User user);
}

