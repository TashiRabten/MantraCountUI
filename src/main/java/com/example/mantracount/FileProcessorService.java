package com.example.mantracount;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class FileProcessorService {

    public static File extractFirstTxtFromZip(File zipFile) throws IOException {
        // Try to extract first .txt file from .zip
        Path tempDir = Files.createTempDirectory("unzipped_temp");

        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                // Skip directories and non-txt files
                if (!entry.isDirectory() && entry.getName().toLowerCase().endsWith(".txt")) {
                    Path extractedPath = tempDir.resolve(Paths.get(entry.getName()).getFileName());
                    Files.copy(zis, extractedPath, StandardCopyOption.REPLACE_EXISTING);
                    return extractedPath.toFile();
                }
            }
        }

        // No .txt found in the zip
        throw new FileNotFoundException("No .txt found in the .zip file. / Nenhum arquivo .txt encontrado no arquivo .zip.");
    }

    public static void processFile(MantraData data) throws Exception {
        try {
            // Read all lines from the file
            List<String> lines = FileLoader.robustReadLines(Paths.get(data.getFilePath()));
            LocalDate targetDate = data.getTargetDate();
            String mantraKeyword = data.getNameToCount();

            // Reset all counters before processing
            data.resetCounts();

            // Create result object to hold processing results
            ProcessResult result = new ProcessResult();

            // Process each line in the file
            for (String line : lines) {
                // Skip empty lines
                if (line.trim().isEmpty()) continue;

                // Parse line data using LineParser
                LineParser.LineData parsed = LineParser.parseLine(line, mantraKeyword);

                // Skip lines with no date or date before target date
                if (parsed.getDate() == null || parsed.getDate().isBefore(targetDate)) continue;

                // Update counts in the data object
                data.setTotalNameCount(data.getTotalNameCount() + parsed.getMantraKeywordCount());
                data.setTotalMantrasCount(data.getTotalMantrasCount() + parsed.getMantraWordsCount());
                data.setTotalFizCount(data.getTotalFizCount() + parsed.getFizCount());
                data.setTotalFizNumbersSum(data.getTotalFizNumbersSum() + parsed.getFizNumber());

                // Update counts in the result object
                result.setTotalMantraKeywordCount(result.getTotalMantraKeywordCount() + parsed.getMantraKeywordCount());
                result.setTotalMantraWordsCount(result.getTotalMantraWordsCount() + parsed.getMantraWordsCount());
                result.setTotalFizCount(result.getTotalFizCount() + parsed.getFizCount());
                result.setTotalFizNumbersSum(result.getTotalFizNumbersSum() + parsed.getFizNumber());

                // Check for mismatches and add to debug if found
                if (parsed.hasMismatch()) {
                    data.addDebugLine(line);
                    result.addMismatchedLine(line);
                }
            }
        } catch (Exception e) {
            throw new IOException(
                    "Error processing file: / Erro ao processar o arquivo: " + e.getMessage(), e
            );
        }
    }

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