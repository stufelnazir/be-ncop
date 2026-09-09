package com.ncop.modules.qa.services;

import com.ncop.common.dto.PageResponse;
import com.ncop.modules.products.entity.Product;
import com.ncop.modules.products.entity.ProductIngredient;
import com.ncop.modules.products.repository.ProductRepository;
import com.ncop.modules.qa.dto.QaMfrRequestDto;
import com.ncop.modules.qa.entity.QaCompositionLine;
import com.ncop.modules.qa.entity.QaMfr;
import com.ncop.modules.qa.entity.QaMfrItem;
import com.ncop.modules.qa.entity.QaRfq;
import com.ncop.modules.qa.enums.QaMfrStatus;
import com.ncop.modules.qa.enums.QaRfqStatus;
import com.ncop.modules.qa.enums.QaStage;
import com.ncop.modules.qa.repository.QaMfrRepository;
import com.ncop.modules.qa.repository.QaRfqRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class QaMfrService {

    private final QaMfrRepository mfrRepository;
    private final QaRfqRepository rfqRepository;
    private final ProductRepository productRepository;
    private final QaFormulaCalculatorService calculatorService;

    public QaMfr createMfr(QaMfrRequestDto dto) {
        QaMfr mfr = new QaMfr();
        generateMfrNumber(mfr);

        mapDtoToEntity(dto, mfr);
        recalculateBatchQuantities(mfr);

        mfr.setCreatedOn(Instant.now());
        mfr.setLastUpdatedOn(Instant.now());

        QaMfr saved = mfrRepository.save(mfr);

        // If created from RFQ, update RFQ status to DRAFT_SAVED
        if (dto.getRfqId() != null) {
            rfqRepository.findById(dto.getRfqId()).ifPresent(rfq -> {
                rfq.setStatus(QaRfqStatus.DRAFT_SAVED);
                rfq.setLastUpdatedOn(Instant.now());
                rfqRepository.save(rfq);
            });
        }

        return saved;
    }

    public QaMfr createMfrFromRfq(String rfqId, Double initialBatchSize, String batchUnit) {
        QaRfq rfq = rfqRepository.findById(rfqId)
                .orElseThrow(() -> new RuntimeException("RFQ not found: " + rfqId));

        QaMfr mfr = new QaMfr();
        generateMfrNumber(mfr);

        mfr.setRfqId(rfq.getId());
        mfr.setRfqNo(rfq.getRfqNo());
        mfr.setProductName(rfq.getProductName() != null ? rfq.getProductName() : rfq.getBrandName());
        mfr.setDosageForm(rfq.getDosageForm());
        mfr.setDosageVariant(rfq.getDosageVariant());
        mfr.setStandard(rfq.getStandard() != null ? rfq.getStandard() : rfq.getPharmacopeia());

        double bSize = (initialBatchSize != null && initialBatchSize > 0) ? initialBatchSize :
                (rfq.getTargetBatchSize() != null && rfq.getTargetBatchSize() > 0 ? rfq.getTargetBatchSize() : 100000.0);
        mfr.setBatchSize(bSize);
        mfr.setBatchUnit(batchUnit != null ? batchUnit : (rfq.getBatchUnit() != null ? rfq.getBatchUnit() : "Tablets"));
        mfr.setStatus(QaMfrStatus.DRAFT);
        mfr.setProposedShelfLifeYears(2.0);

        if (rfq.getChangeParts() != null) {
            mfr.setChangeParts(rfq.getChangeParts());
        }

        // Convert RFQ active ingredients to MFR Items in ACTIVE stage
        List<QaMfrItem> items = new ArrayList<>();
        if (rfq.getCompositionLines() != null) {
            int codeIndex = 1;
            for (QaCompositionLine cLine : rfq.getCompositionLines()) {
                QaMfrItem item = new QaMfrItem();
                item.setStage(QaStage.ACTIVE);
                item.setItemCode(String.format("RM-ACT-%03d", codeIndex++));
                item.setMaterialName(cLine.getApi());
                item.setGrade(cLine.getPharmacopeia());
                item.setLabelClaim(cLine.getLabelClaim());
                item.setClaimUnit(cLine.getClaimUnit() != null ? cLine.getClaimUnit() : "mg");
                item.setOveragePercent(cLine.getOveragePercent());

                Double overagedPerUnit = calculatorService.calculateOveragedQty(cLine.getLabelClaim(), cLine.getOveragePercent());
                item.setOveragedQtyPerUnit(overagedPerUnit);

                Double batchQty = calculatorService.calculateQtyPerBatch(bSize, overagedPerUnit, item.getClaimUnit(), "kg");
                item.setQtyPerBatch(batchQty);
                item.setBatchUnit("kg");
                item.setFunctionCategory(cLine.getFunctionCategory());
                item.setNotes(cLine.getReasonForOverage());
                items.add(item);
            }
        }
        mfr.setItems(items);
        mfr.setCreatedOn(Instant.now());
        mfr.setLastUpdatedOn(Instant.now());

        QaMfr saved = mfrRepository.save(mfr);

        rfq.setStatus(QaRfqStatus.DRAFT_SAVED);
        rfq.setLastUpdatedOn(Instant.now());
        rfqRepository.save(rfq);

        return saved;
    }

    public QaMfr cloneFromProductOrMfr(String targetType, String targetId, String rfqId) {
        QaMfr mfr = new QaMfr();
        generateMfrNumber(mfr);

        QaRfq rfq = null;
        if (rfqId != null && !rfqId.isBlank()) {
            rfq = rfqRepository.findById(rfqId).orElse(null);
            if (rfq != null) {
                mfr.setRfqId(rfq.getId());
                mfr.setRfqNo(rfq.getRfqNo());
                mfr.setChangeParts(rfq.getChangeParts());
            }
        }

        double bSize = (rfq != null && rfq.getTargetBatchSize() != null && rfq.getTargetBatchSize() > 0)
                ? rfq.getTargetBatchSize() : 100000.0;
        mfr.setBatchSize(bSize);
        mfr.setBatchUnit(rfq != null && rfq.getBatchUnit() != null ? rfq.getBatchUnit() : "Tablets");
        mfr.setStatus(QaMfrStatus.DRAFT);

        List<QaMfrItem> items = new ArrayList<>();

        if ("PRODUCT".equalsIgnoreCase(targetType)) {
            Product product = productRepository.findById(targetId)
                    .orElseThrow(() -> new RuntimeException("Product not found: " + targetId));

            mfr.setProductName(product.getBrandName());
            mfr.setDosageForm(product.getDosageForm());
            mfr.setDosageVariant(product.getDosageVariant());

            if (product.getIngredients() != null) {
                int codeIndex = 1;
                for (ProductIngredient ing : product.getIngredients()) {
                    QaMfrItem item = new QaMfrItem();
                    item.setStage(QaStage.ACTIVE);
                    item.setItemCode(String.format("RM-ACT-%03d", codeIndex++));
                    item.setMaterialName(ing.getApi());
                    item.setGrade(ing.getPharmacopeia() != null ? ing.getPharmacopeia() : "IP");

                    double strengthVal = 0.0;
                    if (ing.getStrength() != null) {
                        try {
                            strengthVal = Double.parseDouble(ing.getStrength().replaceAll("[^0-9.]", ""));
                        } catch (Exception ignored) {}
                    }
                    item.setLabelClaim(strengthVal);
                    item.setClaimUnit(ing.getUnit() != null ? ing.getUnit() : "mg");
                    item.setOveragePercent(0.0);
                    item.setOveragedQtyPerUnit(strengthVal);

                    Double batchQty = calculatorService.calculateQtyPerBatch(bSize, strengthVal, item.getClaimUnit(), "kg");
                    item.setQtyPerBatch(batchQty);
                    item.setBatchUnit("kg");
                    item.setFunctionCategory("Active Pharmaceutical Ingredient");
                    items.add(item);
                }
            }
        } else if ("MFR".equalsIgnoreCase(targetType)) {
            QaMfr sourceMfr = mfrRepository.findById(targetId)
                    .orElseThrow(() -> new RuntimeException("Source MFR not found: " + targetId));

            mfr.setProductName(sourceMfr.getProductName());
            mfr.setDosageForm(sourceMfr.getDosageForm());
            mfr.setDosageVariant(sourceMfr.getDosageVariant());
            mfr.setStandard(sourceMfr.getStandard());
            mfr.setTheoreticalYield(sourceMfr.getTheoreticalYield());
            if (sourceMfr.getChangeParts() != null) {
                mfr.setChangeParts(sourceMfr.getChangeParts());
            }

            if (sourceMfr.getItems() != null) {
                for (QaMfrItem sItem : sourceMfr.getItems()) {
                    QaMfrItem copy = new QaMfrItem();
                    copy.setStage(sItem.getStage());
                    copy.setItemCode(sItem.getItemCode());
                    copy.setMaterialName(sItem.getMaterialName());
                    copy.setGrade(sItem.getGrade());
                    copy.setLabelClaim(sItem.getLabelClaim());
                    copy.setClaimUnit(sItem.getClaimUnit());
                    copy.setOveragePercent(sItem.getOveragePercent());
                    copy.setOveragedQtyPerUnit(sItem.getOveragedQtyPerUnit());

                    Double batchQty = calculatorService.calculateQtyPerBatch(bSize, sItem.getOveragedQtyPerUnit(), sItem.getClaimUnit(), "kg");
                    copy.setQtyPerBatch(batchQty);
                    copy.setBatchUnit("kg");
                    copy.setFunctionCategory(sItem.getFunctionCategory());
                    copy.setNotes(sItem.getNotes());
                    items.add(copy);
                }
            }
        }

        mfr.setItems(items);
        mfr.setCreatedOn(Instant.now());
        mfr.setLastUpdatedOn(Instant.now());

        QaMfr saved = mfrRepository.save(mfr);

        if (rfq != null) {
            rfq.setStatus(QaRfqStatus.DRAFT_SAVED);
            rfq.setLastUpdatedOn(Instant.now());
            rfqRepository.save(rfq);
        }

        return saved;
    }

    public PageResponse<QaMfr> getMfrs(Pageable pageable, String search, QaMfrStatus status) {
        List<QaMfr> all = mfrRepository.findAll();

        List<QaMfr> filtered = all.stream().filter(m -> {
            if (status != null && m.getStatus() != status) return false;
            if (search == null || search.isBlank()) return true;

            String q = search.toLowerCase();
            return (m.getMfrNo() != null && m.getMfrNo().toLowerCase().contains(q)) ||
                   (m.getRfqNo() != null && m.getRfqNo().toLowerCase().contains(q)) ||
                   (m.getProductName() != null && m.getProductName().toLowerCase().contains(q)) ||
                   (m.getDosageForm() != null && m.getDosageForm().toLowerCase().contains(q));
        }).toList();

        int pageSize = pageable.getPageSize();
        int pageNumber = pageable.getPageNumber();
        int fromIndex = Math.min(pageNumber * pageSize, filtered.size());
        int toIndex = Math.min(fromIndex + pageSize, filtered.size());
        List<QaMfr> paged = filtered.subList(fromIndex, toIndex);

        return PageResponse.of(paged, pageNumber, pageSize, filtered.size());
    }

    public Optional<QaMfr> getMfrById(String id) {
        return mfrRepository.findById(id);
    }

    public QaMfr updateMfr(String id, QaMfrRequestDto dto) {
        QaMfr mfr = mfrRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("MFR not found with id: " + id));

        mapDtoToEntity(dto, mfr);
        recalculateBatchQuantities(mfr);
        mfr.setLastUpdatedOn(Instant.now());

        return mfrRepository.save(mfr);
    }

    public QaMfr submitMfr(String id, String user) {
        QaMfr mfr = mfrRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("MFR not found: " + id));

        mfr.setStatus(QaMfrStatus.SUBMITTED);
        mfr.setApprovedBy(user);
        mfr.setLastUpdatedOn(Instant.now());

        QaMfr saved = mfrRepository.save(mfr);

        if (mfr.getRfqId() != null) {
            rfqRepository.findById(mfr.getRfqId()).ifPresent(rfq -> {
                rfq.setStatus(QaRfqStatus.COMPLETED);
                rfq.setLastUpdatedOn(Instant.now());
                rfqRepository.save(rfq);
            });
        }

        return saved;
    }

    public void deleteMfr(String id) {
        mfrRepository.findById(id).ifPresent(mfrRepository::delete);
    }

    private void generateMfrNumber(QaMfr mfr) {
        String year = String.valueOf(Year.now().getValue());
        long count = mfrRepository.count();
        String mfrNo = String.format("MFR-%s-%04d", year, count + 1);
        while (mfrRepository.existsByMfrNo(mfrNo)) {
            count++;
            mfrNo = String.format("MFR-%s-%04d", year, count + 1);
        }
        mfr.setMfrNo(mfrNo);
    }

    private void recalculateBatchQuantities(QaMfr mfr) {
        if (mfr.getItems() == null || mfr.getBatchSize() == null || mfr.getBatchSize() <= 0) {
            return;
        }

        for (QaMfrItem item : mfr.getItems()) {
            Double overagedPerUnit = calculatorService.calculateOveragedQty(item.getLabelClaim(), item.getOveragePercent());
            item.setOveragedQtyPerUnit(overagedPerUnit);

            String bUnit = item.getBatchUnit() != null ? item.getBatchUnit() : "kg";
            Double batchQty = calculatorService.calculateQtyPerBatch(mfr.getBatchSize(), overagedPerUnit, item.getClaimUnit(), bUnit);
            item.setQtyPerBatch(batchQty);
        }
    }

    private void mapDtoToEntity(QaMfrRequestDto dto, QaMfr mfr) {
        if (dto.getRfqId() != null) mfr.setRfqId(dto.getRfqId());
        if (dto.getRfqNo() != null) mfr.setRfqNo(dto.getRfqNo());
        if (dto.getProductName() != null) mfr.setProductName(dto.getProductName());
        if (dto.getDosageForm() != null) mfr.setDosageForm(dto.getDosageForm());
        if (dto.getDosageVariant() != null) mfr.setDosageVariant(dto.getDosageVariant());
        if (dto.getStandard() != null) mfr.setStandard(dto.getStandard());

        if (dto.getBatchSize() != null && dto.getBatchSize() > 0) {
            mfr.setBatchSize(dto.getBatchSize());
        }
        if (dto.getBatchUnit() != null) {
            mfr.setBatchUnit(dto.getBatchUnit());
        }
        if (dto.getTheoreticalYield() != null) {
            mfr.setTheoreticalYield(dto.getTheoreticalYield());
        }
        if (dto.getProposedShelfLifeYears() != null) {
            mfr.setProposedShelfLifeYears(dto.getProposedShelfLifeYears());
        }
        if (dto.getTabletColour() != null) {
            mfr.setTabletColour(dto.getTabletColour());
        }
        if (dto.getCoatingPercentage() != null) {
            mfr.setCoatingPercentage(dto.getCoatingPercentage());
        }
        if (dto.getUncoatedAvgWeightMg() != null) {
            mfr.setUncoatedAvgWeightMg(dto.getUncoatedAvgWeightMg());
        }
        if (dto.getCoatedAvgWeightMg() != null) {
            mfr.setCoatedAvgWeightMg(dto.getCoatedAvgWeightMg());
        }
        if (dto.getStatus() != null) {
            mfr.setStatus(dto.getStatus());
        }
        if (dto.getItems() != null) {
            mfr.setItems(dto.getItems());
        }
        if (dto.getChangeParts() != null) {
            mfr.setChangeParts(dto.getChangeParts());
        }
        if (dto.getRemarks() != null) mfr.setRemarks(dto.getRemarks());
        if (dto.getCreatedBy() != null) mfr.setCreatedBy(dto.getCreatedBy());
        if (dto.getApprovedBy() != null) mfr.setApprovedBy(dto.getApprovedBy());
    }
}
