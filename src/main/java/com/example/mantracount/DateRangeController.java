package com.example.mantracount;

import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.util.List;

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
        startDatePicker.setPromptText("MM/DD/YY / Mês/Dia/Ano");
        startDatePicker.setEditable(true);

        // No end date picker by default - it will be in AllMantrasUI
        Label startDateLabel = new Label("Start Date / Data Inicial:");

        datePickerContainer = new HBox(10, startDateLabel, startDatePicker);
        startDatePicker.setPrefWidth(200);  // Increase width
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
            UIUtils.showError("❌ Missing or invalid field \n❌ Campo ausente ou inválido",
                    "Please select the start date\nPor favor, selecione a data inicial");
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
