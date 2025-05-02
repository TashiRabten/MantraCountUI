package com.example.mantracount;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class FileProcessorService {
    public static class ProcessResult {
        private int totalMantraKeywordCount;
        private int totalMantraWordsCount;
        private int totalFizCount;
        private int totalFizNumbersSum;
        private List<String> mismatchedLines;

        public ProcessResult() {
            mismatchedLines = new ArrayList<>();
        }

        // Getters and setters
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

    public static File extractFirstTxtFromZip(File zipFile) throws Exception {
        Path tempDir = Files.createTempDirectory("unzipped_chat");
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (!entry.isDirectory() && entry.getName().toLowerCase().endsWith(".txt")) {
                    Path extractedFilePath = tempDir.resolve(Paths.get(entry.getName()).getFileName());
                    Files.copy(zis, extractedFilePath);
                    return extractedFilePath.toFile();
                }
            }
        }
        throw new FileNotFoundException("No .txt found in .zip.\n(Não há .txt no .zip.)");
    }

    public static ProcessResult processFile(String filePath, String mantraKeyword, LocalDate startDate) throws Exception {
        ProcessResult result = new ProcessResult();
        List<String> lines = Files.readAllLines(Paths.get(filePath), StandardCharsets.UTF_8);

        // Use Java streams to process lines
        lines.stream()
                .filter(line -> !line.trim().isEmpty())
                .forEach(line -> {
                    LineParser.LineData lineData = LineParser.parseLine(line, mantraKeyword);

                    // Skip lines before target date
                    if (lineData.getDate() == null || lineData.getDate().isBefore(startDate)) {
                        return;
                    }

                    // Accumulate counts
                    result.setTotalMantraKeywordCount(result.getTotalMantraKeywordCount() + lineData.getMantraKeywordCount());
                    result.setTotalMantraWordsCount(result.getTotalMantraWordsCount() + lineData.getMantraWordsCount());
                    result.setTotalFizCount(result.getTotalFizCount() + lineData.getFizCount());

                    if (lineData.getFizNumber() != 0) {
                        result.setTotalFizNumbersSum(result.getTotalFizNumbersSum() + lineData.getFizNumber());
                    }

                    if (lineData.hasMismatch()) {
                        result.addMismatchedLine(line);
                    }
                });

        return result;
    }
}