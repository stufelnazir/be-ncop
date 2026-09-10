package com.ncop.modules.qa.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/** A separately actionable product within a customer RFQ. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class QaRfqProduct {
    private String id;
    private String productName;
    private String dosageForm;
    private String standard;
    private List<QaCompositionLine> compositionLines = new ArrayList<>();
    private Double orderQty;
    private String packingSpecs;
    private Double totalTablets;
    private Boolean technicalQueryRaised = false;
}
