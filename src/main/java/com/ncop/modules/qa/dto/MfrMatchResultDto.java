package com.ncop.modules.qa.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MfrMatchResultDto {
    private String targetType; // "PRODUCT" or "MFR"
    private String targetId;
    private String targetCode; // productCode or mfrNo
    private String targetName; // brandName or productName
    private String dosageForm;
    private String dosageVariant;
    private String composition;
    private Double batchSize;
    private String batchUnit;
    private String status;

    private Double totalScore; // 0 to 100
    private Double productNameScore; // max 35
    private Double compositionScore; // max 30
    private Double strengthScore; // max 15
    private Double dosageFormScore; // max 10
    private Double standardScore; // max 5
    private Double statusScore; // max 5
    private Double batchSizeScore; // max 5

    private List<String> matchedIngredients = new ArrayList<>();
}
