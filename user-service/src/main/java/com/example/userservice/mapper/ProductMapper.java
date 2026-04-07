package com.example.userservice.mapper;

import com.example.userservice.domain.Product;
import com.example.userservice.datasource.TargetDataSource;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ProductMapper {

    @TargetDataSource("slave")
    @Select("SELECT * FROM t_product WHERE id = #{id}")
    Product findById(Long id);

    @TargetDataSource("slave")
    @Select("SELECT * FROM t_product ORDER BY created_at DESC")
    List<Product> findAll();

    @TargetDataSource("slave")
    @Select("SELECT * FROM t_product WHERE status = 1 ORDER BY created_at DESC")
    List<Product> findActiveProducts();

    @TargetDataSource("slave")
    @Select("SELECT * FROM t_product WHERE name LIKE CONCAT('%', #{keyword}, '%') OR description LIKE CONCAT('%', #{keyword}, '%')")
    List<Product> searchByKeyword(String keyword);

    @TargetDataSource("master")
    @Insert("INSERT INTO t_product (name, description, price, stock, status, created_at, updated_at) " +
            "VALUES (#{name}, #{description}, #{price}, #{stock}, #{status}, #{createdAt}, #{updatedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Product product);

    @TargetDataSource("master")
    @Update("UPDATE t_product SET name=#{name}, description=#{description}, price=#{price}, " +
            "stock=#{stock}, status=#{status}, updated_at=NOW() WHERE id=#{id}")
    int update(Product product);

    @TargetDataSource("master")
    @Delete("DELETE FROM t_product WHERE id = #{id}")
    int deleteById(Long id);

    @TargetDataSource("master")
    @Update("UPDATE t_product SET stock = stock - 1, updated_at = NOW() WHERE id = #{id} AND stock > 0")
    int decreaseStock(Long id);
}
