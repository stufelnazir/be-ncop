package com.ncop.modules.inquiries.controller;

import com.ncop.common.dto.PageResponse;
import com.ncop.modules.inquiries.dto.CustomerInquiryRequestDto;
import com.ncop.modules.inquiries.entity.CustomerInquiry;
import com.ncop.modules.inquiries.services.CustomerInquiryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/inquiries")
@RequiredArgsConstructor
public class CustomerInquiryController {
    private final CustomerInquiryService inquiryService;

    @PostMapping
    public ResponseEntity<CustomerInquiry> create(@Valid @RequestBody CustomerInquiryRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(inquiryService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CustomerInquiry> update(
            @PathVariable String id,
            @Valid @RequestBody CustomerInquiryRequestDto request) {
        return ResponseEntity.ok(inquiryService.update(id, request));
    }

    @GetMapping
    public ResponseEntity<PageResponse<CustomerInquiry>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<CustomerInquiry> result = inquiryService.list(page, size);
        return ResponseEntity.ok(PageResponse.of(result.getContent(), page, size, result.getTotalElements()));
    }

    @GetMapping("/my")
    public ResponseEntity<PageResponse<CustomerInquiry>> listMine(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<CustomerInquiry> result = inquiryService.listAssignedToCurrentUser(page, size);
        return ResponseEntity.ok(PageResponse.of(result.getContent(), page, size, result.getTotalElements()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CustomerInquiry> get(@PathVariable String id) {
        return ResponseEntity.ok(inquiryService.get(id));
    }
}
