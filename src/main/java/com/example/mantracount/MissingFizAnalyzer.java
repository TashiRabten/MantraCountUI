package com.example.mantracount;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Analyzes lines that may be mantra submissions missing the "fiz" word.
 * This is a separate analysis tool from the main mismatch detection.
 */
public class MissingFizAnalyzer {

    public static class MissingFizResult {
        private final String line;
        private final LocalDate date;
        private final String mantraKeyword;
        private final int mantraWordCount;
        private final int ritoWordCount;
        private final int extractedNumber;

        public MissingFizResult(String line, LocalDate date, String mantraKeyword,
                                int mantraWordCount, int ritoWordCount, int extractedNumber) {
            this.line = line;
            this.date = date;
            this.mantraKeyword = mantraKeyword;
            this.mantraWordCount = mantraWordCount;
            this.ritoWordCount = ritoWordCount;
            this.extractedNumber = extractedNumber;
        }

        public String getLine() { return line; }
        public LocalDate getDate() { return date; }
        public String getMantraKeyword() { return mantraKeyword; }
        public int getMantraWordCount() { return mantraWordCount; }
        public int getRitoWordCount() { return ritoWordCount; }
        public int getExtractedNumber() { return extractedNumber; }
        public int getTotalGenericCount() { return mantraWordCount + ritoWordCount; }
    }

    /**
     * Quick check to see if there are any missing fiz lines (for button state)
     */
    public static boolean hasMissingFizLines(List<String> allLines, LocalDate startDate, String mantraKeyword) {
        for (String line : allLines) {
            LocalDate lineDate = LineParser.extractDate(line);
            if (lineDate == null || lineDate.isBefore(startDate)) {
                continue;
            }

            if (ContentClassificationUtils.shouldExcludeFromCounting(line)) {
                continue;
            }

            if (MantraLineClassifier.isRelevantForSemFiz(line, mantraKeyword)) {
                return true;
            }
        }
        return false;
    }

    public static List<MissingFizResult> findMissingFizLines(List<String> allLines,
                                                             LocalDate startDate,
                                                             String mantraKeyword) {
        List<MissingFizResult> results = new ArrayList<>();

        for (String line : allLines) {
            LocalDate lineDate = LineParser.extractDate(line);
            if (lineDate == null || lineDate.isBefore(startDate)) {
                continue;
            }

            if (ContentClassificationUtils.shouldExcludeFromCounting(line)) {
                continue;
            }

            if (MantraLineClassifier.isRelevantForSemFiz(line, mantraKeyword)) {
                int mantraKeywordCount = LineAnalyzer.countOccurrencesWithWordBoundary(line, mantraKeyword);
                int mantraWordsCount = LineAnalyzer.countMantraOrMantras(line);
                int ritosWordsCount = LineAnalyzer.countRitoOrRitos(line);
                int extractedNumber = LineParser.extractFizNumber(line);
                if (mantraKeywordCount > 0) {
                    results.add(new MissingFizResult(
                            line, lineDate, mantraKeyword,
                            mantraWordsCount, ritosWordsCount, extractedNumber
                    ));
                }
            }
        }

        return results;
    }


    /**
     * Generate a summary of missing fiz analysis
     */
    public static String generateSummary(List<MissingFizResult> results, String mantraKeyword) {
        String capitalizedKeyword = MantrasDisplayController.capitalizeFirst(mantraKeyword);

        if (results.isEmpty()) {
            return "✅ Não foram encontradas linhas com padrão 'mantras/ritos de " + capitalizedKeyword + "' sem palavra de ação.";
        }

        StringBuilder summary = new StringBuilder();
        summary.append("📋 Análise 'Sem Fiz' para: ").append(capitalizedKeyword).append("\n");
        summary.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        summary.append("Total de linhas encontradas: ").append(results.size()).append("\n\n");

        int totalNumbers = 0;
        int totalGenericWords = 0;

        for (MissingFizResult result : results) {
            if (result.getExtractedNumber() > 0) {
                totalNumbers += result.getExtractedNumber();
            }
            totalGenericWords += result.getTotalGenericCount();
        }

        summary.append("📊 Resumo:\n");
        summary.append("• Total de 📿 extraídos: ").append(totalNumbers).append("\n");
        summary.append("• Total de palavras mantra(s)/rito(s): ").append(totalGenericWords).append("\n\n");

        summary.append("⚠ Estas linhas podem precisar da palavra 'Fiz' adicionada.\n");

        return summary.toString();
    }
}