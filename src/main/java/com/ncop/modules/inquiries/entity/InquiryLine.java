package com.ncop.modules.inquiries.entity;

import com.ncop.modules.products.enums.ProductSourcing;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class InquiryLine {
    private String productId;
    private String productName;
    private String genericName;
    private String dosageForm;
    private String dosageVariant;
    private String strength;
    private String pharmacopeia;
    private ProductSourcing sourcing;
    private Long quantityRequired;
    private Long shipperPackRequired;
    private Long tertiaryPackRequired;
    private Long secondaryPackRequired;
    private Long monoBoxPackRequired;
    private Long stripPackRequired;
    private Long tabletPackRequired;
    private BigDecimal targetPrice;
    private String packagingNotes;
}
