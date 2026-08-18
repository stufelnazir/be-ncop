package com.ncop.modules.clients.services;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.ncop.modules.clients.entity.ClientDocument;

import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class FileStorageService {

    @Value("${gcp.storage.bucket-name:}")
    private String gcsBucketName;

    @Value("${gcp.storage.project-id:}")
    private String gcsProjectId;

    @Value("${gcp.storage.credentials-path:}")
    private String gcsCredentialsPath;

    private Storage storage;

    private final Path fileStorageLocation = Paths.get("uploads/client-documents").toAbsolutePath().normalize();

    @Getter
    @Setter
    @AllArgsConstructor
    public static class FileStorageResult {
        private String fileName;
        private String originalFileName;
        private String contentType;
        private long fileSize;
        private String storageType; // "GCS" or "LOCAL"
        private String storagePath;
    }

    public FileStorageService() {
        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (Exception ex) {
            log.error("Could not create local uploads directory: {}", ex.getMessage());
        }
    }

    @PostConstruct
    public void init() {
        if (StringUtils.hasText(gcsBucketName)) {
            try {
                StorageOptions.Builder builder = StorageOptions.newBuilder();
                if (StringUtils.hasText(gcsProjectId)) {
                    builder.setProjectId(gcsProjectId);
                }
                if (StringUtils.hasText(gcsCredentialsPath)) {
                    Path credPath = Paths.get(gcsCredentialsPath);
                    if (Files.exists(credPath)) {
                        builder.setCredentials(GoogleCredentials.fromStream(Files.newInputStream(credPath)));
                    } else {
                        builder.setCredentials(GoogleCredentials.getApplicationDefault());
                    }
                } else {
                    builder.setCredentials(GoogleCredentials.getApplicationDefault());
                }
                this.storage = builder.build().getService();
                log.info("Google Cloud Storage initialized successfully for bucket: {}", gcsBucketName);
            } catch (Exception e) {
                log.warn("GCS credentials not available, using local disk storage: {}", e.getMessage());
                this.storage = null;
            }
        } else {
            log.info("GCS bucket name not configured. Defaulting to local storage at: {}", fileStorageLocation);
        }
    }

    /**
     * Store uploaded file in GCS if configured, else in local directory.
     */
    public FileStorageResult storeFile(MultipartFile file, String clientId, String documentType) {
        if (file == null || file.isEmpty()) {
            return null;
        }

        String rawName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "document";
        String originalFileName = StringUtils.cleanPath(rawName);

        if (originalFileName.contains("..")) {
            throw new RuntimeException("Filename contains invalid path sequence: " + originalFileName);
        }

        String fileExtension = "";
        if (originalFileName.contains(".")) {
            fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
        }

        String uniqueFileName = clientId + "_" + documentType + "_" + UUID.randomUUID().toString().substring(0, 8) + fileExtension;
        String contentType = file.getContentType() != null && !file.getContentType().isEmpty()
                ? file.getContentType()
                : detectContentType(fileExtension);
        long fileSize = file.getSize();

        // 1. Try Google Cloud Storage
        if (this.storage != null && StringUtils.hasText(gcsBucketName)) {
            try {
                String objectName = "clients/" + clientId + "/documents/" + uniqueFileName;
                BlobId blobId = BlobId.of(gcsBucketName, objectName);
                BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                        .setContentType(contentType)
                        .build();

                storage.create(blobInfo, file.getBytes());
                log.info("File uploaded to GCS bucket '{}' at path '{}'", gcsBucketName, objectName);

                return new FileStorageResult(
                        uniqueFileName,
                        originalFileName,
                        contentType,
                        fileSize,
                        "GCS",
                        objectName
                );
            } catch (Exception ex) {
                log.error("Failed uploading to GCS, falling back to local storage: {}", ex.getMessage());
            }
        }

        // 2. Fallback to Local Disk
        try {
            Path targetLocation = this.fileStorageLocation.resolve(uniqueFileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
            log.info("File saved to local storage at: {}", targetLocation);

            return new FileStorageResult(
                    uniqueFileName,
                    originalFileName,
                    contentType,
                    fileSize,
                    "LOCAL",
                    uniqueFileName
            );
        } catch (IOException ex) {
            throw new RuntimeException("Could not store file " + originalFileName + " locally!", ex);
        }
    }

    /**
     * Retrieve document bytes from GCS or local disk.
     */
    public byte[] loadFileBytes(ClientDocument document) {
        if (document == null) {
            return null;
        }

        // 1. Load from GCS
        if ("GCS".equalsIgnoreCase(document.getStorageType()) && this.storage != null && StringUtils.hasText(gcsBucketName)) {
            try {
                BlobId blobId = BlobId.of(gcsBucketName, document.getStoragePath());
                Blob blob = storage.get(blobId);
                if (blob != null && blob.exists()) {
                    return blob.getContent();
                }
                log.warn("Blob not found on GCS: {}", document.getStoragePath());
            } catch (Exception e) {
                log.error("Error reading file from GCS: {}", e.getMessage());
            }
        }

        // 2. Load from Local Disk
        try {
            Path filePath = null;
            if (document.getStoragePath() != null) {
                Path p = Paths.get(document.getStoragePath());
                filePath = p.isAbsolute() ? p : this.fileStorageLocation.resolve(document.getStoragePath());
            }
            if (filePath == null || !Files.exists(filePath)) {
                if (document.getFileName() != null) {
                    filePath = this.fileStorageLocation.resolve(document.getFileName());
                }
            }

            if (filePath != null && Files.exists(filePath)) {
                return Files.readAllBytes(filePath);
            }
            log.warn("Local file not found at: {}", filePath);
        } catch (Exception e) {
            log.error("Error reading local file: {}", e.getMessage());
        }

        return null;
    }

    /**
     * Delete document from GCS or local disk.
     */
    public boolean deleteFile(ClientDocument document) {
        if (document == null) {
            return true;
        }
        boolean deleted = false;

        if ("GCS".equalsIgnoreCase(document.getStorageType()) && this.storage != null && StringUtils.hasText(gcsBucketName)) {
            try {
                BlobId blobId = BlobId.of(gcsBucketName, document.getStoragePath());
                deleted = storage.delete(blobId);
                log.info("Deleted file from GCS: {}", document.getStoragePath());
            } catch (Exception e) {
                log.warn("Error deleting file from GCS: {}", e.getMessage());
            }
        }

        try {
            Path filePath = null;
            if (document.getStoragePath() != null) {
                Path p = Paths.get(document.getStoragePath());
                filePath = p.isAbsolute() ? p : this.fileStorageLocation.resolve(document.getStoragePath());
            }
            if (filePath == null || !Files.exists(filePath)) {
                if (document.getFileName() != null) {
                    filePath = this.fileStorageLocation.resolve(document.getFileName());
                }
            }
            if (filePath != null && Files.exists(filePath)) {
                Files.deleteIfExists(filePath);
                deleted = true;
                log.info("Deleted local file: {}", filePath);
            }
        } catch (Exception e) {
            log.warn("Error deleting local file: {}", e.getMessage());
        }

        return deleted;
    }

    private String detectContentType(String extension) {
        String ext = extension.toLowerCase();
        return switch (ext) {
            case ".pdf" -> "application/pdf";
            case ".png" -> "image/png";
            case ".jpg", ".jpeg" -> "image/jpeg";
            case ".webp" -> "image/webp";
            case ".doc" -> "application/msword";
            case ".docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            default -> "application/octet-stream";
        };
    }
}
