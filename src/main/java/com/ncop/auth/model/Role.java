package com.ncop.auth.model;

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
@Document(collection = "roles")
public class Role {

    @Id
    private String roleId;

    @Indexed(unique = true)
    private String name;

    private String description;

    private boolean active = true;

    private List<String> moduleRights = new ArrayList<>();

    @CreatedDate
    private Instant createdOn;

    @LastModifiedDate
    private Instant lastUpdatedOn;

    public Role(String name) {
        this.name = name;
        this.active = true;
        this.moduleRights = new ArrayList<>();
    }

    public Role(String name, String description) {
        this.name = name;
        this.description = description;
        this.active = true;
        this.moduleRights = new ArrayList<>();
    }
}