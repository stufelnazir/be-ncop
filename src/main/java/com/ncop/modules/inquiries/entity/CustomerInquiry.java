package com.ncop.modules.inquiries.entity;

import com.ncop.modules.inquiries.enums.InquiryPriority;
import com.ncop.modules.inquiries.enums.InquirySource;
import com.ncop.modules.inquiries.enums.InquiryStatus;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Document(collection = "customer_inquiries")
public class CustomerInquiry {
    @Id
    private String id;
    private String rfqNo;
    private LocalDate inquiryDate;
    private String customerId;
    private String customerName;
    private String contactPersonId;
    private String contactPersonName;
    private String contactEmail;
    private InquirySource inquirySource;
    private InquiryPriority priority;
    private LocalDate targetQuoteDate;
    private InquiryStatus status;
    private String notes;
    private List<InquiryLine> lines = new ArrayList<>();
}
