package com.ncop.modules.qa.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class QaQueryRequestDto {
    private String rfqId;
    private String rfqNo;
    private String mfrId;

    private String raisedBy;
    private String raisedTo;
    private String subject;
    private String queryText;
    private String responseText;

    private String status;
}
