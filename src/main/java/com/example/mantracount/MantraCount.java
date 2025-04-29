package com.example.mantracount;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ArrayList;

public class MantraCount {

    public static void processFile(String filePath, String mantraKeyword, String fizKeyword, String mantrasKeyword, LocalDate startDate) throws IOException {
        List<String> lines = Files.readAllLines(Paths.get(filePath), StandardCharsets.UTF_8);

        int totalMantraCount = 0;
        int totalFizCount = 0;
        int totalMantrasWordCount = 0;
        int totalFizNumbersSum = 0;

        List<String> mismatches = new ArrayList<>();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("M/d/yy"); // Accepts 3/12/24 or 03/12/24

        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) {
                continue;
            }

            // Extract date if available
            LocalDate lineDate = null;
            try {
                String[] parts = line.split(",", 2);
                if (parts.length > 0) {
                    String datePart = parts[0].replace("[", "").replace("]", "").trim();
                    lineDate = LocalDate.parse(datePart, formatter);
                }
            } catch (Exception e) {
                // Ignore parse errors
            }

            // Skip lines before start date if date is parsed
            if (lineDate != null && lineDate.isBefore(startDate)) {
                continue;
            }

            // Check if line has an approximate mantra match AND fiz near ": "
            if (hasApproximateMatch(line, mantraKeyword)) {
                int mantraCountInLine = countOccurrences(line, mantraKeyword);
                int fizCountInLine = countOccurrences(line, fizKeyword);
                int mantrasWordCountInLine = countOccurrences(line, mantrasKeyword);

                totalMantraCount += mantraCountInLine;
                totalFizCount += fizCountInLine;
                totalMantrasWordCount += mantrasWordCountInLine;

                // NEW: Parse full number from line
                int fizNumber = extractNumberAfterThirdColon(line);
                if (fizNumber != -1) {
                    totalFizNumbersSum += fizNumber;
                }

                // Mismatches only for debug
                if (fizCountInLine != mantraCountInLine || mantrasWordCountInLine != mantraCountInLine) {
                    mismatches.add(line);
                }
            }
        }

        // Print results after processing
        System.out.println("\nResults:");
        System.out.println("Total " + mantraKeyword + " count: " + totalMantraCount);
        System.out.println("Total Fiz count: " + totalFizCount);
        System.out.println("Total Mantras count: " + totalMantrasWordCount);
        System.out.println("Sum of mantras (fiz numbers sum): " + totalFizNumbersSum);

        // Print mismatches after counts
        if (!mismatches.isEmpty()) {
            System.out.println("\nMismatches Found:");
            for (String mismatch : mismatches) {
                System.out.println(mismatch);
            }
        }
    }

    static boolean hasApproximateMatch(String line, String keyword) {
        String lineLower = line.toLowerCase();
        String keywordLower = keyword.toLowerCase();

        boolean mantraFound = false;
        for (String word : lineLower.split("\\s+")) {
            if (levenshteinDistance(word, keywordLower) <= 2) {
                mantraFound = true;
                break;
            }
        }

        // Check for Fiz close to ": "
        int colonIndex = lineLower.indexOf(": ");
        boolean fizFoundNearColon = false;
        if (colonIndex != -1) {
            int start = colonIndex + 2;
            int end = Math.min(lineLower.length(), start + 10);
            String afterColon = lineLower.substring(start, end);

            for (String word : afterColon.split("\\s+")) {
                if (levenshteinDistance(word, "fiz") <= 1) { // small typo tolerance
                    fizFoundNearColon = true;
                    break;
                }
            }
        }

        return mantraFound && fizFoundNearColon;
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

    public static int extractNumberAfterThirdColon(String line) {
        int firstColon = line.indexOf(":");
        if (firstColon == -1) return -1;

        int secondColon = line.indexOf(":", firstColon + 1);
        if (secondColon == -1) return -1;

        int thirdColon = line.indexOf(":", secondColon + 1);
        if (thirdColon == -1 || thirdColon + 1 >= line.length()) return -1;

        String afterThirdColon = line.substring(thirdColon + 1).trim();

        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("\\d+").matcher(afterThirdColon);

        if (matcher.find()) {
            String numberStr = matcher.group();
            try {
                return Integer.parseInt(numberStr);
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
                    dp[i][j] = 1 + Math.min(
                            dp[i - 1][j - 1],
                            Math.min(dp[i - 1][j], dp[i][j - 1])
                    );
                }
            }
        }
        return dp[a.length()][b.length()];
    }
}
