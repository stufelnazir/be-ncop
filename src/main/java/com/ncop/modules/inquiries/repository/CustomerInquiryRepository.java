package com.ncop.modules.inquiries.repository;

import com.ncop.modules.inquiries.entity.CustomerInquiry;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CustomerInquiryRepository extends MongoRepository<CustomerInquiry, String> {
    boolean existsByRfqNo(String rfqNo);
    Page<CustomerInquiry> findByQaAssigneeIdOrQcAssigneeIdOrRaisedByUserIdOrSalesAssigneeId(
            String qaAssigneeId, String qcAssigneeId, String raisedByUserId, String salesAssigneeId, Pageable pageable);
}
