package com.example.userservice.web;

import com.example.userservice.domain.Product;
import com.example.userservice.service.ProductService;
import com.example.userservice.service.SearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/search")
public class SearchController {

    @Autowired
    private SearchService searchService;

    @Autowired
    private ProductService productService;

    @GetMapping
    public ResponseEntity<List<Product>> search(@RequestParam String keyword) {
        // 简单搜索：基于MySQL LIKE查询
        return ResponseEntity.ok(productService.searchProducts(keyword));
    }

    @PostMapping("/index")
    public ResponseEntity<Map<String, Object>> rebuildIndex() {
        List<Product> products = productService.getAllProducts();
        searchService.indexProducts(products);
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "索引重建完成，共索引 " + products.size() + " 条数据");
        return ResponseEntity.ok(result);
    }

    @PostMapping("/index/{id}")
    public ResponseEntity<Map<String, Object>> indexOne(@PathVariable Long id) {
        Product product = productService.getProductById(id);
        if (product == null) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "商品不存在");
            return ResponseEntity.ok(result);
        }
        searchService.indexProduct(product);
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "索引添加成功");
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/index")
    public ResponseEntity<Map<String, Object>> clearIndex() {
        searchService.deleteAll();
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "索引清除完成");
        return ResponseEntity.ok(result);
    }
}