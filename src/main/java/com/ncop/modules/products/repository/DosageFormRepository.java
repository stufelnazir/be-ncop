package com.ncop.modules.products.repository;

import com.ncop.modules.products.entity.DosageForm;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DosageFormRepository extends MongoRepository<DosageForm, String> {
    Optional<DosageForm> findByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCase(String name);
    List<DosageForm> findByActiveTrueOrderBySortOrderAsc();
    List<DosageForm> findAllByOrderBySortOrderAsc();
}
