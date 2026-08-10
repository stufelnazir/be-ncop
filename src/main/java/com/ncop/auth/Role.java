package com.ncop.auth;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Document(collection = "roles")
public class Role {

    @Id
    private String id;

    private RoleName name;

    public Role() {}
    public Role(RoleName name) { this.name = name; }
}