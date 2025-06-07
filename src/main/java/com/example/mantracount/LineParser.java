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
            // Error parsing date from line, continue with null date
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
        } catch (Exception ex) {
            // Date parsing failed, return null
        }

        return null;
    }

    /**
     * Extracts date parts and creates a LocalDate, handling both formats
     */
    private static LocalDate extractDateParts(String datePart) {
        return DateParser.parseLineDate(datePart);
    }

    public static String formatDate(LocalDate date) {
        if (date == null) return "";
        return DateParser.formatDate(date, true);
    }

    public static LineSplitResult splitEditablePortion(String line) {
        line = line.replaceAll("[\\u200E\\u202A\\u202C\\uFEFF]", "").trim();

        if (line == null || line.trim().isEmpty()) {
            return new LineSplitResult("", "");
        }

        LineSplitResult result = tryProcessBracketFormat(line);
        if (result != null) return result;

        result = tryProcessAndroidFormat(line);
        if (result != null) return result;

        result = tryProcessDateSpaceFormat(line);
        if (result != null) return result;

        return createFallbackResult(line);
    }

    private static LineSplitResult tryProcessBracketFormat(String line) {
        if (!line.startsWith("[")) {
            return null;
        }

        int closeBracketPos = line.indexOf(']');
        if (closeBracketPos <= 0) {
            return null;
        }

        LineSplitResult result = tryExtractWithColon(line, closeBracketPos);
        return result != null ? result : tryExtractWithSpace(line, closeBracketPos);
    }

    private static LineSplitResult tryExtractWithColon(String line, int closeBracketPos) {
        int nameEnd = line.indexOf(':', closeBracketPos + 1);
        if (nameEnd > 0) {
            String fixedPrefix = line.substring(0, nameEnd + 1) + " ";
            String editableSuffix = line.substring(nameEnd + 1).trim();
            return new LineSplitResult(fixedPrefix, editableSuffix);
        }
        return null;
    }

    private static LineSplitResult tryExtractWithSpace(String line, int closeBracketPos) {
        int spaceAfterName = line.indexOf(' ', closeBracketPos + 1);
        if (spaceAfterName > 0) {
            String fixedPrefix = line.substring(0, spaceAfterName) + ": ";
            String editableSuffix = line.substring(spaceAfterName).trim();
            return new LineSplitResult(fixedPrefix, editableSuffix);
        }
        return null;
    }

    private static LineSplitResult tryProcessAndroidFormat(String line) {
        Matcher androidMatcher = ANDROID_DATE_PATTERN.matcher(line);
        if (!androidMatcher.find()) {
            return null;
        }

        int androidMatchEnd = androidMatcher.end();
        int nameEnd = line.indexOf(':', androidMatchEnd);

        if (nameEnd > 0) {
            String fixedPrefix = line.substring(0, nameEnd + 1) + " ";
            String editableSuffix = line.substring(nameEnd + 1).trim();
            return new LineSplitResult(fixedPrefix, editableSuffix);
        }
        return null;
    }

    private static LineSplitResult tryProcessDateSpaceFormat(String line) {
        int firstSpace = line.indexOf(" ");
        if (firstSpace <= 0 || !isValidDateFormat(line, firstSpace)) {
            return null;
        }

        int nameEnd = findFirstNonContextColonIndex(line, firstSpace + 1);
        if (nameEnd > 0) {
            String fixedPrefix = line.substring(0, nameEnd + 1);
            String editableSuffix = line.substring(nameEnd + 1).trim();
            return new LineSplitResult(fixedPrefix, editableSuffix);
        }
        return null;
    }

    private static boolean isValidDateFormat(String line, int firstSpace) {
        return line.substring(0, firstSpace).matches("\\d{1,2}/\\d{1,2}/\\d{2,4}");
    }

    private static LineSplitResult createFallbackResult(String line) {
        int fallbackColon = findFirstNonContextColonIndex(line, 0);
        if (fallbackColon > 0) {
            String fixedPrefix = line.substring(0, fallbackColon + 1);
            String editableSuffix = line.substring(fallbackColon + 1).trim();
            return new LineSplitResult(fixedPrefix, editableSuffix);
        } else {
            return new LineSplitResult("", line);
        }
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
        String lowerCase = line.toLowerCase();

        String[] mantraTypes = StringConstants.MANTRA_TYPES;

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

        String[] countIndicators = {"fiz", "recitei", "fez", "faz"};
        int result = StringUtils.findNumberAfterIndicators(line, countIndicators);
        return result == -1 ? 0 : result;
    }


    /**
     * Enhanced method to extract number from mantra lines.
     */
    public static int extractFizNumber(String line) {
        int number = tryExtractWithFizPattern(line);
        if (number != -1) return number;

        number = tryExtractNumberMantraPattern(line);
        if (number != -1) return number;

        number = tryExtractMantraActionPattern(line);
        if (number != -1) return number;

        number = tryExtractFromMessageContent(line);
        if (number != -1) return number;

        return LineAnalyzer.extractNumberAfterThirdColon(line);
    }

    private static int tryExtractWithFizPattern(String line) {
        Matcher matcher = FIZ_NUMBER_PATTERN.matcher(line.toLowerCase());
        if (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group(2));
            } catch (NumberFormatException e) {
                return -1;
            }
        }
        return -1;
    }

    private static int tryExtractNumberMantraPattern(String line) {
        Pattern numberMantraPattern = Pattern.compile("\\b([0-9]+)\\s+(mantras?|ritos?)\\b", Pattern.CASE_INSENSITIVE);
        Matcher matcher = numberMantraPattern.matcher(line.toLowerCase());
        if (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException e) {
                return -1;
            }
        }
        return -1;
    }

    private static int tryExtractMantraActionPattern(String line) {
        Pattern mantraActionNumberPattern = Pattern.compile("\\b(mantras?|ritos?)\\s+.*\\b(feitos?|completos?)\\s*([0-9]+)?\\b", Pattern.CASE_INSENSITIVE);
        Matcher matcher = mantraActionNumberPattern.matcher(line.toLowerCase());
        if (matcher.find() && matcher.group(3) != null) {
            try {
                return Integer.parseInt(matcher.group(3));
            } catch (NumberFormatException e) {
                return -1;
            }
        }
        return -1;
    }

    private static int tryExtractFromMessageContent(String line) {
        String messageContent = extractMessageContentOnly(line);
        if (messageContent == null || messageContent.isEmpty()) {
            return -1;
        }

        String lowerContent = messageContent.toLowerCase();
        if (!hasMantraRitoContent(lowerContent) || !ActionWordManager.hasActionWords(lowerContent)) {
            return -1;
        }

        return extractMaxNumberFromContent(lowerContent);
    }

    private static boolean hasMantraRitoContent(String lowerContent) {
        return lowerContent.contains("mantra") || lowerContent.contains("mantras") ||
               lowerContent.contains("rito") || lowerContent.contains("ritos");
    }

    private static int extractMaxNumberFromContent(String lowerContent) {
        Pattern numberPattern = Pattern.compile("\\b(\\d+)\\b");
        Matcher matcher = numberPattern.matcher(lowerContent);
        
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
        
        return foundNumbers.isEmpty() ? -1 : Collections.max(foundNumbers);
    }

    /**
     * Extract only the message content, excluding WhatsApp metadata
     */
    private static String extractMessageContentOnly(String line) {
        return ParsingUtils.extractMessageContent(line);
    }
}