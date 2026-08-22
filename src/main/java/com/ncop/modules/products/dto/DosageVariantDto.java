package com.ncop.modules.products.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DosageVariantDto {
    private String name;
    private String description;
    private boolean active = true;
}
