package com.ncop.modules.qa.dto;

import com.ncop.modules.qa.entity.QaChangeParts;
import com.ncop.modules.qa.entity.QaMfrItem;
import com.ncop.modules.qa.enums.QaMfrStatus;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class QaMfrRequestDto {
    private String rfqId;
    private String rfqNo;
    private String rfqProductId;

    private String productName;
    private String dosageForm;
    private String dosageVariant;
    private String standard;

    private Double batchSize;
    private String batchUnit;
    private Double theoreticalYield;
    private Double proposedShelfLifeYears;
    private String tabletColour;
    private Double coatingPercentage;
    private Double uncoatedAvgWeightMg;
    private Double coatedAvgWeightMg;

    private QaMfrStatus status;

    private List<QaMfrItem> items = new ArrayList<>();
    private QaChangeParts changeParts;

    private String remarks;
    private String createdBy;
    private String approvedBy;
    private String nextDepartment;
    private String nextApprover;
}
