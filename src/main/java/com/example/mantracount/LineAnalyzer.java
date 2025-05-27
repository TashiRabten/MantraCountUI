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
     * Enhanced version that checks for synonyms as well as approximate matches
     * FIXED: Now properly detects action words anywhere in the message content
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

        // FIXED: Check for action words anywhere in the message content (not just near colon)
        String messageContent = extractMessageContent(line);
        if (messageContent != null) {
            String messageContentLower = messageContent.toLowerCase();

            // Check for action words anywhere in the message
            String[] actionWords = {"fiz", "fez", "recitei", "faz", "completei", "feitos", "feito", "completo", "completos"};
            for (String action : actionWords) {
                if (messageContentLower.contains(action)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Count all action words in a line (for mismatch detection)
     * Returns 1 if any action words found, 0 if none (prevents double counting issues)
     */
    public static int countAllActionWords(String line) {
        String lineLower = line.toLowerCase();
        String[] actionWords = {"fiz", "fez", "recitei", "faz", "completei", "feitos", "feito", "completo", "completos"};

        // Check if any action words are present
        for (String action : actionWords) {
            Pattern pattern = Pattern.compile("\\b" + Pattern.quote(action) + "\\b", Pattern.CASE_INSENSITIVE);
            if (pattern.matcher(lineLower).find()) {
                return 1; // Found at least one action word, return 1
            }
        }

        return 0; // No action words found
    }

    /**
     * Extract just the message content from WhatsApp line, excluding timestamp and name
     */
    private static String extractMessageContent(String line) {
        // Handle iPhone WhatsApp format: [date, time] Name: Message
        if (line.startsWith("[")) {
            int closeBracket = line.indexOf(']');
            if (closeBracket > 0) {
                int nameEnd = line.indexOf(':', closeBracket + 1);
                if (nameEnd > 0) {
                    return line.substring(nameEnd + 1).trim();
                }
            }
        }

        // Handle Android WhatsApp format: DD/MM/YYYY HH:MM - Name: Message
        Pattern androidPattern = Pattern.compile("^(\\d{1,2}/\\d{1,2}/\\d{2,4})\\s+\\d{1,2}:\\d{1,2}\\s+-\\s+");
        Matcher androidMatcher = androidPattern.matcher(line);
        if (androidMatcher.find()) {
            int androidMatchEnd = androidMatcher.end();
            int nameEnd = line.indexOf(':', androidMatchEnd);
            if (nameEnd > 0) {
                return line.substring(nameEnd + 1).trim();
            }
        }

        // Fallback: try to find first colon that's not part of time notation
        int colonIndex = findFirstNonTimeColon(line);
        if (colonIndex > 0) {
            return line.substring(colonIndex + 1).trim();
        }

        // If no format detected, return the whole line
        return line;
    }

    /**
     * Find first colon that's not part of time notation
     */
    private static int findFirstNonTimeColon(String line) {
        for (int i = 1; i < line.length(); i++) {
            if (line.charAt(i) == ':') {
                // Check if this colon is part of time notation (digit:digit)
                boolean isTimeColon = (i > 0 && Character.isDigit(line.charAt(i - 1))) &&
                        (i < line.length() - 1 && Character.isDigit(line.charAt(i + 1)));

                if (!isTimeColon) {
                    return i;
                }
            }
        }
        return -1;
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

    /**
     * Extract number after key indicators like "fiz"
     */
    public static int extractNumberAfterThirdColon(String line) {
        // Try pattern-based approach first
        Matcher matcher = FIZ_NUMBER_PATTERN.matcher(line.toLowerCase());
        if (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group(2));
            } catch (NumberFormatException e) {
                // Continue to other methods if this fails
            }
        }

        // Fallback: look for numbers after "fiz" indicators
        String lowerCase = line.toLowerCase();
        String[] countIndicators = {"fiz", "recitei", "fez", "faz"};

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