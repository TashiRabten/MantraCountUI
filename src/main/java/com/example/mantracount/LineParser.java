package com.example.mantracount;

import java.time.LocalDate;

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
        } catch (Exception e) {}

        if (MantraCount.hasApproximateMatch(line, mantraKeyword)) {
            int mantraKeywordCount = MantraCount.countOccurrencesWithWordBoundary(line, mantraKeyword);
            int mantraWordsCount = MantraCount.countMantraOrMantras(line);
            int fizCount = MantraCount.countOccurrencesWithWordBoundary(line, "fiz");

            data.setMantraKeywordCount(mantraKeywordCount);
            data.setMantraWordsCount(mantraWordsCount);
            data.setFizCount(fizCount);

            int fizNumber = MantraCount.extractNumberAfterThirdColon(line);
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

    private static boolean hasMismatch(int fizCount, int mantraWordsCount, int mantraKeywordCount, String mantraKeyword, String line) {
        return fizCount != mantraWordsCount ||
                mantraWordsCount != mantraKeywordCount ||
                MantraCount.hasApproximateButNotExactMatch(line, mantraKeyword);
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
}
