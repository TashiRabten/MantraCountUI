package com.example.mantracount;

import java.io.File;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;

/**
 * Helper class for handling missing days detection preparation
 */
public class MissingDaysHelper {

    /**
     * Prepare data for missing days detection
     * @param dateText The date text input by the user
     * @param mantraKeyword The mantra name/keyword to search for
     * @param filePath The path to the file containing mantra records
     * @param mantraData The data object to populate
     * @return true if preparation was successful, false otherwise
     */
    public static boolean prepareDataForMissingDays(String dateText, String mantraKeyword, String filePath, MantraData mantraData) {
        try {
            // Validate inputs
            if (dateText == null || dateText.trim().isEmpty() || UIUtils.isPlaceholder(new javafx.scene.control.TextField(dateText))) {
                UIUtils.showError("Invalid date / Data inválida",
                        "Please enter a valid date. / Por favor, insira uma data válida.");
                return false;
            }

            // Validate mantra name input
            if (mantraKeyword == null || mantraKeyword.trim().isEmpty() || UIUtils.isPlaceholder(new javafx.scene.control.TextField(mantraKeyword))) {
                UIUtils.showError("Invalid mantra name / Nome de mantra inválido",
                        "Please enter a valid mantra name. / Por favor, insira um nome de mantra válido.");
                return false;
            }

            // Validate file path
            if (filePath == null || filePath.trim().isEmpty()) {
                UIUtils.showError("Invalid file / Arquivo inválido",
                        "Please select a valid file. / Por favor, selecione um arquivo válido.");
                return false;
            }

            // Check if file exists
            File file = new File(filePath);
            if (!file.exists() || !file.isFile()) {
                UIUtils.showError("File not found / Arquivo não encontrado",
                        "The selected file does not exist. / O arquivo selecionado não existe.");
                return false;
            }

            // Parse the date
            LocalDate targetDate = DateParser.parseDate(dateText);
            if (targetDate == null) {
                // If DateParser returns null, it means parsing failed but error was already shown
                return false;
            }

            mantraData.setTargetDate(targetDate);
            mantraData.setNameToCount(mantraKeyword);
            mantraData.setFilePath(filePath);

            // Handle ZIP files
            if (filePath.toLowerCase().endsWith(".zip")) {
                try {
                    File extracted = FileProcessorService.extractFirstTxtFromZip(file);
                    mantraData.setFromZip(true);
                    mantraData.setOriginalZipPath(filePath);
                    mantraData.setFilePath(extracted.getAbsolutePath());
                    file = extracted;
                } catch (Exception ex) {
                    ex.printStackTrace();
                    UIUtils.showError("❌ Failed to extract .zip file. / Falha ao extrair arquivo .zip",
                            ex.getMessage());
                    return false;
                }
            }

            // Read the file lines
            try {
                List<String> lines = FileLoader.robustReadLines(file.toPath());
                mantraData.setLines(lines);

                if (lines == null || lines.isEmpty()) {
                    UIUtils.showError("Empty file / Arquivo vazio",
                            "The selected file is empty. / O arquivo selecionado está vazio.");
                    return false;
                }

                return true;
            } catch (Exception e) {
                e.printStackTrace();
                UIUtils.showError("File error / Erro de arquivo",
                        "Failed to read the file: " + e.getMessage() +
                                " / Falha ao ler o arquivo: " + e.getMessage());
                return false;
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            UIUtils.showError("❌ Failed to prepare data / Falha ao preparar dados",
                    ex.getMessage());
            return false;
        }
    }
}