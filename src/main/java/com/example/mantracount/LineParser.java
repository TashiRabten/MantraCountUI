package com.example.mantracount;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
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
        public int getTotalGenericCount() { return mantraWordsCount + ritosWordsCount; } // Combined count
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
            // Extract date using the enhanced method that handles both formats
            LocalDate extractedDate = extractDate(line);
            if (extractedDate != null) {
                data.setDate(extractedDate);
            }
        } catch (Exception e) {
            System.out.print("Error parsing the line \n Erro extraindo a sentença");
        }

        if (LineAnalyzer.hasApproximateMatch(line, mantraKeyword)) {
            int mantraKeywordCount = LineAnalyzer.countOccurrencesWithWordBoundary(line, mantraKeyword);
            int mantraWordsCount = LineAnalyzer.countMantraOrMantras(line);
            int ritosWordsCount = LineAnalyzer.countRitoOrRitos(line); // Count rito/ritos
            int fizCount = LineAnalyzer.countOccurrencesWithWordBoundary(line, "fiz");

            data.setMantraKeywordCount(mantraKeywordCount);
            data.setMantraWordsCount(mantraWordsCount);
            data.setRitosWordsCount(ritosWordsCount); // Set ritos count
            data.setFizCount(fizCount);

            // Enhanced FizNumber extraction that works with both formats
            int fizNumber = extractFizNumber(line);
            if (fizNumber > 0) {
                data.setFizNumber(fizNumber);
            }

            // Use the combined count of mantras+ritos for mismatch detection
            boolean mismatch = hasMismatch(fizCount, mantraWordsCount + ritosWordsCount, mantraKeywordCount, mantraKeyword, line);
            data.setHasMismatch(mismatch);
        }

        return data;
    }

    /**
     * Enhanced method to extract number after "fiz" or similar words.
     * Works with both iOS and Android WhatsApp formats.
     */
    private static int extractFizNumber(String line) {
        // First try direct pattern matching
        Matcher matcher = FIZ_NUMBER_PATTERN.matcher(line.toLowerCase());
        if (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group(2));
            } catch (NumberFormatException e) {
                // Continue to other methods if this fails
            }
        }

        // Fall back to LineAnalyzer's method which now has improved extraction
        return LineAnalyzer.extractNumberAfterThirdColon(line);
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

    public static LocalDate extractDateFromLine(String line) {
        // Simply use the main extractDate method
        return extractDate(line);
    }

    private static boolean hasMismatch(int fizCount, int totalGenericCount, int mantraKeywordCount, String mantraKeyword, String line) {
        return fizCount != totalGenericCount ||
                totalGenericCount != mantraKeywordCount ||
                LineAnalyzer.hasApproximateButNotExactMatch(line, mantraKeyword);
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

    public static boolean hasApproximateMatch(String line, String keyword) {
        String lineLower = line.toLowerCase();
        String keywordLower = keyword.toLowerCase();

        boolean mantraFound = false;
        for (String word : lineLower.split("\\s+")) {
            if (isApproximateWordMatch(word, keywordLower)) {
                mantraFound = true;
                break;
            }
        }

        int colonIndex = lineLower.indexOf(": ");
        boolean fizFoundNearColon = false;
        if (colonIndex != -1) {
            int start = colonIndex + 2;
            int end = Math.min(lineLower.length(), start + 10);
            String afterColon = lineLower.substring(start, end);

            for (String word : afterColon.split("\\s+")) {
                if (levenshteinDistance(word, "fiz") <= 1) {
                    fizFoundNearColon = true;
                    break;
                }
            }
        }

        return mantraFound && fizFoundNearColon;
    }

    public static boolean hasApproximateButNotExactMatch(String line, String keyword) {
        String lineLower = line.toLowerCase();
        String keywordLower = keyword.toLowerCase();

        boolean exactMatch = Pattern.compile("\\b" + Pattern.quote(keywordLower) + "\\b",
                        Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE)
                .matcher(lineLower).find();
        if (exactMatch) return false;

        for (String word : lineLower.split("\\s+")) {
            if (isApproximateWordMatch(word, keywordLower) && !word.equals(keywordLower)) {
                return true;
            }
        }

        return false;
    }

    private static boolean isApproximateWordMatch(String word, String keyword) {
        int threshold;
        int keywordLength = keyword.length();

        if (keywordLength <= 3) return word.equals(keyword);
        else if (keywordLength <= 5) threshold = 1;
        else threshold = 2;

        if (word.startsWith(keyword) && word.length() > keyword.length() + threshold) {
            return false;
        }

        if (word.length() > keyword.length() * 1.5 && word.length() - keyword.length() > 3) {
            return false;
        }

        return levenshteinDistance(word, keyword) <= threshold;
    }

    private static int levenshteinDistance(String a, String b) {
        int[][] dp = new int[a.length() + 1][b.length() + 1];
        for (int i = 0; i <= a.length(); i++) {
            for (int j = 0; j <= b.length(); j++) {
                if (i == 0) dp[i][j] = j;
                else if (j == 0) dp[i][j] = i;
                else if (a.charAt(i - 1) == b.charAt(j - 1)) dp[i][j] = dp[i - 1][j - 1];
                else dp[i][j] = 1 + Math.min(dp[i - 1][j - 1],
                            Math.min(dp[i - 1][j], dp[i][j - 1]));
            }
        }
        return dp[a.length()][b.length()];
    }

    public static int countOccurrencesWithWordBoundary(String line, String keyword) {
        String keywordLower = keyword.toLowerCase();
        Pattern pattern = Pattern.compile("\\b" + Pattern.quote(keywordLower) + "\\b",
                Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
        Matcher matcher = pattern.matcher(line.toLowerCase());

        int count = 0;
        while (matcher.find()) count++;
        return count;
    }

    public static int countOccurrences(String line, String keyword) {
        String lineLower = line.toLowerCase();
        String keywordLower = keyword.toLowerCase();
        int count = 0;
        int index = 0;
        while ((index = lineLower.indexOf(keywordLower, index)) != -1) {
            count++;
            index += keywordLower.length();
        }
        return count;
    }

    public static int countMantraOrMantras(String line) {
        Pattern pattern = Pattern.compile("\\b(mantra|mantras)\\b", Pattern.UNICODE_CASE);
        Matcher matcher = pattern.matcher(line.toLowerCase());

        int count = 0;
        while (matcher.find()) count++;
        return count;
    }
}