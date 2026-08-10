package com.ncop.auth.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

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

    private List<String> moduleRights = new ArrayList<>();

    public Role(String name, List<String> moduleRights) {
        this.name = name;
        this.moduleRights = moduleRights != null ? moduleRights : new ArrayList<>();
    }
}