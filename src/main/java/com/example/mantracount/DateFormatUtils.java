package com.example.mantracount;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Centralized date formatting utilities to eliminate duplication across UI classes.
 * Provides consistent date formatting throughout the application.
 */
public class DateFormatUtils {

    private static final DateTimeFormatter SHORT_DATE_FORMATTER =
            DateTimeFormatter.ofLocalizedDate(java.time.format.FormatStyle.SHORT);

    private static final DateTimeFormatter MEDIUM_DATE_FORMATTER =
            DateTimeFormatter.ofLocalizedDate(java.time.format.FormatStyle.MEDIUM);

    /**
     * Formats date using locale-sensitive short format (used in results displays)
     */
    public static String formatShortDate(LocalDate date) {
        if (date == null) return "";
        return date.format(SHORT_DATE_FORMATTER);
    }

    /**
     * Formats date using locale-sensitive medium format (used in detailed displays)
     */
    public static String formatMediumDate(LocalDate date) {
        if (date == null) return "";
        return date.format(MEDIUM_DATE_FORMATTER);
    }

    /**
     * Formats date range for display in headers
     */
    public static String formatDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null && endDate == null) return "";
        if (startDate == null) return "até " + formatShortDate(endDate);
        if (endDate == null) return "desde " + formatShortDate(startDate);
        return formatShortDate(startDate) + " a " + formatShortDate(endDate);
    }

    /**
     * Creates a results header with date information
     */
    public static String createResultsHeader(LocalDate fromDate) {
        return "✔ Resultados de " + formatShortDate(fromDate) + ":";
    }

    /**
     * Creates a date badge text for UI components
     */
    public static String createDateBadge(LocalDate date, int count, String type) {
        return formatShortDate(date) + " | " + count + " " + type;
    }
}