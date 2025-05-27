package com.example.mantracount;

import java.time.LocalDate;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LineAnalyzer {
    // Pattern to extract numbers after fizKeywords
    private static final Pattern FIZ_NUMBER_PATTERN =
            Pattern.compile("\\b(fiz|fez|recitei|faz)\\s+([0-9]+)\\b", Pattern.CASE_INSENSITIVE);

    /**
     * UPDATED: Now uses centralized classification logic
     * This is the main method that determines if a line should be processed
     */
    public static boolean hasApproximateMatch(String line, String keyword) {
        return MantraLineClassifier.isRelevantMantraEntry(line, keyword);
    }

    /**
     * Use centralized action word counting
     */
    public static int countAllActionWords(String line) {
        return ActionWordManager.countActionWords(line);
    }

    /**
     * ALTERNATIVE APPROACH: If the above doesn't work, try this more flexible version
     * This version looks for the pattern: [number] + [mantra/rito words] + [keyword] + [action words]
     * in any order within the line
     *
     * NOTE: This is kept for backward compatibility but now uses consistent number filter
     */
    public static boolean hasApproximateMatchFlexible(String line, String keyword) {
        // First check if it has numbers (mandatory filter)
        if (!hasNumbers(line)) {
            return false;
        }

        String lineLower = line.toLowerCase();
        String keywordLower = keyword.toLowerCase();

        // Check if the keyword (or its variants) exists
        Set<String> allVariants = SynonymManager.getAllVariants(keywordLower);
        boolean hasKeyword = false;
        for (String variant : allVariants) {
            if (lineLower.contains(variant)) {
                hasKeyword = true;
                break;
            }
        }
        if (!hasKeyword) return false;

        // Check for mantra/rito words
        boolean hasGenericWord = lineLower.contains("mantra") || lineLower.contains("mantras") ||
                lineLower.contains("rito") || lineLower.contains("ritos");
        if (!hasGenericWord) return false;

        // Use centralized action word detection
        boolean hasActionWord = ActionWordManager.hasActionWords(line);

        // Also check for number patterns that might indicate mantra counting
        boolean hasNumber = lineLower.matches(".*\\b\\d+\\b.*");

        return hasActionWord || hasNumber; // Accept if has action word OR number (more flexible)
    }

    /**
     * Enhanced version that detects non-synonym mismatches only
     * UPDATED: Now uses simple number check instead of deprecated method
     */
    public static boolean hasApproximateButNotExactMatch(String line, String keyword) {
        // Only check for approximate matches if this line has numbers
        if (!hasNumbers(line)) {
            return false;
        }

        String lineLower = line.toLowerCase();
        String keywordLower = keyword.toLowerCase();

        // Check for exact match first
        boolean exactMatch = Pattern.compile("\\b" + Pattern.quote(keywordLower) + "\\b",
                        Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE)
                .matcher(lineLower).find();
        if (exactMatch) return false;

        // Check if it's a synonym match - if so, DON'T flag as mismatch
        Set<String> allVariants = SynonymManager.getAllVariants(keywordLower);
        for (String word : lineLower.split("\\s+")) {
            String cleanWord = word.replaceAll("[^a-záàâãéêíóôõúüç]", "");
            if (allVariants.contains(cleanWord)) {
                return false; // It's a valid synonym, no mismatch
            }
        }

        // Only check for approximate matches (typos) if no synonym found
        for (String word : lineLower.split("\\s+")) {
            String cleanWord = word.replaceAll("[^a-záàâãéêíóôõúüç]", "");
            if (MantraLineClassifier.isApproximateWordMatch(cleanWord, keywordLower) && !cleanWord.equals(keywordLower)) {
                return true; // This is a typo/approximate match, flag it
            }
        }

        return false;
    }

    /**
     * Helper method - delegate to MantraLineClassifier to avoid duplication
     */
    private static boolean hasNumbers(String line) {
        return line != null && line.matches(".*\\d+.*");
    }

    /**
     * Enhanced counting that includes synonyms
     * UPDATED: Uses simple number check instead of deprecated method
     */
    public static int countOccurrencesWithWordBoundary(String line, String keyword) {
        // Only count if this line has numbers
        if (!hasNumbers(line)) {
            return 0;
        }

        String keywordLower = keyword.toLowerCase();
        int count = 0;

        // Count all variants (including synonyms)
        Set<String> allVariants = SynonymManager.getAllVariants(keywordLower);
        for (String variant : allVariants) {
            Pattern pattern = Pattern.compile("\\b" + Pattern.quote(variant) + "\\b",
                    Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
            Matcher matcher = pattern.matcher(line.toLowerCase());
            while (matcher.find()) count++;
        }

        return count;
    }

    public static int countMantraOrMantras(String line) {
        Pattern pattern = Pattern.compile("\\b(mantra|mantras)\\b", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(line.toLowerCase());

        int count = 0;
        while (matcher.find()) count++;
        return count;
    }

    public static int countRitoOrRitos(String line) {
        Pattern pattern = Pattern.compile("\\b(rito|ritos)\\b", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(line.toLowerCase());

        int count = 0;
        while (matcher.find()) count++;
        return count;
    }

    public static int extractNumberAfterThirdColon(String line) {
        // Try pattern-based approach first - UPDATE THIS
        // OLD: Pattern.compile("\\b(fiz|fez|recitei|faz)\\s+([0-9]+)\\b", Pattern.CASE_INSENSITIVE);

        // NEW: Build pattern dynamically from ActionWordManager
        String[] actionWords = ActionWordManager.getActionWords();
        String actionPattern = String.join("|", actionWords);
        Pattern FIZ_NUMBER_PATTERN = Pattern.compile("\\b(" + actionPattern + ")\\s+([0-9]+)\\b", Pattern.CASE_INSENSITIVE);

        Matcher matcher = FIZ_NUMBER_PATTERN.matcher(line.toLowerCase());
        if (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group(2));
            } catch (NumberFormatException e) {
                // Continue to other methods if this fails
            }
        }

        // Fallback: look for numbers after ALL action words
        String lowerCase = line.toLowerCase();
        String[] countIndicators = ActionWordManager.getActionWords(); // Use full list

        for (String indicator : countIndicators) {
            int position = lowerCase.indexOf(indicator);
            if (position >= 0) {
                String afterIndicator = lowerCase.substring(position + indicator.length());
                return extractFirstNumber(afterIndicator);
            }
        }

        return -1;
    }

    /**
     * Get context lines around a missing date
     */
    public static List<String> getAllContextLines(List<String> allLines, LocalDate missingDate) {
        List<String> result = new ArrayList<>();

        List<LocalDate> datesToInclude = Arrays.asList(
                missingDate.minusDays(1),
                missingDate,
                missingDate.plusDays(1),
                missingDate.plusDays(2)
        );

        for (String line : allLines) {
            LocalDate date = LineParser.extractDate(line);
            if (date != null && datesToInclude.contains(date)) {
                result.add(line);
            }
        }

        result.sort(Comparator.comparing(LineParser::extractDate));
        return result;
    }

    // Helper methods - reuse shared logic
    private static boolean isApproximateWordMatch(String word, String keyword) {
        return MantraLineClassifier.isApproximateWordMatch(word, keyword);
    }

    private static int levenshteinDistance(String a, String b) {
        return MantraLineClassifier.levenshteinDistance(a, b);
    }

    private static int extractFirstNumber(String text) {
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
                return -1;
            }
        }

        return -1;
    }
}