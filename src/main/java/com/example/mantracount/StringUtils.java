package com.example.mantracount;

/**
 * Utility class for common string operations.
 * Consolidates duplicated string processing methods across the application.
 */
public class StringUtils {

    private StringUtils() {
        // Utility class - prevent instantiation
    }

    /**
     * Calculates the Levenshtein distance between two strings.
     * The Levenshtein distance is the minimum number of single-character edits
     * (insertions, deletions, or substitutions) required to transform one string into another.
     *
     * @param a the first string
     * @param b the second string
     * @return the Levenshtein distance between the two strings
     */
    public static int levenshteinDistance(String a, String b) {
        if (a == null || b == null) {
            throw new IllegalArgumentException("Strings cannot be null");
        }

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

    /**
     * Extracts the first number found in a text string.
     * Scans the text character by character and builds the first complete number encountered.
     *
     * @param text the text to search for a number
     * @return the first number found, or -1 if no number is found or parsing fails
     */
    public static int extractFirstNumber(String text) {
        if (text == null || text.isEmpty()) {
            return -1;
        }

        StringBuilder numberBuilder = new StringBuilder();
        boolean foundDigit = false;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);

            if (Character.isDigit(c)) {
                numberBuilder.append(c);
                foundDigit = true;
            } else if (foundDigit) {
                // Stop at first non-digit after finding digits
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

    /**
     * Finds the first number that follows any of the given count indicators in a line.
     * Searches for indicators in the line and extracts the first number found after any indicator.
     * 
     * @param line The line to search
     * @param countIndicators Array of indicator words to search for
     * @return The first number found after an indicator, or -1 if none found
     */
    public static int findNumberAfterIndicators(String line, String[] countIndicators) {
        if (line == null || countIndicators == null) {
            return -1;
        }

        String lowerCase = line.toLowerCase();
        
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
        
        return -1;
    }
}