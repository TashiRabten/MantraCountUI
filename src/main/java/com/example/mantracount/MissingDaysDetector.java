package com.example.mantracount;

import java.time.LocalDate;
import java.util.*;

public class MissingDaysDetector {

    public static class MissingDayInfo {
        private final LocalDate missingDate;
        private final LocalDate previousDate;
        private final LocalDate nextDate;

        public MissingDayInfo(LocalDate missingDate, LocalDate previousDate, LocalDate nextDate) {
            this.missingDate = missingDate;
            this.previousDate = previousDate;
            this.nextDate = nextDate;
        }

        public LocalDate getDate() {
            return missingDate;
        }

        public LocalDate getPreviousDate() {
            return previousDate;
        }

        public LocalDate getNextDate() {
            return nextDate;
        }

        @Override
        public String toString() {
            return "Missing Day: " + missingDate;
        }
    }

    public static List<MissingDayInfo> detectMissingDays(List<String> lines, LocalDate targetDate, String mantraKeyword) {
        List<MissingDayInfo> missingDays = new ArrayList<>();
        Set<LocalDate> relevantDates = new TreeSet<>();

        // Find all dates with relevant mantra entries
        for (String line : lines) {
            if (LineAnalyzer.hasApproximateMatch(line, mantraKeyword)) {
                LocalDate date = LineParser.extractDate(line);
                if (date != null) {
                    relevantDates.add(date);
                }
            }
        }

        if (relevantDates.isEmpty()) return missingDays;

        List<LocalDate> sortedDates = new ArrayList<>(relevantDates);
        LocalDate startDate = targetDate != null ? targetDate : sortedDates.get(0);
        LocalDate endDate = sortedDates.get(sortedDates.size() - 1);

        // Check each day in the range for missing entries
        LocalDate current = startDate;
        while (!current.isAfter(endDate)) {
            if (!relevantDates.contains(current)) {
                LocalDate prev = findClosestPreviousDate(sortedDates, current);
                LocalDate next = findClosestNextDate(sortedDates, current);
                missingDays.add(new MissingDayInfo(current, prev, next));
            }
            current = current.plusDays(1);
        }

        return missingDays;
    }

    private static LocalDate findClosestPreviousDate(List<LocalDate> sortedDates, LocalDate targetDate) {
        LocalDate prev = null;
        for (LocalDate date : sortedDates) {
            if (date.isBefore(targetDate)) {
                prev = date;
            } else {
                break;
            }
        }
        return prev;
    }

    private static LocalDate findClosestNextDate(List<LocalDate> sortedDates, LocalDate targetDate) {
        for (LocalDate date : sortedDates) {
            if (date.isAfter(targetDate)) {
                return date;
            }
        }
        return null;
    }

    public static List<String> findPotentialIssues(List<String> lines, LocalDate missingDate, String mantraKeyword) {
        List<String> potentialIssues = new ArrayList<>();

        // Find lines around the missing date that might be related
        for (String line : lines) {
            LocalDate lineDate = LineParser.extractDate(line);
            if (lineDate != null) {
                // Check if line is within 1 day of missing date
                long daysDiff = Math.abs(lineDate.toEpochDay() - missingDate.toEpochDay());
                if (daysDiff <= 1) {
                    potentialIssues.add(line);
                }
            }
        }

        return potentialIssues;
    }
}