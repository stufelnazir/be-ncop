package com.ncop.modules.qa.entity;

import com.ncop.modules.qa.enums.QaPriority;
import com.ncop.modules.qa.enums.QaRfqStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "qa_rfqs")
public class QaRfq {

    @Id
    private String id;

    @Indexed(unique = true)
    private String rfqNo; // e.g. RFQ-QA-2026-0001

    private String inquiryId; // Optional reference to Sales Inquiry
    private String sourceRfqNo; // RFQ number from the Sales inquiry
    private String sourceProductId; // Makes Sales -> QA synchronization idempotent

    private String customerId;
    private String customerName;
    private String customerCode;
    private String customerType;

    private String productName;
    private String brandName;
    private String dosageForm; // Tablet, Capsule, Syrup, Injection, Ointment
    private String dosageVariant; // Extended Release, Sugar Free, etc.
    private String category; // Therapeutic Category
    private String pharmacopeia; // IP, BP, USP
    private String standard; // IP, BP, USP, In-House

    private QaRfqStatus status = QaRfqStatus.FORMULA_PENDING;
    private QaPriority priority = QaPriority.MEDIUM;

    private String assignedToId;
    private String assignedToName;
    private String assignedBy; // e.g. Sales Team, Technical Sales
    private String dueDate; // YYYY-MM-DD
    private String createdDate; // YYYY-MM-DD

    private List<QaCompositionLine> compositionLines = new ArrayList<>();
    private QaChangeParts changeParts = new QaChangeParts();

    private String packagingSpec;
    private Double orderQty; // e.g. 12000
    private String packingSpecs; // e.g. 10x1x10
    private Double totalTablets; // e.g. 12000 * 10 * 1 * 10 = 1,200,000
    private Double targetBatchSize;
    private String batchUnit = "Tablets"; // Tablets, Capsules, Litres, Kg
    private String remarks;

    @CreatedDate
    private Instant createdOn = Instant.now();

    @LastModifiedDate
    private Instant lastUpdatedOn = Instant.now();
}
