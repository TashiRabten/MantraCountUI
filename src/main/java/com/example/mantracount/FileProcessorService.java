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

/**
 * Service class for processing mantra files.
 * Classe de serviço para processamento de arquivos de mantra.
 */
public class FileProcessorService {

    /**
     * Extracts the first .txt file from a ZIP archive.
     * Extrai o primeiro arquivo .txt de um arquivo ZIP.
     *
     * @param zipFile The ZIP file to extract from / O arquivo ZIP do qual extrair
     * @return The extracted txt file / O arquivo txt extraído
     * @throws IOException If extraction fails / Se a extração falhar
     */
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
        throw new FileNotFoundException("No .txt found in the .zip file. / Nenhum arquivo .txt encontrado no arquivo .zip.");
    }

    /**
     * Process the file and populate the mantra data with results.
     * Processa o arquivo e preenche os dados de mantra com os resultados.
     *
     * @param data The data object to populate with results / O objeto de dados a ser preenchido com os resultados
     * @throws Exception If processing fails / Se o processamento falhar
     */
    public static void processFile(MantraData data) throws Exception {
        try {
            // Read all lines from the file
            // Lê todas as linhas do arquivo
            List<String> lines = FileLoader.robustReadLines(Paths.get(data.getFilePath()));
            LocalDate targetDate = data.getTargetDate();
            String mantraKeyword = data.getNameToCount();

            // Reset all counters before processing
            // Redefine todos os contadores antes do processamento
            data.resetCounts();

            // Create result object to hold processing results
            // Cria objeto de resultado para armazenar os resultados do processamento
            ProcessResult result = new ProcessResult();

            // Process each line in the file
            // Processa cada linha no arquivo
            for (String line : lines) {
                // Skip empty lines
                // Ignora linhas vazias
                if (line.trim().isEmpty()) continue;

                // Parse line data using LineParser
                // Analisa os dados da linha usando LineParser
                LineParser.LineData parsed = LineParser.parseLine(line, mantraKeyword);

                // Skip lines with no date or date before target date
                // Ignora linhas sem data ou com data anterior à data alvo
                if (parsed.getDate() == null || parsed.getDate().isBefore(targetDate)) continue;

                // Update counts in the data object
                // Atualiza contagens no objeto de dados
                data.setTotalNameCount(data.getTotalNameCount() + parsed.getMantraKeywordCount());
                data.setTotalMantrasCount(data.getTotalMantrasCount() + parsed.getMantraWordsCount());
                data.setTotalFizCount(data.getTotalFizCount() + parsed.getFizCount());
                data.setTotalFizNumbersSum(data.getTotalFizNumbersSum() + parsed.getFizNumber());

                // Update counts in the result object
                // Atualiza contagens no objeto de resultado
                result.setTotalMantraKeywordCount(result.getTotalMantraKeywordCount() + parsed.getMantraKeywordCount());
                result.setTotalMantraWordsCount(result.getTotalMantraWordsCount() + parsed.getMantraWordsCount());
                result.setTotalFizCount(result.getTotalFizCount() + parsed.getFizCount());
                result.setTotalFizNumbersSum(result.getTotalFizNumbersSum() + parsed.getFizNumber());

                // Check for mismatches and add to debug if found
                // Verifica inconsistências e adiciona ao debug se encontradas
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

    /**
     * Inner class to store processing results.
     * Classe interna para armazenar resultados de processamento.
     */
    public static class ProcessResult {
        private int totalMantraKeywordCount; // Total count of the target mantra keyword / Contagem total da palavra-chave de mantra alvo
        private int totalMantraWordsCount;   // Total count of "mantra" words / Contagem total de palavras "mantra"
        private int totalFizCount;           // Total count of "fiz" words / Contagem total de palavras "fiz"
        private int totalFizNumbersSum;      // Sum of numbers following "fiz" / Soma dos números seguindo "fiz"
        private final List<String> mismatchedLines = new ArrayList<>(); // Lines with mismatches / Linhas com inconsistências

        /**
         * Get the total count of the target mantra keyword.
         * Obtém a contagem total da palavra-chave de mantra alvo.
         */
        public int getTotalMantraKeywordCount() { return totalMantraKeywordCount; }

        /**
         * Set the total count of the target mantra keyword.
         * Define a contagem total da palavra-chave de mantra alvo.
         */
        public void setTotalMantraKeywordCount(int count) { this.totalMantraKeywordCount = count; }

        /**
         * Get the total count of "mantra" words.
         * Obtém a contagem total de palavras "mantra".
         */
        public int getTotalMantraWordsCount() { return totalMantraWordsCount; }

        /**
         * Set the total count of "mantra" words.
         * Define a contagem total de palavras "mantra".
         */
        public void setTotalMantraWordsCount(int count) { this.totalMantraWordsCount = count; }

        /**
         * Get the total count of "fiz" words.
         * Obtém a contagem total de palavras "fiz".
         */
        public int getTotalFizCount() { return totalFizCount; }

        /**
         * Set the total count of "fiz" words.
         * Define a contagem total de palavras "fiz".
         */
        public void setTotalFizCount(int count) { this.totalFizCount = count; }

        /**
         * Get the sum of numbers following "fiz".
         * Obtém a soma dos números seguindo "fiz".
         */
        public int getTotalFizNumbersSum() { return totalFizNumbersSum; }

        /**
         * Set the sum of numbers following "fiz".
         * Define a soma dos números seguindo "fiz".
         */
        public void setTotalFizNumbersSum(int sum) { this.totalFizNumbersSum = sum; }

        /**
         * Get the list of lines with mismatches.
         * Obtém a lista de linhas com inconsistências.
         */
        public List<String> getMismatchedLines() { return mismatchedLines; }

        /**
         * Add a line with mismatch to the list.
         * Adiciona uma linha com inconsistência à lista.
         */
        public void addMismatchedLine(String line) { this.mismatchedLines.add(line); }
    }
}