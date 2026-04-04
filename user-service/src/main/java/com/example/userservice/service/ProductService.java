package com.example.userservice.service;

import com.example.userservice.domain.Product;
import com.example.userservice.mapper.ProductMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductService {

    @Autowired
    private ProductMapper productMapper;

    public Product getProductById(Long id) {
        return productMapper.findById(id);
    }

    public List<Product> getAllProducts() {
        return productMapper.findAll();
    }

    public List<Product> getActiveProducts() {
        return productMapper.findActiveProducts();
    }

    public List<Product> searchProducts(String keyword) {
        return productMapper.searchByKeyword(keyword);
    }

    public Product createProduct(Product product) {
        productMapper.insert(product);
        return product;
    }

    public boolean updateProduct(Product product) {
        return productMapper.update(product) > 0;
    }

    public boolean deleteProduct(Long id) {
        return productMapper.deleteById(id) > 0;
    }

    public boolean decreaseStock(Long id) {
        return productMapper.decreaseStock(id) > 0;
    }
}
