package com.ncop.modules.products.controller;

import com.ncop.modules.products.dto.DosageFormRequestDto;
import com.ncop.modules.products.entity.DosageForm;
import com.ncop.modules.products.services.DosageFormService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/dosage-forms")
@RequiredArgsConstructor
public class DosageFormController {

    private final DosageFormService dosageFormService;

    @GetMapping
    public ResponseEntity<List<DosageForm>> getAllDosageForms(@RequestParam(defaultValue = "false") boolean activeOnly) {
        if (activeOnly) {
            return ResponseEntity.ok(dosageFormService.getActiveDosageForms());
        }
        return ResponseEntity.ok(dosageFormService.getAllDosageForms());
    }

    @GetMapping("/{id}")
    public ResponseEntity<DosageForm> getDosageFormById(@PathVariable String id) {
        return ResponseEntity.ok(dosageFormService.getDosageFormById(id));
    }

    @PostMapping
    public ResponseEntity<DosageForm> createDosageForm(@Valid @RequestBody DosageFormRequestDto request) {
        DosageForm created = dosageFormService.createDosageForm(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<DosageForm> updateDosageForm(@PathVariable String id, @Valid @RequestBody DosageFormRequestDto request) {
        DosageForm updated = dosageFormService.updateDosageForm(id, request);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDosageForm(@PathVariable String id) {
        dosageFormService.deleteDosageForm(id);
        return ResponseEntity.noContent().build();
    }
}
