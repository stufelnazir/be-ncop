package com.ncop.modules.clients.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.ncop.modules.clients.enums.CustomerType;
import com.ncop.modules.clients.enums.ClientLevel;

@Getter
@Setter
@Document(collection = "clients")
public class Client {

    @Id
    private String id;

    private String customerCode; // auto-generated, e.g. CUST-000123

    private CustomerType customerType;

    private String companyName;

    private String tradeName;

    // Used purely to compute clientLevel on the fly — never store the tier itself
    private BigDecimal annualTurnover = BigDecimal.ZERO;

    private PaymentTerms paymentTerms;

    private List<Address> addresses = new ArrayList<>();

    private List<PointOfContact> pointOfContacts = new ArrayList<>();

    private List<ClientDocument> documents = new ArrayList<>();

    private BankDetail bankDetail;

    public Client() {}

    // Computed, never persisted
    public ClientLevel getClientLevel() {
        BigDecimal turnover = annualTurnover == null ? BigDecimal.ZERO : annualTurnover;
        BigDecimal crore = new BigDecimal("10000000"); // 1 Cr = 1,00,00,000

        if (turnover.compareTo(crore.multiply(BigDecimal.TEN)) > 0) return ClientLevel.PLATINUM;      // > 10 Cr
        if (turnover.compareTo(crore.multiply(new BigDecimal("5"))) >= 0) return ClientLevel.GOLD;      // 5-10 Cr
        if (turnover.compareTo(crore) >= 0) return ClientLevel.SILVER;                                  // 1-5 Cr
        if (turnover.compareTo(new BigDecimal("2500000")) >= 0) return ClientLevel.BRONZE;              // 25L-1Cr
        return ClientLevel.NO_VIP;
    }
}