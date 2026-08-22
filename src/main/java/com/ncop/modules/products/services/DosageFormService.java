package com.ncop.modules.products.services;

import com.ncop.auth.exception.DuplicateResourceException;
import com.ncop.auth.exception.ResourceNotFoundException;
import com.ncop.modules.products.dto.DosageFormRequestDto;
import com.ncop.modules.products.dto.DosageVariantDto;
import com.ncop.modules.products.entity.DosageForm;
import com.ncop.modules.products.entity.DosageVariant;
import com.ncop.modules.products.repository.DosageFormRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class DosageFormService {

    private final DosageFormRepository dosageFormRepository;

    public List<DosageForm> getAllDosageForms() {
        return dosageFormRepository.findAllByOrderBySortOrderAsc();
    }

    public List<DosageForm> getActiveDosageForms() {
        return dosageFormRepository.findByActiveTrueOrderBySortOrderAsc();
    }

    public DosageForm getDosageFormById(String id) {
        return dosageFormRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Dosage form not found with id: " + id));
    }

    public DosageForm createDosageForm(DosageFormRequestDto request) {
        String name = request.getName().trim();
        if (dosageFormRepository.existsByNameIgnoreCase(name)) {
            throw new DuplicateResourceException("Dosage form with name \"" + name + "\" already exists");
        }

        DosageForm df = new DosageForm();
        df.setName(name);
        df.setDescription(request.getDescription());
        df.setActive(request.isActive());
        df.setSortOrder(request.getSortOrder());
        df.setCreatedOn(Instant.now());
        df.setLastUpdatedOn(Instant.now());

        if (request.getVariants() != null) {
            List<DosageVariant> variants = request.getVariants().stream()
                    .map(v -> new DosageVariant(v.getName().trim(), v.getDescription(), v.isActive()))
                    .toList();
            df.setVariants(new ArrayList<>(variants));
        }

        return dosageFormRepository.save(df);
    }

    public DosageForm updateDosageForm(String id, DosageFormRequestDto request) {
        DosageForm existing = getDosageFormById(id);

        String newName = request.getName().trim();
        if (!existing.getName().equalsIgnoreCase(newName)) {
            Optional<DosageForm> byName = dosageFormRepository.findByNameIgnoreCase(newName);
            if (byName.isPresent() && !byName.get().getId().equals(id)) {
                throw new DuplicateResourceException("Dosage form with name \"" + newName + "\" already exists");
            }
            existing.setName(newName);
        }

        existing.setDescription(request.getDescription());
        existing.setActive(request.isActive());
        existing.setSortOrder(request.getSortOrder());
        existing.setLastUpdatedOn(Instant.now());

        if (request.getVariants() != null) {
            List<DosageVariant> variants = request.getVariants().stream()
                    .map(v -> new DosageVariant(v.getName().trim(), v.getDescription(), v.isActive()))
                    .toList();
            existing.setVariants(new ArrayList<>(variants));
        }

        return dosageFormRepository.save(existing);
    }

    public void deleteDosageForm(String id) {
        if (!dosageFormRepository.existsById(id)) {
            throw new ResourceNotFoundException("Dosage form not found with id: " + id);
        }
        dosageFormRepository.deleteById(id);
    }

    /**
     * Pre-populate database with all standard dosage forms & variants from pharmaceutical specifications if empty.
     */
    @PostConstruct
    public void seedDefaultDosageFormsIfEmpty() {
        if (dosageFormRepository.count() > 0) {
            log.info("Dosage forms already configured (count: {})", dosageFormRepository.count());
            return;
        }

        log.info("Seeding standard Dosage Forms and Variants into MongoDB...");

        List<DosageForm> seedList = new ArrayList<>();
        int order = 1;

        // Tablet
        seedList.add(createSeedForm("Tablet", "Solid oral dosage form", order++,
                List.of("Film Coated Tablet", "Enteric Coated Tablet", "Chewable Tablet", "Effervescent Tablet", "Sustained Release Tablet", "Dispersible Tablet", "Uncoated Tablet", "Sublingual Tablet")));

        // Capsule
        seedList.add(createSeedForm("Capsule", "Encapsulated solid dosage form", order++,
                List.of("Hard Gelatin Capsule", "Soft Gelatin Capsule", "Delayed Release Capsule", "Enteric Coated Capsule")));

        // Syrup
        seedList.add(createSeedForm("Syrup", "Viscous liquid oral formulation", order++,
                List.of("Sugar Based Syrup", "Sugar Free Syrup")));

        // Suspension
        seedList.add(createSeedForm("Suspension", "Heterogeneous liquid containing dispersed solid particles", order++,
                List.of("Ready to Use Suspension", "Dry Suspension (Dry Syrup)")));

        // Solution
        seedList.add(createSeedForm("Solution", "Clear homogeneous liquid", order++,
                List.of("Oral Solution", "Topical Solution")));

        // Oral Drops
        seedList.add(createSeedForm("Oral Drops", "Concentrated liquid drops for pediatric/infant use", order++,
                List.of("Paediatric Drops", "Infant Drops")));

        // Elixir
        seedList.add(createSeedForm("Elixir", "Clear, sweetened hydroalcoholic liquid", order++,
                List.of("Alcoholic Elixir", "Non-Alcoholic Elixir")));

        // Linctus
        seedList.add(createSeedForm("Linctus", "Viscous liquid for soothing cough relief", order++,
                List.of("Cough Linctus")));

        // Injection
        seedList.add(createSeedForm("Injection", "Sterile parenteral liquid or reconstitutable powder", order++,
                List.of("Liquid Injection", "Dry Powder Injection", "Lyophilized Injection")));

        // Vial
        seedList.add(createSeedForm("Vial", "Small glass or plastic vessel for liquid or powder", order++,
                List.of("Single Dose Vial", "Multi Dose Vial")));

        // Ampoule
        seedList.add(createSeedForm("Ampoule", "Hermetically sealed glass capsule", order++,
                List.of("Glass Ampoule", "Plastic Ampoule")));

        // Prefilled Syringe
        seedList.add(createSeedForm("Prefilled Syringe", "Ready-to-administer syringe system", order++,
                List.of("Disposable PFS", "Safety Needle PFS")));

        // IV Infusion
        seedList.add(createSeedForm("IV Infusion", "Large and small volume intravenous parenteral solutions", order++,
                List.of("Large Volume Parenteral (LVP)", "Small Volume Parenteral (SVP)")));

        // Cartridge
        seedList.add(createSeedForm("Cartridge", "Standardized container for pen delivery devices", order++,
                List.of("Pen Cartridge", "Dental Cartridge")));

        // Cream
        seedList.add(createSeedForm("Cream", "Semisolid emulsion for topical/mucosal application", order++,
                List.of("Oil in Water Cream", "Water in Oil Cream", "Rectal Cream", "Vaginal Cream")));

        // Ointment
        seedList.add(createSeedForm("Ointment", "Homogeneous, viscous semisolid preparation", order++,
                List.of("Hydrocarbon Ointment", "Water Soluble Ointment", "Sterile Ointment")));

        // Gel
        seedList.add(createSeedForm("Gel", "Semisolid colloidal system", order++,
                List.of("Transparent Gel", "Emulgel", "Oral Ulcer Gel", "Dental Fluoride Gel", "Rectal Gel", "Vaginal Gel", "Ophthalmic Gel", "Nasal Gel")));

        // Lotion
        seedList.add(createSeedForm("Lotion", "Low-to-medium viscosity topical liquid", order++,
                List.of("Topical Lotion")));

        // Paste
        seedList.add(createSeedForm("Paste", "High solid content thick semisolid", order++,
                List.of("Medicated Paste", "Dental Paste")));

        // Liniment
        seedList.add(createSeedForm("Liniment", "Medicated liquid for rubbing on skin", order++,
                List.of("Topical Liniment")));

        // Spray
        seedList.add(createSeedForm("Spray", "Fine mist or aerosol spray", order++,
                List.of("Topical Spray", "Ear Spray", "Metered Dose Spray")));

        // Eye Ointment
        seedList.add(createSeedForm("Eye Ointment", "Sterile ophthalmic ointment", order++,
                List.of("Sterile Ointment")));

        // Eye Gel
        seedList.add(createSeedForm("Eye Gel", "Sterile ophthalmic gel", order++,
                List.of("Ophthalmic Gel")));

        // Ear Drops
        seedList.add(createSeedForm("Ear Drops", "Sterile otic drop formulation", order++,
                List.of("Sterile Ear Drops")));

        // Ear Spray
        seedList.add(createSeedForm("Ear Spray", "Otic spray device", order++,
                List.of("Ear Spray")));

        // Nasal Drops
        seedList.add(createSeedForm("Nasal Drops", "Intranasal drop solution", order++,
                List.of("Sterile Nasal Drops")));

        // Nasal Spray
        seedList.add(createSeedForm("Nasal Spray", "Intranasal metered spray", order++,
                List.of("Metered Dose Spray")));

        // Nasal Gel
        seedList.add(createSeedForm("Nasal Gel", "Intranasal gel formulation", order++,
                List.of("Nasal Gel")));

        // MDI
        seedList.add(createSeedForm("MDI", "Metered Dose Inhaler", order++,
                List.of("Metered Dose Inhaler")));

        // DPI
        seedList.add(createSeedForm("DPI", "Dry Powder Inhaler", order++,
                List.of("Dry Powder Inhaler")));

        // Nebulizer
        seedList.add(createSeedForm("Nebulizer", "Inhalation solution for nebulizers", order++,
                List.of("Nebulizer Solution", "Nebulizer Suspension")));

        // Inhalation Capsule
        seedList.add(createSeedForm("Inhalation Capsule", "Rotacap / DPI capsule", order++,
                List.of("DPI Capsule")));

        // Suppository
        seedList.add(createSeedForm("Suppository", "Solid rectal insertion dosage form", order++,
                List.of("Adult Suppository", "Pediatric Suppository")));

        // Enema
        seedList.add(createSeedForm("Enema", "Liquid rectal administration solution", order++,
                List.of("Ready to Use Enema")));

        // Vaginal Tablet
        seedList.add(createSeedForm("Vaginal Tablet", "Solid tablet for vaginal insertion", order++,
                List.of("Vaginal Tablet")));

        // Vaginal Capsule
        seedList.add(createSeedForm("Vaginal Capsule", "Soft gelatin capsule for vaginal use", order++,
                List.of("Soft Vaginal Capsule")));

        // Pessary
        seedList.add(createSeedForm("Pessary", "Vaginal pessary dosage form", order++,
                List.of("Vaginal Pessary")));

        // Mouthwash
        seedList.add(createSeedForm("Mouthwash", "Antiseptic oral rinse", order++,
                List.of("Antiseptic Mouthwash")));

        // Gargle
        seedList.add(createSeedForm("Gargle", "Medicated oral gargle", order++,
                List.of("Medicated Gargle")));

        // Patch
        seedList.add(createSeedForm("Patch", "Transdermal delivery system", order++,
                List.of("Matrix Patch", "Reservoir Patch")));

        // Implant
        seedList.add(createSeedForm("Implant", "Subdermal delivery system", order++,
                List.of("Biodegradable Implant", "Non-Biodegradable Implant")));

        // Medical Gas
        seedList.add(createSeedForm("Medical Gas", "Medical gases for inhalation/anesthesia", order++,
                List.of("Oxygen", "Nitrous Oxide")));

        // Medicated Film
        seedList.add(createSeedForm("Medicated Film", "Oral dissolving or sublingual film", order++,
                List.of("Buccal Film", "Sublingual Film")));

        // Sachet
        seedList.add(createSeedForm("Sachet", "Single-dose pouch packaging", order++,
                List.of("Powder Sachet", "Granule Sachet", "Liquid Sachet", "Gel Sachet")));

        dosageFormRepository.saveAll(seedList);
        log.info("Successfully seeded {} standard Dosage Forms and Variants", seedList.size());
    }

    private DosageForm createSeedForm(String name, String description, int sortOrder, List<String> variantNames) {
        DosageForm df = new DosageForm();
        df.setName(name);
        df.setDescription(description);
        df.setSortOrder(sortOrder);
        df.setActive(true);
        df.setCreatedOn(Instant.now());
        df.setLastUpdatedOn(Instant.now());

        List<DosageVariant> variants = variantNames.stream()
                .map(v -> new DosageVariant(v, "", true))
                .toList();
        df.setVariants(new ArrayList<>(variants));
        return df;
    }
}
