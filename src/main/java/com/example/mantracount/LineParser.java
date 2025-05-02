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

        // Getters and setters
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

        // Parse date
        try {
            int startBracket = line.indexOf('[');
            int comma = line.indexOf(',');

            if (startBracket != -1 && comma != -1 && comma > startBracket + 1) {
                String datePart = line.substring(startBracket + 1, comma).trim();

                if (datePart.matches("\\d{1,2}/\\d{1,2}/\\d{2}")) {
                    data.setDate(DateParser.parseLineDate(datePart));
                }
            }
        } catch (Exception e) {
            // Date parsing failed
        }

        // Only process lines with approximate mantra match
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

            boolean fizMismatch = fizCount != mantraWordsCount;
            boolean mantraWordsMismatch = mantraWordsCount != mantraKeywordCount;
            boolean mantraNameMismatch = MantraCount.hasApproximateButNotExactMatch(line, mantraKeyword);

            data.setHasMismatch(fizMismatch || mantraWordsMismatch || mantraNameMismatch);
        }

        return data;
    }
}
