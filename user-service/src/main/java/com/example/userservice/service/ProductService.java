package com.example.userservice.service;

import com.example.userservice.domain.Product;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class ProductService {

    private static final Logger log = LoggerFactory.getLogger(ProductService.class);

    // 内存存储（用于无数据库环境）
    private final ConcurrentHashMap<Long, Product> inMemoryProducts = new ConcurrentHashMap<>();
    private final AtomicLong idCounter = new AtomicLong(100);

    public ProductService() {
        // 初始化示例数据
        Product p1 = new Product();
        p1.setId(1L);
        p1.setName("iPhone 15 Pro Max");
        p1.setDescription("苹果旗舰手机");
        p1.setPrice(new BigDecimal("9999"));
        p1.setStock(50);
        p1.setStatus(1);
        inMemoryProducts.put(1L, p1);

        Product p2 = new Product();
        p2.setId(2L);
        p2.setName("MacBook Pro M3");
        p2.setDescription("苹果专业笔记本");
        p2.setPrice(new BigDecimal("15999"));
        p2.setStock(20);
        p2.setStatus(1);
        inMemoryProducts.put(2L, p2);

        Product p3 = new Product();
        p3.setId(3L);
        p3.setName("AirPods Pro 2");
        p3.setDescription("苹果降噪耳机");
        p3.setPrice(new BigDecimal("1999"));
        p3.setStock(100);
        p3.setStatus(1);
        inMemoryProducts.put(3L, p3);

        Product p4 = new Product();
        p4.setId(4L);
        p4.setName("Apple Watch S9");
        p4.setDescription("苹果智能手表");
        p4.setPrice(new BigDecimal("2999"));
        p4.setStock(30);
        p4.setStatus(1);
        inMemoryProducts.put(4L, p4);
    }

    public Product getProductById(Long id) {
        return inMemoryProducts.get(id);
    }

    public List<Product> getAllProducts() {
        return new ArrayList<>(inMemoryProducts.values());
    }

    public List<Product> getActiveProducts() {
        return inMemoryProducts.values().stream()
                .filter(p -> p.getStatus() != null && p.getStatus() == 1)
                .toList();
    }

    public List<Product> searchProducts(String keyword) {
        return inMemoryProducts.values().stream()
                .filter(p -> (p.getName() != null && p.getName().contains(keyword)) ||
                           (p.getDescription() != null && p.getDescription().contains(keyword)))
                .toList();
    }

    public Product createProduct(Product product) {
        Long id = idCounter.incrementAndGet();
        product.setId(id);
        inMemoryProducts.put(id, product);
        log.info("创建商品（内存）: id={}, name={}", id, product.getName());
        return product;
    }

    public boolean updateProduct(Product product) {
        if (inMemoryProducts.containsKey(product.getId())) {
            inMemoryProducts.put(product.getId(), product);
            return true;
        }
        return false;
    }

    public boolean deleteProduct(Long id) {
        return inMemoryProducts.remove(id) != null;
    }

    public boolean decreaseStock(Long id) {
        Product p = inMemoryProducts.get(id);
        if (p != null && p.getStock() != null && p.getStock() > 0) {
            p.setStock(p.getStock() - 1);
            return true;
        }
        return false;
    }

    public List<java.util.Map<String, Object>> getSeckillProducts() {
        return getActiveProducts().stream()
                .map(p -> {
                    java.util.Map<String, Object> item = new java.util.HashMap<>();
                    item.put("id", p.getId());
                    item.put("name", p.getName());
                    item.put("seckillPrice", p.getPrice().multiply(new BigDecimal("0.8")));
                    item.put("originalPrice", p.getPrice());
                    item.put("stock", p.getStock());
                    item.put("sold", 0);
                    return item;
                })
                .toList();
    }
}
