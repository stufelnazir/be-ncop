package com.ncop.modules.clientmaster.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.ncop.modules.clientmaster.dto.ClientRegistrationDto;
import com.ncop.modules.clientmaster.dto.ClientRequestDto;
import com.ncop.modules.clientmaster.entity.Client;
import com.ncop.modules.clientmaster.entity.ClientDocument;
import com.ncop.modules.clientmaster.enums.DocumentType;
import com.ncop.modules.clientmaster.repository.ClientRepository;
import com.ncop.modules.clientmaster.services.ClientService;
import com.ncop.common.services.EmailService;
import com.ncop.modules.clientmaster.services.FileStorageService;

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

    // Client facing endpoint to upload a document
    @PostMapping("/{id}/documents")
    public ResponseEntity<Client> uploadDocument(
            @PathVariable String id,
            @RequestParam("file") MultipartFile file,
            @RequestParam("documentType") DocumentType documentType) {
        
        return clientRepository.findById(id).map(client -> {
            String fileUrl = fileStorageService.storeFile(file, id, documentType.name());
            
            ClientDocument doc = new ClientDocument();
            doc.setId(UUID.randomUUID().toString());
            doc.setDocumentType(documentType);
            doc.setFileUrl(fileUrl);
            
            client.getDocuments().add(doc);
            Client updatedClient = clientRepository.save(client);
            
            return ResponseEntity.ok(updatedClient);
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
