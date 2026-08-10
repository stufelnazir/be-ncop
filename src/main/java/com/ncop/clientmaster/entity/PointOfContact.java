package com.ncop.clientmaster.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "client_contacts")
public class PointOfContact {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String personName;
    private String designation;
    private String department;
    private String phone;
    private String email;

    @ManyToOne
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    public PointOfContact() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getPersonName() { return personName; }
    public void setPersonName(String personName) { this.personName = personName; }
    public String getDesignation() { return designation; }
    public void setDesignation(String designation) { this.designation = designation; }
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public Client getClient() { return client; }
    public void setClient(Client client) { this.client = client; }
}