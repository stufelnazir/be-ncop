package com.ncop.modules.qa.controller;

import com.ncop.modules.qa.dto.QaKpiDto;
import com.ncop.modules.qa.services.QaDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/qa/dashboard")
@RequiredArgsConstructor
public class QaDashboardController {

    private final QaDashboardService dashboardService;

    @GetMapping("/kpis")
    public ResponseEntity<QaKpiDto> getKpis() {
        return ResponseEntity.ok(dashboardService.getKpis());
    }
}
