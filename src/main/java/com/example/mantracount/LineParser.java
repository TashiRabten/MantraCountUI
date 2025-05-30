package com.example.mantracount;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LineParser {
    public static class LineData {
        private LocalDate date;
        private int mantraKeywordCount;
        private int fizCount;
        private int mantraWordsCount;
        private int ritosWordsCount; // New field for rito/ritos count
        private int fizNumber;
        private boolean hasMismatch;

        public LocalDate getDate() { return date; }
        public void setDate(LocalDate date) { this.date = date; }
        public int getMantraKeywordCount() { return mantraKeywordCount; }
        public void setMantraKeywordCount(int count) { this.mantraKeywordCount = count; }
        public int getFizCount() { return fizCount; }
        public void setFizCount(int count) { this.fizCount = count; }
        public int getMantraWordsCount() { return mantraWordsCount; }
        public void setMantraWordsCount(int count) { this.mantraWordsCount = count; }
        public int getRitosWordsCount() { return ritosWordsCount; }
        public void setRitosWordsCount(int count) { this.ritosWordsCount = count; }

        public int getFizNumber() { return fizNumber; }
        public void setFizNumber(int number) { this.fizNumber = number; }
        public boolean hasMismatch() { return hasMismatch; }
        public void setHasMismatch(boolean mismatch) { this.hasMismatch = mismatch; }
    }

    // Pattern for the Android WhatsApp date format: DD/MM/YYYY HH:MM - Name:
    private static final Pattern ANDROID_DATE_PATTERN = Pattern.compile("^(\\d{1,2}/\\d{1,2}/\\d{2,4})\\s+\\d{1,2}:\\d{1,2}\\s+-\\s+");

    // Pattern to extract numbers after fizKeywords
    private static final Pattern FIZ_NUMBER_PATTERN =
            Pattern.compile("\\b(fiz|fez|recitei|faz)\\s+([0-9]+)\\b", Pattern.CASE_INSENSITIVE);

    public static LineData parseLine(String line, String mantraKeyword) {
        LineData data = new LineData();
        line = line.trim();

        try {
            LocalDate extractedDate = extractDate(line);
            if (extractedDate != null) {
                data.setDate(extractedDate);
            }
        } catch (Exception e) {
            System.out.print("Error parsing the line \n Erro extraindo a sentença");
        }

        // Use shared classification logic
        if (MantraLineClassifier.isRelevantMantraEntry(line, mantraKeyword)) {
            int mantraKeywordCount = LineAnalyzer.countOccurrencesWithWordBoundary(line, mantraKeyword);
            int mantraWordsCount = LineAnalyzer.countMantraOrMantras(line);
            int ritosWordsCount = LineAnalyzer.countRitoOrRitos(line);

            // Use centralized action word counting
            int fizCount = ActionWordManager.countActionWords(line);

            data.setMantraKeywordCount(mantraKeywordCount);
            data.setMantraWordsCount(mantraWordsCount);
            data.setRitosWordsCount(ritosWordsCount);
            data.setFizCount(fizCount);

            int fizNumber = extractFizNumber(line);
            if (fizNumber > 0) {
                data.setFizNumber(fizNumber);
            }

            // Use shared mismatch detection logic
            boolean mismatch = MantraLineClassifier.hasMismatchIssues(line, mantraKeyword,
                    fizCount, mantraWordsCount + ritosWordsCount, mantraKeywordCount);
            data.setHasMismatch(mismatch);
        }

        return data;
    }

    // Remove the old methods since they're now in MantraLineClassifier
    // hasMismatch() and hasNumbersInEditablePortionForMantraContext() are replaced

    /**
     * Helper method to check if text contains numbers
     */
    private static boolean containsNumbers(String text) {
        if (text == null) return false;
        return text.matches(".*\\d+.*");
    }

    public static LocalDate extractDate(String line) {
        if (line == null || line.isEmpty()) {
            return null;
        }

        try {
            // First, try iPhone format with brackets: [DD/MM/YY, HH:MM:SS]
            int startBracket = line.indexOf('[');
            int comma = line.indexOf(',');
            if (startBracket != -1 && comma != -1 && comma > startBracket + 1) {
                String datePart = line.substring(startBracket + 1, comma).trim();
                if (datePart.matches("\\d{1,2}/\\d{1,2}/\\d{2,4}")) {
                    return extractDateParts(datePart);
                }
            }

            // Next, try Android format: DD/MM/YYYY HH:MM - Name:
            Matcher androidMatcher = ANDROID_DATE_PATTERN.matcher(line);
            if (androidMatcher.find()) {
                String datePart = androidMatcher.group(1).trim();
                if (datePart.matches("\\d{1,2}/\\d{1,2}/\\d{2,4}")) {
                    return extractDateParts(datePart);
                }
            }
        } catch (Exception ignored) {}

        return null;
    }

    /**
     * Extracts date parts and creates a LocalDate, handling both formats
     */
    private static LocalDate extractDateParts(String datePart) {
        String[] parts = datePart.split("/");
        if (parts.length == 3) {
            int first = Integer.parseInt(parts[0]);
            int second = Integer.parseInt(parts[1]);
            int year = Integer.parseInt(parts[2]);

            // Adjust year if it's 2 digits
            if (year < 100) {
                year += 2000;
            }

            // Try with file format first
            try {
                if (DateParser.getCurrentDateFormat() == DateParser.DateFormat.BR_FORMAT) {
                    // Brazilian format: day/month/year
                    return LocalDate.of(year, second, first);
                } else {
                    // US format: month/day/year
                    return LocalDate.of(year, first, second);
                }
            } catch (Exception e) {
                // If that fails, try the opposite format
                try {
                    if (DateParser.getCurrentDateFormat() == DateParser.DateFormat.BR_FORMAT) {
                        // Try US format instead
                        return LocalDate.of(year, first, second);
                    } else {
                        // Try BR format instead
                        return LocalDate.of(year, second, first);
                    }
                } catch (Exception ignored) {
                    // Both attempts failed
                }
            }
        }
        return null;
    }

    public static String formatDate(LocalDate date) {
        if (date == null) return "";
        return DateParser.formatDate(date, true); // Use the detected format with 2-digit year
    }

    // Updated to handle both WhatsApp formats
    public static LineSplitResult splitEditablePortion(String line) {
        line = line.replaceAll("[\\u200E\\u202A\\u202C\\uFEFF]", "").trim();
        String fixedPrefix = "";
        String editableSuffix = "";

        if (line == null || line.trim().isEmpty()) return new LineSplitResult("", "");

        // Handle iPhone WhatsApp format: [date, time] Name: Message
        if (line.startsWith("[")) {
            int closeBracketPos = line.indexOf(']');
            if (closeBracketPos > 0) {
                // Find the colon after the name
                int nameEnd = line.indexOf(':', closeBracketPos + 1);

                // If there's a proper colon separator
                if (nameEnd > 0) {
                    fixedPrefix = line.substring(0, nameEnd + 1) + " ";  // Add space after colon
                    editableSuffix = line.substring(nameEnd + 1).trim();
                    return new LineSplitResult(fixedPrefix, editableSuffix);
                }
                // If no colon found, look for the first space after the name as fallback
                else {
                    int spaceAfterName = line.indexOf(' ', closeBracketPos + 1);
                    if (spaceAfterName > 0) {
                        fixedPrefix = line.substring(0, spaceAfterName) + ": ";  // Add colon and space
                        editableSuffix = line.substring(spaceAfterName).trim();
                        return new LineSplitResult(fixedPrefix, editableSuffix);
                    }
                }
            }
        }

        // Handle Android WhatsApp format: DD/MM/YYYY HH:MM - Name: Message
        Matcher androidMatcher = ANDROID_DATE_PATTERN.matcher(line);
        if (androidMatcher.find()) {
            int androidMatchEnd = androidMatcher.end();
            int nameEnd = line.indexOf(':', androidMatchEnd);

            if (nameEnd > 0) {
                fixedPrefix = line.substring(0, nameEnd + 1) + " ";  // Add space after colon
                editableSuffix = line.substring(nameEnd + 1).trim();
                return new LineSplitResult(fixedPrefix, editableSuffix);
            }
        }

        // Try date format without brackets
        int firstSpace = line.indexOf(" ");
        if (firstSpace > 0 && line.substring(0, firstSpace).matches("\\d{1,2}/\\d{1,2}/\\d{2,4}")) {
            int nameEnd = findFirstNonContextColonIndex(line, firstSpace + 1);
            if (nameEnd > 0) {
                fixedPrefix = line.substring(0, nameEnd + 1);
                editableSuffix = line.substring(nameEnd + 1).trim();
                return new LineSplitResult(fixedPrefix, editableSuffix);
            }
        }

        // Fallback to first colon
        int fallbackColon = findFirstNonContextColonIndex(line, 0);
        if (fallbackColon > 0) {
            fixedPrefix = line.substring(0, fallbackColon + 1);
            editableSuffix = line.substring(fallbackColon + 1).trim();
        } else {
            // If no colon found, make the entire line editable
            fixedPrefix = "";
            editableSuffix = line;
        }

        return new LineSplitResult(fixedPrefix, editableSuffix);
    }

    private static int findFirstNonContextColonIndex(String line, int startPos) {
        // Skip colons that are part of time notations (e.g., 10:30)
        boolean inTimeNotation = false;

        for (int i = startPos; i < line.length(); i++) {
            char c = line.charAt(i);

            // Check for time notation pattern
            if (i > 0 && Character.isDigit(line.charAt(i-1)) && c == ':' &&
                    i < line.length()-1 && Character.isDigit(line.charAt(i+1))) {
                inTimeNotation = true;
                continue;
            }

            // Reset time notation flag after passing the time
            if (inTimeNotation && !Character.isDigit(c) && c != ':') {
                inTimeNotation = false;
            }

            // Return position of colon if not in time notation
            if (!inTimeNotation && c == ':' &&
                    (i + 1 == line.length() || Character.isWhitespace(line.charAt(i + 1)) || line.charAt(i + 1) == '<')) {
                return i;
            }
        }
        return -1;
    }

    private static int findColonSplitPoint(String line, int startPos) {
        for (int i = startPos; i < line.length(); i++) {
            if (line.charAt(i) == ':' && (i + 1 == line.length() || Character.isWhitespace(line.charAt(i + 1)) || line.charAt(i + 1) == '<')) {
                return i;
            }
        }
        return -1;
    }

    public static class LineSplitResult {
        private final String fixedPrefix;
        private final String editableSuffix;

        public LineSplitResult(String fixedPrefix, String editableSuffix) {
            this.fixedPrefix = fixedPrefix;
            this.editableSuffix = editableSuffix;
        }

        public String getFixedPrefix() { return fixedPrefix; }
        public String getEditableSuffix() { return editableSuffix; }
    }

    public boolean containsMantraContent(String line) {
        return MantraLineClassifier.isRelevantForAllMantras(line);
    }

    public String extractMantraType(String line) {
        String lowerCase = line.toLowerCase();

        // Common mantra types - expand based on your needs
        String[] mantraTypes = {"refúgio", "vajrasattva", "refugio", "guru", "bodisatva", "guru",
                "bodhisattva", "buda", "buddha", "tara", "medicine", "medicina", "preliminares", "tare"};

        for (String type : mantraTypes) {
            if (lowerCase.contains(type)) {
                // Capitalize first letter for display
                return type.substring(0, 1).toUpperCase() + type.substring(1);
            }
        }

        // If no specific type found
        if (lowerCase.contains("mantra")) {
            return "Mantra";
        } else if (lowerCase.contains("rito")) {
            return "Rito";
        }

        return "Desconhecido";
    }

    public int extractMantraCount(String line) {
        // First try direct pattern matching
        Matcher matcher = FIZ_NUMBER_PATTERN.matcher(line.toLowerCase());
        if (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group(2));
            } catch (NumberFormatException e) {
                // Continue to next approach if this fails
            }
        }

        // Next try with LineAnalyzer's improved method
        int extractedNumber = LineAnalyzer.extractNumberAfterThirdColon(line);
        if (extractedNumber > 0) {
            return extractedNumber;
        }

        // Fallback to simple number extraction after "fiz" or similar words
        String lowerCase = line.toLowerCase();
        String[] countIndicators = {"fiz", "recitei", "fez", "faz"};

        for (String indicator : countIndicators) {
            int position = lowerCase.indexOf(indicator);
            if (position >= 0) {
                // Look for a number after the indicator
                String afterIndicator = lowerCase.substring(position + indicator.length());
                return extractFirstNumber(afterIndicator);
            }
        }

        return 0;
    }

    private int extractFirstNumber(String text) {
        StringBuilder numberBuilder = new StringBuilder();
        boolean foundDigit = false;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);

            if (Character.isDigit(c)) {
                numberBuilder.append(c);
                foundDigit = true;
            } else if (foundDigit) {
                // Stop after the first sequence of digits
                break;
            }
        }

        if (numberBuilder.length() > 0) {
            try {
                return Integer.parseInt(numberBuilder.toString());
            } catch (NumberFormatException e) {
                return 0;
            }
        }

        return 0;
    }

    /**
     * UPDATED: Enhanced method to extract number from mantra lines.
     * Now uses centralized ActionWordManager for action word detection
     */
    public static int extractFizNumber(String line) {
        // Method 1: Direct pattern matching for "fiz/recitei + number"
        Matcher matcher = FIZ_NUMBER_PATTERN.matcher(line.toLowerCase());
        if (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group(2));
            } catch (NumberFormatException e) {
                // Continue to other methods if this fails
            }
        }

        // Method 2: Number + mantras/ritos pattern (108 mantras, 72 ritos)
        Pattern numberMantraPattern = Pattern.compile("\\b([0-9]+)\\s+(mantras?|ritos?)\\b", Pattern.CASE_INSENSITIVE);
        matcher = numberMantraPattern.matcher(line.toLowerCase());
        if (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException e) {
                // Continue to other methods if this fails
            }
        }

        // Method 3: Mantras/ritos + action words + number pattern (mantras feitos 72)
        Pattern mantraActionNumberPattern = Pattern.compile("\\b(mantras?|ritos?)\\s+.*\\b(feitos?|completos?)\\s*([0-9]+)?\\b", Pattern.CASE_INSENSITIVE);
        matcher = mantraActionNumberPattern.matcher(line.toLowerCase());
        if (matcher.find() && matcher.group(3) != null) {
            try {
                return Integer.parseInt(matcher.group(3));
            } catch (NumberFormatException e) {
                // Continue to other methods if this fails
            }
        }

        // Method 4: Look for any number near mantras/ritos and action words
        String messageContent = extractMessageContentOnly(line);
        if (messageContent != null && !messageContent.isEmpty()) {
            String lowerContent = messageContent.toLowerCase();

            // Check if line has both mantras/ritos AND action words
            boolean hasMantraRito = lowerContent.contains("mantra") || lowerContent.contains("mantras") ||
                    lowerContent.contains("rito") || lowerContent.contains("ritos");

            // UPDATED: Use centralized action word detection
            boolean hasActionWord = ActionWordManager.hasActionWords(lowerContent);

            if (hasMantraRito && hasActionWord) {
                // Find all numbers in the message content
                Pattern numberPattern = Pattern.compile("\\b(\\d+)\\b");
                matcher = numberPattern.matcher(lowerContent);

                List<Integer> foundNumbers = new ArrayList<>();
                while (matcher.find()) {
                    try {
                        int num = Integer.parseInt(matcher.group(1));
                        // Filter reasonable mantra counts (typically 1-10000)
                        if (num >= 1 && num <= 10000) {
                            foundNumbers.add(num);
                        }
                    } catch (NumberFormatException e) {
                        // Skip invalid numbers
                    }
                }

                // Return the largest number found (likely the mantra count)
                if (!foundNumbers.isEmpty()) {
                    return Collections.max(foundNumbers);
                }
            }
        }

        // Method 5: Fall back to LineAnalyzer's method
        return LineAnalyzer.extractNumberAfterThirdColon(line);
    }

    /**
     * Extract only the message content, excluding WhatsApp metadata
     */
    private static String extractMessageContentOnly(String line) {
        // Handle iPhone WhatsApp format: [date, time] Name: Message
        if (line.startsWith("[")) {
            int closeBracket = line.indexOf(']');
            if (closeBracket > 0) {
                int nameEnd = line.indexOf(':', closeBracket + 1);
                if (nameEnd > 0) {
                    return line.substring(nameEnd + 1).trim();
                }
            }
        }

        // Handle Android WhatsApp format: DD/MM/YYYY HH:MM - Name: Message
        Pattern androidPattern = Pattern.compile("^(\\d{1,2}/\\d{1,2}/\\d{2,4})\\s+\\d{1,2}:\\d{1,2}\\s+-\\s+");
        Matcher androidMatcher = androidPattern.matcher(line);
        if (androidMatcher.find()) {
            int androidMatchEnd = androidMatcher.end();
            int nameEnd = line.indexOf(':', androidMatchEnd);
            if (nameEnd > 0) {
                return line.substring(nameEnd + 1).trim();
            }
        }

        // Fallback: try to find first colon that's not part of time notation
        int colonIndex = findFirstNonTimeColon(line);
        if (colonIndex > 0) {
            return line.substring(colonIndex + 1).trim();
        }

        // If no format detected, return the whole line
        return line;
    }

    /**
     * Find first colon that's not part of time notation
     */
    private static int findFirstNonTimeColon(String line) {
        for (int i = 1; i < line.length(); i++) {
            if (line.charAt(i) == ':') {
                // Check if this colon is part of time notation (digit:digit)
                boolean isTimeColon = (i > 0 && Character.isDigit(line.charAt(i - 1))) &&
                        (i < line.length() - 1 && Character.isDigit(line.charAt(i + 1)));

                if (!isTimeColon) {
                    return i;
                }
            }
        }
        return -1;
    }
}