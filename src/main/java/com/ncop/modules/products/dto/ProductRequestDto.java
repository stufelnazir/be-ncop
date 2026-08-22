package com.ncop.modules.products.dto;

import com.ncop.modules.products.enums.ProductStatus;
import jakarta.validation.constraints.NotBlank;
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
public class ProductRequestDto {
    private String productCode; // Optional, auto-generated if blank

    @NotBlank(message = "Brand name is required")
    private String brandName;

    private String category;

    private String therapeuticClass;

    @NotBlank(message = "Dosage form (Level 1) is required")
    private String dosageForm;

    private String dosageVariant; // Level 2

    private List<ProductIngredientDto> ingredients = new ArrayList<>();

    private String customComposition; // Optional override; auto-computed if blank

    private String packaging;

    private Long moq;

    private Double unitPrice;

    private String currency;

    private String shelfLife;

    private String storageCondition;

    private String description;

    private ProductStatus status;
}
