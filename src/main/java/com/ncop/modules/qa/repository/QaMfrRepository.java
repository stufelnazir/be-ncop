package com.ncop.modules.qa.repository;

import com.ncop.modules.qa.entity.QaMfr;
import com.ncop.modules.qa.enums.QaMfrStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QaMfrRepository extends MongoRepository<QaMfr, String> {
    Optional<QaMfr> findByMfrNo(String mfrNo);
    boolean existsByMfrNo(String mfrNo);
    List<QaMfr> findByRfqId(String rfqId);
    List<QaMfr> findByStatus(QaMfrStatus status);
    long countByStatus(QaMfrStatus status);
}
