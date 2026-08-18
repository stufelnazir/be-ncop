package com.ncop.modules.clients.dto;

import java.math.BigDecimal;
import java.util.List;

import com.ncop.modules.clients.entity.Address;
import com.ncop.modules.clients.entity.PaymentTerms;
import com.ncop.modules.clients.entity.PointOfContact;
import com.ncop.modules.clients.enums.CustomerType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ClientRequestDto {
    private String customerCode;

    @NotNull(message = "Customer type is required")
    private CustomerType customerType;

    @NotBlank(message = "Company name is required")
    private String companyName;

    private String tradeName;

    private BigDecimal annualTurnover;

    private PaymentTerms paymentTerms;

    private List<Address> addresses;

    private List<PointOfContact> pointOfContacts;
}
