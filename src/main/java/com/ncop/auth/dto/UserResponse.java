package com.ncop.auth.dto;

import com.ncop.auth.enums.UserStatus;
import com.ncop.auth.enums.UserType;
import com.ncop.auth.util.DateTimeFormatterUtil;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {

    private String id;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String fullName;
    private List<String> roleIds;
    private List<String> roleNames;
    private List<String> moduleRights;
    private UserStatus userStatus;
    private UserType userType;

    @JsonProperty("createdOn")
    private Instant createdOn;

    @JsonProperty("createdOnUtcDateTimeFormatted")
    private String createdOnUtcDateTimeFormatted;

    @JsonProperty("createdOnCurrentTimezoneDateFormatted")
    private String createdOnCurrentTimezoneDateFormatted;

    @JsonProperty("lastUpdatedOn")
    private Instant lastUpdatedOn;

    @JsonProperty("lastUpdatedOnUtcDateTimeFormatted")
    private String lastUpdatedOnUtcDateTimeFormatted;

    @JsonProperty("lastUpdatedOnCurrentTimezoneDateFormatted")
    private String lastUpdatedOnCurrentTimezoneDateFormatted;

    @JsonProperty("lastLoginDate")
    private Instant lastLoginDate;

    @JsonProperty("lastLoginDateUtcDateTimeFormatted")
    private String lastLoginDateUtcDateTimeFormatted;

    @JsonProperty("lastLoginDateCurrentTimezoneDateFormatted")
    private String lastLoginDateCurrentTimezoneDateFormatted;

    // Method to format all date fields
    public void formatAllDates() {
        if (createdOn != null) {
            this.createdOnUtcDateTimeFormatted = DateTimeFormatterUtil.formatToUtcDateTime(createdOn);
            this.createdOnCurrentTimezoneDateFormatted = DateTimeFormatterUtil.formatToCurrentTimezoneDateTime(createdOn);
        }
        if (lastUpdatedOn != null) {
            this.lastUpdatedOnUtcDateTimeFormatted = DateTimeFormatterUtil.formatToUtcDateTime(lastUpdatedOn);
            this.lastUpdatedOnCurrentTimezoneDateFormatted = DateTimeFormatterUtil.formatToCurrentTimezoneDateTime(lastUpdatedOn);
        }
        if (lastLoginDate != null) {
            this.lastLoginDateUtcDateTimeFormatted = DateTimeFormatterUtil.formatToUtcDateTime(lastLoginDate);
            this.lastLoginDateCurrentTimezoneDateFormatted = DateTimeFormatterUtil.formatToCurrentTimezoneDateTime(lastLoginDate);
        }
    }
}
