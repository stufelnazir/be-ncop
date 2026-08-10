package com.ncop.auth.util;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class DateTimeFormatterUtil {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private DateTimeFormatterUtil() {
        // Utility class
    }

    /**
     * Format Instant to UTC datetime string (dd/MM/yyyy HH:mm:ss)
     */
    public static String formatToUtcDateTime(Instant instant) {
        if (instant == null) return null;
        ZonedDateTime utcDateTime = instant.atZone(ZoneId.of("UTC"));
        return utcDateTime.format(DATE_TIME_FORMATTER);
    }

    /**
     * Format Instant to current timezone datetime string (dd/MM/yyyy HH:mm:ss)
     */
    public static String formatToCurrentTimezoneDateTime(Instant instant) {
        if (instant == null) return null;
        ZonedDateTime currentTimeZoneDateTime = instant.atZone(ZoneId.systemDefault());
        return currentTimeZoneDateTime.format(DATE_TIME_FORMATTER);
    }

    /**
     * Container class to hold both formatted dates
     */
    public static class FormattedDateTimes {
        private final String utcDateTimeFormatted;
        private final String currentTimezoneDateFormatted;

        public FormattedDateTimes(Instant instant) {
            this.utcDateTimeFormatted = formatToUtcDateTime(instant);
            this.currentTimezoneDateFormatted = formatToCurrentTimezoneDateTime(instant);
        }

        public String getUtcDateTimeFormatted() {
            return utcDateTimeFormatted;
        }

        public String getCurrentTimezoneDateFormatted() {
            return currentTimezoneDateFormatted;
        }
    }
}

