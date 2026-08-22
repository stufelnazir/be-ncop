package com.ncop.modules.products.services;

import com.ncop.auth.exception.DuplicateResourceException;
import com.ncop.auth.exception.ResourceNotFoundException;
import com.ncop.common.dto.PageResponse;
import com.ncop.modules.clients.services.FileStorageService;
import com.ncop.modules.products.dto.ProductIngredientDto;
import com.ncop.modules.products.dto.ProductRequestDto;
import com.ncop.modules.products.dto.ProductResponseDto;
import com.ncop.modules.products.entity.Product;
import com.ncop.modules.products.entity.ProductDocument;
import com.ncop.modules.products.entity.ProductIngredient;
import com.ncop.modules.products.enums.ProductDocumentType;
import com.ncop.modules.products.enums.ProductStatus;
import com.ncop.modules.products.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;
    private final FileStorageService fileStorageService;

    public PageResponse<ProductResponseDto> getProducts(Pageable pageable, String search, String category, String dosageForm, String status) {
        List<Product> all = productRepository.findAll();

        List<ProductResponseDto> filtered = all.stream()
                .filter(p -> {
                    // Search
                    if (StringUtils.hasText(search)) {
                        String q = search.toLowerCase().trim();
                        boolean matchName = p.getBrandName() != null && p.getBrandName().toLowerCase().contains(q);
                        boolean matchCode = p.getProductCode() != null && p.getProductCode().toLowerCase().contains(q);
                        boolean matchCategory = p.getCategory() != null && p.getCategory().toLowerCase().contains(q);
                        boolean matchComposition = p.getComposition() != null && p.getComposition().toLowerCase().contains(q);
                        boolean matchIngredients = p.getIngredients() != null && p.getIngredients().stream()
                                .anyMatch(i -> i.getApi() != null && i.getApi().toLowerCase().contains(q));

                        if (!matchName && !matchCode && !matchCategory && !matchComposition && !matchIngredients) {
                            return false;
                        }
                    }

                    // Category Filter
                    if (StringUtils.hasText(category) && !"ALL".equalsIgnoreCase(category)) {
                        if (p.getCategory() == null || !p.getCategory().equalsIgnoreCase(category)) {
                            return false;
                        }
                    }

                    // Dosage Form Filter
                    if (StringUtils.hasText(dosageForm) && !"ALL".equalsIgnoreCase(dosageForm)) {
                        if (p.getDosageForm() == null || !p.getDosageForm().equalsIgnoreCase(dosageForm)) {
                            return false;
                        }
                    }

                    // Status Filter
                    if (StringUtils.hasText(status) && !"ALL".equalsIgnoreCase(status)) {
                        if (p.getStatus() == null || !p.getStatus().name().equalsIgnoreCase(status)) {
                            return false;
                        }
                    }

                    return true;
                })
                .sorted(Comparator.comparing(Product::getCreatedOn, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::toResponseDto)
                .toList();

        int pageSize = pageable.getPageSize();
        int pageNumber = pageable.getPageNumber();
        int fromIndex = Math.min(pageNumber * pageSize, filtered.size());
        int toIndex = Math.min(fromIndex + pageSize, filtered.size());
        List<ProductResponseDto> paged = filtered.subList(fromIndex, toIndex);

        return PageResponse.of(paged, pageNumber, pageSize, filtered.size());
    }

    public List<ProductResponseDto> getAllProducts() {
        return productRepository.findAll().stream()
                .sorted(Comparator.comparing(Product::getBrandName, Comparator.nullsLast(String::compareToIgnoreCase)))
                .map(this::toResponseDto)
                .toList();
    }

    public ProductResponseDto getProductById(String id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        return toResponseDto(product);
    }

    public ProductResponseDto createProduct(ProductRequestDto request) {
        Product product = new Product();

        // 1. Auto-generate product code if not provided
        String productCode = request.getProductCode();
        if (!StringUtils.hasText(productCode)) {
            long count = productRepository.count();
            productCode = String.format("PROD-%06d", count + 1);
            while (productRepository.existsByProductCode(productCode)) {
                count++;
                productCode = String.format("PROD-%06d", count + 1);
            }
        } else {
            productCode = productCode.trim().toUpperCase();
            if (productRepository.existsByProductCode(productCode)) {
                throw new DuplicateResourceException("Product code \"" + productCode + "\" already exists");
            }
        }
        product.setProductCode(productCode);

        // 2. Set basic details
        product.setBrandName(request.getBrandName().trim());
        product.setCategory(request.getCategory() != null ? request.getCategory().trim() : "General");
        product.setTherapeuticClass(request.getTherapeuticClass() != null ? request.getTherapeuticClass().trim() : "");
        product.setDosageForm(request.getDosageForm().trim());
        product.setDosageVariant(request.getDosageVariant() != null ? request.getDosageVariant().trim() : "");

        // 3. Set ingredients
        List<ProductIngredient> ingredients = mapIngredients(request.getIngredients());
        product.setIngredients(ingredients);

        // 4. Auto-compute composition formula
        if (StringUtils.hasText(request.getCustomComposition())) {
            product.setComposition(request.getCustomComposition().trim());
        } else {
            product.setComposition(computeComposition(ingredients, product.getDosageVariant(), product.getDosageForm()));
        }

        // 5. Commercial & packaging details
        product.setPackaging(request.getPackaging());
        product.setMoq(request.getMoq());
        product.setUnitPrice(request.getUnitPrice());
        product.setCurrency(StringUtils.hasText(request.getCurrency()) ? request.getCurrency().trim().toUpperCase() : "USD");
        product.setShelfLife(request.getShelfLife());
        product.setStorageCondition(request.getStorageCondition());
        product.setDescription(request.getDescription());
        product.setStatus(request.getStatus() != null ? request.getStatus() : ProductStatus.ACTIVE);
        product.setCreatedOn(Instant.now());
        product.setLastUpdatedOn(Instant.now());

        Product saved = productRepository.save(product);
        log.info("Created product: {} ({})", saved.getBrandName(), saved.getProductCode());
        return toResponseDto(saved);
    }

    public ProductResponseDto updateProduct(String id, ProductRequestDto request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));

        if (StringUtils.hasText(request.getProductCode())) {
            String newCode = request.getProductCode().trim().toUpperCase();
            if (!newCode.equalsIgnoreCase(product.getProductCode()) && productRepository.existsByProductCode(newCode)) {
                throw new DuplicateResourceException("Product code \"" + newCode + "\" already exists");
            }
            product.setProductCode(newCode);
        }

        product.setBrandName(request.getBrandName().trim());
        product.setCategory(request.getCategory() != null ? request.getCategory().trim() : "General");
        product.setTherapeuticClass(request.getTherapeuticClass() != null ? request.getTherapeuticClass().trim() : "");
        product.setDosageForm(request.getDosageForm().trim());
        product.setDosageVariant(request.getDosageVariant() != null ? request.getDosageVariant().trim() : "");

        List<ProductIngredient> ingredients = mapIngredients(request.getIngredients());
        product.setIngredients(ingredients);

        if (StringUtils.hasText(request.getCustomComposition())) {
            product.setComposition(request.getCustomComposition().trim());
        } else {
            product.setComposition(computeComposition(ingredients, product.getDosageVariant(), product.getDosageForm()));
        }

        product.setPackaging(request.getPackaging());
        product.setMoq(request.getMoq());
        product.setUnitPrice(request.getUnitPrice());
        if (StringUtils.hasText(request.getCurrency())) {
            product.setCurrency(request.getCurrency().trim().toUpperCase());
        }
        product.setShelfLife(request.getShelfLife());
        product.setStorageCondition(request.getStorageCondition());
        product.setDescription(request.getDescription());
        if (request.getStatus() != null) {
            product.setStatus(request.getStatus());
        }
        product.setLastUpdatedOn(Instant.now());

        Product saved = productRepository.save(product);
        return toResponseDto(saved);
    }

    public void deleteProduct(String id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));

        // Clean up documents from storage if any
        if (product.getDocuments() != null) {
            for (ProductDocument doc : product.getDocuments()) {
                // Remove from storage
                com.ncop.modules.clients.entity.ClientDocument cd = new com.ncop.modules.clients.entity.ClientDocument();
                cd.setStorageType(doc.getStorageType());
                cd.setStoragePath(doc.getStoragePath());
                cd.setFileName(doc.getFileName());
                fileStorageService.deleteFile(cd);
            }
        }

        productRepository.deleteById(id);
    }

    public ProductResponseDto uploadProductDocument(String id, MultipartFile file, ProductDocumentType documentType) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));

        FileStorageService.FileStorageResult result = fileStorageService.storeFile(file, id, documentType.name());
        if (result == null) {
            throw new RuntimeException("Failed to upload product document");
        }

        String docId = UUID.randomUUID().toString();
        String viewUrl = "/api/v1/products/" + id + "/documents/" + docId + "/view";

        ProductDocument doc = new ProductDocument();
        doc.setId(docId);
        doc.setDocumentType(documentType);
        doc.setFileName(result.getFileName());
        doc.setOriginalFileName(result.getOriginalFileName());
        doc.setContentType(result.getContentType());
        doc.setFileSize(result.getFileSize());
        doc.setStorageType(result.getStorageType());
        doc.setStoragePath(result.getStoragePath());
        doc.setFileUrl(viewUrl);
        doc.setUploadedAt(new Date());

        if (product.getDocuments() == null) {
            product.setDocuments(new ArrayList<>());
        }

        // Replace existing document of same type if present
        product.getDocuments().removeIf(d -> d.getDocumentType() == documentType);
        product.getDocuments().add(doc);

        Product saved = productRepository.save(product);
        return toResponseDto(saved);
    }

    public void deleteProductDocument(String id, String docId) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));

        if (product.getDocuments() != null) {
            Optional<ProductDocument> docOpt = product.getDocuments().stream()
                    .filter(d -> docId.equals(d.getId()))
                    .findFirst();

            if (docOpt.isPresent()) {
                ProductDocument doc = docOpt.get();
                com.ncop.modules.clients.entity.ClientDocument cd = new com.ncop.modules.clients.entity.ClientDocument();
                cd.setStorageType(doc.getStorageType());
                cd.setStoragePath(doc.getStoragePath());
                cd.setFileName(doc.getFileName());
                fileStorageService.deleteFile(cd);

                product.getDocuments().remove(doc);
                productRepository.save(product);
            }
        }
    }

    public Map<String, Long> getProductMetrics() {
        List<Product> all = productRepository.findAll();
        long total = all.size();
        long active = all.stream().filter(p -> p.getStatus() == ProductStatus.ACTIVE).count();
        long underDev = all.stream().filter(p -> p.getStatus() == ProductStatus.UNDER_DEVELOPMENT).count();
        long discontinued = all.stream().filter(p -> p.getStatus() == ProductStatus.DISCONTINUED).count();

        Map<String, Long> metrics = new HashMap<>();
        metrics.put("total", total);
        metrics.put("active", active);
        metrics.put("underDevelopment", underDev);
        metrics.put("discontinued", discontinued);
        return metrics;
    }

    // ── Helper methods ────────────────────────────────────────────────────────

    /**
     * Live compute pharmaceutical composition formula:
     * e.g. "Paracetamol 500mg + Diclofenac Potassium 50mg BP Sugar Free Syrup"
     */
    public String computeComposition(List<ProductIngredient> ingredients, String dosageVariant, String dosageForm) {
        if (ingredients == null || ingredients.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            if (StringUtils.hasText(dosageVariant)) sb.append(dosageVariant);
            else if (StringUtils.hasText(dosageForm)) sb.append(dosageForm);
            return sb.toString().trim();
        }

        StringBuilder formula = new StringBuilder();
        Set<String> pharmacopeias = new LinkedHashSet<>();

        for (int i = 0; i < ingredients.size(); i++) {
            ProductIngredient ing = ingredients.get(i);
            if (i > 0) {
                formula.append(" + ");
            }

            if (StringUtils.hasText(ing.getApi())) {
                formula.append(ing.getApi().trim());
            }

            if (StringUtils.hasText(ing.getStrength())) {
                formula.append(" ").append(ing.getStrength().trim());
                if (StringUtils.hasText(ing.getUnit())) {
                    formula.append(ing.getUnit().trim());
                }
            }

            if (StringUtils.hasText(ing.getPharmacopeia()) && !"NONE".equalsIgnoreCase(ing.getPharmacopeia()) && !"IN-HOUSE".equalsIgnoreCase(ing.getPharmacopeia())) {
                pharmacopeias.add(ing.getPharmacopeia().trim().toUpperCase());
            }
        }

        // Append pharmacopeia standard if unified
        if (!pharmacopeias.isEmpty()) {
            formula.append(" ").append(String.join("/", pharmacopeias));
        }

        // Append Dosage Variant or Form
        if (StringUtils.hasText(dosageVariant)) {
            formula.append(" ").append(dosageVariant.trim());
        } else if (StringUtils.hasText(dosageForm)) {
            formula.append(" ").append(dosageForm.trim());
        }

        return formula.toString().trim();
    }

    private List<ProductIngredient> mapIngredients(List<ProductIngredientDto> dtos) {
        if (dtos == null) return new ArrayList<>();
        return dtos.stream()
                .filter(d -> StringUtils.hasText(d.getApi()))
                .map(d -> new ProductIngredient(
                        d.getApi().trim(),
                        d.getStrength() != null ? d.getStrength().trim() : "",
                        d.getUnit() != null ? d.getUnit().trim() : "mg",
                        d.getPharmacopeia() != null ? d.getPharmacopeia().trim() : "BP"
                ))
                .collect(Collectors.toList());
    }

    private ProductResponseDto toResponseDto(Product p) {
        List<ProductIngredientDto> ingDtos = p.getIngredients() != null
                ? p.getIngredients().stream()
                .map(i -> new ProductIngredientDto(i.getApi(), i.getStrength(), i.getUnit(), i.getPharmacopeia()))
                .toList()
                : new ArrayList<>();

        return new ProductResponseDto(
                p.getId(),
                p.getProductCode(),
                p.getBrandName(),
                p.getCategory(),
                p.getTherapeuticClass(),
                p.getDosageForm(),
                p.getDosageVariant(),
                ingDtos,
                p.getComposition(),
                p.getPackaging(),
                p.getMoq(),
                p.getUnitPrice(),
                p.getCurrency(),
                p.getShelfLife(),
                p.getStorageCondition(),
                p.getDescription(),
                p.getStatus(),
                p.getDocuments() != null ? p.getDocuments() : new ArrayList<>(),
                p.getCreatedOn(),
                p.getLastUpdatedOn()
        );
    }
}
