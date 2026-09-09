package com.ncop.modules.qa.dto;

import com.ncop.modules.qa.entity.QaChangeParts;
import com.ncop.modules.qa.entity.QaCompositionLine;
import com.ncop.modules.qa.enums.QaPriority;
import com.ncop.modules.qa.enums.QaRfqStatus;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class QaRfqRequestDto {
    private String inquiryId;
    private String customerId;
    private String customerName;
    private String customerCode;
    private String customerType;

    private String productName;
    private String brandName;
    private String dosageForm;
    private String dosageVariant;
    private String category;
    private String pharmacopeia;
    private String standard;

    private QaRfqStatus status;
    private QaPriority priority;

    private String assignedToId;
    private String assignedToName;
    private String assignedBy;
    private String dueDate;
    private String createdDate;

    private List<QaCompositionLine> compositionLines = new ArrayList<>();
    private QaChangeParts changeParts;

    private String packagingSpec;
    private Double orderQty;
    private String packingSpecs;
    private Double totalTablets;
    private Double targetBatchSize;
    private String batchUnit;
    private String remarks;
}
