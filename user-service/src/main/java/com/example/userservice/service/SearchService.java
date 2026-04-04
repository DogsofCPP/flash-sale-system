package com.example.userservice.service;

import com.example.userservice.document.ProductDocument;
import com.example.userservice.domain.Product;
import com.example.userservice.repository.ProductElasticsearchRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Lazy
public class SearchService {

    private static final Logger log = LoggerFactory.getLogger(SearchService.class);

    private final Optional<ProductElasticsearchRepository> elasticsearchRepository;

    @Autowired
    public SearchService(@Lazy ProductElasticsearchRepository elasticsearchRepository) {
        this.elasticsearchRepository = Optional.ofNullable(elasticsearchRepository);
    }

    @PostConstruct
    public void init() {
        elasticsearchRepository.ifPresent(repo -> {
            try {
                log.info("Elasticsearch repository initialized successfully");
            } catch (Exception e) {
                log.warn("Elasticsearch not available, search functionality will be disabled: {}", e.getMessage());
            }
        });
    }

    public void indexProduct(Product product) {
        ProductDocument doc = new ProductDocument(
            product.getId(),
            product.getName(),
            product.getDescription(),
            product.getPrice(),
            product.getStock(),
            product.getStatus(),
            product.getCreatedAt(),
            product.getUpdatedAt()
        );
        elasticsearchRepository.ifPresent(repo -> repo.save(doc));
    }

    public void indexProducts(List<Product> products) {
        List<ProductDocument> documents = products.stream()
            .map(p -> new ProductDocument(
                p.getId(), p.getName(), p.getDescription(),
                p.getPrice(), p.getStock(), p.getStatus(),
                p.getCreatedAt(), p.getUpdatedAt()
            ))
            .collect(Collectors.toList());
        elasticsearchRepository.ifPresent(repo -> repo.saveAll(documents));
    }

    public List<ProductDocument> search(String keyword) {
        return elasticsearchRepository
            .map(repo -> repo.findByNameContainingOrDescriptionContaining(keyword, keyword))
            .orElseGet(Collections::emptyList);
    }

    public List<ProductDocument> searchByStatus(Integer status) {
        return elasticsearchRepository
            .map(repo -> repo.findByStatus(status))
            .orElseGet(Collections::emptyList);
    }

    public void deleteProduct(String id) {
        elasticsearchRepository.ifPresent(repo -> repo.deleteById(id));
    }

    public void deleteAll() {
        elasticsearchRepository.ifPresent(repo -> repo.deleteAll());
    }
}
