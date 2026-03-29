package com.example.userservice.repository;

import com.example.userservice.document.ProductDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductElasticsearchRepository extends ElasticsearchRepository<ProductDocument, String> {

    List<ProductDocument> findByNameContainingOrDescriptionContaining(String name, String description);

    List<ProductDocument> findByStatus(Integer status);
}
