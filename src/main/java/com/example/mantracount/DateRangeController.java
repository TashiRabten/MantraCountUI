package com.example.mantracount;

import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import javafx.util.StringConverter;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;

/**
 * Handles all date range selection functionality for the Mantra application.
 * This class manages the start and end date pickers, validation, and date range operations.
 */
public class DateRangeController {

    private final DatePicker startDatePicker;
    private final HBox datePickerContainer;

    /**
     * Creates a new DateRangeController with date pickers ready to use.
     */
    public DateRangeController() {
        // Initialize start date picker
        startDatePicker = new DatePicker();

        // Set the prompt text based on detected format or locale
        boolean isBrazilFormat = DateParser.getCurrentDateFormat() == DateParser.DateFormat.BR_FORMAT;
        String formatExample = DateParser.getDateFormatExample();

        // Convert format example to Portuguese
        String portugueseFormatText;
        if (formatExample.contains("MM/DD")) {
            portugueseFormatText = "MM/DD/AA"; // US format in Portuguese
        } else {
            portugueseFormatText = "DD/MM/AA"; // BR format in Portuguese
        }

        startDatePicker.setPromptText(portugueseFormatText);
        startDatePicker.setEditable(true);

        // Add English tooltip to date picker
        Tooltip datePickerTooltip = new Tooltip("Start Date - Select the date from which to start counting mantras");
        datePickerTooltip.setShowDelay(Duration.millis(300));
        datePickerTooltip.setHideDelay(Duration.millis(100));
        Tooltip.install(startDatePicker, datePickerTooltip);

        // Set up the converter to handle both formats
        startDatePicker.setConverter(new StringConverter<LocalDate>() {
            @Override
            public String toString(LocalDate date) {
                if (date != null) {
                    // Use the detected format
                    return DateParser.formatDate(date, true);
                } else {
                    return "";
                }
            }

            /**
             * Format a date using the currently detected format
             */
            public String formatDate(LocalDate date) {
                if (date == null) return "";
                return DateParser.formatDate(date, false); // Use 4-digit year for display
            }

            @Override
            public LocalDate fromString(String string) {
                if (string != null && !string.isEmpty()) {
                    try {
                        // Use our enhanced DateParser that handles both formats
                        return DateParser.parseDate(string);
                    } catch (DateTimeParseException e) {
                        // Parsing failed
                        return null;
                    }
                } else {
                    return null;
                }
            }
        });

        // Portuguese label with English tooltip
        Label startDateLabel = new Label(" - Data Inicial");

        Tooltip labelTooltip = new Tooltip("Start Date - Select the date from which to start counting mantras");
        labelTooltip.setShowDelay(Duration.millis(300));
        labelTooltip.setHideDelay(Duration.millis(100));
        Tooltip.install(startDateLabel, labelTooltip);

        datePickerContainer = new HBox(10, startDatePicker, startDateLabel);
        startDatePicker.setPrefWidth(190);  // Increase width
        datePickerContainer.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
    }

    // Retain the methods for end date but make them optional:
    public LocalDate getEndDate() {
        return null; // End date no longer stored here
    }

    /**
     * Gets the date picker UI container to add to the main UI.
     * @return The HBox containing the date pickers and labels
     */
    public HBox getDatePickerContainer() {
        return datePickerContainer;
    }

    /**
     * Gets the currently selected start date.
     * @return The selected start date or null if not selected
     */
    public LocalDate getStartDate() {
        return startDatePicker.getValue();
    }

    /**
     * Sets the start date value programmatically.
     * @param date The date to set
     */
    public void setStartDate(LocalDate date) {
        startDatePicker.setValue(date);
    }

    /**
     * Validates that the start date is selected.
     * @return true if valid, false otherwise
     */
    public boolean validateStartDate() {
        if (startDatePicker.getValue() == null) {
            UIUtils.showError("Missing or invalid field: \n Please enter valid date",
                    "Campo ausente ou inválido:\nPor favor, insira data válida");
            return false;
        }
        return true;
    }

    /**
     * Determines a valid date range for display using reasonable defaults if needed.
     * @param data The mantra data to use for finding default dates
     * @return An array with [startDate, endDate]
     */
    public LocalDate[] getDateRangeWithDefaults(MantraData data) {
        LocalDate startDate = getStartDate();
        LocalDate endDate = getEndDate();

        // Handle missing start date - find earliest date in file
        if (startDate == null) {
            LocalDate earliestDate = findEarliestDate(data.getLines());

            if (earliestDate != null) {
                startDate = earliestDate;
            } else {
                return null; // Couldn't determine start date
            }
        }

        // Handle missing end date - use today
        if (endDate == null) {
            endDate = LocalDate.now();
        }

        // Make sure end date is not before start date
        if (endDate.isBefore(startDate)) {
            return null; // Invalid date range
        }

        return new LocalDate[] { startDate, endDate };
    }

    /**
     * Finds the earliest date in a list of lines.
     * @param lines The lines to scan for dates
     * @return The earliest date found or null if none found
     */
    private LocalDate findEarliestDate(List<String> lines) {
        LocalDate earliestDate = null;
        for (String line : lines) {
            LocalDate lineDate = LineParser.extractDate(line);
            if (lineDate != null && (earliestDate == null || lineDate.isBefore(earliestDate))) {
                earliestDate = lineDate;
            }
        }
        return earliestDate;
    }
}