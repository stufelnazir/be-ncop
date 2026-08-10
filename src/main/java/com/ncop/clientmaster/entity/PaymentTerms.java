package com.ncop.clientmaster.entity;

import jakarta.persistence.Embeddable;

@Embeddable
public class PaymentTerms {

    private Double advancePercent;
    private Double beforeDispatchPercent;
    private Integer afterDispatchDays;
    private Double afterDispatchPercent;

    public Double getAdvancePercent() { return advancePercent; }
    public void setAdvancePercent(Double advancePercent) { this.advancePercent = advancePercent; }
    public Double getBeforeDispatchPercent() { return beforeDispatchPercent; }
    public void setBeforeDispatchPercent(Double beforeDispatchPercent) { this.beforeDispatchPercent = beforeDispatchPercent; }
    public Integer getAfterDispatchDays() { return afterDispatchDays; }
    public void setAfterDispatchDays(Integer afterDispatchDays) { this.afterDispatchDays = afterDispatchDays; }
    public Double getAfterDispatchPercent() { return afterDispatchPercent; }
    public void setAfterDispatchPercent(Double afterDispatchPercent) { this.afterDispatchPercent = afterDispatchPercent; }
}