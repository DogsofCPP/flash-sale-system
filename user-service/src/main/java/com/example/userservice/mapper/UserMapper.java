package com.example.userservice.mapper;

import com.example.userservice.domain.User;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface UserMapper {

    @Select("SELECT * FROM t_user WHERE id = #{id}")
    User findById(Long id);

    @Select("SELECT * FROM t_user WHERE username = #{username}")
    User findByUsername(String username);

    @Select("SELECT * FROM t_user")
    List<User> findAll();

    @Insert("INSERT INTO t_user(username, password_hash, phone, status) " +
            "VALUES(#{username}, #{passwordHash}, #{phone}, #{status})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(User user);

    @Update("UPDATE t_user SET password_hash=#{passwordHash}, phone=#{phone}, " +
            "status=#{status}, updated_at=NOW() WHERE id=#{id}")
    int update(User user);

    @Delete("DELETE FROM t_user WHERE id=#{id}")
    int deleteById(Long id);
}
