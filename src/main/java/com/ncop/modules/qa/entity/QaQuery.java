package com.ncop.modules.qa.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "qa_queries")
public class QaQuery {

    @Id
    private String id;

    @Indexed(unique = true)
    private String queryNo; // e.g. TQ-2026-0001

    private String rfqId;
    private String rfqNo;
    private String mfrId;

    private String raisedBy;
    private String raisedTo;
    private String subject;
    private String queryText;
    private String responseText;

    private String status = "OPEN"; // OPEN, RESOLVED

    @CreatedDate
    private Instant createdOn = Instant.now();

    private Instant resolvedOn;
}
