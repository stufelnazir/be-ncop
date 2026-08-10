package com.ncop.clientmaster.entity;

import com.ncop.services.clientmaster.enums.AddressType;
import jakarta.persistence.*;

@Entity
@Table(name = "client_addresses")
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AddressType type;

    private String line1;
    private String line2;
    private String city;
    private String state;
    private String country;
    private String pinCode;

    @ManyToOne
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    public Address() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public AddressType getType() { return type; }
    public void setType(AddressType type) { this.type = type; }
    public String getLine1() { return line1; }
    public void setLine1(String line1) { this.line1 = line1; }
    public String getLine2() { return line2; }
    public void setLine2(String line2) { this.line2 = line2; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }
    public String getPinCode() { return pinCode; }
    public void setPinCode(String pinCode) { this.pinCode = pinCode; }
    public Client getClient() { return client; }
    public void setClient(Client client) { this.client = client; }
}