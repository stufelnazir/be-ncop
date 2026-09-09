package com.ncop.modules.qa.services;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class QaFormulaCalculatorService {

    /**
     * Overaged Qty = Label Claim * (1 + Overage% / 100)
     */
    public Double calculateOveragedQty(Double labelClaim, Double overagePercent) {
        if (labelClaim == null) return 0.0;
        double overage = (overagePercent != null) ? overagePercent : 0.0;
        double result = labelClaim * (1.0 + (overage / 100.0));
        return roundTo(result, 4);
    }

    /**
     * Calculates quantity required for the given batch size.
     * Standard Conversion Rules:
     * 1 kg = 1,000 g = 1,000,000 mg = 1,000,000,000 mcg (10^9 mcg)
     * 1 L = 1,000 ml
     */
    public Double calculateQtyPerBatch(Double batchSize, Double overagedQtyPerUnit, String claimUnit, String batchUnit) {
        if (batchSize == null || overagedQtyPerUnit == null || batchSize <= 0 || overagedQtyPerUnit <= 0) {
            return 0.0;
        }

        String unit = claimUnit != null ? claimUnit.trim().toLowerCase() : "mg";
        String bUnit = batchUnit != null ? batchUnit.trim().toLowerCase() : "kg";

        // Convert per-unit overaged quantity to base standard milligrams (mg) or milliliters (ml)
        double qtyInBaseMgOrMl = overagedQtyPerUnit;
        if (unit.equals("mcg") || unit.equals("μg")) {
            qtyInBaseMgOrMl = overagedQtyPerUnit / 1_000.0; // 1000 mcg = 1 mg
        } else if (unit.equals("g") || unit.equals("gm")) {
            qtyInBaseMgOrMl = overagedQtyPerUnit * 1_000.0; // 1 g = 1000 mg
        } else if (unit.equals("kg")) {
            qtyInBaseMgOrMl = overagedQtyPerUnit * 1_000_000.0; // 1 kg = 1,000,000 mg
        } else if (unit.equals("l") || unit.equals("litre") || unit.equals("liter")) {
            qtyInBaseMgOrMl = overagedQtyPerUnit * 1_000.0; // 1 L = 1000 ml
        }

        // Total in base mg or ml
        double totalBase = batchSize * qtyInBaseMgOrMl;

        // Convert total to target batchUnit (default kg or L)
        double batchResult;
        if (bUnit.contains("kg")) {
            batchResult = totalBase / 1_000_000.0; // mg to kg
        } else if (bUnit.contains("g") && !bUnit.contains("kg")) {
            batchResult = totalBase / 1_000.0; // mg to g
        } else if (bUnit.contains("l") && !bUnit.contains("ml")) {
            batchResult = totalBase / 1_000.0; // ml to L
        } else if (bUnit.contains("ml")) {
            batchResult = totalBase;
        } else {
            // fallback: return in kg if large, or raw total
            batchResult = totalBase / 1_000_000.0;
        }

        return roundTo(batchResult, 4);
    }

    private Double roundTo(double value, int places) {
        if (Double.isNaN(value) || Double.isInfinite(value)) return 0.0;
        BigDecimal bd = BigDecimal.valueOf(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }
}
