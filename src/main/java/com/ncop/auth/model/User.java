package com.ncop.auth.model;

import com.ncop.auth.enums.UserStatus;
import com.ncop.auth.enums.UserType;
import com.ncop.auth.util.DateTimeFormatterUtil;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "users")
public class User {

    @Id
    private String id;

    @Indexed(unique = true)
    @NotBlank
    private String username;

    @Indexed(unique = true)
    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String password;

    private String firstName;

    private String lastName;

    private List<String> roleIds = new ArrayList<>();

    private UserStatus userStatus = UserStatus.PENDING;

    private UserType userType = UserType.EMPLOYEE;

    @CreatedDate
    private Instant createdOn;

    @Transient
    private String createdOnUtcDateTimeFormatted;

    @Transient
    private String createdOnCurrentTimezoneDateFormatted;

    @LastModifiedDate
    private Instant lastUpdatedOn;

    @Transient
    private String lastUpdatedOnUtcDateTimeFormatted;

    @Transient
    private String lastUpdatedOnCurrentTimezoneDateFormatted;

    private Instant lastLoginDate;

    @Transient
    private String lastLoginDateUtcDateTimeFormatted;

    @Transient
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