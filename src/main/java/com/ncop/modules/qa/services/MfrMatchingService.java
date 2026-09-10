package com.ncop.modules.qa.services;

import com.ncop.modules.products.entity.Product;
import com.ncop.modules.products.entity.ProductIngredient;
import com.ncop.modules.products.repository.ProductRepository;
import com.ncop.modules.qa.dto.MfrMatchResultDto;
import com.ncop.modules.qa.entity.QaCompositionLine;
import com.ncop.modules.qa.entity.QaMfr;
import com.ncop.modules.qa.entity.QaMfrItem;
import com.ncop.modules.qa.entity.QaRfq;
import com.ncop.modules.qa.enums.QaMfrStatus;
import com.ncop.modules.qa.repository.QaMfrRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
@RequiredArgsConstructor
public class MfrMatchingService {

    private final ProductRepository productRepository;
    private final QaMfrRepository qaMfrRepository;

    public List<MfrMatchResultDto> findMatchesForRfq(QaRfq rfq) {
        List<MfrMatchResultDto> results = new ArrayList<>();

        String searchProductName = rfq.getProductName() != null ? rfq.getProductName() : (rfq.getBrandName() != null ? rfq.getBrandName() : "");
        String searchDosage = rfq.getDosageForm() != null ? rfq.getDosageForm() : "";
        String searchStandard = rfq.getStandard() != null ? rfq.getStandard() : (rfq.getPharmacopeia() != null ? rfq.getPharmacopeia() : "");
        List<QaCompositionLine> rfqIngredients = rfq.getCompositionLines() != null ? rfq.getCompositionLines() : Collections.emptyList();

        // 1. Search against Product Catalogue
        List<Product> products = productRepository.findAll();
        for (Product product : products) {
            MfrMatchResultDto match = scoreProductMatch(product, searchProductName, searchDosage, searchStandard, rfqIngredients);
            if (match.getTotalScore() > 15.0) {
                results.add(match);
            }
        }

        // 2. Search against Existing Approved / Draft MFRs
        List<QaMfr> mfrs = qaMfrRepository.findAll();
        for (QaMfr mfr : mfrs) {
            // Avoid matching with itself
            if (rfq.getId() != null && rfq.getId().equals(mfr.getRfqId())) {
                continue;
            }
            MfrMatchResultDto match = scoreMfrMatch(
                    mfr,
                    rfq,
                    searchProductName,
                    searchDosage,
                    searchStandard,
                    rfqIngredients
            );
            if (match.getTotalScore() > 15.0) {
                results.add(match);
            }
        }

        // Sort descending by total score
        results.sort((a, b) -> Double.compare(b.getTotalScore(), a.getTotalScore()));

        // Return top 15 results
        if (results.size() > 15) {
            return new ArrayList<>(results.subList(0, 15));
        }
        return results;
    }

    private MfrMatchResultDto scoreProductMatch(Product p, String rfqName, String rfqDosage, String rfqStandard, List<QaCompositionLine> rfqIngredients) {
        double pNameScore = computeStringSimilarity(rfqName, p.getBrandName()) * 35.0;

        List<String> matched = new ArrayList<>();
        double compScore = 0.0;
        double strScore = 0.0;

        List<ProductIngredient> pIngs = p.getIngredients() != null ? p.getIngredients() : Collections.emptyList();
        if (!rfqIngredients.isEmpty() && !pIngs.isEmpty()) {
            int matchedCount = 0;
            int strengthMatchCount = 0;

            for (QaCompositionLine rIng : rfqIngredients) {
                if (rIng.getApi() == null || rIng.getApi().isBlank()) continue;
                String rApi = rIng.getApi().trim().toLowerCase();

                for (ProductIngredient pIng : pIngs) {
                    if (pIng.getApi() == null) continue;
                    String pApi = pIng.getApi().trim().toLowerCase();

                    if (pApi.contains(rApi) || rApi.contains(pApi) || computeStringSimilarity(rApi, pApi) > 0.7) {
                        matchedCount++;
                        matched.add(pIng.getApi());

                        // Check strength
                        if (rIng.getLabelClaim() != null && pIng.getStrength() != null) {
                            try {
                                double pStr = Double.parseDouble(pIng.getStrength().replaceAll("[^0-9.]", ""));
                                if (Math.abs(pStr - rIng.getLabelClaim()) < 0.01) {
                                    strengthMatchCount++;
                                }
                            } catch (Exception ignored) {}
                        }
                        break;
                    }
                }
            }

            compScore = ((double) matchedCount / Math.max(rfqIngredients.size(), 1)) * 30.0;
            strScore = ((double) strengthMatchCount / Math.max(rfqIngredients.size(), 1)) * 15.0;
        }

        double dosageScore = computeDosageScore(rfqDosage, p.getDosageForm());
        double standardScore = (rfqStandard != null && !rfqStandard.isBlank() && p.getComposition() != null && p.getComposition().toUpperCase().contains(rfqStandard.toUpperCase())) ? 5.0 : 2.5;
        double statusScore = 5.0; // Products in master catalogue are active

        double total = roundTo(pNameScore + compScore + strScore + dosageScore + standardScore + statusScore, 1);

        MfrMatchResultDto dto = new MfrMatchResultDto();
        dto.setTargetType("PRODUCT");
        dto.setTargetId(p.getId());
        dto.setTargetCode(p.getProductCode());
        dto.setTargetName(p.getBrandName());
        dto.setDosageForm(p.getDosageForm());
        dto.setDosageVariant(p.getDosageVariant());
        dto.setComposition(p.getComposition());
        dto.setStatus("ACTIVE");
        dto.setTotalScore(total);
        dto.setProductNameScore(roundTo(pNameScore, 1));
        dto.setCompositionScore(roundTo(compScore, 1));
        dto.setStrengthScore(roundTo(strScore, 1));
        dto.setDosageFormScore(roundTo(dosageScore, 1));
        dto.setStandardScore(roundTo(standardScore, 1));
        dto.setStatusScore(roundTo(statusScore, 1));
        dto.setBatchSizeScore(0.0);
        dto.setMatchedIngredients(matched);

        return dto;
    }

    private MfrMatchResultDto scoreMfrMatch(
            QaMfr mfr,
            QaRfq rfq,
            String rfqName,
            String rfqDosage,
            String rfqStandard,
            List<QaCompositionLine> rfqIngredients
    ) {
        double pNameScore = computeStringSimilarity(rfqName, mfr.getProductName()) * 35.0;

        List<String> matched = new ArrayList<>();
        double compScore = 0.0;
        double strScore = 0.0;

        List<QaMfrItem> mfrItems = mfr.getItems() != null ? mfr.getItems() : Collections.emptyList();
        if (!rfqIngredients.isEmpty() && !mfrItems.isEmpty()) {
            int matchedCount = 0;
            int strengthMatchCount = 0;

            for (QaCompositionLine rIng : rfqIngredients) {
                if (rIng.getApi() == null || rIng.getApi().isBlank()) continue;
                String rApi = rIng.getApi().trim().toLowerCase();

                for (QaMfrItem mItem : mfrItems) {
                    if (mItem.getMaterialName() == null) continue;
                    String mMat = mItem.getMaterialName().trim().toLowerCase();

                    if (mMat.contains(rApi) || rApi.contains(mMat) || computeStringSimilarity(rApi, mMat) > 0.7) {
                        matchedCount++;
                        matched.add(mItem.getMaterialName());

                        if (rIng.getLabelClaim() != null && mItem.getLabelClaim() != null) {
                            if (Math.abs(mItem.getLabelClaim() - rIng.getLabelClaim()) < 0.01) {
                                strengthMatchCount++;
                            }
                        }
                        break;
                    }
                }
            }

            compScore = ((double) matchedCount / Math.max(rfqIngredients.size(), 1)) * 30.0;
            strScore = ((double) strengthMatchCount / Math.max(rfqIngredients.size(), 1)) * 15.0;
        }

        double dosageScore = computeDosageScore(rfqDosage, mfr.getDosageForm());
        double standardScore = (rfqStandard != null && !rfqStandard.isBlank() && mfr.getStandard() != null && mfr.getStandard().equalsIgnoreCase(rfqStandard)) ? 5.0 : 2.5;
        double statusScore = mfr.getStatus() == QaMfrStatus.APPROVED ? 5.0 : 3.0;
        double batchSizeScore = computeBatchSizeScore(rfq.getTargetBatchSize(), mfr.getBatchSize());

        double total = roundTo(pNameScore + compScore + strScore + dosageScore + standardScore + statusScore + batchSizeScore, 1);

        MfrMatchResultDto dto = new MfrMatchResultDto();
        dto.setTargetType("MFR");
        dto.setTargetId(mfr.getId());
        dto.setTargetCode(mfr.getMfrNo());
        dto.setTargetName(mfr.getProductName());
        dto.setDosageForm(mfr.getDosageForm());
        dto.setDosageVariant(mfr.getDosageVariant());
        dto.setComposition(mfr.getProductName() + " (" + (mfr.getStandard() != null ? mfr.getStandard() : "Standard") + ")");
        dto.setBatchSize(mfr.getBatchSize());
        dto.setBatchUnit(mfr.getBatchUnit());
        dto.setStatus(mfr.getStatus() == null ? null : mfr.getStatus().name());
        dto.setTotalScore(total);
        dto.setProductNameScore(roundTo(pNameScore, 1));
        dto.setCompositionScore(roundTo(compScore, 1));
        dto.setStrengthScore(roundTo(strScore, 1));
        dto.setDosageFormScore(roundTo(dosageScore, 1));
        dto.setStandardScore(roundTo(standardScore, 1));
        dto.setStatusScore(roundTo(statusScore, 1));
        dto.setBatchSizeScore(roundTo(batchSizeScore, 1));
        dto.setMatchedIngredients(matched);

        return dto;
    }

    private double computeDosageScore(String dosageA, String dosageB) {
        if (dosageA == null || dosageB == null || dosageA.isBlank() || dosageB.isBlank()) return 5.0;
        if (dosageA.trim().equalsIgnoreCase(dosageB.trim())) return 10.0;
        String a = dosageA.toLowerCase();
        String b = dosageB.toLowerCase();
        if ((a.contains("tab") && b.contains("tab")) ||
            (a.contains("cap") && b.contains("cap")) ||
            (a.contains("syr") && b.contains("syr")) ||
            (a.contains("inj") && b.contains("inj"))) {
            return 7.5;
        }
        return 0.0;
    }

    private double computeBatchSizeScore(Double requested, Double candidate) {
        if (requested == null || requested <= 0 || candidate == null || candidate <= 0) return 2.5;
        double difference = Math.abs(requested - candidate) / Math.max(requested, candidate);
        if (difference <= 0.05) return 5.0;
        if (difference <= 0.25) return 3.0;
        return 1.0;
    }

    private double computeStringSimilarity(String s1, String s2) {
        if (s1 == null || s2 == null) return 0.0;
        String a = s1.trim().toLowerCase();
        String b = s2.trim().toLowerCase();
        if (a.isEmpty() || b.isEmpty()) return 0.0;
        if (a.equals(b)) return 1.0;
        if (a.contains(b) || b.contains(a)) return 0.85;

        // Token Jaccard similarity
        Set<String> setA = new HashSet<>(Arrays.asList(a.split("\\s+")));
        Set<String> setB = new HashSet<>(Arrays.asList(b.split("\\s+")));
        Set<String> union = new HashSet<>(setA);
        union.addAll(setB);
        Set<String> intersect = new HashSet<>(setA);
        intersect.retainAll(setB);

        if (union.isEmpty()) return 0.0;
        return (double) intersect.size() / union.size();
    }

    private Double roundTo(double value, int places) {
        if (Double.isNaN(value) || Double.isInfinite(value)) return 0.0;
        BigDecimal bd = BigDecimal.valueOf(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }
}
