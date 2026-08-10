package com.ncop.auth.dto;

import lombok.Getter;
import lombok.Setter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.ncop.auth.util.DateTimeFormatterUtil;
import java.time.Instant;

@Getter
@Setter
public class ErrorResponse {

    @JsonProperty("timestamp")
    private Instant timestamp;

    @JsonProperty("status")
    private int status;

    @JsonProperty("error")
    private String error;

    @JsonProperty("message")
    private String message;

    @JsonProperty("utcDateTimeFormatted")
    private String utcDateTimeFormatted;

    @JsonProperty("currentTimezoneDateFormatted")
    private String currentTimezoneDateFormatted;

    public ErrorResponse() {
        this.timestamp = Instant.now();
        formatDates();
    }

    public ErrorResponse(int status, String error, String message) {
        this.timestamp = Instant.now();
        this.status = status;
        this.error = error;
        this.message = message;
        formatDates();
    }

    private void formatDates() {
        this.utcDateTimeFormatted = DateTimeFormatterUtil.formatToUtcDateTime(timestamp);
        this.currentTimezoneDateFormatted = DateTimeFormatterUtil.formatToCurrentTimezoneDateTime(timestamp);
    }
}


