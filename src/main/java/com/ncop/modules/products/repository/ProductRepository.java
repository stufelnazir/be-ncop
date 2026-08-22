package com.ncop.modules.products.repository;

import com.ncop.modules.products.entity.Product;
import com.ncop.modules.products.enums.ProductStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends MongoRepository<Product, String> {
    Optional<Product> findByProductCode(String productCode);
    boolean existsByProductCode(String productCode);
    List<Product> findByStatus(ProductStatus status);
    List<Product> findByCategory(String category);
    List<Product> findByDosageForm(String dosageForm);
}
