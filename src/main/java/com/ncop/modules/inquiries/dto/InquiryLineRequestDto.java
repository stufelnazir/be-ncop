package com.ncop.modules.inquiries.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import com.ncop.modules.products.enums.ProductSourcing;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class InquiryLineRequestDto {
    @NotBlank(message = "Product is required") private String productId;
    @NotNull(message = "Source is required") private ProductSourcing sourcing;
    @NotNull(message = "Quantity required is required") private Long quantityRequired;
    private Long shipperPackRequired;
    private Long tertiaryPackRequired;
    private Long secondaryPackRequired;
    private Long monoBoxPackRequired;
    private Long stripPackRequired;
    private Long tabletPackRequired;
    private BigDecimal targetPrice;
    private String packagingNotes;
}
