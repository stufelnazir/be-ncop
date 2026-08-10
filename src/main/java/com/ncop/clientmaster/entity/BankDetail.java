package com.ncop.clientmaster.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "client_bank_details")
public class BankDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String accountHolderName;
    private String accountNumber;
    private String bankName;
    private String branchName;
    private String ifscCode;
    private String swiftCode;

    @OneToOne
    @JoinColumn(name = "client_id", nullable = false, unique = true)
    private Client client;

    public BankDetail() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getAccountHolderName() { return accountHolderName; }
    public void setAccountHolderName(String accountHolderName) { this.accountHolderName = accountHolderName; }
    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }
    public String getBankName() { return bankName; }
    public void setBankName(String bankName) { this.bankName = bankName; }
    public String getBranchName() { return branchName; }
    public void setBranchName(String branchName) { this.branchName = branchName; }
    public String getIfscCode() { return ifscCode; }
    public void setIfscCode(String ifscCode) { this.ifscCode = ifscCode; }
    public String getSwiftCode() { return swiftCode; }
    public void setSwiftCode(String swiftCode) { this.swiftCode = swiftCode; }
    public Client getClient() { return client; }
    public void setClient(Client client) { this.client = client; }
}