package com.ncop.modules.products.dto;

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
public class DosageFormRequestDto {
    @NotBlank(message = "Dosage form name is required")
    private String name;

    private String description;

    private List<DosageVariantDto> variants = new ArrayList<>();

    private boolean active = true;

    private int sortOrder = 0;
}
