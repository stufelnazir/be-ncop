package com.ncop.modules.clients.entity;

import com.ncop.modules.clients.enums.AddressType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Address {

    private String id;

    private AddressType type;

    private String line1;
    private String line2;
    private String city;
    private String state;
    private String country;
    private String pinCode;

    private Client client;

    public Address() {}
}