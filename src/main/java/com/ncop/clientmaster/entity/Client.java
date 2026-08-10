package com.ncop.clientmaster.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import com.ncop.services.clientmaster.entity.Address;
import com.ncop.services.clientmaster.entity.PointOfContact;
import com.ncop.services.clientmaster.entity.ClientDocument;
import com.ncop.services.clientmaster.entity.BankDetail;
import com.ncop.services.clientmaster.entity.PaymentTerms;
import com.ncop.services.clientmaster.enums.CustomerType;
import com.ncop.services.clientmaster.enums.ClientLevel;

@Entity
@Table(name = "clients")
public class Client {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String customerCode; // auto-generated, e.g. CUST-000123

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CustomerType customerType;

    @Column(nullable = false)
    private String companyName;

    private String tradeName;

    // Used purely to compute clientLevel on the fly — never store the tier itself
    private BigDecimal annualTurnover = BigDecimal.ZERO;

    @Embedded
    private PaymentTerms paymentTerms;

    @OneToMany(mappedBy = "client", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Address> addresses = new ArrayList<>();

    @OneToMany(mappedBy = "client", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PointOfContact> pointOfContacts = new ArrayList<>();

    @OneToMany(mappedBy = "client", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ClientDocument> documents = new ArrayList<>();

    @OneToOne(mappedBy = "client", cascade = CascadeType.ALL, orphanRemoval = true)
    private BankDetail bankDetail;

    public Client() {}

    // Computed, never persisted
    @Transient
    public ClientLevel getClientLevel() {
        BigDecimal turnover = annualTurnover == null ? BigDecimal.ZERO : annualTurnover;
        BigDecimal crore = new BigDecimal("10000000"); // 1 Cr = 1,00,00,000

        if (turnover.compareTo(crore.multiply(BigDecimal.TEN)) > 0) return ClientLevel.PLATINUM;      // > 10 Cr
        if (turnover.compareTo(crore.multiply(new BigDecimal("5"))) >= 0) return ClientLevel.GOLD;      // 5-10 Cr
        if (turnover.compareTo(crore) >= 0) return ClientLevel.SILVER;                                  // 1-5 Cr
        if (turnover.compareTo(new BigDecimal("2500000")) >= 0) return ClientLevel.BRONZE;              // 25L-1Cr
        return ClientLevel.NO_VIP;
    }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCustomerCode() { return customerCode; }
    public void setCustomerCode(String customerCode) { this.customerCode = customerCode; }
    public CustomerType getCustomerType() { return customerType; }
    public void setCustomerType(CustomerType customerType) { this.customerType = customerType; }
    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }
    public String getTradeName() { return tradeName; }
    public void setTradeName(String tradeName) { this.tradeName = tradeName; }
    public BigDecimal getAnnualTurnover() { return annualTurnover; }
    public void setAnnualTurnover(BigDecimal annualTurnover) { this.annualTurnover = annualTurnover; }
    public PaymentTerms getPaymentTerms() { return paymentTerms; }
    public void setPaymentTerms(PaymentTerms paymentTerms) { this.paymentTerms = paymentTerms; }
    public List<Address> getAddresses() { return addresses; }
    public void setAddresses(List<Address> addresses) { this.addresses = addresses; }
    public List<PointOfContact> getPointOfContacts() { return pointOfContacts; }
    public void setPointOfContacts(List<PointOfContact> pointOfContacts) { this.pointOfContacts = pointOfContacts; }
    public List<ClientDocument> getDocuments() { return documents; }
    public void setDocuments(List<ClientDocument> documents) { this.documents = documents; }
    public BankDetail getBankDetail() { return bankDetail; }
    public void setBankDetail(BankDetail bankDetail) { this.bankDetail = bankDetail; }
}