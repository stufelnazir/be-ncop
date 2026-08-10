package com.ncop.clientmaster.entity;

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

    private Client client;

    public PointOfContact() {}
}