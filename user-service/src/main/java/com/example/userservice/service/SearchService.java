package com.example.userservice.service;

import com.example.userservice.domain.Product;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class SearchService {

    private static final Logger log = LoggerFactory.getLogger(SearchService.class);

    public void indexProduct(Product product) {
        log.info("商品索引功能已禁用 (ES未启动): {}", product.getName());
    }

    public void indexProducts(List<Product> products) {
        log.info("商品批量索引功能已禁用 (ES未启动): {} 个商品", products.size());
    }

    public List<Product> search(String keyword) {
        log.info("搜索功能已禁用 (ES未启动): {}", keyword);
        return new ArrayList<>();
    }

    public void deleteProduct(Long id) {
        log.info("删除索引功能已禁用 (ES未启动): {}", id);
    }

    public void deleteAll() {
        log.info("清空索引功能已禁用 (ES未启动)");
    }
}
