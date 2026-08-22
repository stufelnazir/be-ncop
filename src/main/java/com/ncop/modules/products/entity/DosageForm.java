package com.ncop.modules.products.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "dosage_forms")
public class DosageForm {

    @Id
    private String id;

    @Indexed(unique = true)
    private String name; // Level 1 Dosage Form (e.g. Syrup, Injection, Cream)

    private String description;

    private List<DosageVariant> variants = new ArrayList<>(); // Level 2 Variants

    private boolean active = true;

    private int sortOrder = 0;

    @CreatedDate
    private Instant createdOn = Instant.now();

    @LastModifiedDate
    private Instant lastUpdatedOn = Instant.now();
}
