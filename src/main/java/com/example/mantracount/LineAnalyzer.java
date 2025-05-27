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
     * Enhanced version using centralized action word detection
     */
    public static boolean hasApproximateMatch(String line, String keyword) {
        String lineLower = line.toLowerCase();
        String keywordLower = keyword.toLowerCase();

        boolean mantraFound = false;

        // First check for exact keyword match or its synonyms
        Set<String> allVariants = SynonymManager.getAllVariants(keywordLower);
        for (String word : lineLower.split("\\s+")) {
            String cleanWord = word.replaceAll("[^a-záàâãéêíóôõúüç]", "");
            if (allVariants.contains(cleanWord) || isApproximateWordMatch(cleanWord, keywordLower)) {
                mantraFound = true;
                break;
            }
        }

        if (!mantraFound) return false;

        // Check for mantra/rito words
        boolean hasMantraRitoWord = lineLower.contains("mantra") || lineLower.contains("mantras") ||
                lineLower.contains("rito") || lineLower.contains("ritos");

        if (!hasMantraRitoWord) return false;

        // Use centralized action word detection
        return ActionWordManager.hasActionWords(line);
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
     */
    public static boolean hasApproximateMatchFlexible(String line, String keyword) {
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
     */
    public static boolean hasApproximateButNotExactMatch(String line, String keyword) {
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
            if (isApproximateWordMatch(cleanWord, keywordLower) && !cleanWord.equals(keywordLower)) {
                return true; // This is a typo/approximate match, flag it
            }
        }

        return false;
    }

    /**
     * Enhanced counting that includes synonyms
     */
    public static int countOccurrencesWithWordBoundary(String line, String keyword) {
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

    // Helper methods
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