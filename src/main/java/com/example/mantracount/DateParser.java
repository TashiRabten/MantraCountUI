package com.example.mantracount;

import java.time.LocalDate;
import java.time.chrono.IsoChronology;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DateParser {
    // Format patterns
    private static final String US_SHORT_PATTERN = "M/d/yy";
    private static final String US_LONG_PATTERN = "M/d/yyyy";
    private static final String BR_SHORT_PATTERN = "d/M/yy";
    private static final String BR_LONG_PATTERN = "d/M/yyyy";

    // Formatters for different date styles
    private static final DateTimeFormatter US_SHORT_FORMATTER = DateTimeFormatter.ofPattern(US_SHORT_PATTERN);
    private static final DateTimeFormatter US_LONG_FORMATTER = DateTimeFormatter.ofPattern(US_LONG_PATTERN);
    private static final DateTimeFormatter BR_SHORT_FORMATTER = DateTimeFormatter.ofPattern(BR_SHORT_PATTERN);
    private static final DateTimeFormatter BR_LONG_FORMATTER = DateTimeFormatter.ofPattern(BR_LONG_PATTERN);

    // Pattern to extract date components
    private static final Pattern DATE_PATTERN = Pattern.compile("(\\d{1,2})/(\\d{1,2})/(\\d{2,4})");

    // Track the detected file format
    private static DateFormat detectedFileFormat = null;

    // Track the user input format (may differ from file format)
    // Default to system locale rather than inferring from input
    private static DateFormat userInputFormat = null;

    public enum DateFormat {
        US_FORMAT,    // MM/DD/YY
        BR_FORMAT     // DD/MM/YY
    }

    /**
     * Detects the date format used in a list of text lines
     * @param lines The text lines to analyze
     * @return The detected date format
     */
    public static DateFormat detectDateFormat(List<String> lines) {
        if (detectedFileFormat != null) {
            return detectedFileFormat;
        }

        if (lines == null || lines.isEmpty()) {
            return setDefaultDetectedFormat();
        }

        VotingResult votes = analyzeLines(lines);
        detectedFileFormat = determineFormatFromVotes(votes);
        
        logDetectionResult(votes);
        initializeUserInputFormat();
        
        return detectedFileFormat;
    }

    private static DateFormat setDefaultDetectedFormat() {
        detectedFileFormat = getDefaultDateFormat();
        System.out.println("No lines to analyze. Using system locale default: " + detectedFileFormat);
        return detectedFileFormat;
    }

    private static VotingResult analyzeLines(List<String> lines) {
        VotingResult result = new VotingResult();
        int analyzedLines = 0;

        for (String line : lines) {
            if (analyzedLines >= 100) break;

            Matcher matcher = DATE_PATTERN.matcher(line);
            while (matcher.find()) {
                analyzedLines++;
                processDateMatch(matcher, result);
            }
        }
        
        return result;
    }

    private static void processDateMatch(Matcher matcher, VotingResult result) {
        int firstNumber = Integer.parseInt(matcher.group(1));
        int secondNumber = Integer.parseInt(matcher.group(2));

        if (isDecisiveBRFormat(firstNumber, secondNumber)) {
            result.brFormatVotes += 5;
            result.decisiveVotes++;
        } else if (isDecisiveUSFormat(firstNumber, secondNumber)) {
            result.usFormatVotes += 5;
            result.decisiveVotes++;
        } else if (isAmbiguousCase(firstNumber, secondNumber)) {
            addAmbiguousVote(result);
        }
    }

    private static boolean isDecisiveBRFormat(int firstNumber, int secondNumber) {
        return firstNumber > 12 && firstNumber <= 31 && secondNumber <= 12;
    }

    private static boolean isDecisiveUSFormat(int firstNumber, int secondNumber) {
        return secondNumber > 12 && secondNumber <= 31 && firstNumber <= 12;
    }

    private static boolean isAmbiguousCase(int firstNumber, int secondNumber) {
        return firstNumber <= 12 && secondNumber <= 12;
    }

    private static void addAmbiguousVote(VotingResult result) {
        if (Locale.getDefault().getCountry().equals("BR")) {
            result.brFormatVotes += 1;
        } else {
            result.usFormatVotes += 1;
        }
    }

    private static DateFormat determineFormatFromVotes(VotingResult votes) {
        if (votes.decisiveVotes >= 3) {
            return (votes.brFormatVotes > votes.usFormatVotes) ? DateFormat.BR_FORMAT : DateFormat.US_FORMAT;
        } else if (votes.brFormatVotes > votes.usFormatVotes * 2) {
            return DateFormat.BR_FORMAT;
        } else if (votes.usFormatVotes > votes.brFormatVotes * 2) {
            return DateFormat.US_FORMAT;
        } else {
            return getDefaultDateFormat();
        }
    }

    private static void logDetectionResult(VotingResult votes) {
        System.out.println("Detected file date format: " + detectedFileFormat +
                " (BR votes: " + votes.brFormatVotes + ", US votes: " + votes.usFormatVotes +
                ", Decisive votes: " + votes.decisiveVotes + ")");
    }

    private static void initializeUserInputFormat() {
        if (userInputFormat == null) {
            userInputFormat = getDefaultDateFormat();
            System.out.println("Using system locale for user input date format: " + userInputFormat);
        }
    }

    private static class VotingResult {
        int usFormatVotes = 0;
        int brFormatVotes = 0;
        int decisiveVotes = 0;
    }

    /**
     * Determines the default date format based on system locale
     */
    private static DateFormat getDefaultDateFormat() {
        Locale locale = Locale.getDefault();
        return usesDayFirstFormat(locale) ? DateFormat.BR_FORMAT : DateFormat.US_FORMAT;
    }

    /**
     * Determines if a locale typically uses day-first date format
     */
    private static boolean usesDayFirstFormat(Locale locale) {
        java.text.DateFormat dateFormat = java.text.DateFormat.getDateInstance(
                java.text.DateFormat.SHORT, locale);

        if (dateFormat instanceof java.text.SimpleDateFormat) {
            String pattern = ((java.text.SimpleDateFormat) dateFormat).toPattern().toLowerCase();

            // Check if day appears before month in the pattern
            int dayIndex = pattern.indexOf('d');
            int monthIndex = pattern.indexOf('m');

            if (dayIndex >= 0 && monthIndex >= 0) {
                return dayIndex < monthIndex;
            }
        }

        // Fallback based on common locale conventions
        String country = locale.getCountry();
        return "BR,AR,ES,FR,IT,PT,DE,IN,RU,CH,AU,GB,IE,NZ,ZA".contains(country);
    }

    /**
     * Resets the detected format, useful when loading a new file
     */
    public static void resetDetectedFormat() {
        detectedFileFormat = null;
        // Do not reset userInputFormat - it should persist between file loads
    }

    /**
     * Infers the format used in a date string by examining its components
     * This is only used for dates that definitively reveal their format (like 13/01/2023)
     * @param dateString The date string to analyze
     * @return The inferred format or null if cannot be determined
     */
    public static DateFormat inferDateFormat(String dateString) {
        Matcher matcher = DATE_PATTERN.matcher(dateString);
        if (matcher.find()) {
            int firstNumber = Integer.parseInt(matcher.group(1));
            int secondNumber = Integer.parseInt(matcher.group(2));

            // Decisive case: if first number > 12, it must be DD/MM format
            if (firstNumber > 12 && firstNumber <= 31 && secondNumber <= 12) {
                return DateFormat.BR_FORMAT;
            }
            // Decisive case: if second number > 12, it must be MM/DD format
            else if (secondNumber > 12 && secondNumber <= 31 && firstNumber <= 12) {
                return DateFormat.US_FORMAT;
            }
        }
        return null; // Cannot determine format definitively
    }

    /**
     * Parse a date string using the detected or specified format
     */
    public static LocalDate parseDate(String dateString) throws DateTimeParseException {
        dateString = dateString.trim();

        // Try to infer the format from the date string itself
        // Only update userInputFormat if we can definitively determine it
        DateFormat inferredFormat = inferDateFormat(dateString);
        if (inferredFormat != null) {
            // Temporarily use the inferred format for this parsing only
            // but don't change the user's preferred format
            System.out.println("Inferred format from date string: " + inferredFormat);
            try {
                return parseDateWithFormat(dateString, inferredFormat);
            } catch (DateTimeParseException e) {
                // Continue to next approach if this fails
            }
        }

        // If user input format is set, try it first
        if (userInputFormat != null) {
            try {
                return parseDateWithFormat(dateString, userInputFormat);
            } catch (DateTimeParseException e) {
                // If parsing fails with user input format, continue with next approach
            }
        }

        // If format hasn't been detected yet, use system locale as fallback
        DateFormat format = detectedFileFormat != null ? detectedFileFormat : getDefaultDateFormat();

        try {
            return parseDateWithFormat(dateString, format);
        } catch (DateTimeParseException e) {
            // If parsing fails with the detected format, try the other format as fallback
            try {
                DateFormat alternateFormat = (format == DateFormat.US_FORMAT) ?
                        DateFormat.BR_FORMAT : DateFormat.US_FORMAT;

                LocalDate result = parseDateWithFormat(dateString, alternateFormat);

                // Do not update userInputFormat here - this is just a fallback

                return result;
            } catch (DateTimeParseException fallbackException) {
                // If both formats fail, throw the original exception
                throw e;
            }
        }
    }

    /**
     * Parse a date string using a specific format
     */
    private static LocalDate parseDateWithFormat(String dateString, DateFormat format) throws DateTimeParseException {
        if (dateString.matches("\\d{1,2}/\\d{1,2}/\\d{4}")) {
            // Long format (4-digit year)
            return LocalDate.parse(dateString,
                    format == DateFormat.US_FORMAT ? US_LONG_FORMATTER : BR_LONG_FORMATTER);
        } else if (dateString.matches("\\d{1,2}/\\d{1,2}/\\d{2}")) {
            // Short format (2-digit year)
            return LocalDate.parse(dateString,
                    format == DateFormat.US_FORMAT ? US_SHORT_FORMATTER : BR_SHORT_FORMATTER);
        }
        throw new DateTimeParseException("Invalid date format", dateString, 0);
    }

    /**
     * Parse a date from a line text (for WhatsApp and similar formats)
     */
    public static LocalDate parseLineDate(String datePart) {
        try {
            return parseWithFormat(datePart, getCurrentDateFormat());
        } catch (NumberFormatException | DateTimeParseException e) {
            // Try the opposite format as fallback
            try {
                DateFormat oppositeFormat = getCurrentDateFormat() == DateFormat.BR_FORMAT 
                    ? DateFormat.US_FORMAT : DateFormat.BR_FORMAT;
                return parseWithFormat(datePart, oppositeFormat);
            } catch (Exception ignored) {
                // Both approaches failed
            }
        } catch (Exception ignored) {
            // Any other exception
        }
        return null;
    }

    /**
     * Helper method to parse date string with a specific format
     */
    private static LocalDate parseWithFormat(String datePart, DateFormat format) {
        String[] parts = datePart.split("/");
        if (parts.length == 3) {
            int first = Integer.parseInt(parts[0]);
            int second = Integer.parseInt(parts[1]);
            int year = Integer.parseInt(parts[2]);

            // Adjust year if it's 2 digits
            if (year < 100) {
                year += 2000;
            }

            // Apply the specified format
            if (format == DateFormat.BR_FORMAT) {
                // Brazilian format: day/month/year
                return LocalDate.of(year, second, first);
            } else {
                // US format: month/day/year
                return LocalDate.of(year, first, second);
            }
        }
        throw new NumberFormatException("Invalid date parts");
    }

    /**
     * Format a date using the currently detected format
     */
    public static String formatDate(LocalDate date, boolean useShortYear) {
        if (date == null) return "";

        // For display purposes, use the user's local format preference
        DateFormat format = userInputFormat != null ? userInputFormat :
                (getDefaultDateFormat());

        if (format == DateFormat.US_FORMAT) {
            return date.format(useShortYear ? US_SHORT_FORMATTER : US_LONG_FORMATTER);
        } else {
            return date.format(useShortYear ? BR_SHORT_FORMATTER : BR_LONG_FORMATTER);
        }
    }

    /**
     * Gets a user-friendly date format string for display purposes
     */
    public static String getDateFormatExample() {
        // Use the user's preferred format for UI display
        DateFormat format = userInputFormat != null ? userInputFormat :
                (getDefaultDateFormat());
        return format == DateFormat.BR_FORMAT ? "DD/MM/YY" : "MM/DD/YY";
    }

    /**
     * Gets the currently detected file format
     */
    public static DateFormat getCurrentDateFormat() {
        return detectedFileFormat != null ? detectedFileFormat : getDefaultDateFormat();
    }

    /**
     * Gets the user input format
     */
    public static DateFormat getUserInputFormat() {
        return userInputFormat != null ? userInputFormat : getDefaultDateFormat();
    }

    /**
     * Sets the user input format explicitly
     */
    public static void setUserInputFormat(DateFormat format) {
        userInputFormat = format;
        System.out.println("User input date format set to: " + userInputFormat);
    }
}