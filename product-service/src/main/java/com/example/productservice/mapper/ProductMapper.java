package com.example.productservice.mapper;

import com.example.productservice.domain.Product;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface ProductMapper {

    @Select("SELECT * FROM t_product WHERE id = #{id}")
    Product findById(Long id);

    @Select("SELECT * FROM t_product WHERE status = 1")
    List<Product> findActiveProducts();

    @Select("SELECT * FROM t_product")
    List<Product> findAll();

    @Select("SELECT * FROM t_product WHERE name LIKE CONCAT('%', #{keyword}, '%')")
    List<Product> searchByKeyword(String keyword);

    @Insert("INSERT INTO t_product(name, description, price, stock, status) " +
            "VALUES(#{name}, #{description}, #{price}, #{stock}, #{status})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Product product);

    @Update("UPDATE t_product SET name=#{name}, description=#{description}, " +
            "price=#{price}, stock=#{stock}, status=#{status}, updated_at=NOW() WHERE id=#{id}")
    int update(Product product);

    @Delete("DELETE FROM t_product WHERE id=#{id}")
    int deleteById(Long id);
}
