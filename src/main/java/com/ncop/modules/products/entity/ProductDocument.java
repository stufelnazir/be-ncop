package com.ncop.modules.products.entity;

import com.ncop.modules.products.enums.ProductDocumentType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductDocument {
    private String id;
    private ProductDocumentType documentType;
    private String fileName;
    private String originalFileName;
    private String contentType;
    private Long fileSize;
    private String fileUrl;
    private String storageType; // "GCS" or "LOCAL"
    private String storagePath;
    private Date uploadedAt = new Date();
}
