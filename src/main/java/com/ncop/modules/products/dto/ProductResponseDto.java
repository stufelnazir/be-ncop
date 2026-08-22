package com.ncop.modules.products.dto;

import com.ncop.modules.products.entity.ProductDocument;
import com.ncop.modules.products.enums.ProductStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponseDto {
    private String id;
    private String productCode;
    private String brandName;
    private String category;
    private String therapeuticClass;
    private String dosageForm;
    private String dosageVariant;
    private List<ProductIngredientDto> ingredients = new ArrayList<>();
    private String composition;
    private String packaging;
    private Long moq;
    private Double unitPrice;
    private String currency;
    private String shelfLife;
    private String storageCondition;
    private String description;
    private ProductStatus status;
    private List<ProductDocument> documents = new ArrayList<>();
    private Instant createdOn;
    private Instant lastUpdatedOn;
}
