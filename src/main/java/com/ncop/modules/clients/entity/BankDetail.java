package com.ncop.modules.clients.entity;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BankDetail {

    private String id;

    private String accountHolderName;
    private String accountNumber;
    private String bankName;
    private String branchName;
    private String ifscCode;
    private String swiftCode;

    private Client client;

    public BankDetail() {}
}