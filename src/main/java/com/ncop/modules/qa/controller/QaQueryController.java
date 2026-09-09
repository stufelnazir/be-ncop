package com.ncop.modules.qa.controller;

import com.ncop.modules.qa.dto.QaQueryRequestDto;
import com.ncop.modules.qa.entity.QaQuery;
import com.ncop.modules.qa.entity.QaRfq;
import com.ncop.modules.qa.enums.QaRfqStatus;
import com.ncop.modules.qa.repository.QaQueryRepository;
import com.ncop.modules.qa.repository.QaRfqRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.Year;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/qa/queries")
@RequiredArgsConstructor
public class QaQueryController {

    private final QaQueryRepository queryRepository;
    private final QaRfqRepository rfqRepository;

    @GetMapping
    public ResponseEntity<List<QaQuery>> getAllQueries() {
        return ResponseEntity.ok(queryRepository.findAll());
    }

    @PostMapping
    public ResponseEntity<QaQuery> raiseQuery(@RequestBody QaQueryRequestDto dto) {
        QaQuery query = new QaQuery();

        String year = String.valueOf(Year.now().getValue());
        long count = queryRepository.count();
        String qNo = String.format("TQ-%s-%04d", year, count + 1);
        while (queryRepository.existsByQueryNo(qNo)) {
            count++;
            qNo = String.format("TQ-%s-%04d", year, count + 1);
        }
        query.setQueryNo(qNo);

        query.setRfqId(dto.getRfqId());
        query.setRfqNo(dto.getRfqNo());
        query.setMfrId(dto.getMfrId());
        query.setRaisedBy(dto.getRaisedBy() != null ? dto.getRaisedBy() : "QA Team");
        query.setRaisedTo(dto.getRaisedTo() != null ? dto.getRaisedTo() : "Sales Team");
        query.setSubject(dto.getSubject());
        query.setQueryText(dto.getQueryText());
        query.setStatus("OPEN");
        query.setCreatedOn(Instant.now());

        QaQuery saved = queryRepository.save(query);

        // Update RFQ status to QUERY_RAISED if rfqId is provided
        if (dto.getRfqId() != null) {
            rfqRepository.findById(dto.getRfqId()).ifPresent(rfq -> {
                rfq.setStatus(QaRfqStatus.QUERY_RAISED);
                rfq.setLastUpdatedOn(Instant.now());
                rfqRepository.save(rfq);
            });
        }

        return new ResponseEntity<>(saved, HttpStatus.CREATED);
    }

    @GetMapping("/rfq/{rfqId}")
    public ResponseEntity<List<QaQuery>> getQueriesByRfq(@PathVariable String rfqId) {
        return ResponseEntity.ok(queryRepository.findByRfqId(rfqId));
    }

    @PutMapping("/{id}/resolve")
    public ResponseEntity<QaQuery> resolveQuery(@PathVariable String id, @RequestBody Map<String, String> body) {
        return queryRepository.findById(id).map(query -> {
            if (body != null && body.containsKey("responseText")) {
                query.setResponseText(body.get("responseText"));
            }
            query.setStatus("RESOLVED");
            query.setResolvedOn(Instant.now());
            QaQuery saved = queryRepository.save(query);

            // If all queries for this RFQ are resolved, revert status from QUERY_RAISED to FORMULA_PENDING or DRAFT_SAVED
            if (query.getRfqId() != null) {
                List<QaQuery> remainingOpen = queryRepository.findByRfqId(query.getRfqId()).stream()
                        .filter(q -> "OPEN".equalsIgnoreCase(q.getStatus()))
                        .toList();

                if (remainingOpen.isEmpty()) {
                    rfqRepository.findById(query.getRfqId()).ifPresent(rfq -> {
                        if (rfq.getStatus() == QaRfqStatus.QUERY_RAISED) {
                            rfq.setStatus(rfq.getCompositionLines().isEmpty() ? QaRfqStatus.FORMULA_PENDING : QaRfqStatus.DRAFT_SAVED);
                            rfq.setLastUpdatedOn(Instant.now());
                            rfqRepository.save(rfq);
                        }
                    });
                }
            }

            return ResponseEntity.ok(saved);
        }).orElse(ResponseEntity.notFound().build());
    }
}
