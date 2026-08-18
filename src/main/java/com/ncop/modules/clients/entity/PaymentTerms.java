package com.ncop.modules.clients.entity;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PaymentTerms {

    private Double advancePercent;
    private Double beforeDispatchPercent;
    private Integer afterDispatchDays;
    private Double afterDispatchPercent;
}