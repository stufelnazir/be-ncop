package com.ncop.modules.qa.controller;

import com.ncop.common.dto.PageResponse;
import com.ncop.modules.qa.dto.QaMfrRequestDto;
import com.ncop.modules.qa.entity.QaMfr;
import com.ncop.modules.qa.enums.QaMfrStatus;
import com.ncop.modules.qa.services.QaMfrService;
import com.ncop.modules.clients.services.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/qa/mfrs")
@RequiredArgsConstructor
public class QaMfrController {

    private final QaMfrService mfrService;
    private final FileStorageService fileStorageService;

    @PostMapping
    public ResponseEntity<QaMfr> createMfr(@RequestBody QaMfrRequestDto dto) {
        QaMfr created = mfrService.createMfr(dto);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @PostMapping("/from-rfq")
    public ResponseEntity<QaMfr> createFromRfq(@RequestBody Map<String, Object> body) {
        String rfqId = (String) body.get("rfqId");
        Double batchSize = null;
        if (body.get("batchSize") != null) {
            batchSize = Double.valueOf(body.get("batchSize").toString());
        }
        String batchUnit = (String) body.get("batchUnit");
        String rfqProductId = (String) body.get("rfqProductId");

        QaMfr created = mfrService.createMfrFromRfq(rfqId, rfqProductId, batchSize, batchUnit);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @PostMapping("/clone")
    public ResponseEntity<QaMfr> cloneMfr(@RequestBody Map<String, String> body) {
        String targetType = body.get("targetType");
        String targetId = body.get("targetId");
        String rfqId = body.get("rfqId");

        QaMfr cloned = mfrService.cloneFromProductOrMfr(targetType, targetId, rfqId);
        return new ResponseEntity<>(cloned, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<PageResponse<QaMfr>> getMfrs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) QaMfrStatus status) {

        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(mfrService.getMfrs(pageable, search, status));
    }

    @GetMapping("/{id}")
    public ResponseEntity<QaMfr> getMfrById(@PathVariable String id) {
        return mfrService.getMfrById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<QaMfr> updateMfr(@PathVariable String id, @RequestBody QaMfrRequestDto dto) {
        try {
            QaMfr updated = mfrService.updateMfr(id, dto);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{id}/submit")
    public ResponseEntity<QaMfr> submitMfr(@PathVariable String id, @RequestBody(required = false) Map<String, String> body) {
        String user = body != null && body.containsKey("user") ? body.get("user") : "QA Reviewer";
        try {
            QaMfr submitted = mfrService.submitMfr(id, user);
            return ResponseEntity.ok(submitted);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping(value = "/{id}/change-parts/upload", consumes = "multipart/form-data")
    public ResponseEntity<?> uploadChangePart(@PathVariable String id, @RequestParam("type") String type, @RequestParam("file") MultipartFile file) {
        if (!("compression".equals(type) || "strip".equals(type)) || !isAllowedLayout(file)) return ResponseEntity.badRequest().body("Only PDF and image layouts are allowed");
        return mfrService.getMfrById(id).map(mfr -> {
            FileStorageService.FileStorageResult stored = fileStorageService.storeFile(file, id, "QA_" + type.toUpperCase() + "_CP");
            if (stored == null) return ResponseEntity.badRequest().body("Unable to store file");
            if ("compression".equals(type)) { mfr.getChangeParts().setCompressionCpFileName(stored.getOriginalFileName()); mfr.getChangeParts().setCompressionCpFileUrl(stored.getStoragePath()); }
            else { mfr.getChangeParts().setStripCpFileName(stored.getOriginalFileName()); mfr.getChangeParts().setStripCpFileUrl(stored.getStoragePath()); }
            return ResponseEntity.ok(mfrService.save(mfr));
        }).orElse(ResponseEntity.notFound().build());
    }

    private boolean isAllowedLayout(MultipartFile file) {
        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase();
        return contentType.equals("application/pdf") || contentType.startsWith("image/") || name.matches(".*\\.(pdf|png|jpe?g|webp)$");
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMfr(@PathVariable String id) {
        mfrService.deleteMfr(id);
        return ResponseEntity.noContent().build();
    }
}
