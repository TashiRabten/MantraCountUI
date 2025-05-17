package com.example.mantracount;

import java.io.IOException;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Processes file contents to count mantras and identify mismatches.
 * This class is responsible for the business logic of analyzing mantra data.
 */
public class FileProcessorService {

    /**
     * Process the file and update the mantra data with the results.
     * This method performs the actual counting of mantras.
     *
     * @param data The mantra data model to update with results
     * @throws Exception If processing fails
     */
    public static void processFile(MantraData data) throws Exception {
        try {
            // Read all lines from the file using FileLoader
            List<String> lines = data.getLines();
            if (lines == null || lines.isEmpty()) {
                throw new IOException("No lines loaded from file");
            }

            LocalDate targetDate = data.getTargetDate();
            String mantraKeyword = data.getNameToCount();

            System.out.println("Processing file with " + lines.size() + " lines");
            System.out.println("Target date: " + targetDate);
            System.out.println("Mantra keyword: " + mantraKeyword);

            // Reset all counters before processing
            data.resetCounts();

            // Create result object to hold processing results
            ProcessResult result = new ProcessResult();

            // Track statistics
            int processedLines = 0;
            int skippedDueToDate = 0;
            int skippedEmptyLines = 0;
            int skippedNoDate = 0;

            // Process each line in the file
            for (String line : lines) {
                // Skip empty lines
                if (line.trim().isEmpty()) {
                    skippedEmptyLines++;
                    continue;
                }

                // Parse line data using LineParser
                LineParser.LineData parsed = LineParser.parseLine(line, mantraKeyword);

                // Skip lines with no date
                if (parsed.getDate() == null) {
                    skippedNoDate++;
                    continue;
                }

                // Skip lines before target date
                if (parsed.getDate().isBefore(targetDate)) {
                    skippedDueToDate++;
                    continue;
                }

                // Line passed all filters, process it
                processedLines++;

                // Update counts in the data object
                data.setTotalNameCount(data.getTotalNameCount() + parsed.getMantraKeywordCount());
                data.setTotalMantrasCount(data.getTotalMantrasCount() + parsed.getMantraWordsCount());
                data.setTotalFizCount(data.getTotalFizCount() + parsed.getFizCount());
                data.setTotalFizNumbersSum(data.getTotalFizNumbersSum() + parsed.getFizNumber());

                // Debug output for lines with matches
                if (parsed.getMantraKeywordCount() > 0 || parsed.getMantraWordsCount() > 0) {
                    System.out.println("Processed line with mantra: " + line);
                    System.out.println("  - Date: " + parsed.getDate());
                    System.out.println("  - Mantra keyword count: " + parsed.getMantraKeywordCount());
                    System.out.println("  - Mantra words count: " + parsed.getMantraWordsCount());
                    System.out.println("  - Fiz count: " + parsed.getFizCount());
                    System.out.println("  - Fiz number: " + parsed.getFizNumber());
                    System.out.println("  - Has mismatch: " + parsed.hasMismatch());
                }

                // Update counts in the result object
                result.setTotalMantraKeywordCount(result.getTotalMantraKeywordCount() + parsed.getMantraKeywordCount());
                result.setTotalMantraWordsCount(result.getTotalMantraWordsCount() + parsed.getMantraWordsCount());
                result.setTotalFizCount(result.getTotalFizCount() + parsed.getFizCount());
                result.setTotalFizNumbersSum(result.getTotalFizNumbersSum() + parsed.getFizNumber());

                // Check for mismatches and add to debug if found
                if (parsed.hasMismatch()) {
                    data.addDebugLine(line);
                    result.addMismatchedLine(line);
                    System.out.println("  - Added to mismatches list");
                }
            }

            // Final debug output to verify total counts
            System.out.println("Processing complete:");
            System.out.println("  - Processed lines: " + processedLines);
            System.out.println("  - Skipped due to date: " + skippedDueToDate);
            System.out.println("  - Skipped empty lines: " + skippedEmptyLines);
            System.out.println("  - Skipped with no date: " + skippedNoDate);
            System.out.println("  - Total mantra keyword count: " + data.getTotalNameCount());
            System.out.println("  - Total mantra words count: " + data.getTotalMantrasCount());
            System.out.println("  - Total fiz count: " + data.getTotalFizCount());
            System.out.println("  - Total fiz numbers sum: " + data.getTotalFizNumbersSum());
            System.out.println("  - Mismatched lines: " + data.getDebugLines().size());

        } catch (Exception e) {
            System.err.println("Error processing file: " + e.getMessage());
            e.printStackTrace();
            throw new IOException("Error processing file: / Erro ao processar o arquivo: " + e.getMessage(), e);
        }
    }

    /**
     * Container for the results of processing a file.
     */
    public static class ProcessResult {
        private int totalMantraKeywordCount; // Total count of the target mantra keyword
        private int totalMantraWordsCount;   // Total count of "mantra" words
        private int totalFizCount;           // Total count of "fiz" words
        private int totalFizNumbersSum;      // Sum of numbers following "fiz"
        private final List<String> mismatchedLines = new ArrayList<>(); // Lines with mismatches

        public int getTotalMantraKeywordCount() { return totalMantraKeywordCount; }
        public void setTotalMantraKeywordCount(int count) { this.totalMantraKeywordCount = count; }

        public int getTotalMantraWordsCount() { return totalMantraWordsCount; }
        public void setTotalMantraWordsCount(int count) { this.totalMantraWordsCount = count; }

        public int getTotalFizCount() { return totalFizCount; }
        public void setTotalFizCount(int count) { this.totalFizCount = count; }

        public int getTotalFizNumbersSum() { return totalFizNumbersSum; }
        public void setTotalFizNumbersSum(int sum) { this.totalFizNumbersSum = sum; }

        public List<String> getMismatchedLines() { return mismatchedLines; }
        public void addMismatchedLine(String line) { this.mismatchedLines.add(line); }
    }
}