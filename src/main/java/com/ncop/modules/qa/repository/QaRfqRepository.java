package com.ncop.modules.qa.repository;

import com.ncop.modules.qa.entity.QaRfq;
import com.ncop.modules.qa.enums.QaRfqStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QaRfqRepository extends MongoRepository<QaRfq, String> {
    Optional<QaRfq> findByRfqNo(String rfqNo);
    boolean existsByRfqNo(String rfqNo);
    List<QaRfq> findByStatus(QaRfqStatus status);
    long countByStatus(QaRfqStatus status);
    List<QaRfq> findByCustomerId(String customerId);
    List<QaRfq> findByInquiryId(String inquiryId);
}
