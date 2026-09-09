package com.ncop.modules.qa.controller;

import com.ncop.common.dto.PageResponse;
import com.ncop.modules.qa.dto.MfrMatchResultDto;
import com.ncop.modules.qa.dto.QaRfqRequestDto;
import com.ncop.modules.qa.entity.QaRfq;
import com.ncop.modules.qa.enums.QaPriority;
import com.ncop.modules.qa.enums.QaRfqStatus;
import com.ncop.modules.qa.services.QaRfqService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/qa/rfqs")
@RequiredArgsConstructor
public class QaRfqController {

    private final QaRfqService rfqService;

    @PostMapping
    public ResponseEntity<QaRfq> createRfq(@RequestBody QaRfqRequestDto dto) {
        QaRfq created = rfqService.createRfq(dto);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<PageResponse<QaRfq>> getRfqs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) QaRfqStatus status,
            @RequestParam(required = false) QaPriority priority,
            @RequestParam(required = false) String dosageForm) {

        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(rfqService.getRfqs(pageable, search, status, priority, dosageForm));
    }

    @GetMapping("/{id}")
    public ResponseEntity<QaRfq> getRfqById(@PathVariable String id) {
        return rfqService.getRfqById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<QaRfq> updateRfq(@PathVariable String id, @RequestBody QaRfqRequestDto dto) {
        try {
            QaRfq updated = rfqService.updateRfq(id, dto);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRfq(@PathVariable String id) {
        rfqService.deleteRfq(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/matches")
    public ResponseEntity<List<MfrMatchResultDto>> getMfrMatches(@PathVariable String id) {
        try {
            List<MfrMatchResultDto> matches = rfqService.getMfrMatches(id);
            return ResponseEntity.ok(matches);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
