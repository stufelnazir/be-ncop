package com.ncop.modules.clientmaster.entity;

import java.util.Date;

import com.ncop.modules.clientmaster.enums.DocumentType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ClientDocument {

    private String id;

    private DocumentType documentType;

    private String fileName;

    private String originalFileName;

    private String contentType;

    private Long fileSize;

    private String fileUrl;

    private String storageType; // "GCS" or "LOCAL"

    private String storagePath; // GCS object name or local file path

    private Date uploadedAt = new Date();

    private Client client;

    public ClientDocument() {}
}