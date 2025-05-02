package com.example.mantracount;

import java.io.IOException;
import java.time.LocalDate;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class MantraCount {

    public static void processFile(String filePath, String mantraKeyword, String fizKeyword, String mantrasKeyword, LocalDate startDate) throws IOException {
        try {
            // Delegate to FileProcessorService
            FileProcessorService.ProcessResult result =
                    FileProcessorService.processFile(filePath, mantraKeyword, startDate);

            // If you still need console output for debugging or standalone usage:
//            System.out.println("\nResults:");
//            System.out.println("Total " + mantraKeyword + " count: " + result.getTotalMantraKeywordCount());
//            System.out.println("Total 'Fiz' count: " + result.getTotalFizCount());
//            System.out.println("Total 'Mantra(s)' count: " + result.getTotalMantraWordsCount());
//            System.out.println("Sum of mantras: " + result.getTotalFizNumbersSum());

            if (!result.getMismatchedLines().isEmpty()) {
                System.out.println("\nMismatches Found:");
                for (String mismatch : result.getMismatchedLines()) {
                    System.out.println(mismatch);
                }
            }
        } catch (Exception e) {
            throw new IOException("Error processing file: " + e.getMessage(), e);
        }
    }

    public static boolean hasApproximateMatch(String line, String keyword) {
        String lineLower = line.toLowerCase();
        String keywordLower = keyword.toLowerCase();

        boolean mantraFound = false;
        for (String word : lineLower.split("\\s+")) {
            if (levenshteinDistance(word, keywordLower) <= 2) {
                mantraFound = true;
                break;
            }
        }

        int colonIndex = lineLower.indexOf(": ");
        boolean fizFoundNearColon = false;
        if (colonIndex != -1) {
            int start = colonIndex + 2;
            int end = Math.min(lineLower.length(), start + 10);
            String afterColon = lineLower.substring(start, end);

            for (String word : afterColon.split("\\s+")) {
                if (levenshteinDistance(word, "fiz") <= 1) {
                    fizFoundNearColon = true;
                    break;
                }
            }
        }

        return mantraFound && fizFoundNearColon;
    }

    /**
     * Checks if the line has an approximate match to the keyword but not an exact match.
     * This helps identify lines where a variant spelling of the mantra is used.
     */
    public static boolean hasApproximateButNotExactMatch(String line, String keyword) {
        String lineLower = line.toLowerCase();
        String keywordLower = keyword.toLowerCase();

        // First, check if there's an exact match with word boundaries
        boolean exactMatch = Pattern.compile("\\b" + Pattern.quote(keywordLower) + "\\b",
                        Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE)
                .matcher(lineLower).find();
        if (exactMatch) {
            return false; // If we have an exact match, return false (no mismatch)
        }

        // If no exact match, check for approximate matches
        for (String word : lineLower.split("\\s+")) {
            if (levenshteinDistance(word, keywordLower) <= 2 && !word.equals(keywordLower)) {
                return true; // Found an approximate but not exact match
            }
        }

        return false;
    }

    /**
     * Counts occurrences of a word with proper word boundaries
     * This ensures we don't count substrings within words
     */
    public static int countOccurrencesWithWordBoundary(String line, String keyword) {
        String keywordLower = keyword.toLowerCase();
        // Create pattern with word boundaries and case insensitivity
        Pattern pattern = Pattern.compile("\\b" + Pattern.quote(keywordLower) + "\\b",
                Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
        Matcher matcher = pattern.matcher(line.toLowerCase());

        int count = 0;
        while (matcher.find()) {
            count++;
        }
        return count;
    }

    public static int countOccurrences(String line, String keyword) {
        String lineLower = line.toLowerCase();
        String keywordLower = keyword.toLowerCase();
        int count = 0;
        int index = 0;
        while ((index = lineLower.indexOf(keywordLower, index)) != -1) {
            count++;
            index += keywordLower.length();
        }
        return count;
    }

    // Counts both "mantra" and "mantras"
    public static int countMantraOrMantras(String line) {
        String lower = line.toLowerCase();
        int count = 0;

        // Use word boundary checks to ensure we're matching whole words
        Pattern pattern = Pattern.compile("\\b(mantra|mantras)\\b", Pattern.UNICODE_CASE);
        Matcher matcher = pattern.matcher(lower);

        while (matcher.find()) {
            count++;
        }

        return count;
    }

    public static int extractNumberAfterThirdColon(String line) {
        int firstColon = line.indexOf(":");
        if (firstColon == -1) return -1;

        int secondColon = line.indexOf(":", firstColon + 1);
        if (secondColon == -1) return -1;

        int thirdColon = line.indexOf(":", secondColon + 1);
        if (thirdColon == -1 || thirdColon + 1 >= line.length()) return -1;

        String afterThirdColon = line.substring(thirdColon + 1).trim();

        Matcher matcher = Pattern.compile("\\d+").matcher(afterThirdColon);

        if (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group());
            } catch (NumberFormatException e) {
                return -1;
            }
        }
        return -1;
    }

    private static int levenshteinDistance(String a, String b) {
        int[][] dp = new int[a.length() + 1][b.length() + 1];
        for (int i = 0; i <= a.length(); i++) {
            for (int j = 0; j <= b.length(); j++) {
                if (i == 0) {
                    dp[i][j] = j;
                } else if (j == 0) {
                    dp[i][j] = i;
                } else if (a.charAt(i - 1) == b.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1];
                } else {
                    dp[i][j] = 1 + Math.min(dp[i - 1][j - 1],
                            Math.min(dp[i - 1][j], dp[i][j - 1]));
                }
            }
        }
        return dp[a.length()][b.length()];
    }
}