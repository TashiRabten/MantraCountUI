package com.example.mantracount;

import java.time.LocalDate;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

public class MissingDaysDetector {

    // Inner class to represent the Missing Day Info
    public static class MissingDayInfo {
        private final LocalDate date;
        private final LocalDate prevDate;
        private final LocalDate nextDate;

        public MissingDayInfo(LocalDate date, LocalDate prevDate, LocalDate nextDate) {
            this.date = date;
            this.prevDate = prevDate;
            this.nextDate = nextDate;
        }

        public LocalDate getDate() {
            return date;
        }

        public LocalDate getPrevDate() {
            return prevDate;
        }

        public LocalDate getNextDate() {
            return nextDate;
        }

        @Override
        public String toString() {
            return "Missing Day: " + date;
        }
    }

    // Detect missing days between the start date and the actual dates in the file
    public static List<String> findMissingDays(List<String> lines, LocalDate targetDate, String mantraKeyword) {
        List<String> missingDays = new ArrayList<>();
        LocalDate currentDate = targetDate;

        for (String line : lines) {
            if (line.contains(mantraKeyword)) {
                // Extract date from the line
                LocalDate lineDate = LineParser.extractDateFromLine(line);
                if (lineDate != null && lineDate.isAfter(currentDate)) {
                    // Check for missing days between currentDate and lineDate
                    while (currentDate.isBefore(lineDate)) {
                        missingDays.add("Missing day: " + currentDate.toString());
                        currentDate = currentDate.plusDays(1);
                    }
                }
                currentDate = lineDate;
            }
        }
        return missingDays;
    }

    // Detect missing days and generate MissingDayInfo objects
    public static List<MissingDayInfo> detectMissingDays(List<String> lines, LocalDate targetDate, String mantraKeyword) {
        List<MissingDayInfo> missingDays = new ArrayList<>();
        LocalDate currentDate = targetDate;

        // Extract dates and identify the gaps
        for (String line : lines) {
            LocalDate lineDate = LineParser.extractDateFromLine(line);
            if (lineDate != null && lineDate.isAfter(currentDate)) {
                missingDays.add(new MissingDayInfo(lineDate, currentDate, lineDate));
                currentDate = lineDate;
            }
        }

        return missingDays;
    }

    // Find potential issues related to missing days in the lines
    public static List<String> findPotentialIssues(List<String> lines, LocalDate missingDate, String mantraKeyword) {
        List<String> suspects = new ArrayList<>();
        // Look for lines with dates close to the missing date
        LocalDate prevDate = null;
        LocalDate nextDate = null;
        int prevIndex = -1;
        int nextIndex = -1;

        for (int i = 0; i < lines.size(); i++) {
            LocalDate lineDate = LineParser.extractDateFromLine(lines.get(i));
            if (lineDate != null) {
                if (lineDate.isBefore(missingDate) && (prevDate == null || lineDate.isAfter(prevDate))) {
                    prevDate = lineDate;
                    prevIndex = i;
                }
                if (lineDate.isAfter(missingDate) && (nextDate == null || lineDate.isBefore(nextDate))) {
                    nextDate = lineDate;
                    nextIndex = i;
                }
            }
        }

        // Check the lines before and after the missing date
        if (prevIndex != -1) suspects.add(lines.get(prevIndex));
        if (nextIndex != -1) suspects.add(lines.get(nextIndex));

        // Also check for lines that match formatted date strings
        String monthStr = String.format("%02d", missingDate.getMonthValue());
        String dayStr = String.format("%02d", missingDate.getDayOfMonth());
        String yearStr = String.valueOf(missingDate.getYear());
        String shortYearStr = yearStr.substring(2);

        List<String> datePatterns = Arrays.asList(
                monthStr + "/" + dayStr,
                dayStr + "/" + monthStr,
                monthStr + "/" + dayStr + "/" + shortYearStr,
                monthStr + "/" + dayStr + "/" + yearStr,
                dayStr + "/" + monthStr + "/" + shortYearStr,
                dayStr + "/" + monthStr + "/" + yearStr
        );

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            for (String pattern : datePatterns) {
                if (line.contains(pattern) && !suspects.contains(line)) {
                    suspects.add(line);
                    // Add context lines if needed
                    if (i > 0 && !suspects.contains(lines.get(i - 1))) {
                        suspects.add(lines.get(i - 1));
                    }
                    if (i < lines.size() - 1 && !suspects.contains(lines.get(i + 1))) {
                        suspects.add(lines.get(i + 1));
                    }
                    break;
                }
            }
        }

        return suspects;
    }
}
