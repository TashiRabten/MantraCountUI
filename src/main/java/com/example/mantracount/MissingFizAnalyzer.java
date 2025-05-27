package com.example.mantracount;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

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

            if (hasMantraDeKeywordPattern(line, mantraKeyword) && !hasActionWords(line)) {
                int mantraKeywordCount = LineAnalyzer.countOccurrencesWithWordBoundary(line, mantraKeyword);
                if (mantraKeywordCount > 0) {
                    return true; // Found at least one case
                }
            }
        }
        return false; // No cases found
    }

    /**
     * Finds lines that look like mantra submissions but are missing "fiz" words
     */
    public static List<MissingFizResult> findMissingFizLines(List<String> allLines,
                                                             LocalDate startDate,
                                                             String mantraKeyword) {
        List<MissingFizResult> results = new ArrayList<>();

        for (String line : allLines) {
            // Extract date
            LocalDate lineDate = LineParser.extractDate(line);
            if (lineDate == null || lineDate.isBefore(startDate)) {
                continue;
            }

            // Check if this line has the "mantras/ritos de keyword" pattern
            if (hasMantraDeKeywordPattern(line, mantraKeyword)) {
                // Make sure it doesn't already have "fiz" words (avoid duplicates with main detection)
                if (!hasActionWords(line)) {
                    // Count the components
                    int mantraKeywordCount = LineAnalyzer.countOccurrencesWithWordBoundary(line, mantraKeyword);
                    int mantraWordsCount = LineAnalyzer.countMantraOrMantras(line);
                    int ritosWordsCount = LineAnalyzer.countRitoOrRitos(line);
                    int extractedNumber = LineAnalyzer.extractNumberAfterThirdColon(line);

                    // Only include if we found the mantra keyword
                    if (mantraKeywordCount > 0) {
                        results.add(new MissingFizResult(
                                line, lineDate, mantraKeyword,
                                mantraWordsCount, ritosWordsCount, extractedNumber
                        ));
                    }
                }
            }
        }

        return results;
    }

    /**
     * Check for the flexible pattern: "mantra(s) [keyword]" or "rito(s) [keyword]"
     * Now works with Portuguese prepositions and without requiring them - catches patterns like:
     * - "540 mantras do Guru"
     * - "108 ritos preliminares"
     * - "mantras de refúgio"
     * - "27 mantras vajrasattva"
     */
    private static boolean hasMantraDeKeywordPattern(String line, String mantraKeyword) {
        String lineLower = line.toLowerCase();
        String keywordLower = mantraKeyword.toLowerCase();

        // Get all variants of the keyword
        Set<String> allVariants = SynonymManager.getAllVariants(keywordLower);

        // Check for flexible patterns: "mantra(s) [keyword]" or "rito(s) [keyword]"
        for (String variant : allVariants) {
            // Pattern 1: mantra(s) followed by keyword (with various Portuguese prepositions or none)
            String[] mantraPatterns = {
                    "mantras\\s+(de\\s+|do\\s+|da\\s+|dos\\s+|das\\s+)?" + Pattern.quote(variant),  // mantras [preposition] keyword
                    "mantra\\s+(de\\s+|do\\s+|da\\s+|dos\\s+|das\\s+)?" + Pattern.quote(variant),   // mantra [preposition] keyword
                    "ritos\\s+(de\\s+|do\\s+|da\\s+|dos\\s+|das\\s+)?" + Pattern.quote(variant),    // ritos [preposition] keyword
                    "rito\\s+(de\\s+|do\\s+|da\\s+|dos\\s+|das\\s+)?" + Pattern.quote(variant)      // rito [preposition] keyword
            };

            for (String patternStr : mantraPatterns) {
                Pattern pattern = Pattern.compile(patternStr, Pattern.CASE_INSENSITIVE);
                if (pattern.matcher(lineLower).find()) {
                    return true;
                }
            }

            // Pattern 2: keyword followed by mantra(s) - for cases like "refúgio mantras"
            String[] reversePatterns = {
                    Pattern.quote(variant) + "\\s+mantras",
                    Pattern.quote(variant) + "\\s+mantra",
                    Pattern.quote(variant) + "\\s+ritos",
                    Pattern.quote(variant) + "\\s+rito"
            };

            for (String patternStr : reversePatterns) {
                Pattern pattern = Pattern.compile(patternStr, Pattern.CASE_INSENSITIVE);
                if (pattern.matcher(lineLower).find()) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Check if line has action words (to avoid duplicates with main detection)
     */
    private static boolean hasActionWords(String line) {
        String lineLower = line.toLowerCase();
        String[] actionWords = {"fiz", "fez", "recitei", "faz", "completei", "feitos", "feito", "completo", "completos"};

        for (String action : actionWords) {
            if (lineLower.contains(action)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Generate a summary of missing fiz analysis
     */
    public static String generateSummary(List<MissingFizResult> results, String mantraKeyword) {
        // Capitalize the mantra keyword
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
        summary.append("• Total de números extraídos: ").append(totalNumbers).append("\n");
        summary.append("• Total de palavras mantra(s)/rito(s): ").append(totalGenericWords).append("\n\n");

        summary.append("⚠ Estas linhas podem precisar da palavra 'Fiz' adicionada.\n");

        return summary.toString();
    }
}