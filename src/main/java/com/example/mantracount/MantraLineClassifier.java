package com.example.mantracount;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * Centralized classifier for determining if lines are relevant mantra count entries.
 * This eliminates duplication between counting logic and mismatch detection.
 */
public class MantraLineClassifier {

    /**
     * Determines if a line is a relevant mantra count entry for main processing.
     * Requirements: numbers + keyword approximation + action words
     *
     * @param line The line to classify
     * @param mantraKeyword The keyword being searched for
     * @return true if the line should be processed as a mantra entry
     */
    public static boolean isRelevantMantraEntry(String line, String mantraKeyword) {
        if (!hasNumbersInEditablePortion(line)) {
            return false;
        }

        if (!hasKeywordApproximation(line, mantraKeyword)) {
            return false;
        }

        return ActionWordManager.hasActionWords(line);
    }

    public static boolean isRelevantForAllMantras(String line) {
        if (!hasNumbersInEditablePortion(line)) {
            return false;
        }

        if (!hasExplicitMantraRitoWords(line)) {
            return false;
        }

        String messageContent = MessageContentManager.extractMessageContent(line);
        if (messageContent == null || messageContent.isEmpty()) {
            return false;
        }

        String messageLower = messageContent.toLowerCase();

        String[] excludePatterns = {
                "por \\d+ minutos",
                "durante \\d+ minutos",
                "em \\d+ minutos",
                "demoro \\d+",
                "mantras? por minuto",
                "mantras? por hora",
                "mantras?/min",
                "por dia",
                "item",
                "imagine[im]",
                "mente que",
                "quando faço",
                "associação conceitual",
                "principio eu",
                "direcion",
                "forma[rn]do",
                "óctuplo",
                "preciso esclarecer",
                "algo de fato",
                "retardo",
                "discurso",
                "faculdade",
                "provas com",
                "de \\d+ minutos de prática"
        };

        for (String pattern : excludePatterns) {
            if (Pattern.compile(pattern, Pattern.CASE_INSENSITIVE).matcher(messageLower).find()) {
                return false;
            }
        }

        boolean hasActionWord = ActionWordManager.hasActionWords(line);

        boolean hasCountingPattern = Pattern.compile(
                "^[^.!?]*\\b\\d{2,}\\s+(mantras?|ritos?)\\b|" +
                        "\\b(mantras?|ritos?)\\s+[^.!?]*?\\b\\d{2,}\\b|" +
                        "ofere[çc]o\\s+[\\d.,]+\\s*(mantras?|ritos?)|" +
                        "completo\\s*[-–]\\s*\\d+|" +
                        "\\b\\d{2,}-\\d{2,}\\s+(mantras?|ritos?)"
                , Pattern.CASE_INSENSITIVE).matcher(messageLower).find();

        if (messageLower.matches(".*\\d+[.,]\\d+\\s*(mantras?|ritos?).*")) {
            return false;
        }

        return hasActionWord || hasCountingPattern;
    }

    /**
     * Determines if a line is relevant for Sem Fiz feature.
     * Requirements: numbers + specific mantra pattern + NO action words + NO problematic descriptors
     *
     * @param line The line to classify
     * @param mantraKeyword The keyword being searched for
     * @return true if the line should be shown in Sem Fiz
     */
    public static boolean isRelevantForSemFiz(String line, String mantraKeyword) {
        if (!hasNumbersInEditablePortion(line)) {
            return false;
        }

        if (hasProblematicDescriptors(line)) {
            return false;
        }

        if (!hasMantraDeKeywordPattern(line, mantraKeyword)) {
            return false;
        }

        boolean hasFiz = ActionWordManager.hasFizWord(line);
        boolean result = !hasFiz;

        return result;
    }

    /**
     * Filter out lines with problematic descriptors that indicate duration/measurement talk
     * rather than actual mantra counting entries
     */
    private static boolean hasProblematicDescriptors(String line) {
        String lineLower = line.toLowerCase();

        String[] problematicPatterns = {
                "por minuto", "por hora", "minutos para", "horas para",
                "demoro", "duração", "tempo para", "levo.*minutos",
                "desde que comecei", "ganhei.*por", "desenvolvi"
        };

        return java.util.Arrays.stream(problematicPatterns)
                .anyMatch(pattern -> {
                    if (pattern.contains(".*")) {
                        return java.util.regex.Pattern.compile(pattern).matcher(lineLower).find();
                    } else {
                        return lineLower.contains(pattern);
                    }
                });
    }

    /**
     * Check for the flexible pattern: "mantra(s) [keyword]" or "rito(s) [keyword]"
     */
    private static boolean hasMantraDeKeywordPattern(String line, String mantraKeyword) {
        String lineLower = line.toLowerCase();
        String keywordLower = mantraKeyword.toLowerCase();

        Set<String> allVariants = SynonymManager.getAllVariants(keywordLower);

        for (String variant : allVariants) {
            String[] mantraPatterns = {
                    "mantras\\s+(de\\s+|do\\s+|da\\s+|dos\\s+|das\\s+)?" + Pattern.quote(variant),
                    "mantra\\s+(de\\s+|do\\s+|da\\s+|dos\\s+|das\\s+)?" + Pattern.quote(variant),
                    "ritos\\s+(de\\s+|do\\s+|da\\s+|dos\\s+|das\\s+)?" + Pattern.quote(variant),
                    "rito\\s+(de\\s+|do\\s+|da\\s+|dos\\s+|das\\s+)?" + Pattern.quote(variant)
            };

            for (String patternStr : mantraPatterns) {
                Pattern pattern = Pattern.compile(patternStr, Pattern.CASE_INSENSITIVE);
                if (pattern.matcher(lineLower).find()) {
                    return true;
                }
            }

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
     * Check if line has approximation to the keyword
     */
    private static boolean hasKeywordApproximation(String line, String keyword) {
        String lineLower = line.toLowerCase();
        String keywordLower = keyword.toLowerCase();

        Set<String> allVariants = SynonymManager.getAllVariants(keywordLower);
        for (String word : lineLower.split("\\s+")) {
            String cleanWord = word.replaceAll("[^a-záàâãéêíóôõúüç]", "");
            if (allVariants.contains(cleanWord) || isApproximateWordMatch(cleanWord, keywordLower)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasExplicitMantraRitoWords(String line) {
        String lineLower = line.toLowerCase();

        Pattern mantraPattern = Pattern.compile("\\b(mantra|mantras|rito|ritos)\\b", Pattern.CASE_INSENSITIVE);
        return mantraPattern.matcher(lineLower).find();
    }

    /**
     * Check if line has numbers in the editable portion only
     */
    private static boolean hasNumbersInEditablePortion(String line) {
        LineParser.LineSplitResult splitResult = LineParser.splitEditablePortion(line);
        String editablePart = splitResult.getEditableSuffix();

        if (editablePart == null || editablePart.trim().isEmpty()) {
            return false;
        }

        return editablePart.matches(".*\\d+.*");
    }

    public static boolean isApproximateWordMatch(String word, String keyword) {
        Set<String> commonWordBlacklist = Set.of(
                "para", "pela", "pelo", "cara", "vara", "data", "taxa",
                "sala", "fala", "mala", "bala", "gala", "rara", "sara",
                "area", "aria", "era", "ora", "uma", "usa", "mas"
        );

        if (commonWordBlacklist.contains(word.toLowerCase())) {
            return false;
        }

        if (word.length() < 3 || keyword.length() < 3) {
            return word.equals(keyword);
        }

        if (word.length() > keyword.length() * 2) {
            return false;
        }

        int threshold;
        int keywordLength = keyword.length();

        if (keywordLength <= 3) return word.equals(keyword);
        else if (keywordLength <= 5) threshold = 1;
        else threshold = 2;

        int distance = levenshteinDistance(word, keyword);
        if (distance > threshold) {
            return false;
        }

        double similarity = 1.0 - ((double) distance / Math.max(word.length(), keyword.length()));

        return similarity >= 0.6;
    }

    /**
     * Helper method for Levenshtein distance calculation
     */
    public static int levenshteinDistance(String a, String b) {
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

    /**
     * Check if a line has mismatch issues (for lines already determined to be relevant).
     * This should only be called after isRelevantMantraEntry() returns true.
     *
     * @param line The line to check
     * @param mantraKeyword The keyword being searched for
     * @param fizCount Count of action words found
     * @param totalGenericCount Count of mantra/rito words found
     * @param mantraKeywordCount Count of specific keyword found
     * @return true if the line has mismatch issues
     */
    public static boolean hasMismatchIssues(String line, String mantraKeyword,
                                            int fizCount, int totalGenericCount, int mantraKeywordCount) {
        int allActionWordsCount = ActionWordManager.countActionWords(line);
        boolean countMismatch = allActionWordsCount != totalGenericCount || totalGenericCount != mantraKeywordCount;

        boolean approximateButNotExact = LineAnalyzer.hasApproximateButNotExactMatch(line, mantraKeyword);

        boolean hasActionWords = ActionWordManager.hasActionWords(line);
        boolean numbersButNoAction = !hasActionWords;

        return countMismatch || approximateButNotExact || numbersButNoAction;
    }

    /**
     * Helper method to check if text contains numbers
     *
     * @param text The text to check
     * @return true if text contains any digits
     */
    private static boolean containsNumbers(String text) {
        if (text == null) return false;
        return text.matches(".*\\d+.*");
    }

    /**
     * Convenience method that combines relevance check and mismatch detection.
     * Use this when you need both checks together.
     *
     * @param line The line to analyze
     * @param mantraKeyword The keyword being searched for
     * @return ClassificationResult with both relevance and mismatch information
     */
    public static ClassificationResult classifyLine(String line, String mantraKeyword) {
        boolean isRelevant = isRelevantMantraEntry(line, mantraKeyword);

        if (!isRelevant) {
            return new ClassificationResult(false, false);
        }

        LineParser.LineData lineData = LineParser.parseLine(line, mantraKeyword);
        boolean hasMismatch = hasMismatchIssues(line, mantraKeyword,
                lineData.getFizCount(),
                lineData.getMantraWordsCount() + lineData.getRitosWordsCount(),
                lineData.getMantraKeywordCount());

        return new ClassificationResult(true, hasMismatch);
    }

    /**
     * Result class for line classification
     */
    public static class ClassificationResult {
        private final boolean isRelevant;
        private final boolean hasMismatch;

        public ClassificationResult(boolean isRelevant, boolean hasMismatch) {
            this.isRelevant = isRelevant;
            this.hasMismatch = hasMismatch;
        }

        public boolean isRelevant() { return isRelevant; }
        public boolean hasMismatch() { return hasMismatch; }
    }
}