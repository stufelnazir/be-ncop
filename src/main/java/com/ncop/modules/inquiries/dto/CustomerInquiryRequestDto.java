package com.ncop.modules.inquiries.dto;

import com.ncop.modules.inquiries.enums.InquiryPriority;
import com.ncop.modules.inquiries.enums.InquirySource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class CustomerInquiryRequestDto {
    @NotBlank(message = "Customer is required") private String customerId;
    @NotBlank(message = "Contact person is required") private String contactPersonId;
    @NotNull(message = "Inquiry source is required") private InquirySource inquirySource;
    @NotNull(message = "Priority is required") private InquiryPriority priority;
    private LocalDate targetQuoteDate;
    private String notes;
    @Valid @NotEmpty(message = "At least one product is required") private List<InquiryLineRequestDto> lines = new ArrayList<>();
}
