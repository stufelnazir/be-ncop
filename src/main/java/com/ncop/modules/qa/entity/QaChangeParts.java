package com.ncop.modules.qa.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class QaChangeParts {
    // General / adaptive dosage form identifier
    private String dosageForm;

    // Solid Oral: Tablets
    private String toolingType;          // D Tooling, B Tooling, BB Tooling
    private String punchShape;           // Round, Oval, Capsule, Bi-convex
    private String punchSize;            // e.g. 12mm x 6mm
    private String embossingUpper;       // e.g. N-500
    private String embossingLower;       // e.g. Breakline

    // Solid Oral: Capsules
    private String capsuleSize;          // Size 00, Size 0, Size 1, Size 2, Size 3, Size 4
    private String capsuleType;          // Hard Gelatin, HPMC Vegetarian

    // Packaging: Blister / Strip
    private String blisterFormat;        // 10x10, 1x10, 3x10
    private String blisterPvcThickness;  // e.g. 250 micron PVC/PVDC
    private String blisterAluFoilGsm;    // e.g. 20 micron Hard Tempered

    // Liquid Oral: Bottles / Syrups
    private String bottleMaterial;       // PET Amber, Glass Amber, HDPE White
    private String bottleVolume;         // 60 ml, 100 ml, 200 ml
    private String capType;              // CRC Cap, ROPP Cap, Flip-Off Seal, Screw Cap
    private String measuringCup;         // 10ml Graduated, None

    // Topicals / Semi-solids: Ointments / Creams
    private String tubeMaterial;         // Lami Tube, Aluminum Tube
    private String tubeSize;             // 15g, 30g, 50g
    private String nozzleType;           // Ophthalmic nozzle, Standard round nozzle

    // Section 1: Dimensions & Layouts (Spec Alignment)
    private String tabletDiameterMm;     // e.g. 9 mm
    private String stripSizeMm;          // e.g. 78x32
    private String monoCartonSize;       // e.g. 80x35x40 (LxWxH)
    private String shipperBoxSize;       // e.g. 400x340x390 (LxWxH)
    private Boolean changePartAvailable; // Yes / No
    private String compressionCpFileName;
    private String compressionCpFileUrl;
    private String stripCpFileName;
    private String stripCpFileUrl;

    private String notes;
}
