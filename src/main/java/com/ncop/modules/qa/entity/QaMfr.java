package com.ncop.modules.qa.entity;

import com.ncop.modules.qa.enums.QaMfrStatus;
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
@Document(collection = "qa_mfrs")
public class QaMfr {

    @Id
    private String id;

    @Indexed(unique = true)
    private String mfrNo; // e.g. MFR-2026-0001

    private String rfqId; // Reference to QaRfq
    private String rfqNo;

    private String productName;
    private String dosageForm;
    private String dosageVariant;
    private String standard;

    private Double batchSize = 100000.0; // e.g. 1,00,000 units
    private String batchUnit = "Tablets";
    private Double theoreticalYield = 100.0; // in %
    private Double proposedShelfLifeYears = 2.0; // Shelf life in years
    private String tabletColour; // Module 3 Coating spec
    private Double coatingPercentage; // Module 3 Coating spec
    private Double uncoatedAvgWeightMg; // Auto: API + Excipients
    private Double coatedAvgWeightMg; // Auto: Uncoated + Coating

    private QaMfrStatus status = QaMfrStatus.DRAFT;

    private List<QaMfrItem> items = new ArrayList<>();
    private QaChangeParts changeParts = new QaChangeParts();

    private String remarks;
    private String createdBy;
    private String approvedBy;

    @CreatedDate
    private Instant createdOn = Instant.now();

    @LastModifiedDate
    private Instant lastUpdatedOn = Instant.now();
}
