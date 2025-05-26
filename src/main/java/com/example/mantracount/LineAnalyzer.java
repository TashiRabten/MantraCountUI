package com.example.mantracount;

import java.time.LocalDate;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LineAnalyzer {
    // Add the Android WhatsApp date pattern
    private static final Pattern ANDROID_DATE_PATTERN =
            Pattern.compile("^(\\d{1,2}/\\d{1,2}/\\d{2,4})\\s+\\d{1,2}:\\d{1,2}\\s+-\\s+");

    private static final Pattern DATE_PATTERN = Pattern.compile("\\[(\\d{1,2}/\\d{1,2}/\\d{2,4})");
    private static final int CONTEXT_LINES = 10; // Number of lines before and after to include

    // Pattern to extract numbers after fizKeywords
    private static final Pattern FIZ_NUMBER_PATTERN =
            Pattern.compile("\\b(fiz|fez|recitei|faz)\\s+([0-9]+)\\b", Pattern.CASE_INSENSITIVE);

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
                result.add(line); // ← NO mantraKeyword filter here!
            }
        }

        result.sort(Comparator.comparing(LineParser::extractDate));
        return result;
    }

    /**
     * Enhanced version that checks for synonyms as well as approximate matches
     */
    public static boolean hasApproximateMatch(String line, String keyword) {
        String lineLower = line.toLowerCase();
        String keywordLower = keyword.toLowerCase();

        boolean mantraFound = false;

        // First check for exact keyword match
        for (String word : lineLower.split("\\s+")) {
            if (isApproximateWordMatch(word, keywordLower)) {
                mantraFound = true;
                break;
            }
        }

        // If no exact match found, check for synonym matches
        if (!mantraFound) {
            Set<String> synonyms = SynonymManager.getAllVariants(keywordLower);
            for (String synonym : synonyms) {
                for (String word : lineLower.split("\\s+")) {
                    if (isApproximateWordMatch(word, synonym)) {
                        mantraFound = true;
                        break;
                    }
                }
                if (mantraFound) break;
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

    public static boolean hasApproximateButNotExactMatch(String line, String keyword) {
        String lineLower = line.toLowerCase();
        String keywordLower = keyword.toLowerCase();

        // Check for exact match first
        boolean exactMatch = Pattern.compile("\\b" + Pattern.quote(keywordLower) + "\\b",
                        Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE)
                .matcher(lineLower).find();
        if (exactMatch) return false;

        // Check if it's a synonym match - if so, DON'T flag as mismatch
        String canonicalKeyword = SynonymManager.getCanonicalForm(keywordLower);
        Set<String> allVariants = SynonymManager.getAllVariants(canonicalKeyword);

        for (String word : lineLower.split("\\s+")) {
            // Clean punctuation from the word before checking
            String cleanWord = word.replaceAll("[^a-záàâãéêíóôõúüç]", "");

            // If word is a known synonym variant, don't flag as mismatch
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

        // Count exact matches
        Pattern exactPattern = Pattern.compile("\\b" + Pattern.quote(keywordLower) + "\\b",
                Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
        Matcher exactMatcher = exactPattern.matcher(line.toLowerCase());
        while (exactMatcher.find()) count++;

        // Count synonym matches
        Set<String> synonyms = SynonymManager.getAllVariants(keywordLower);
        for (String synonym : synonyms) {
            if (!synonym.equals(keywordLower)) { // Don't double-count the canonical form
                Pattern synonymPattern = Pattern.compile("\\b" + Pattern.quote(synonym) + "\\b",
                        Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
                Matcher synonymMatcher = synonymPattern.matcher(line.toLowerCase());
                while (synonymMatcher.find()) count++;
            }
        }

        return count;
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

    public static int countMantraOrMantras(String line) {
        Pattern pattern = Pattern.compile("\\b(mantra|mantras)\\b", Pattern.UNICODE_CASE);
        Matcher matcher = pattern.matcher(line.toLowerCase());

        int count = 0;
        while (matcher.find()) count++;
        return count;
    }

    /**
     * Counts occurrences of "rito" or "ritos" in a line
     * @param line The line to analyze
     * @return Number of occurrences
     */
    public static int countRitoOrRitos(String line) {
        Pattern pattern = Pattern.compile("\\b(rito|ritos)\\b", Pattern.UNICODE_CASE);
        Matcher matcher = pattern.matcher(line.toLowerCase());

        int count = 0;
        while (matcher.find()) count++;
        return count;
    }

    /**
     * Improved method to extract number after key indicators like "fiz".
     * Works with both iOS and Android WhatsApp formats.
     * @param line The line to analyze
     * @return The extracted number or -1 if not found
     */
    public static int extractNumberAfterThirdColon(String line) {
        // First try the pattern-based approach for both formats
        Matcher matcher = FIZ_NUMBER_PATTERN.matcher(line.toLowerCase());
        if (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group(2));
            } catch (NumberFormatException e) {
                // Continue to other methods if this fails
            }
        }

        // Original approach for iPhone format with three colons
        int firstColon = line.indexOf(":");
        if (firstColon == -1) return -1;

        int secondColon = line.indexOf(":", firstColon + 1);
        if (secondColon == -1) return -1;

        int thirdColon = line.indexOf(":", secondColon + 1);
        if (thirdColon == -1 || thirdColon + 1 >= line.length()) return -1;

        String afterThirdColon = line.substring(thirdColon + 1).trim();
        matcher = Pattern.compile("\\d+").matcher(afterThirdColon);

        if (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group());
            } catch (NumberFormatException e) {
                return -1;
            }
        }

        // Last resort - try to find any number after occurrence of "fiz"
        String lowerCase = line.toLowerCase();
        String[] countIndicators = {"fiz", "recitei", "fez", "faz"};

        for (String indicator : countIndicators) {
            int position = lowerCase.indexOf(indicator);
            if (position >= 0) {
                // Look for a number after the indicator
                String afterIndicator = lowerCase.substring(position + indicator.length());
                return extractFirstNumber(afterIndicator);
            }
        }

        return -1;
    }

    /**
     * Helper method to extract the first number from a string
     */
    private static int extractFirstNumber(String text) {
        StringBuilder numberBuilder = new StringBuilder();
        boolean foundDigit = false;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);

            if (Character.isDigit(c)) {
                numberBuilder.append(c);
                foundDigit = true;
            } else if (foundDigit) {
                // Stop after the first sequence of digits
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

    public static int findStartingLineIndex(List<String> allLines, LocalDate targetDate) {
        if (allLines == null || allLines.isEmpty()) {
            return 0;
        }

        // First, try to find exact date match
        String dateStr = formatDateForComparison(targetDate);

        int bestMatchIndex = -1;
        int closestDateDifference = Integer.MAX_VALUE;

        // If the file is very large, use binary search or sampling
        if (allLines.size() > 10000) {
            int step = Math.max(1, allLines.size() / 100); // Sample every 1% of lines
            for (int i = 0; i < allLines.size(); i += step) {
                int result = checkDateMatch(allLines, i, targetDate, dateStr);
                if (result == 0) {
                    bestMatchIndex = i;
                    break;
                } else if (result > 0 && result < closestDateDifference) {
                    closestDateDifference = result;
                    bestMatchIndex = i;
                }
            }
        }

        // If sampling didn't find a match or file is small, do a full scan
        if (bestMatchIndex == -1) {
            for (int i = 0; i < allLines.size(); i++) {
                int result = checkDateMatch(allLines, i, targetDate, dateStr);
                if (result == 0) {
                    bestMatchIndex = i;
                    break;
                } else if (result > 0 && result < closestDateDifference) {
                    closestDateDifference = result;
                    bestMatchIndex = i;
                }
            }
        }

        // If we found a match, calculate the context start
        if (bestMatchIndex != -1) {
            // Calculate the context start, ensuring it's not negative
            int contextStart = Math.max(0, bestMatchIndex - CONTEXT_LINES);
            System.out.println("DEBUG: Found match at line " + bestMatchIndex + ", context starts at " + contextStart);
            return contextStart;
        }

        // If no match found, return the beginning of the file
        return 0;
    }

    private static int checkDateMatch(List<String> allLines, int lineIndex, LocalDate targetDate, String targetDateStr) {
        String line = allLines.get(lineIndex);

        // Check for both iPhone and Android formats
        LocalDate lineDate = extractDateForMatching(line);
        if (lineDate != null) {
            // If we found a date, calculate how many days it's from our target
            long daysBetween = Math.abs(targetDate.toEpochDay() - lineDate.toEpochDay());
            return (int) daysBetween + 1; // +1 to ensure exact match is preferred
        }

        return -1; // No match
    }

    /**
     * Extracts a date from a line - supporting both formats for matching purposes
     */
    private static LocalDate extractDateForMatching(String line) {
        // First try iPhone format
        Matcher matcher = DATE_PATTERN.matcher(line);
        if (matcher.find()) {
            String dateStr = matcher.group(1);
            try {
                return tryParseWithBothFormats(dateStr.split("/"));
            } catch (Exception ignored) {
                // Continue to next format
            }
        }

        // Next try Android format
        matcher = ANDROID_DATE_PATTERN.matcher(line);
        if (matcher.find()) {
            String dateStr = matcher.group(1);
            try {
                return tryParseWithBothFormats(dateStr.split("/"));
            } catch (Exception ignored) {
                // Both format attempts failed
            }
        }

        return null;
    }

    /**
     * Tries to parse date parts using both US and BR formats
     * @param dateParts Array of date components [first, second, year]
     * @return LocalDate if successfully parsed, null otherwise
     */
    private static LocalDate tryParseWithBothFormats(String[] dateParts) {
        if (dateParts.length != 3) return null;

        try {
            int first = Integer.parseInt(dateParts[0]);
            int second = Integer.parseInt(dateParts[1]);
            int year = Integer.parseInt(dateParts[2]);

            // Adjust year if it's 2 digits
            if (year < 100) {
                year += 2000;
            }

            // Try first with file format
            try {
                if (DateParser.getCurrentDateFormat() == DateParser.DateFormat.BR_FORMAT) {
                    // Brazilian format: day/month/year
                    return LocalDate.of(year, second, first);
                } else {
                    // US format: month/day/year
                    return LocalDate.of(year, first, second);
                }
            } catch (Exception e) {
                // If that fails, try the opposite format
                try {
                    if (DateParser.getCurrentDateFormat() == DateParser.DateFormat.BR_FORMAT) {
                        // Try US format
                        return LocalDate.of(year, first, second);
                    } else {
                        // Try Brazilian format
                        return LocalDate.of(year, second, first);
                    }
                } catch (Exception ignored) {
                    // Both attempts failed
                }
            }
        } catch (NumberFormatException e) {
            // Invalid number format
        }

        return null;
    }

    private static String formatDateForComparison(LocalDate date) {
        DateParser.DateFormat format = DateParser.getCurrentDateFormat();

        if (format == DateParser.DateFormat.BR_FORMAT) {
            // Brazilian format: day/month/yy
            return String.format("%d/%d/%02d",
                    date.getDayOfMonth(),
                    date.getMonthValue(),
                    date.getYear() % 100);
        } else {
            // US format: month/day/yy
            return String.format("%d/%d/%02d",
                    date.getMonthValue(),
                    date.getDayOfMonth(),
                    date.getYear() % 100);
        }
    }
}