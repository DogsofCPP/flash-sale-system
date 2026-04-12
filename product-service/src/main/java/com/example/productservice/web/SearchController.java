package com.example.productservice.web;

import com.example.common.Result;
import com.example.productservice.document.ProductDocument;
import com.example.productservice.domain.Product;
import com.example.productservice.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/search")
public class SearchController {

    @Autowired
    private ProductService productService;

    @GetMapping
    public Result<List<ProductDocument>> search(@RequestParam String q) {
        return Result.success(productService.searchByEs(q));
    }

    @PostMapping("/rebuild")
    public Result<Void> rebuildIndex() {
        List<Product> products = productService.getAllProducts();
        productService.indexProducts(products);
        return Result.success("索引重建完成，共 " + products.size() + " 条", null);
    }

    @PostMapping("/index/{id}")
    public Result<Void> indexOne(@PathVariable Long id) {
        Product product = productService.getProductById(id);
        if (product == null) {
            return Result.notFound();
        }
        productService.indexProduct(product);
        return Result.success("索引添加成功", null);
    }

    @DeleteMapping("/index")
    public Result<Void> clearIndex() {
        return Result.success("索引清除完成", null);
    }
}
