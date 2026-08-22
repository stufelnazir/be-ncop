package com.ncop.modules.products.controller;

import com.ncop.common.dto.PageResponse;
import com.ncop.modules.clients.services.FileStorageService;
import com.ncop.modules.products.dto.ProductRequestDto;
import com.ncop.modules.products.dto.ProductResponseDto;
import com.ncop.modules.products.entity.ProductDocument;
import com.ncop.modules.products.enums.ProductDocumentType;
import com.ncop.modules.products.repository.ProductRepository;
import com.ncop.modules.products.services.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final ProductRepository productRepository;
    private final FileStorageService fileStorageService;

    @GetMapping
    public ResponseEntity<PageResponse<ProductResponseDto>> getProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String dosageForm,
            @RequestParam(required = false) String status) {

        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(productService.getProducts(pageable, search, category, dosageForm, status));
    }

    @GetMapping("/all")
    public ResponseEntity<List<ProductResponseDto>> getAllProducts() {
        return ResponseEntity.ok(productService.getAllProducts());
    }

    @GetMapping("/metrics")
    public ResponseEntity<Map<String, Long>> getProductMetrics() {
        return ResponseEntity.ok(productService.getProductMetrics());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponseDto> getProductById(@PathVariable String id) {
        return ResponseEntity.ok(productService.getProductById(id));
    }

    @PostMapping
    public ResponseEntity<ProductResponseDto> createProduct(@Valid @RequestBody ProductRequestDto request) {
        ProductResponseDto created = productService.createProduct(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductResponseDto> updateProduct(@PathVariable String id, @Valid @RequestBody ProductRequestDto request) {
        ProductResponseDto updated = productService.updateProduct(id, request);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable String id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }

    // ── Document Management ───────────────────────────────────────────────────

    @PostMapping("/{id}/documents")
    public ResponseEntity<ProductResponseDto> uploadDocument(
            @PathVariable String id,
            @RequestParam("file") MultipartFile file,
            @RequestParam("documentType") ProductDocumentType documentType) {
        ProductResponseDto updated = productService.uploadProductDocument(id, file, documentType);
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/{id}/documents/{docId}/view")
    public ResponseEntity<byte[]> viewDocument(@PathVariable String id, @PathVariable String docId) {
        ProductResponseDto product = productService.getProductById(id);
        Optional<ProductDocument> docOpt = product.getDocuments().stream()
                .filter(d -> docId.equals(d.getId()))
                .findFirst();

        if (docOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        ProductDocument doc = docOpt.get();
        com.ncop.modules.clients.entity.ClientDocument cd = new com.ncop.modules.clients.entity.ClientDocument();
        cd.setStorageType(doc.getStorageType());
        cd.setStoragePath(doc.getStoragePath());
        cd.setFileName(doc.getFileName());

        byte[] data = fileStorageService.loadFileBytes(cd);
        if (data == null) {
            return ResponseEntity.notFound().build();
        }

        MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
        if (doc.getContentType() != null) {
            try {
                mediaType = MediaType.parseMediaType(doc.getContentType());
            } catch (Exception ignored) {}
        }

        String displayName = doc.getOriginalFileName() != null ? doc.getOriginalFileName() : "document";
        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + displayName + "\"")
                .body(data);
    }

    @GetMapping("/{id}/documents/{docId}/download")
    public ResponseEntity<byte[]> downloadDocument(@PathVariable String id, @PathVariable String docId) {
        ProductResponseDto product = productService.getProductById(id);
        Optional<ProductDocument> docOpt = product.getDocuments().stream()
                .filter(d -> docId.equals(d.getId()))
                .findFirst();

        if (docOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        ProductDocument doc = docOpt.get();
        com.ncop.modules.clients.entity.ClientDocument cd = new com.ncop.modules.clients.entity.ClientDocument();
        cd.setStorageType(doc.getStorageType());
        cd.setStoragePath(doc.getStoragePath());
        cd.setFileName(doc.getFileName());

        byte[] data = fileStorageService.loadFileBytes(cd);
        if (data == null) {
            return ResponseEntity.notFound().build();
        }

        String displayName = doc.getOriginalFileName() != null ? doc.getOriginalFileName() : "document";
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + displayName + "\"")
                .body(data);
    }

    @DeleteMapping("/{id}/documents/{docId}")
    public ResponseEntity<Void> deleteDocument(@PathVariable String id, @PathVariable String docId) {
        productService.deleteProductDocument(id, docId);
        return ResponseEntity.noContent().build();
    }
}
