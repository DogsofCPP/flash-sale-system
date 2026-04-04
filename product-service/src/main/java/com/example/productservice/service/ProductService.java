package com.example.productservice.service;

import com.example.productservice.domain.Product;
import com.example.productservice.mapper.ProductMapper;
import com.example.productservice.repository.ProductElasticsearchRepository;
import com.example.productservice.document.ProductDocument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class ProductService {

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired(required = false)
    private ProductElasticsearchRepository esRepository;

    private static final String PRODUCT_KEY = "product:";

    public Product getProductById(Long id) {
        Product p = (Product) redisTemplate.opsForValue().get(PRODUCT_KEY + id);
        if (p != null) return p;
        p = productMapper.findById(id);
        if (p != null) {
            redisTemplate.opsForValue().set(PRODUCT_KEY + id, p, 30, TimeUnit.MINUTES);
        }
        return p;
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

    public List<ProductDocument> searchByEs(String keyword) {
        if (esRepository == null) {
            return List.of();
        }
        return esRepository.findByNameContainingOrDescriptionContaining(keyword, keyword);
    }

    public Product createProduct(Product product) {
        product.setStatus(1);
        productMapper.insert(product);
        return product;
    }

    public boolean updateProduct(Product product) {
        int result = productMapper.update(product);
        if (result > 0) {
            redisTemplate.delete(PRODUCT_KEY + product.getId());
        }
        return result > 0;
    }

    public boolean deleteProduct(Long id) {
        int result = productMapper.deleteById(id);
        if (result > 0) {
            redisTemplate.delete(PRODUCT_KEY + id);
        }
        return result > 0;
    }

    public void indexProduct(Product product) {
        if (esRepository == null) return;
        ProductDocument doc = new ProductDocument();
        doc.setId(product.getId());
        doc.setName(product.getName());
        doc.setDescription(product.getDescription());
        doc.setPrice(product.getPrice());
        doc.setStock(product.getStock());
        doc.setStatus(String.valueOf(product.getStatus()));
        esRepository.save(doc);
    }

    public void indexProducts(List<Product> products) {
        if (esRepository == null) return;
        products.forEach(this::indexProduct);
    }
}
