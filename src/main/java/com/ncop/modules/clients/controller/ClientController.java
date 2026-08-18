package com.ncop.modules.clients.controller;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.ncop.common.services.EmailService;
import com.ncop.modules.clients.dto.ClientRegistrationDto;
import com.ncop.modules.clients.dto.ClientRequestDto;
import com.ncop.modules.clients.entity.Client;
import com.ncop.modules.clients.entity.ClientDocument;
import com.ncop.modules.clients.enums.DocumentType;
import com.ncop.modules.clients.repository.ClientRepository;
import com.ncop.modules.clients.services.ClientService;
import com.ncop.modules.clients.services.FileStorageService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/clients")
@RequiredArgsConstructor
public class ClientController {

    private final ClientService clientService;
    private final ClientRepository clientRepository;
    private final FileStorageService fileStorageService;
    private final EmailService emailService;

    @PostMapping
    public ResponseEntity<Client> createClient(@Valid @RequestBody ClientRequestDto requestDto) {
        Client client = clientService.createClient(requestDto);
        return new ResponseEntity<>(client, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<Client>> getAllClients() {
        return ResponseEntity.ok(clientService.getAllClients());
    }

    @GetMapping("/count")
    public ResponseEntity<Long> getClientCount() {
        return ResponseEntity.ok(clientRepository.count());
    }

    @GetMapping("/level-counts")
    public ResponseEntity<java.util.Map<String, Long>> getClientLevelCounts() {
        List<Client> allClients = clientService.getAllClients();
        java.util.Map<String, Long> counts = allClients.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        c -> c.getClientLevel().name(),
                        java.util.stream.Collectors.counting()
                ));
        // Ensure all levels are present in the response (even if 0)
        for (com.ncop.modules.clients.enums.ClientLevel level : com.ncop.modules.clients.enums.ClientLevel.values()) {
            counts.putIfAbsent(level.name(), 0L);
        }
        return ResponseEntity.ok(counts);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Client> getClientById(@PathVariable String id) {
        return clientService.getClientById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<Client> updateClient(@PathVariable String id, @Valid @RequestBody ClientRequestDto requestDto) {
        try {
            Client client = clientService.updateClient(id, requestDto);
            return ResponseEntity.ok(client);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // Client facing endpoint to submit bank details
    @PostMapping("/{id}/register")
    public ResponseEntity<Client> submitRegistration(@PathVariable String id, @RequestBody ClientRegistrationDto registrationDto) {
        return clientRepository.findById(id).map(client -> {
            client.setBankDetail(registrationDto.getBankDetail());
            Client updatedClient = clientRepository.save(client);
            return ResponseEntity.ok(updatedClient);
        }).orElse(ResponseEntity.notFound().build());
    }

    // Upload document endpoint (handles both GCS & Local via FileStorageService)
    @PostMapping("/{id}/documents")
    public ResponseEntity<Client> uploadDocument(
            @PathVariable String id,
            @RequestParam("file") MultipartFile file,
            @RequestParam("documentType") DocumentType documentType) {
        
        return clientRepository.findById(id).map(client -> {
            FileStorageService.FileStorageResult result = fileStorageService.storeFile(file, id, documentType.name());
            if (result == null) {
                return ResponseEntity.badRequest().<Client>build();
            }

            String docId = UUID.randomUUID().toString();
            String viewUrl = "/api/clients/" + id + "/documents/" + docId + "/view";

            ClientDocument doc = new ClientDocument();
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

            if (client.getDocuments() == null) {
                client.setDocuments(new ArrayList<>());
            }

            // Replace existing doc of same type if already uploaded
            client.getDocuments().removeIf(d -> d.getDocumentType() == documentType);
            client.getDocuments().add(doc);

            Client updatedClient = clientRepository.save(client);
            return ResponseEntity.ok(updatedClient);
        }).orElse(ResponseEntity.notFound().build());
    }

    // Stream document inline for in-browser / in-app PDF & Doc viewing
    @GetMapping("/{id}/documents/{docId}/view")
    public ResponseEntity<byte[]> viewDocument(@PathVariable String id, @PathVariable String docId) {
        Optional<Client> clientOpt = clientRepository.findById(id);
        if (clientOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Client client = clientOpt.get();
        if (client.getDocuments() == null) {
            return ResponseEntity.notFound().build();
        }

        Optional<ClientDocument> docOpt = client.getDocuments().stream()
                .filter(d -> docId.equals(d.getId()))
                .findFirst();

        if (docOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        ClientDocument doc = docOpt.get();
        byte[] data = fileStorageService.loadFileBytes(doc);
        if (data == null) {
            return ResponseEntity.notFound().build();
        }

        MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
        if (doc.getContentType() != null) {
            try {
                mediaType = MediaType.parseMediaType(doc.getContentType());
            } catch (Exception ignored) {}
        }

        String displayName = doc.getOriginalFileName() != null ? doc.getOriginalFileName() : (doc.getFileName() != null ? doc.getFileName() : "document");

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + displayName + "\"")
                .body(data);
    }

    // Download document as attachment
    @GetMapping("/{id}/documents/{docId}/download")
    public ResponseEntity<byte[]> downloadDocument(@PathVariable String id, @PathVariable String docId) {
        Optional<Client> clientOpt = clientRepository.findById(id);
        if (clientOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Client client = clientOpt.get();
        if (client.getDocuments() == null) {
            return ResponseEntity.notFound().build();
        }

        Optional<ClientDocument> docOpt = client.getDocuments().stream()
                .filter(d -> docId.equals(d.getId()))
                .findFirst();

        if (docOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        ClientDocument doc = docOpt.get();
        byte[] data = fileStorageService.loadFileBytes(doc);
        if (data == null) {
            return ResponseEntity.notFound().build();
        }

        String displayName = doc.getOriginalFileName() != null ? doc.getOriginalFileName() : (doc.getFileName() != null ? doc.getFileName() : "document");

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + displayName + "\"")
                .body(data);
    }

    // Delete document
    @DeleteMapping("/{id}/documents/{docId}")
    public ResponseEntity<Client> deleteDocument(@PathVariable String id, @PathVariable String docId) {
        return clientRepository.findById(id).map(client -> {
            if (client.getDocuments() == null) {
                return ResponseEntity.notFound().<Client>build();
            }

            Optional<ClientDocument> docOpt = client.getDocuments().stream()
                    .filter(d -> docId.equals(d.getId()))
                    .findFirst();

            if (docOpt.isPresent()) {
                ClientDocument doc = docOpt.get();
                fileStorageService.deleteFile(doc);
                client.getDocuments().remove(doc);
                Client updatedClient = clientRepository.save(client);
                return ResponseEntity.ok(updatedClient);
            }
            return ResponseEntity.notFound().<Client>build();
        }).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/send-email")
    public ResponseEntity<Void> resendRegistrationEmail(@PathVariable String id) {
        return clientRepository.findById(id).map(client -> {
            if (client.getPointOfContacts() != null) {
                client.getPointOfContacts().forEach(poc -> {
                    if (poc.getEmail() != null && !poc.getEmail().isEmpty()) {
                        String registrationLink = "http://localhost:3000/register-client/" + client.getId();
                        emailService.sendRegistrationEmail(poc.getEmail(), client.getCompanyName(), registrationLink);
                    }
                });
            }
            return ResponseEntity.ok().<Void>build();
        }).orElse(ResponseEntity.notFound().build());
    }
}
