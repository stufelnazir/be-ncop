package com.ncop.modules.qa.controller;

import com.ncop.common.dto.PageResponse;
import com.ncop.modules.qa.dto.QaMfrRequestDto;
import com.ncop.modules.qa.entity.QaMfr;
import com.ncop.modules.qa.enums.QaMfrStatus;
import com.ncop.modules.qa.services.QaMfrService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/qa/mfrs")
@RequiredArgsConstructor
public class QaMfrController {

    private final QaMfrService mfrService;

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

        QaMfr created = mfrService.createMfrFromRfq(rfqId, batchSize, batchUnit);
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
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMfr(@PathVariable String id) {
        mfrService.deleteMfr(id);
        return ResponseEntity.noContent().build();
    }
}
