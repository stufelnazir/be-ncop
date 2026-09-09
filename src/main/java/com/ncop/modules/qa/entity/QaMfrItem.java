package com.ncop.modules.qa.entity;

import com.ncop.modules.qa.enums.QaStage;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class QaMfrItem {
    private QaStage stage = QaStage.ACTIVE;
    private String itemCode;
    private String materialName;
    private String grade; // e.g. IP, BP, USP, Ph.Eur, Tech, Pure
    private Double labelClaim;
    private String claimUnit = "mg";
    private Double overagePercent = 0.0;
    private Double overagedQtyPerUnit = 0.0; // in claimUnit / unit dose
    private Double qtyPerBatch = 0.0;
    private String batchUnit = "kg"; // kg, g, L, ml
    private String functionCategory; // Active, Diluent, Binder, Disintegrant, Lubricant, Colorant, Solvent
    private String notes;
}
