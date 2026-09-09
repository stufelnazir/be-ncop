package com.ncop.modules.qa.services;

import com.ncop.modules.clients.entity.Client;
import com.ncop.modules.inquiries.entity.CustomerInquiry;
import com.ncop.modules.inquiries.entity.InquiryLine;
import com.ncop.modules.inquiries.enums.InquiryPriority;
import com.ncop.modules.products.enums.ProductSourcing;
import com.ncop.modules.qa.entity.QaRfq;
import com.ncop.modules.qa.dto.QaRfqRequestDto;
import com.ncop.modules.qa.enums.QaPriority;
import com.ncop.modules.qa.enums.QaRfqStatus;
import com.ncop.modules.qa.repository.QaRfqRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QaRfqServiceTest {

    @Mock private QaRfqRepository rfqRepository;
    @Mock private QaFormulaCalculatorService calculatorService;
    @Mock private MfrMatchingService matchingService;

    @Test
    void assignedInHouseInquiryCreatesVisibleQaWorkItem() {
        when(rfqRepository.findByInquiryId("inquiry-1")).thenReturn(List.of());
        when(rfqRepository.count()).thenReturn(0L);
        when(rfqRepository.existsByRfqNo(anyString())).thenReturn(false);
        when(rfqRepository.save(any(QaRfq.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CustomerInquiry inquiry = new CustomerInquiry();
        inquiry.setId("inquiry-1");
        inquiry.setRfqNo("RFQ-2026-00001");
        inquiry.setInquiryDate(LocalDate.of(2026, 9, 9));
        inquiry.setCustomerId("customer-1");
        inquiry.setCustomerName("Acme Pharma");
        inquiry.setPriority(InquiryPriority.CRITICAL);
        inquiry.setQaAssigneeId("qa-1");
        inquiry.setQaAssigneeName("QA Reviewer");
        inquiry.setSalesAssigneeName("Sales Person");
        inquiry.setTargetQuoteDate(LocalDate.of(2026, 9, 12));

        InquiryLine line = new InquiryLine();
        line.setProductId("product-1");
        line.setProductName("Paracetamol 500");
        line.setDosageForm("Tablet");
        line.setPharmacopeia("IP");
        line.setSourcing(ProductSourcing.IN_HOUSE);
        line.setQuantityRequired(100L);
        line.setCalculatedTabletQuantity(1_000L);
        inquiry.setLines(List.of(line));

        new QaRfqService(rfqRepository, calculatorService, matchingService)
                .syncAssignedInquiry(inquiry, new Client());

        ArgumentCaptor<QaRfq> captor = ArgumentCaptor.forClass(QaRfq.class);
        verify(rfqRepository).save(captor.capture());
        QaRfq saved = captor.getValue();
        assertNotNull(saved.getRfqNo());
        assertEquals("RFQ-2026-00001", saved.getSourceRfqNo());
        assertEquals("inquiry-1", saved.getInquiryId());
        assertEquals("product-1", saved.getSourceProductId());
        assertEquals("qa-1", saved.getAssignedToId());
        assertEquals("QA Reviewer", saved.getAssignedToName());
        assertEquals("Sales Person", saved.getAssignedBy());
        assertEquals(QaPriority.URGENT, saved.getPriority());
        assertEquals(QaRfqStatus.FORMULA_PENDING, saved.getStatus());
        assertEquals(1_000D, saved.getTotalTablets());
        assertEquals("2026-09-12", saved.getDueDate());
    }

    @Test
    void qaWorkbenchUpdateKeepsSalesAssignmentLink() {
        QaRfq existing = new QaRfq();
        existing.setId("qa-rfq-1");
        existing.setInquiryId("inquiry-1");
        existing.setAssignedToId("qa-1");
        existing.setAssignedToName("QA Reviewer");
        existing.setDueDate("2026-09-12");
        when(rfqRepository.findById("qa-rfq-1")).thenReturn(Optional.of(existing));
        when(rfqRepository.save(any(QaRfq.class))).thenAnswer(invocation -> invocation.getArgument(0));

        QaRfqRequestDto workbenchChanges = new QaRfqRequestDto();
        workbenchChanges.setProductName("Updated product");
        QaRfq updated = new QaRfqService(rfqRepository, calculatorService, matchingService)
                .updateRfq("qa-rfq-1", workbenchChanges);

        assertEquals("inquiry-1", updated.getInquiryId());
        assertEquals("qa-1", updated.getAssignedToId());
        assertEquals("QA Reviewer", updated.getAssignedToName());
        assertEquals("2026-09-12", updated.getDueDate());
    }
}
