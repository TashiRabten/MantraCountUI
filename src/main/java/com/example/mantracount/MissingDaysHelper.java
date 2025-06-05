package com.example.mantracount;

import java.io.File;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;

public class MissingDaysHelper {

    public static boolean prepareDataForMissingDays(String dateText, String mantraKeyword, String filePath, MantraData mantraData) {
        try {
            if (!validateInputs(dateText, mantraKeyword, filePath)) {
                return false;
            }

            LocalDate targetDate = parseAndValidateDate(dateText);
            if (targetDate == null) {
                return false;
            }

            File file = validateAndGetFile(filePath);
            if (file == null) {
                return false;
            }

            setBasicMantraData(mantraData, targetDate, mantraKeyword, filePath);

            File processedFile = handleZipFileIfNeeded(file, filePath, mantraData);
            if (processedFile == null) {
                return false;
            }

            return loadFileLines(processedFile, mantraData);

        } catch (Exception ex) {
            ex.printStackTrace();
            UIUtils.showError("❌ Failed to prepare data / Falha ao preparar dados",
                    ex.getMessage());
            return false;
        }
    }

    private static boolean validateInputs(String dateText, String mantraKeyword, String filePath) {
        if (dateText == null || dateText.trim().isEmpty() || UIUtils.isPlaceholder(new javafx.scene.control.TextField(dateText))) {
            UIUtils.showError("Invalid date / Data inválida",
                    "Please enter a valid date. / Por favor, insira uma data válida.");
            return false;
        }

        if (mantraKeyword == null || mantraKeyword.trim().isEmpty() || UIUtils.isPlaceholder(new javafx.scene.control.TextField(mantraKeyword))) {
            UIUtils.showError("Invalid mantra name / Nome de mantra inválido",
                    "Please enter a valid mantra name. / Por favor, insira um nome de mantra válido.");
            return false;
        }

        if (filePath == null || filePath.trim().isEmpty()) {
            UIUtils.showError("Invalid file / Arquivo inválido",
                    "Please select a valid file. / Por favor, selecione um arquivo válido.");
            return false;
        }

        return true;
    }

    private static LocalDate parseAndValidateDate(String dateText) {
        LocalDate targetDate = DateParser.parseDate(dateText);
        if (targetDate == null) {
            // If DateParser returns null, it means parsing failed but error was already shown
            return null;
        }
        return targetDate;
    }

    private static File validateAndGetFile(String filePath) {
        File file = new File(filePath);
        if (!file.exists() || !file.isFile()) {
            UIUtils.showError("File not found / Arquivo não encontrado",
                    "The selected file does not exist. / O arquivo selecionado não existe.");
            return null;
        }
        return file;
    }

    private static void setBasicMantraData(MantraData mantraData, LocalDate targetDate, String mantraKeyword, String filePath) {
        mantraData.setTargetDate(targetDate);
        mantraData.setNameToCount(mantraKeyword);
        mantraData.setFilePath(filePath);
    }

    private static File handleZipFileIfNeeded(File file, String filePath, MantraData mantraData) {
        if (!filePath.toLowerCase().endsWith(".zip")) {
            return file;
        }

        try {
            FileLoader.ExtractedFileInfo extractInfo = FileLoader.extractFirstTxtFromZip(file);
            File extracted = extractInfo.getExtractedFile();
            String originalEntryName = extractInfo.getOriginalEntryName();

            mantraData.setFromZip(true);
            mantraData.setOriginalZipPath(filePath);
            mantraData.setOriginalZipEntryName(originalEntryName);
            mantraData.setFilePath(extracted.getAbsolutePath());
            
            return extracted;
        } catch (Exception ex) {
            ex.printStackTrace();
            UIUtils.showError("❌ Failed to extract .zip file. / Falha ao extrair arquivo .zip",
                    ex.getMessage());
            return null;
        }
    }

    private static boolean loadFileLines(File file, MantraData mantraData) {
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
    }
}