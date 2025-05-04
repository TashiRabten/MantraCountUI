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
        public int getFizNumber() { return fizNumber; }
        public void setFizNumber(int number) { this.fizNumber = number; }
        public boolean hasMismatch() { return hasMismatch; }
        public void setHasMismatch(boolean mismatch) { this.hasMismatch = mismatch; }
    }

    public static LineData parseLine(String line, String mantraKeyword) {
        LineData data = new LineData();
        line = line.trim();

        try {
            int startBracket = line.indexOf('[');
            int comma = line.indexOf(',');

            if (startBracket != -1 && comma != -1 && comma > startBracket + 1) {
                String datePart = line.substring(startBracket + 1, comma).trim();
                if (datePart.matches("\\d{1,2}/\\d{1,2}/\\d{2,4}")) {
                    data.setDate(DateParser.parseLineDate(datePart));
                }
            }
        } catch (Exception e) { System.out.print("Error parsing the line \n Erro extraindo a sentença");}

        if (LineAnalyzer.hasApproximateMatch(line, mantraKeyword)) {
            int mantraKeywordCount = LineAnalyzer.countOccurrencesWithWordBoundary(line, mantraKeyword);
            int mantraWordsCount = LineAnalyzer.countMantraOrMantras(line);
            int fizCount = LineAnalyzer.countOccurrencesWithWordBoundary(line, "fiz");

            data.setMantraKeywordCount(mantraKeywordCount);
            data.setMantraWordsCount(mantraWordsCount);
            data.setFizCount(fizCount);

            int fizNumber = LineAnalyzer.extractNumberAfterThirdColon(line);
            if (fizNumber != -1) {
                data.setFizNumber(fizNumber);
            }

            boolean mismatch = hasMismatch(fizCount, mantraWordsCount, mantraKeywordCount, mantraKeyword, line);
            data.setHasMismatch(mismatch);
        }

        return data;
    }

    public static LocalDate extractDate(String line) {
        try {
            int startBracket = line.indexOf('[');
            int comma = line.indexOf(',');
            if (startBracket != -1 && comma != -1 && comma > startBracket + 1) {
                String datePart = line.substring(startBracket + 1, comma).trim();
                if (datePart.matches("\\d{1,2}/\\d{1,2}/\\d{2,4}")) {
                    return DateParser.parseLineDate(datePart);
                }
            }
        } catch (Exception ignored) {}
        return null;
    }
    public static String formatDate(LocalDate date) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd/yy");  // Or use "dd/MM/yyyy" based on locale
        return date.format(formatter);
    }

    public static LocalDate extractDateFromLine(String line) {
        try {
            int startBracket = line.indexOf('[');
            int comma = line.indexOf(',');

            if (startBracket != -1 && comma != -1 && comma > startBracket + 1) {
                String datePart = line.substring(startBracket + 1, comma).trim();
                return DateParser.parseDate(datePart);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static boolean hasMismatch(int fizCount, int mantraWordsCount, int mantraKeywordCount, String mantraKeyword, String line) {
        return fizCount != mantraWordsCount ||
                mantraWordsCount != mantraKeywordCount ||
                LineAnalyzer.hasApproximateButNotExactMatch(line, mantraKeyword);
    }

    public static LineSplitResult splitEditablePortion(String line) {
        line = line.replaceAll("[\\u200E\\u202A\\u202C\\uFEFF]", "").trim();
        String fixedPrefix = line;
        String editableSuffix = "";

        if (line == null || line.trim().isEmpty()) return new LineSplitResult("", "");

        int closeBracketPos = -1;
        if (line.startsWith("[")) {
            for (int i = 1; i < line.length() - 1; i++) {
                if (line.charAt(i) == ']' && line.charAt(i + 1) == ' ') {
                    closeBracketPos = i;
                    break;
                }
            }
            if (closeBracketPos > 0) {
                int nameStart = closeBracketPos + 2;
                int nameEnd = findColonSplitPoint(line, nameStart);
                if (nameEnd > 0) {
                    fixedPrefix = line.substring(0, nameEnd + 1);
                    editableSuffix = line.substring(nameEnd + 1).trim();
                    return new LineSplitResult(fixedPrefix, editableSuffix);
                }
            }
        }

        int firstSpace = line.indexOf(" ");
        if (firstSpace > 0 && line.substring(0, firstSpace).matches("\\d{1,2}/\\d{1,2}/\\d{2,4}")) {
            int nameEnd = findColonSplitPoint(line, firstSpace + 1);
            if (nameEnd > 0) {
                fixedPrefix = line.substring(0, nameEnd + 1);
                editableSuffix = line.substring(nameEnd + 1).trim();
                return new LineSplitResult(fixedPrefix, editableSuffix);
            }
        }

        int fallbackColon = line.indexOf(":");
        if (fallbackColon > 0) {
            fixedPrefix = line.substring(0, fallbackColon + 1);
            editableSuffix = line.substring(fallbackColon + 1).trim();
        }

        return new LineSplitResult(fixedPrefix, editableSuffix);
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

    public static int extractNumberAfterThirdColon(String line) {
        int firstColon = line.indexOf(":");
        if (firstColon == -1) return -1;

        int secondColon = line.indexOf(":", firstColon + 1);
        if (secondColon == -1) return -1;

        int thirdColon = line.indexOf(":", secondColon + 1);
        if (thirdColon == -1 || thirdColon + 1 >= line.length()) return -1;

        String afterThirdColon = line.substring(thirdColon + 1).trim();
        Matcher matcher = Pattern.compile("\\d+").matcher(afterThirdColon);

        if (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group());
            } catch (NumberFormatException e) {
                return -1;
            }
        }
        return -1;
    }
}
