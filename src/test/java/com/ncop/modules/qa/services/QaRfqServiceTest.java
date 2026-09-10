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
    @Mock private QaRfqAccessService accessService;

    @Test
    void outsourcedInquiryReachesQcAndReassignmentPreservesFormula() {
        CustomerInquiry inquiry = new CustomerInquiry();
        inquiry.setId("inquiry-qc");
        inquiry.setQcAssigneeId("qc-new");
        inquiry.setQcAssigneeName("QC Reviewer");
        InquiryLine line = new InquiryLine();
        line.setProductId("outsourced-product");
        line.setProductName("Outsourced product");
        line.setSourcing(ProductSourcing.OUTSOURCED);
        inquiry.setLines(List.of(line));
        QaRfq existing = new QaRfq();
        existing.setSourceProductId(line.getProductId());
        existing.setAssignedToId("qc-old");
        existing.setStatus(QaRfqStatus.DRAFT_SAVED);
        when(rfqRepository.findByInquiryId(inquiry.getId())).thenReturn(List.of(existing));

        new QaRfqService(rfqRepository, calculatorService, matchingService, accessService)
                .syncAssignedInquiry(inquiry, new Client());

        verify(rfqRepository).save(existing);
        assertEquals("qc-new", existing.getAssignedToId());
        assertEquals(QaRfqStatus.DRAFT_SAVED, existing.getStatus());
    }

    @Test
    void assignedRecordsAreFilteredBeforePaginationAndDetailLookup() {
        QaRfq mine = new QaRfq();
        mine.setAssignedToId("qa-me");
        QaRfq other = new QaRfq();
        other.setAssignedToId("qa-other");
        when(accessService.currentUserFilter()).thenReturn(r -> "qa-me".equals(r.getAssignedToId()));
        when(rfqRepository.findAll()).thenReturn(List.of(other, mine));
        when(rfqRepository.findById("other")).thenReturn(Optional.of(other));
        var service = new QaRfqService(rfqRepository, calculatorService, matchingService, accessService);
        var page = service.getRfqs(org.springframework.data.domain.PageRequest.of(0, 1), null, null, null, null);
        assertEquals(List.of(mine), page.getContent());
        assertEquals(1, page.getTotalElements());
        assertEquals(Optional.empty(), service.getRfqById("other"));
    }

    @Test
    void removedProductsLoseTheirAssignment() {
        CustomerInquiry inquiry = new CustomerInquiry();
        inquiry.setId("inquiry-1");
        inquiry.setLines(List.of());
        QaRfq removed = new QaRfq();
        removed.setAssignedToId("qa-old");
        when(rfqRepository.findByInquiryId(inquiry.getId())).thenReturn(List.of(removed));
        new QaRfqService(rfqRepository, calculatorService, matchingService, accessService)
                .syncAssignedInquiry(inquiry, new Client());
        assertEquals(null, removed.getAssignedToId());
        verify(rfqRepository).save(removed);
    }

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

        new QaRfqService(rfqRepository, calculatorService, matchingService, accessService)
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
        when(accessService.currentUserFilter()).thenReturn(r -> true);
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
        QaRfq updated = new QaRfqService(rfqRepository, calculatorService, matchingService, accessService)
                .updateRfq("qa-rfq-1", workbenchChanges);

        assertEquals("inquiry-1", updated.getInquiryId());
        assertEquals("qa-1", updated.getAssignedToId());
        assertEquals("QA Reviewer", updated.getAssignedToName());
        assertEquals("2026-09-12", updated.getDueDate());
    }
}
