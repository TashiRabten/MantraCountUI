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
        private int ritosWordsCount;
        private int fizNumber;
        private boolean hasMismatch;
        private AllMantrasUI allMantrasUI;

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

    private static final Pattern ANDROID_DATE_PATTERN = Pattern.compile("^(\\d{1,2}/\\d{1,2}/\\d{2,4})\\s+\\d{1,2}:\\d{1,2}\\s+-\\s+");

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

        if (MantraLineClassifier.isRelevantMantraEntry(line, mantraKeyword)) {
            int mantraKeywordCount = LineAnalyzer.countOccurrencesWithWordBoundary(line, mantraKeyword);
            int mantraWordsCount = LineAnalyzer.countMantraOrMantras(line);
            int ritosWordsCount = LineAnalyzer.countRitoOrRitos(line);

            int fizCount = ActionWordManager.countActionWords(line);

            data.setMantraKeywordCount(mantraKeywordCount);
            data.setMantraWordsCount(mantraWordsCount);
            data.setRitosWordsCount(ritosWordsCount);
            data.setFizCount(fizCount);

            int fizNumber = extractFizNumber(line);
            if (fizNumber > 0) {
                data.setFizNumber(fizNumber);
            }

            boolean mismatch = MantraLineClassifier.hasMismatchIssues(line, mantraKeyword,
                    fizCount, mantraWordsCount + ritosWordsCount, mantraKeywordCount);
            data.setHasMismatch(mismatch);
        }

        return data;
    }

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
            int startBracket = line.indexOf('[');
            int comma = line.indexOf(',');
            if (startBracket != -1 && comma != -1 && comma > startBracket + 1) {
                String datePart = line.substring(startBracket + 1, comma).trim();
                if (datePart.matches("\\d{1,2}/\\d{1,2}/\\d{2,4}")) {
                    return extractDateParts(datePart);
                }
            }

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

            if (year < 100) {
                year += 2000;
            }

            try {
                if (DateParser.getCurrentDateFormat() == DateParser.DateFormat.BR_FORMAT) {
                    return LocalDate.of(year, second, first);
                } else {
                    return LocalDate.of(year, first, second);
                }
            } catch (Exception e) {
                try {
                    if (DateParser.getCurrentDateFormat() == DateParser.DateFormat.BR_FORMAT) {
                        return LocalDate.of(year, first, second);
                    } else {
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
        return DateParser.formatDate(date, true);
    }

    public static LineSplitResult splitEditablePortion(String line) {
        line = line.replaceAll("[\\u200E\\u202A\\u202C\\uFEFF]", "").trim();
        String fixedPrefix = "";
        String editableSuffix = "";

        if (line == null || line.trim().isEmpty()) return new LineSplitResult("", "");

        if (line.startsWith("[")) {
            int closeBracketPos = line.indexOf(']');
            if (closeBracketPos > 0) {
                int nameEnd = line.indexOf(':', closeBracketPos + 1);

                if (nameEnd > 0) {
                    fixedPrefix = line.substring(0, nameEnd + 1) + " ";
                    editableSuffix = line.substring(nameEnd + 1).trim();
                    return new LineSplitResult(fixedPrefix, editableSuffix);
                }
                else {
                    int spaceAfterName = line.indexOf(' ', closeBracketPos + 1);
                    if (spaceAfterName > 0) {
                        fixedPrefix = line.substring(0, spaceAfterName) + ": ";
                        editableSuffix = line.substring(spaceAfterName).trim();
                        return new LineSplitResult(fixedPrefix, editableSuffix);
                    }
                }
            }
        }

        Matcher androidMatcher = ANDROID_DATE_PATTERN.matcher(line);
        if (androidMatcher.find()) {
            int androidMatchEnd = androidMatcher.end();
            int nameEnd = line.indexOf(':', androidMatchEnd);

            if (nameEnd > 0) {
                fixedPrefix = line.substring(0, nameEnd + 1) + " ";
                editableSuffix = line.substring(nameEnd + 1).trim();
                return new LineSplitResult(fixedPrefix, editableSuffix);
            }
        }

        int firstSpace = line.indexOf(" ");
        if (firstSpace > 0 && line.substring(0, firstSpace).matches("\\d{1,2}/\\d{1,2}/\\d{2,4}")) {
            int nameEnd = findFirstNonContextColonIndex(line, firstSpace + 1);
            if (nameEnd > 0) {
                fixedPrefix = line.substring(0, nameEnd + 1);
                editableSuffix = line.substring(nameEnd + 1).trim();
                return new LineSplitResult(fixedPrefix, editableSuffix);
            }
        }

        int fallbackColon = findFirstNonContextColonIndex(line, 0);
        if (fallbackColon > 0) {
            fixedPrefix = line.substring(0, fallbackColon + 1);
            editableSuffix = line.substring(fallbackColon + 1).trim();
        } else {
            fixedPrefix = "";
            editableSuffix = line;
        }

        return new LineSplitResult(fixedPrefix, editableSuffix);
    }

    private static int findFirstNonContextColonIndex(String line, int startPos) {
        boolean inTimeNotation = false;

        for (int i = startPos; i < line.length(); i++) {
            char c = line.charAt(i);

            if (i > 0 && Character.isDigit(line.charAt(i-1)) && c == ':' &&
                    i < line.length()-1 && Character.isDigit(line.charAt(i+1))) {
                inTimeNotation = true;
                continue;
            }

            if (inTimeNotation && !Character.isDigit(c) && c != ':') {
                inTimeNotation = false;
            }

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
        AllMantrasUI allMantrasUI = new AllMantrasUI();
        String lowerCase = line.toLowerCase();

        String[] mantraTypes =  allMantrasUI.mantraTypes;

        for (String type : mantraTypes) {
            if (lowerCase.contains(type)) {
                return type.substring(0, 1).toUpperCase() + type.substring(1);
            }
        }

        if (lowerCase.contains("mantra")) {
            return "Mantra";
        } else if (lowerCase.contains("rito")) {
            return "Rito";
        }

        return "Desconhecido";
    }

    public int extractMantraCount(String line) {
        Matcher matcher = FIZ_NUMBER_PATTERN.matcher(line.toLowerCase());
        if (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group(2));
            } catch (NumberFormatException e) {
                // Continue to next approach if this fails
            }
        }

        int extractedNumber = LineAnalyzer.extractNumberAfterThirdColon(line);
        if (extractedNumber > 0) {
            return extractedNumber;
        }

        String lowerCase = line.toLowerCase();
        String[] countIndicators = {"fiz", "recitei", "fez", "faz"};

        for (String indicator : countIndicators) {
            int position = lowerCase.indexOf(indicator);
            if (position >= 0) {
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
     * Enhanced method to extract number from mantra lines.
     */
    public static int extractFizNumber(String line) {
        Matcher matcher = FIZ_NUMBER_PATTERN.matcher(line.toLowerCase());
        if (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group(2));
            } catch (NumberFormatException e) {
                // Continue to other methods if this fails
            }
        }

        Pattern numberMantraPattern = Pattern.compile("\\b([0-9]+)\\s+(mantras?|ritos?)\\b", Pattern.CASE_INSENSITIVE);
        matcher = numberMantraPattern.matcher(line.toLowerCase());
        if (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException e) {
                // Continue to other methods if this fails
            }
        }

        Pattern mantraActionNumberPattern = Pattern.compile("\\b(mantras?|ritos?)\\s+.*\\b(feitos?|completos?)\\s*([0-9]+)?\\b", Pattern.CASE_INSENSITIVE);
        matcher = mantraActionNumberPattern.matcher(line.toLowerCase());
        if (matcher.find() && matcher.group(3) != null) {
            try {
                return Integer.parseInt(matcher.group(3));
            } catch (NumberFormatException e) {
                // Continue to other methods if this fails
            }
        }

        String messageContent = extractMessageContentOnly(line);
        if (messageContent != null && !messageContent.isEmpty()) {
            String lowerContent = messageContent.toLowerCase();

            boolean hasMantraRito = lowerContent.contains("mantra") || lowerContent.contains("mantras") ||
                    lowerContent.contains("rito") || lowerContent.contains("ritos");

            boolean hasActionWord = ActionWordManager.hasActionWords(lowerContent);

            if (hasMantraRito && hasActionWord) {
                Pattern numberPattern = Pattern.compile("\\b(\\d+)\\b");
                matcher = numberPattern.matcher(lowerContent);

                List<Integer> foundNumbers = new ArrayList<>();
                while (matcher.find()) {
                    try {
                        int num = Integer.parseInt(matcher.group(1));
                        if (num >= 1 && num <= 10000) {
                            foundNumbers.add(num);
                        }
                    } catch (NumberFormatException e) {
                        // Skip invalid numbers
                    }
                }

                if (!foundNumbers.isEmpty()) {
                    return Collections.max(foundNumbers);
                }
            }
        }

        return LineAnalyzer.extractNumberAfterThirdColon(line);
    }

    /**
     * Extract only the message content, excluding WhatsApp metadata
     */
    private static String extractMessageContentOnly(String line) {
        if (line.startsWith("[")) {
            int closeBracket = line.indexOf(']');
            if (closeBracket > 0) {
                int nameEnd = line.indexOf(':', closeBracket + 1);
                if (nameEnd > 0) {
                    return line.substring(nameEnd + 1).trim();
                }
            }
        }

        Pattern androidPattern = Pattern.compile("^(\\d{1,2}/\\d{1,2}/\\d{2,4})\\s+\\d{1,2}:\\d{1,2}\\s+-\\s+");
        Matcher androidMatcher = androidPattern.matcher(line);
        if (androidMatcher.find()) {
            int androidMatchEnd = androidMatcher.end();
            int nameEnd = line.indexOf(':', androidMatchEnd);
            if (nameEnd > 0) {
                return line.substring(nameEnd + 1).trim();
            }
        }

        int colonIndex = findFirstNonTimeColon(line);
        if (colonIndex > 0) {
            return line.substring(colonIndex + 1).trim();
        }

        return line;
    }

    /**
     * Find first colon that's not part of time notation
     */
    private static int findFirstNonTimeColon(String line) {
        for (int i = 1; i < line.length(); i++) {
            if (line.charAt(i) == ':') {
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