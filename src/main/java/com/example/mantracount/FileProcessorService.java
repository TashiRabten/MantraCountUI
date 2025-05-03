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
        // Tenta extrair o primeiro arquivo .txt do .zip
        Path tempDir = Files.createTempDirectory("unzipped_temp");

        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                // Skip directories and non-txt files
                // Ignora diretórios e arquivos que não são .txt
                if (!entry.isDirectory() && entry.getName().toLowerCase().endsWith(".txt")) {
                    Path extractedPath = tempDir.resolve(Paths.get(entry.getName()).getFileName());
                    Files.copy(zis, extractedPath, StandardCopyOption.REPLACE_EXISTING);
                    return extractedPath.toFile();
                }
            }
        }

        // No .txt found in the zip
        // Nenhum .txt encontrado no zip
        throw new FileNotFoundException("No .txt found in the .zip file.\n(Nenhum .txt encontrado no arquivo .zip.)");
    }

    public static void processFile(MantraData data) throws Exception {
        try {
            List<String> lines = FileLoader.robustReadLines(Paths.get(data.getFilePath()));
            LocalDate targetDate = data.getTargetDate();
            String mantraKeyword = data.getNameToCount();

            data.resetCounts();

            ProcessResult result = new ProcessResult();

            for (String line : lines) {
                if (line.trim().isEmpty()) continue;

                LineParser.LineData parsed = LineParser.parseLine(line, mantraKeyword);
                if (parsed.getDate() == null || parsed.getDate().isBefore(targetDate)) continue;

                data.setTotalNameCount(data.getTotalNameCount() + parsed.getMantraKeywordCount());
                data.setTotalMantrasCount(data.getTotalMantrasCount() + parsed.getMantraWordsCount());
                data.setTotalFizCount(data.getTotalFizCount() + parsed.getFizCount());
                data.setTotalFizNumbersSum(data.getTotalFizNumbersSum() + parsed.getFizNumber());

                result.setTotalMantraKeywordCount(result.getTotalMantraKeywordCount() + parsed.getMantraKeywordCount());
                result.setTotalMantraWordsCount(result.getTotalMantraWordsCount() + parsed.getMantraWordsCount());
                result.setTotalFizCount(result.getTotalFizCount() + parsed.getFizCount());
                result.setTotalFizNumbersSum(result.getTotalFizNumbersSum() + parsed.getFizNumber());

                if (parsed.hasMismatch()) {
                    data.addDebugLine(line);
                    result.addMismatchedLine(line);
                }
            }
        } catch (Exception e) {
            throw new IOException(
                    "Error processing file:\n" +
                            "Erro ao processar o arquivo:\n" +
                            e.getMessage(), e
            );
        }
    }



    // Inner class to use only if needed elsewhere (can also be moved to its own file)
    public static class ProcessResult {
        private int totalMantraKeywordCount;
        private int totalMantraWordsCount;
        private int totalFizCount;
        private int totalFizNumbersSum;
        private final List<String> mismatchedLines = new ArrayList<>();

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
