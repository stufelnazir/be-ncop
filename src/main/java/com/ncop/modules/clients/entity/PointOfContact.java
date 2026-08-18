package com.ncop.modules.clients.entity;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PointOfContact {

    private String id;

    private String personName;
    private String designation;
    private String department;
    private String phone;
    private String email;
    private Boolean primary;

    private Client client;

    public PointOfContact() {}
}