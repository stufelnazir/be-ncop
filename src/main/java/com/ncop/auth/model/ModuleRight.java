package com.ncop.auth.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "module_rights")
public class ModuleRight {

    @Id
    private String id;

    private String name;

    private String label;

    private String description;

    @CreatedDate
    private Instant createdOn;

    @LastModifiedDate
    private Instant lastUpdatedOn;
}

