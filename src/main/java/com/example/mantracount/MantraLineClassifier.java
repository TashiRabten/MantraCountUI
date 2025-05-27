package com.example.mantracount;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * Centralized classifier for determining if lines are relevant mantra count entries.
 * This eliminates duplication between counting logic and mismatch detection.
 */
public class MantraLineClassifier {

    /**
     * Determines if a line is a relevant mantra count entry for MAIN PROCESSING.
     * Requirements: numbers + keyword approximation + action words
     *
     * @param line The line to classify
     * @param mantraKeyword The keyword being searched for
     * @return true if the line should be processed as a mantra entry
     */
    public static boolean isRelevantMantraEntry(String line, String mantraKeyword) {
        // FIRST CHECK: Must have numbers (if no numbers, immediately exclude)
        if (!hasNumbers(line)) {
            return false;
        }

        // Check for keyword approximation
        if (!hasKeywordApproximation(line, mantraKeyword)) {
            return false;
        }

        // Check for action words
        return ActionWordManager.hasActionWords(line);
    }

    /**
     * Determines if a line is relevant for ALL MANTRAS feature.
     * Requirements: numbers + "mantras"/"ritos" words + action words
     *
     * @param line The line to classify
     * @return true if the line should be shown in All Mantras
     */
    public static boolean isRelevantForAllMantras(String line) {
        // FIRST CHECK: Must have numbers (if no numbers, immediately exclude)
        if (!hasNumbers(line)) {
            return false;
        }

        // Check for explicit mantra/rito words
        if (!hasExplicitMantraRitoWords(line)) {
            return false;
        }

        // Check for action words
        return ActionWordManager.hasActionWords(line);
    }

    /**
     * Determines if a line is relevant for SEM FIZ feature.
     * Requirements: numbers + specific mantra pattern + NO action words + NO problematic descriptors
     *
     * @param line The line to classify
     * @param mantraKeyword The keyword being searched for
     * @return true if the line should be shown in Sem Fiz
     */
    public static boolean isRelevantForSemFiz(String line, String mantraKeyword) {
        // FIRST CHECK: Must have numbers (if no numbers, immediately exclude)
        if (!hasNumbers(line)) {
            return false;
        }

        // SECOND CHECK: Filter out problematic descriptors (duration/measurement talk)
        if (hasProblematicDescriptors(line)) {
            return false;
        }

        // Check for specific mantra/keyword pattern (more restrictive than general approximation)
        if (!hasMantraDeKeywordPattern(line, mantraKeyword)) {
            return false;
        }

        // Must NOT have action words (that's what we're looking for)
        boolean hasAction = ActionWordManager.hasActionWords(line);
        boolean result = !hasAction;

        // ONLY DEBUG WHEN ALL CHECKS PASS (result = true)
        if (result) {
            System.out.println("DEBUG SemFiz FOUND: " + line.substring(0, Math.min(80, line.length())) + "...");
            System.out.println("DEBUG SemFiz: hasNumbers = true, hasMantraPattern = true, hasAction = " + hasAction);
        }

        return result;
    }

    /**
     * Filter out lines with problematic descriptors that indicate duration/measurement talk
     * rather than actual mantra counting entries
     */
    private static boolean hasProblematicDescriptors(String line) {
        String lineLower = line.toLowerCase();

        // Only filter very specific duration/measurement patterns
        String[] problematicPatterns = {
                "por minuto", "por hora", "minutos para", "horas para",
                "demoro", "duração", "tempo para", "levo.*minutos",
                "desde que comecei", "ganhei.*por", "desenvolvi"
        };

        return java.util.Arrays.stream(problematicPatterns)
                .anyMatch(pattern -> {
                    if (pattern.contains(".*")) {
                        // Use regex for patterns with wildcards
                        return java.util.regex.Pattern.compile(pattern).matcher(lineLower).find();
                    } else {
                        // Simple contains check for literal patterns
                        return lineLower.contains(pattern);
                    }
                });
    }

    /**
     * Check for the flexible pattern: "mantra(s) [keyword]" or "rito(s) [keyword]"
     * Moved from MissingFizAnalyzer to reuse in classification logic
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
     * Check if line has approximation to the keyword
     */
    private static boolean hasKeywordApproximation(String line, String keyword) {
        String lineLower = line.toLowerCase();
        String keywordLower = keyword.toLowerCase();

        // Check for exact keyword match or its synonyms
        Set<String> allVariants = SynonymManager.getAllVariants(keywordLower);
        for (String word : lineLower.split("\\s+")) {
            String cleanWord = word.replaceAll("[^a-záàâãéêíóôõúüç]", "");
            if (allVariants.contains(cleanWord) || isApproximateWordMatch(cleanWord, keywordLower)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if line has explicit "mantras" or "ritos" words
     */
    private static boolean hasExplicitMantraRitoWords(String line) {
        String lineLower = line.toLowerCase();
        return lineLower.contains("mantra") || lineLower.contains("mantras") ||
                lineLower.contains("rito") || lineLower.contains("ritos");
    }

    /**
     * Check if line has numbers anywhere in the EDITABLE PORTION (reusing existing logic)
     */
    private static boolean hasNumbers(String line) {
        // Reuse the existing editable portion extraction logic
        LineParser.LineSplitResult splitResult = LineParser.splitEditablePortion(line);
        String editablePart = splitResult.getEditableSuffix();

        if (editablePart == null || editablePart.trim().isEmpty()) {
            return false;
        }

        return editablePart.matches(".*\\d+.*");
    }

    /**
     * Helper method for approximate word matching (PUBLIC so other classes can use it)
     */
    public static boolean isApproximateWordMatch(String word, String keyword) {
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

    /**
     * Helper method for Levenshtein distance calculation (PUBLIC so other classes can use it)
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

    // REMOVED DEPRECATED METHOD - All code should use specific methods now
    // hasNumbersInMantraContext() was causing issues by mixing different requirements

    /**
     * Check if a line has mismatch issues (for lines already determined to be relevant).
     * This should only be called AFTER isRelevantMantraEntry() returns true.
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
        // Count mismatch: action words ≠ generic words ≠ keyword count
        int allActionWordsCount = ActionWordManager.countActionWords(line);
        boolean countMismatch = allActionWordsCount != totalGenericCount || totalGenericCount != mantraKeywordCount;

        // Approximate keyword match (typos in keyword)
        boolean approximateButNotExact = LineAnalyzer.hasApproximateButNotExactMatch(line, mantraKeyword);

        // Missing action words (numbers + mantra context but no action words)
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

        // If relevant, parse the line to get counts for mismatch detection
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