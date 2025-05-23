package com.example.mantracount;

import java.time.LocalDate;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MissingDaysDetector {
    // Add Android WhatsApp format support
    private static final Pattern ANDROID_DATE_PATTERN =
            Pattern.compile("^(\\d{1,2}/\\d{1,2}/\\d{2,4})\\s+\\d{1,2}:\\d{1,2}\\s+-\\s+");

    public static class MissingDayInfo {
        private LocalDate missingDate;
        private LocalDate previousDate;
        private LocalDate nextDate;
        private List<String> contextBeforeEntries = new ArrayList<>();
        private List<String> contextAfterEntries = new ArrayList<>();


        public MissingDayInfo(LocalDate missingDate, LocalDate previousDate, LocalDate nextDate) {
            this.missingDate = missingDate;
            this.previousDate = previousDate;
            this.nextDate = nextDate;
        }

        public LocalDate getMissingDate() { return missingDate; }
        public LocalDate getPreviousDate() { return previousDate; }
        public LocalDate getNextDate() { return nextDate; }
        public List<String> getContextBeforeEntries() { return contextBeforeEntries; }
        public List<String> getContextAfterEntries() { return contextAfterEntries; }

        public void setContextBeforeEntries(List<String> entries) {
            if (entries != null) this.contextBeforeEntries = new ArrayList<>(entries);
        }

        public void setContextAfterEntries(List<String> entries) {
            if (entries != null) this.contextAfterEntries = new ArrayList<>(entries);
        }

        public LocalDate getDate() { return missingDate; }
        public LocalDate getPrevDate() { return previousDate; }

        @Override
        public String toString() {
            return "Missing Day: " + missingDate;
        }
    }

    public static List<MissingDayInfo> detectMissingDays(List<String> lines, LocalDate targetDate, String mantraKeyword) {
        List<MissingDayInfo> missingDays = new ArrayList<>();
        Map<LocalDate, List<String>> relevantEntriesByDate = new HashMap<>();
        Set<LocalDate> relevantDates = new TreeSet<>();
        Map<LocalDate, List<String>> entriesByDate = new HashMap<>();

        for (String line : lines) {
            // Modified to use the enhanced hasMantraOrRitoMatch method
            if (hasMantraOrRitoMatch(line, mantraKeyword)) {
                LocalDate date = LineParser.extractDate(line);
                if (date != null) {
                    relevantDates.add(date);
                    relevantEntriesByDate.computeIfAbsent(date, k -> new ArrayList<>()).add(line);
                }
            }
        }

        if (relevantDates.isEmpty()) return missingDays;

        List<LocalDate> sortedDates = new ArrayList<>(relevantDates);
        Collections.sort(sortedDates);

        LocalDate startDate = targetDate != null ? targetDate : sortedDates.get(0);
        LocalDate endDate = sortedDates.get(sortedDates.size() - 1);

        LocalDate current = startDate;
        int idx = 0;
        while (!current.isAfter(endDate)) {
            if (!relevantDates.contains(current)) {
                // Find the closest previous date that has entries
                LocalDate prev = null;
                for (LocalDate date : sortedDates) {
                    if (date.isBefore(current)) {
                        prev = date;
                    } else {
                        break;
                    }
                }

                // Find the closest next date that has entries
                LocalDate next = null;
                for (LocalDate date : sortedDates) {
                    if (date.isAfter(current)) {
                        next = date;
                        break;
                    }
                }

                MissingDayInfo info = new MissingDayInfo(current, prev, next);

                if (prev != null && relevantEntriesByDate.containsKey(prev)) {
                    info.setContextBeforeEntries(entriesByDate.get(current));
                }
                List<String> afterEntries = new ArrayList<>();
                if (next != null && relevantEntriesByDate.containsKey(next)) {
                    afterEntries.addAll(relevantEntriesByDate.get(next));
                    int nextIdx = sortedDates.indexOf(next);
                    if (nextIdx + 1 < sortedDates.size()) {
                        LocalDate secondAfter = sortedDates.get(nextIdx + 1);
                        if (relevantEntriesByDate.containsKey(secondAfter)) {
                            afterEntries.addAll(relevantEntriesByDate.get(secondAfter));
                        }
                    }
                }
                info.setContextAfterEntries(entriesByDate.get(next));

                missingDays.add(info);
            } else {
                if (idx < sortedDates.size() && current.equals(sortedDates.get(idx))) {
                    idx++;
                }
            }
            current = current.plusDays(1);
        }

        return missingDays;
    }
    /**
     * Checks if a line contains either mantra or rito entries matching the keyword
     */
    private static boolean hasMantraOrRitoMatch(String line, String keyword) {
        // First check using the original LineAnalyzer method
        if (LineAnalyzer.hasApproximateMatch(line, keyword)) {
            return true;
        }

        // Also check for rito-specific lines that might not be caught by hasApproximateMatch
        String lineLower = line.toLowerCase();
        String keywordLower = keyword.toLowerCase();

        boolean keywordFound = lineLower.contains(keywordLower);
        boolean ritoFound = lineLower.contains("rito") || lineLower.contains("ritos");
        boolean fizFound = lineLower.contains("fiz") || lineLower.contains("fez") ||
                lineLower.contains("recitei") || lineLower.contains("faz");

        return keywordFound && ritoFound && fizFound;
    }

    public static List<String> findPotentialIssues(List<String> lines, LocalDate missingDate, String mantraKeyword) {
        List<String> suspects = new ArrayList<>();
        LocalDate prevDate = null;
        LocalDate nextDate = null;
        int prevIndex = -1;
        int nextIndex = -1;

        for (int i = 0; i < lines.size(); i++) {
            LocalDate lineDate = LineParser.extractDate(lines.get(i));
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

        if (prevIndex != -1) suspects.add(lines.get(prevIndex));
        if (nextIndex != -1) suspects.add(lines.get(nextIndex));

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