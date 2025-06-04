package com.example.mantracount;

import java.io.IOException;
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
                data.setTotalRitosCount(data.getTotalRitosCount() + parsed.getRitosWordsCount()); // Add ritos count
                data.setTotalFizCount(data.getTotalFizCount() + parsed.getFizCount());
                data.setTotalFizNumbersSum(data.getTotalFizNumbersSum() + parsed.getFizNumber());


                // Update counts in the result object
                result.setTotalMantraKeywordCount(result.getTotalMantraKeywordCount() + parsed.getMantraKeywordCount());
                result.setTotalMantraWordsCount(result.getTotalMantraWordsCount() + parsed.getMantraWordsCount());
                result.setTotalRitosWordsCount(result.getTotalRitosWordsCount() + parsed.getRitosWordsCount()); // Add ritos count
                result.setTotalFizCount(result.getTotalFizCount() + parsed.getFizCount());
                result.setTotalFizNumbersSum(result.getTotalFizNumbersSum() + parsed.getFizNumber());

                // Check for mismatches and add to debug if found
                if (parsed.hasMismatch()) {
                    data.addDebugLine(line);
                    result.addMismatchedLine(line);
                }
            }
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
        private int totalRitosWordsCount;    // Total count of "rito" words
        private int totalFizCount;           // Total count of "fiz" words
        private int totalFizNumbersSum;      // Sum of numbers following "fiz"
        private final List<String> mismatchedLines = new ArrayList<>(); // Lines with mismatches

        public int getTotalMantraKeywordCount() { return totalMantraKeywordCount; }
        public void setTotalMantraKeywordCount(int count) { this.totalMantraKeywordCount = count; }

        public int getTotalMantraWordsCount() { return totalMantraWordsCount; }
        public void setTotalMantraWordsCount(int count) { this.totalMantraWordsCount = count; }

        public int getTotalRitosWordsCount() { return totalRitosWordsCount; }
        public void setTotalRitosWordsCount(int count) { this.totalRitosWordsCount = count; }

        public int getTotalGenericCount() { return totalMantraWordsCount + totalRitosWordsCount; }

        public int getTotalFizCount() { return totalFizCount; }
        public void setTotalFizCount(int count) { this.totalFizCount = count; }

        public int getTotalFizNumbersSum() { return totalFizNumbersSum; }
        public void setTotalFizNumbersSum(int sum) { this.totalFizNumbersSum = sum; }

        public List<String> getMismatchedLines() { return mismatchedLines; }
        public void addMismatchedLine(String line) { this.mismatchedLines.add(line); }
    }
}