   package com.example.mantracount;

   import java.time.LocalDate;
   import java.time.format.DateTimeFormatter;
   import java.util.ArrayList;
   import java.util.List;
   import java.util.regex.Matcher;
   import java.util.regex.Pattern;

   import java.util.Arrays;
import java.util.Comparator;


    public class LineAnalyzer {
//        private static final Pattern DATE_PATTERN = Pattern.compile("\\[(\\d{1,2}/\\d{1,2}/\\d{2,4})\\]");
        private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MM/dd/yy");
        private static final Pattern DATE_PATTERN = Pattern.compile("\\[(\\d{1,2}/\\d{1,2}/\\d{2,4})");
        private static final int CONTEXT_LINES = 10; // Number of lines before and after to include

//        public static List<String> getAllContextLines(List<String> allLines, LocalDate targetDate) {
//            int startIdx = findStartingLineIndex(allLines, targetDate);
//            if (startIdx == -1) {
//                return new ArrayList<>();
//            }
//
//            // Get a reasonable context window (10 lines before and after)
//            int contextStart = Math.max(0, startIdx - 10);
//            int contextEnd = Math.min(allLines.size(), startIdx + 10);
//
//            return new ArrayList<>(allLines.subList(contextStart, contextEnd));
//        }

        // Find the line index where the target date would be (or should be)
//        public static int findStartingLineIndex(List<String> allLines, LocalDate targetDate) {
//            String targetDateStr = targetDate.format(DATE_FORMATTER);
//
//            for (int i = 0; i < allLines.size(); i++) {
//                String line = allLines.get(i);
//                Matcher matcher = DATE_PATTERN.matcher(line);
//                if (matcher.find()) {
//                    String dateStr = matcher.group(1);
//                    try {
//                        // Parse date and compare
//                        LocalDate lineDate = LocalDate.parse(dateStr, DATE_FORMATTER);
//                        if (lineDate.equals(targetDate) || lineDate.isAfter(targetDate)) {
//                            return i;
//                        }
//                    } catch (Exception e) {
//                        // Skip unparseable dates
//                    }
//                }
//            }
//
//            // If no matching or later date found, suggest end of file
//            return allLines.size() - 1;
//        }
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


        public static boolean hasApproximateMatch(String line, String keyword) {
        String lineLower = line.toLowerCase();
        String keywordLower = keyword.toLowerCase();

        boolean mantraFound = false;
        for (String word : lineLower.split("\\s+")) {
            if (isApproximateWordMatch(word, keywordLower)) {
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

    public static boolean hasApproximateButNotExactMatch(String line, String keyword) {
        String lineLower = line.toLowerCase();
        String keywordLower = keyword.toLowerCase();

        boolean exactMatch = Pattern.compile("\\b" + Pattern.quote(keywordLower) + "\\b",
                        Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE)
                .matcher(lineLower).find();
        if (exactMatch) return false;

        for (String word : lineLower.split("\\s+")) {
            if (isApproximateWordMatch(word, keywordLower) && !word.equals(keywordLower)) {
                return true;
            }
        }

        return false;
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

    public static int countOccurrencesWithWordBoundary(String line, String keyword) {
        String keywordLower = keyword.toLowerCase();
        Pattern pattern = Pattern.compile("\\b" + Pattern.quote(keywordLower) + "\\b",
                Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
        Matcher matcher = pattern.matcher(line.toLowerCase());

        int count = 0;
        while (matcher.find()) count++;
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

    public static int countMantraOrMantras(String line) {
        Pattern pattern = Pattern.compile("\\b(mantra|mantras)\\b", Pattern.UNICODE_CASE);
        Matcher matcher = pattern.matcher(line.toLowerCase());

        int count = 0;
        while (matcher.find()) count++;
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

            /**
             * Finds the starting line index for a given date in the file.
             * Searches for the closest match to the given date.
             *
             * @param allLines The full content of the file
             * @param targetDate The date to search for
             * @return The line index where the context should start
             */
            public static int findStartingLineIndex(List<String> allLines, LocalDate targetDate) {
                if (allLines == null || allLines.isEmpty()) {
                    return 0;
                }

                // First, try to find exact date match
                String dateStr = formatDateForComparison(targetDate);
                System.out.println("DEBUG: Looking for date in format: " + dateStr);

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
                System.out.println("DEBUG: No match found, defaulting to start of file");
                return 0;
            }

            /**
             * Checks if a line matches the target date and returns a score
             * 0 = exact match, >0 = approximate match (lower is better), -1 = no match
             */
            private static int checkDateMatch(List<String> allLines, int lineIndex, LocalDate targetDate, String targetDateStr) {
                String line = allLines.get(lineIndex);
                Matcher matcher = DATE_PATTERN.matcher(line);

                if (matcher.find()) {
                    String dateStr = matcher.group(1);
                    // Exact match
                    if (dateStr.equals(targetDateStr)) {
                        return 0;
                    }

                    // Try to parse the date for approximate matching
                    try {
                        String[] dateParts = dateStr.split("/");
                        if (dateParts.length == 3) {
                            int month = Integer.parseInt(dateParts[0]);
                            int day = Integer.parseInt(dateParts[1]);
                            int year = Integer.parseInt(dateParts[2]);

                            // Handle 2-digit years
                            if (year < 100) {
                                year += 2000;
                            }

                            LocalDate lineDate = LocalDate.of(year, month, day);
                            // Calculate difference in days
                            long daysBetween = Math.abs(targetDate.toEpochDay() - lineDate.toEpochDay());
                            return (int) daysBetween + 1; // +1 to ensure exact match is preferred
                        }
                    } catch (Exception e) {
                        // If parsing fails, just continue
                    }
                }

                return -1; // No match
            }

            /**
             * Gets all context lines around a target date
             */
//            public static List<String> getAllContextLines(List<String> allLines, LocalDate targetDate) {
//                int startIndex = findStartingLineIndex(allLines, targetDate);
//                int endIndex = Math.min(allLines.size(), startIndex + CONTEXT_LINES * 2 + 1);
//
//                List<String> contextLines = new ArrayList<>();
//                for (int i = startIndex; i < endIndex; i++) {
//                    if (i < allLines.size()) {
//                        contextLines.add(allLines.get(i));
//                    }
//                }
//
//                System.out.println("DEBUG: Collected " + contextLines.size() + " context lines from " +
//                        startIndex + " to " + (endIndex - 1));
//                return contextLines;
//            }

            /**
             * Formats a date for comparison with file date strings
             */
            private static String formatDateForComparison(LocalDate date) {
                return (date.getMonthValue()) + "/" + date.getDayOfMonth() + "/" +
                        (date.getYear() % 100); // Use 2-digit year like in the file
            }
        }



