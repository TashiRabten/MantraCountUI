
// File: MissingDaysHelper.java
package com.example.mantracount;

import javafx.scene.control.Alert;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

public class MissingDaysHelper {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public static boolean isDateLine(String line) {
        return line.startsWith("[") && line.contains("]");
    }

    public static LocalDate extractDate(String line) {
        try {
            int start = line.indexOf('[') + 1;
            int end = line.indexOf(']');
            if (start >= 0 && end > start) {
                String dateStr = line.substring(start, end);
                return LocalDate.parse(dateStr, DATE_FORMATTER);
            }
        } catch (DateTimeParseException | StringIndexOutOfBoundsException e) {
            // ignore or log
        }
        return null;
    }

    public static boolean prepareDataForMissingDays(String dateText, String name, String path, MantraData data) {
        ValidationResult result = InputValidator.validateInputs(dateText, name, path);
        if (!result.isValid()) {
            UIUtils.showError(result.getErrorMessage());
            return false;
        }

        LocalDate parsedDate = DateParser.parseDate(dateText);
        data.setTargetDate(parsedDate);
        data.setNameToCount(name);
        data.setFilePath(path);
        data.setFromZip(path.toLowerCase().endsWith(".zip"));

        return true;
    }



    public static List<LocalDate> findMissingDays(MantraData data) {
        Set<LocalDate> presentDates = new HashSet<>();
        List<String> lines = data.getLines();

        for (String line : lines) {
            if (isDateLine(line)) {
                LocalDate date = extractDate(line);
                if (date != null && !date.isBefore(data.getTargetDate())) {
                    presentDates.add(date);
                }
            }
        }

        List<LocalDate> missingDays = new ArrayList<>();

        if (presentDates.isEmpty()) {
            return missingDays;
        }

        LocalDate start = data.getTargetDate();
        LocalDate end = Collections.max(presentDates);

        LocalDate current = start;
        while (!current.isAfter(end)) {
            if (!presentDates.contains(current)) {
                missingDays.add(current);
            }
            current = current.plusDays(1);
        }

        return missingDays;
    }

    public static void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setContentText(message);
        alert.showAndWait();
    }
}