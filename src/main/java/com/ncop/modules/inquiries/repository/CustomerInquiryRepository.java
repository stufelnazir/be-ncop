package com.ncop.modules.inquiries.repository;

import com.ncop.modules.inquiries.entity.CustomerInquiry;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface CustomerInquiryRepository extends MongoRepository<CustomerInquiry, String> {
    boolean existsByRfqNo(String rfqNo);
}
