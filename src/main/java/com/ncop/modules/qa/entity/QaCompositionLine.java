package com.ncop.modules.qa.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class QaCompositionLine {
    private String api;
    private Double labelClaim;
    private String claimUnit = "mg"; // mg, mcg, g, kg, % w/v, % w/w, IU
    private Double overagePercent = 0.0;
    private Double overagedQty = 0.0;
    private String reasonForOverage;
    private String pharmacopeia = "IP"; // IP, BP, USP, EP, In-House
    private String functionCategory = "Active Pharmaceutical Ingredient";
}
