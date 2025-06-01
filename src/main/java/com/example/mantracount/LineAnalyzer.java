package com.example.mantracount;

import java.time.LocalDate;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LineAnalyzer {
    private static final Pattern FIZ_NUMBER_PATTERN =
            Pattern.compile("\\b(fiz|fez|recitei|faz)\\s+([0-9]+)\\b", Pattern.CASE_INSENSITIVE);

    /**
     * Enhanced version using centralized action word detection
     */
    public static boolean hasApproximateMatch(String line, String keyword) {
        String lineLower = line.toLowerCase();
        String keywordLower = keyword.toLowerCase();

        boolean mantraFound = false;

        Set<String> allVariants = SynonymManager.getAllVariants(keywordLower);
        for (String word : lineLower.split("\\s+")) {
            String cleanWord = word.replaceAll("[^a-záàâãéêíóôõúüç]", "");
            if (allVariants.contains(cleanWord) || isApproximateWordMatch(cleanWord, keywordLower)) {
                mantraFound = true;
                break;
            }
        }

        if (!mantraFound) return false;

        boolean hasMantraRitoWord = lineLower.contains("mantra") || lineLower.contains("mantras") ||
                lineLower.contains("rito") || lineLower.contains("ritos");

        if (!hasMantraRitoWord) return false;

        return ActionWordManager.hasActionWords(line);
    }

    /**
     * Use centralized action word counting
     */
    public static int countAllActionWords(String line) {
        return ActionWordManager.countActionWords(line);
    }

    /**
     * Flexible version that looks for the pattern: [number] + [mantra/rito words] + [keyword] + [action words]
     * in any order within the line
     */
    public static boolean hasApproximateMatchFlexible(String line, String keyword) {
        String lineLower = line.toLowerCase();
        String keywordLower = keyword.toLowerCase();

        Set<String> allVariants = SynonymManager.getAllVariants(keywordLower);
        boolean hasKeyword = false;
        for (String variant : allVariants) {
            if (lineLower.contains(variant)) {
                hasKeyword = true;
                break;
            }
        }
        if (!hasKeyword) return false;

        boolean hasGenericWord = lineLower.contains("mantra") || lineLower.contains("mantras") ||
                lineLower.contains("rito") || lineLower.contains("ritos");
        if (!hasGenericWord) return false;

        boolean hasActionWord = ActionWordManager.hasActionWords(line);

        boolean hasNumber = lineLower.matches(".*\\b\\d+\\b.*");

        return hasActionWord || hasNumber;
    }

    /**
     * Enhanced version that detects non-synonym mismatches only
     */
    public static boolean hasApproximateButNotExactMatch(String line, String keyword) {
        String lineLower = line.toLowerCase();
        String keywordLower = keyword.toLowerCase();

        boolean exactMatch = Pattern.compile("\\b" + Pattern.quote(keywordLower) + "\\b",
                        Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE)
                .matcher(lineLower).find();
        if (exactMatch) return false;

        Set<String> allVariants = SynonymManager.getAllVariants(keywordLower);
        for (String word : lineLower.split("\\s+")) {
            String cleanWord = word.replaceAll("[^a-záàâãéêíóôõúüç]", "");
            if (allVariants.contains(cleanWord)) {
                return false;
            }
        }

        for (String word : lineLower.split("\\s+")) {
            String cleanWord = word.replaceAll("[^a-záàâãéêíóôõúüç]", "");
            if (isApproximateWordMatch(cleanWord, keywordLower) && !cleanWord.equals(keywordLower)) {
                return true;
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

        String lowerCase = line.toLowerCase();
        String[] countIndicators = ActionWordManager.getActionWords();

        for (String indicator : countIndicators) {
            int position = lowerCase.indexOf(indicator);
            if (position >= 0) {
                String afterIndicator = lowerCase.substring(position + indicator.length());
                int number = extractFirstNumber(afterIndicator);
                if (number > 0) {
                    return number;
                }
            }
        }

        LineParser.LineSplitResult splitResult = LineParser.splitEditablePortion(line);
        String editablePart = splitResult.getEditableSuffix();

        if (editablePart != null && !editablePart.trim().isEmpty()) {
            Pattern numberPattern = Pattern.compile("\\b(\\d+)\\b");
            Matcher numberMatcher = numberPattern.matcher(editablePart);

            if (numberMatcher.find()) {
                try {
                    return Integer.parseInt(numberMatcher.group(1));
                } catch (NumberFormatException e) {
                    // Continue if parsing fails
                }
            }
        }

        return 0;
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