package com.example.productservice.web;

import com.example.common.Result;
import com.example.productservice.domain.Product;
import com.example.productservice.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    @Autowired
    private ProductService productService;

    @GetMapping("/{id}")
    public Result<Product> getProduct(@PathVariable("id") Long id) {
        Product product = productService.getProductById(id);
        if (product == null) {
            return Result.notFound();
        }
        return Result.success(product);
    }

    @GetMapping
    public Result<List<Product>> list() {
        return Result.success(productService.getActiveProducts());
    }

    @GetMapping("/search")
    public Result<List<Product>> search(@RequestParam String keyword) {
        return Result.success(productService.searchProducts(keyword));
    }

    @PostMapping
    public Result<Product> create(@RequestBody Product product) {
        return Result.success("创建成功", productService.createProduct(product));
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable("id") Long id, @RequestBody Product product) {
        product.setId(id);
        boolean ok = productService.updateProduct(product);
        return ok ? Result.success() : Result.error("更新失败");
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable("id") Long id) {
        boolean ok = productService.deleteProduct(id);
        return ok ? Result.success() : Result.error("删除失败");
    }
}
