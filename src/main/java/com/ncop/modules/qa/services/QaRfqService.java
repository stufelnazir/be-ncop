package com.ncop.modules.qa.services;

import com.ncop.common.dto.PageResponse;
import com.ncop.modules.clients.entity.Client;
import com.ncop.modules.inquiries.entity.CustomerInquiry;
import com.ncop.modules.inquiries.entity.InquiryLine;
import com.ncop.modules.inquiries.enums.InquiryPriority;
import com.ncop.modules.products.enums.ProductSourcing;
import com.ncop.modules.qa.dto.MfrMatchResultDto;
import com.ncop.modules.qa.dto.QaRfqRequestDto;
import com.ncop.modules.qa.entity.QaCompositionLine;
import com.ncop.modules.qa.entity.QaRfq;
import com.ncop.modules.qa.entity.QaRfqProduct;
import com.ncop.modules.qa.enums.QaPriority;
import com.ncop.modules.qa.enums.QaRfqStatus;
import com.ncop.modules.qa.repository.QaRfqRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.Year;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class QaRfqService {

    private final QaRfqRepository rfqRepository;
    private final QaFormulaCalculatorService calculatorService;
    private final MfrMatchingService matchingService;
    private final QaRfqAccessService accessService;

    public List<QaRfq> getVisibleRfqs() {
        return rfqRepository.findAll().stream().filter(accessService.currentUserFilter()).toList();
    }

    public QaRfq createRfq(QaRfqRequestDto dto) {
        QaRfq rfq = new QaRfq();
        rfq.setRfqNo(nextRfqNo());

        mapDtoToEntity(dto, rfq);
        rfq.setCreatedOn(Instant.now());
        rfq.setLastUpdatedOn(Instant.now());

        return rfqRepository.save(rfq);
    }

    public PageResponse<QaRfq> getRfqs(Pageable pageable, String search, QaRfqStatus status, QaPriority priority, String dosageForm) {
        List<QaRfq> all = getVisibleRfqs();

        List<QaRfq> filtered = all.stream().filter(r -> {
            if (status != null && r.getStatus() != status) return false;
            if (priority != null && r.getPriority() != priority) return false;
            if (dosageForm != null && !dosageForm.isBlank() && !dosageForm.equalsIgnoreCase("all") &&
                (r.getDosageForm() == null || !r.getDosageForm().equalsIgnoreCase(dosageForm))) return false;
            if (search == null || search.isBlank()) return true;

            String q = search.toLowerCase();
            return (r.getRfqNo() != null && r.getRfqNo().toLowerCase().contains(q)) ||
                   (r.getProductName() != null && r.getProductName().toLowerCase().contains(q)) ||
                   (r.getBrandName() != null && r.getBrandName().toLowerCase().contains(q)) ||
                   (r.getSourceRfqNo() != null && r.getSourceRfqNo().toLowerCase().contains(q)) ||
                   (r.getCustomerName() != null && r.getCustomerName().toLowerCase().contains(q)) ||
                   (r.getCustomerCode() != null && r.getCustomerCode().toLowerCase().contains(q)) ||
                   (r.getDosageForm() != null && r.getDosageForm().toLowerCase().contains(q));
        }).sorted(Comparator.comparing(
                QaRfq::getCreatedOn,
                Comparator.nullsLast(Comparator.reverseOrder())))
          .toList();

        int pageSize = pageable.getPageSize();
        int pageNumber = pageable.getPageNumber();
        int fromIndex = Math.min(pageNumber * pageSize, filtered.size());
        int toIndex = Math.min(fromIndex + pageSize, filtered.size());
        List<QaRfq> paged = filtered.subList(fromIndex, toIndex);

        return PageResponse.of(paged, pageNumber, pageSize, filtered.size());
    }

    public Optional<QaRfq> getRfqById(String id) {
        return rfqRepository.findById(id).filter(accessService.currentUserFilter());
    }

    public QaRfq updateRfq(String id, QaRfqRequestDto dto) {
        QaRfq rfq = getRfqById(id)
                .orElseThrow(() -> new RuntimeException("RFQ not found with id: " + id));

        mapDtoToEntity(dto, rfq);
        rfq.setLastUpdatedOn(Instant.now());

        return rfqRepository.save(rfq);
    }

    public void deleteRfq(String id) {
        getRfqById(id).ifPresent(rfqRepository::delete);
    }

    public List<MfrMatchResultDto> getMfrMatches(String id) {
        QaRfq rfq = getRfqById(id)
                .orElseThrow(() -> new RuntimeException("RFQ not found with id: " + id));
        return matchingService.findMatchesForRfq(rfq);
    }

    public List<MfrMatchResultDto> getMfrMatches(String id, String productId) {
        QaRfq rfq = getRfqById(id).orElseThrow(() -> new RuntimeException("RFQ not found with id: " + id));
        if (productId == null || productId.isBlank()) return matchingService.findMatchesForRfq(rfq);
        QaRfqProduct product = rfq.getProducts().stream().filter(item -> productId.equals(item.getId())).findFirst()
                .orElseThrow(() -> new RuntimeException("RFQ product not found: " + productId));
        QaRfq matchContext = new QaRfq();
        matchContext.setId(rfq.getId());
        matchContext.setProductName(product.getProductName());
        matchContext.setDosageForm(product.getDosageForm());
        matchContext.setStandard(product.getStandard());
        matchContext.setCompositionLines(product.getCompositionLines());
        matchContext.setTargetBatchSize(product.getTotalTablets());
        return matchingService.findMatchesForRfq(matchContext);
    }

    /**
     * Creates or refreshes a work item for each product's assigned QA or QC reviewer.
     * Existing work items are updated in place so editing/reassigning an inquiry never
     * creates duplicates or discards formula work already completed by QA.
     */
    public void syncAssignedInquiry(CustomerInquiry inquiry, Client client) {
        List<QaRfq> existing = new ArrayList<>(rfqRepository.findByInquiryId(inquiry.getId()));
        List<QaRfq> refreshed = new ArrayList<>();
        for (InquiryLine line : inquiry.getLines()) {
            boolean outsourced = line.getSourcing() == ProductSourcing.OUTSOURCED;
            if (!outsourced && line.getSourcing() != ProductSourcing.IN_HOUSE) continue;
            String assigneeId = outsourced ? inquiry.getQcAssigneeId() : inquiry.getQaAssigneeId();
            String assigneeName = outsourced ? inquiry.getQcAssigneeName() : inquiry.getQaAssigneeName();
            if (assigneeId == null || assigneeId.isBlank()) continue;

            QaRfq rfq = existing.stream()
                    .filter(item -> line.getProductId().equals(item.getSourceProductId()))
                    .findFirst()
                    .orElseGet(() -> existing.stream()
                            .filter(item -> item.getSourceProductId() == null)
                            .filter(item -> line.getProductName().equals(item.getProductName()))
                            .findFirst()
                            .orElseGet(() -> {
                                QaRfq created = new QaRfq();
                                created.setRfqNo(nextRfqNo());
                                created.setInquiryId(inquiry.getId());
                                created.setCreatedOn(Instant.now());
                                created.setCreatedDate(inquiry.getInquiryDate() == null
                                        ? java.time.LocalDate.now().toString()
                                        : inquiry.getInquiryDate().toString());
                                return created;
                            }));

            rfq.setInquiryId(inquiry.getId());
            rfq.setSourceRfqNo(inquiry.getRfqNo());
            rfq.setSourceProductId(line.getProductId());
            rfq.setCustomerId(inquiry.getCustomerId());
            rfq.setCustomerName(inquiry.getCustomerName());
            rfq.setCustomerCode(client.getCustomerCode());
            rfq.setCustomerType(client.getCustomerType() == null ? null : client.getCustomerType().name());
            rfq.setProductName(line.getProductName());
            rfq.setBrandName(line.getProductName());
            rfq.setDosageForm(line.getDosageForm());
            rfq.setDosageVariant(line.getDosageVariant());
            rfq.setPharmacopeia(line.getPharmacopeia());
            rfq.setStandard(line.getPharmacopeia());
            rfq.setPriority(toQaPriority(inquiry.getPriority()));
            rfq.setAssignedToId(assigneeId);
            rfq.setAssignedToName(assigneeName);
            rfq.setAssignedBy(firstNonBlank(
                    inquiry.getSalesAssigneeName(), inquiry.getRaisedByUserName(), "Sales Team"));
            rfq.setDueDate(inquiry.getTargetQuoteDate() == null ? null : inquiry.getTargetQuoteDate().toString());
            rfq.setOrderQty(line.getQuantityRequired() == null ? null : line.getQuantityRequired().doubleValue());
            rfq.setTotalTablets(line.getCalculatedTabletQuantity() == null
                    ? null : line.getCalculatedTabletQuantity().doubleValue());
            if (rfq.getTargetBatchSize() == null && rfq.getTotalTablets() != null) {
                rfq.setTargetBatchSize(rfq.getTotalTablets());
            }
            rfq.setPackagingSpec(line.getPackagingNotes());
            rfq.setRemarks(inquiry.getNotes());
            rfq.setLastUpdatedOn(Instant.now());
            rfqRepository.save(rfq);
            refreshed.add(rfq);
            if (!existing.contains(rfq)) existing.add(rfq);
        }
        for (QaRfq stale : existing) {
            if (!refreshed.contains(stale)) {
                stale.setAssignedToId(null);
                stale.setAssignedToName(null);
                rfqRepository.save(stale);
            }
        }
    }

    private QaPriority toQaPriority(InquiryPriority priority) {
        if (priority == null) return QaPriority.MEDIUM;
        return priority == InquiryPriority.CRITICAL
                ? QaPriority.URGENT
                : QaPriority.valueOf(priority.name());
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) return value;
        }
        return null;
    }

    private String nextRfqNo() {
        String year = String.valueOf(Year.now().getValue());
        long count = rfqRepository.count();
        String rfqNo = String.format("RFQ-QA-%s-%04d", year, count + 1);
        while (rfqRepository.existsByRfqNo(rfqNo)) {
            rfqNo = String.format("RFQ-QA-%s-%04d", year, ++count + 1);
        }
        return rfqNo;
    }

    private void mapDtoToEntity(QaRfqRequestDto dto, QaRfq rfq) {
        if (dto.getInquiryId() != null) rfq.setInquiryId(dto.getInquiryId());
        if (dto.getCustomerId() != null) rfq.setCustomerId(dto.getCustomerId());
        if (dto.getCustomerName() != null) rfq.setCustomerName(dto.getCustomerName());
        if (dto.getCustomerCode() != null) rfq.setCustomerCode(dto.getCustomerCode());
        if (dto.getCustomerType() != null) rfq.setCustomerType(dto.getCustomerType());

        rfq.setProductName(dto.getProductName());
        rfq.setBrandName(dto.getBrandName());
        rfq.setDosageForm(dto.getDosageForm());
        rfq.setDosageVariant(dto.getDosageVariant());
        rfq.setCategory(dto.getCategory());
        rfq.setPharmacopeia(dto.getPharmacopeia());
        rfq.setStandard(dto.getStandard());

        if (dto.getStatus() != null) {
            rfq.setStatus(dto.getStatus());
        }
        if (dto.getPriority() != null) {
            rfq.setPriority(dto.getPriority());
        }

        if (dto.getAssignedToId() != null) rfq.setAssignedToId(dto.getAssignedToId());
        if (dto.getAssignedToName() != null) rfq.setAssignedToName(dto.getAssignedToName());
        rfq.setAssignedBy(dto.getAssignedBy() != null ? dto.getAssignedBy() : (rfq.getAssignedBy() != null ? rfq.getAssignedBy() : "Sales & Tech"));
        if (dto.getDueDate() != null) rfq.setDueDate(dto.getDueDate());
        if (dto.getCreatedDate() != null) {
            rfq.setCreatedDate(dto.getCreatedDate());
        } else if (rfq.getCreatedDate() == null) {
            rfq.setCreatedDate(java.time.LocalDate.now().toString());
        }

        // Process composition lines with auto overage calculation
        if (dto.getCompositionLines() != null) {
            List<QaCompositionLine> processedLines = new ArrayList<>();
            for (QaCompositionLine line : dto.getCompositionLines()) {
                Double overaged = calculatorService.calculateOveragedQty(line.getLabelClaim(), line.getOveragePercent());
                line.setOveragedQty(overaged);
                processedLines.add(line);
            }
            rfq.setCompositionLines(processedLines);
        }

        if (dto.getProducts() != null && !dto.getProducts().isEmpty()) {
            List<QaRfqProduct> products = new ArrayList<>();
            for (QaRfqProduct product : dto.getProducts()) {
                product.setId(product.getId() == null || product.getId().isBlank()
                        ? UUID.randomUUID().toString() : product.getId());
                List<QaCompositionLine> lines = product.getCompositionLines() == null
                        ? new ArrayList<>() : product.getCompositionLines();
                for (QaCompositionLine line : lines) {
                    line.setOveragedQty(calculatorService.calculateOveragedQty(line.getLabelClaim(), line.getOveragePercent()));
                }
                product.setCompositionLines(lines);
                if (product.getOrderQty() != null && product.getPackingSpecs() != null) {
                    product.setTotalTablets(product.getOrderQty() * parsePackingMultiplier(product.getPackingSpecs()));
                }
                products.add(product);
            }
            rfq.setProducts(products);
            // Keep legacy summary fields in sync with the first product for old clients.
            QaRfqProduct first = products.get(0);
            rfq.setProductName(first.getProductName());
            rfq.setDosageForm(first.getDosageForm());
            rfq.setStandard(first.getStandard());
            rfq.setCompositionLines(first.getCompositionLines());
            rfq.setOrderQty(first.getOrderQty());
            rfq.setPackingSpecs(first.getPackingSpecs());
            rfq.setTotalTablets(first.getTotalTablets());
        } else if ((rfq.getProducts() == null || rfq.getProducts().isEmpty()) && dto.getProductName() != null) {
            // Backward-compatible creation path: make the existing single product a line item.
            QaRfqProduct product = new QaRfqProduct();
            product.setId(UUID.randomUUID().toString());
            product.setProductName(dto.getProductName());
            product.setDosageForm(dto.getDosageForm());
            product.setStandard(dto.getStandard());
            product.setCompositionLines(rfq.getCompositionLines());
            product.setOrderQty(dto.getOrderQty());
            product.setPackingSpecs(dto.getPackingSpecs());
            if (dto.getOrderQty() != null && dto.getPackingSpecs() != null) {
                product.setTotalTablets(dto.getOrderQty() * parsePackingMultiplier(dto.getPackingSpecs()));
            }
            rfq.setProducts(List.of(product));
        }

        if (dto.getChangeParts() != null) {
            rfq.setChangeParts(dto.getChangeParts());
        }

        rfq.setPackagingSpec(dto.getPackagingSpec());
        if (dto.getProducts() == null || dto.getProducts().isEmpty()) {
            rfq.setOrderQty(dto.getOrderQty());
            rfq.setPackingSpecs(dto.getPackingSpecs());
            if (dto.getTotalTablets() != null && dto.getTotalTablets() > 0) {
                rfq.setTotalTablets(dto.getTotalTablets());
            } else if (dto.getOrderQty() != null && dto.getPackingSpecs() != null) {
                double multiplier = parsePackingMultiplier(dto.getPackingSpecs());
                rfq.setTotalTablets(dto.getOrderQty() * multiplier);
            }
        }

        Double targetBatch = dto.getTargetBatchSize();
        if ((targetBatch == null || targetBatch == 0) && rfq.getTotalTablets() != null && rfq.getTotalTablets() > 0) {
            targetBatch = rfq.getTotalTablets();
        }
        rfq.setTargetBatchSize(targetBatch);
        rfq.setBatchUnit(dto.getBatchUnit() != null ? dto.getBatchUnit() : (rfq.getBatchUnit() != null ? rfq.getBatchUnit() : "Tablets"));
        rfq.setRemarks(dto.getRemarks());
    }

    private double parsePackingMultiplier(String specs) {
        if (specs == null || specs.isBlank()) return 1.0;
        try {
            String[] parts = specs.toLowerCase().split("[x*]");
            double total = 1.0;
            for (String part : parts) {
                String clean = part.replaceAll("[^0-9.]", "").trim();
                if (!clean.isEmpty()) {
                    total *= Double.parseDouble(clean);
                }
            }
            return total > 0 ? total : 1.0;
        } catch (Exception e) {
            return 1.0;
        }
    }
}
