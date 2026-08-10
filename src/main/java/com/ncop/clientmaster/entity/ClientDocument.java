package com.ncop.clientmaster.entity;

import com.ncop.services.clientmaster.enums.DocumentType;
import jakarta.persistence.*;

@Entity
@Table(name = "client_documents")
public class ClientDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DocumentType documentType;

    private String fileUrl; // populated once file upload is wired up

    @ManyToOne
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    public ClientDocument() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public DocumentType getDocumentType() { return documentType; }
    public void setDocumentType(DocumentType documentType) { this.documentType = documentType; }
    public String getFileUrl() { return fileUrl; }
    public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl; }
    public Client getClient() { return client; }
    public void setClient(Client client) { this.client = client; }
}