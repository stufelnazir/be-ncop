package com.ncop.clientmaster.entity;

import com.ncop.clientmaster.enums.DocumentType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ClientDocument {

    private String id;

    private DocumentType documentType;

    private String fileUrl; // populated once file upload is wired up

    private Client client;

    public ClientDocument() {}
}