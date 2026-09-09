package com.ncop.modules.qa.repository;

import com.ncop.modules.qa.entity.QaQuery;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QaQueryRepository extends MongoRepository<QaQuery, String> {
    Optional<QaQuery> findByQueryNo(String queryNo);
    boolean existsByQueryNo(String queryNo);
    List<QaQuery> findByRfqId(String rfqId);
    List<QaQuery> findByStatus(String status);
    long countByStatus(String status);
}
