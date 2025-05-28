package com.example.mantracount;

import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

/**
 * Enhanced UIUtils with consistent error handling and bilingual message formatting.
 * All messages follow the format: emoji + English text on line 1, emoji + Portuguese text on line 2.
 */
public class UIUtils {

    /**
     * Shows error with consistent bilingual format using StringConstants
     */
    public static void showError(String englishMessage, String portugueseMessage) {
        String message = StringConstants.createBilingualError(englishMessage, portugueseMessage);
        showAlert(AlertType.ERROR, "Error / Erro", message);
    }

    /**
     * Shows success with consistent bilingual format
     */
    public static void showSuccess(String englishMessage, String portugueseMessage) {
        String message = StringConstants.createBilingualSuccess(englishMessage, portugueseMessage);
        showAlert(AlertType.INFORMATION, "Success / Sucesso", message);
    }

    /**
     * Shows warning with consistent bilingual format
     */
    public static void showWarning(String englishMessage, String portugueseMessage) {
        String message = StringConstants.createBilingualWarning(englishMessage, portugueseMessage);
        showAlert(AlertType.WARNING, "Warning / Aviso", message);
    }

    /**
     * Shows info with consistent bilingual format
     */
    public static void showInfo(String englishMessage, String portugueseMessage) {
        String message = StringConstants.createBilingualInfo(englishMessage, portugueseMessage);
        showAlert(AlertType.INFORMATION, "Info / Informação", message);
    }

    /**
     * Shows confirmation dialog with bilingual format
     */
    public static boolean showConfirmation(String englishTitle, String portugueseTitle,
                                           String englishMessage, String portugueseMessage) {
        String title = englishTitle + " / " + portugueseTitle;
        String message = englishMessage + "\n" + portugueseMessage;
        return showConfirmationAlert(title, message);
    }

    // Convenience methods using predefined messages
    public static void showFileSavedSuccess() {
        showAlert(AlertType.INFORMATION, "Success / Sucesso", StringConstants.SuccessMessages.FILE_SAVED);
    }

    public static void showFileLoadedSuccess() {
        showAlert(AlertType.INFORMATION, "Success / Sucesso", StringConstants.SuccessMessages.FILE_LOADED);
    }

    public static void showFileLoadError() {
        showAlert(AlertType.ERROR, "Error / Erro", StringConstants.ErrorMessages.FILE_LOAD_ERROR);
    }

    public static void showFileSaveError() {
        showAlert(AlertType.ERROR, "Error / Erro", StringConstants.ErrorMessages.FILE_SAVE_ERROR);
    }

    public static void showFileNotFoundError() {
        showAlert(AlertType.ERROR, "Error / Erro", StringConstants.ErrorMessages.FILE_NOT_FOUND);
    }

    public static void showExtractZipError() {
        showAlert(AlertType.ERROR, "Error / Erro", StringConstants.ErrorMessages.EXTRACT_ZIP_ERROR);
    }

    public static void showNoChangesInfo() {
        showAlert(AlertType.INFORMATION, "Info / Informação", StringConstants.ErrorMessages.NO_CHANGES);
    }

    public static void showNoMismatchesSuccess() {
        showAlert(AlertType.INFORMATION, "Success / Sucesso", StringConstants.SuccessMessages.NO_MISMATCHES);
    }

    public static void showNoMissingDaysSuccess() {
        showAlert(AlertType.INFORMATION, "Success / Sucesso", StringConstants.SuccessMessages.NO_MISSING_DAYS);
    }

    public static void showNoMissingFizSuccess() {
        showAlert(AlertType.INFORMATION, "Success / Sucesso", StringConstants.SuccessMessages.NO_MISSING_FIZ);
    }

    public static void showChangesRevertedSuccess() {
        showAlert(AlertType.INFORMATION, "Success / Sucesso", StringConstants.SuccessMessages.CHANGES_REVERTED);
    }

    public static void showNoSearchResultsInfo() {
        showAlert(AlertType.INFORMATION, "Info / Informação", StringConstants.ErrorMessages.NO_SEARCH_RESULTS);
    }

    public static void showUpdateSuccess() {
        showAlert(AlertType.INFORMATION, "Update / Atualização", StringConstants.SuccessMessages.UPDATE_SUCCESS);
    }

    public static void showUpdateFailed(String details) {
        String message = StringConstants.ErrorMessages.UPDATE_FAILED;
        if (details != null && !details.isEmpty()) {
            message += "\n" + details;
        }
        showAlert(AlertType.ERROR, "Update Error / Erro de Atualização", message);
    }

    public static void showValidationError(String validationMessage) {
        showAlert(AlertType.ERROR, "Validation Error / Erro de Validação", validationMessage);
    }

    // Field validation methods using StringConstants
    public static boolean validateField(TextField field, String placeholder,
                                        String englishLabel, String portugueseLabel) {
        if (isFieldEmpty(field, placeholder)) {
            showError("Missing or invalid field: " + englishLabel,
                    "Campo ausente ou inválido: " + portugueseLabel);
            return false;
        }
        return true;
    }

    public static boolean validateDateField(TextField field, String placeholder) {
        if (isFieldEmpty(field, placeholder)) {
            showValidationError(StringConstants.ValidationMessages.MISSING_DATE);
            return false;
        }
        return true;
    }

    public static boolean validateMantraField(TextField field, String placeholder) {
        if (isFieldEmpty(field, placeholder)) {
            showValidationError(StringConstants.ValidationMessages.MISSING_MANTRA);
            return false;
        }
        return true;
    }

    public static boolean validateFileField(TextField field, String placeholder) {
        if (isFieldEmpty(field, placeholder)) {
            showValidationError(StringConstants.ValidationMessages.MISSING_FILE);
            return false;
        }
        return true;
    }

    public static boolean validateDateRange(java.time.LocalDate startDate, java.time.LocalDate endDate) {
        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            showValidationError(StringConstants.ValidationMessages.INVALID_DATE_RANGE);
            return false;
        }
        return true;
    }

    // File operation error helpers
    public static void showFileError(String operation, Exception ex) {
        String englishMsg = "Failed to " + operation + ": " + ex.getMessage();
        String portugueseMsg = "Falha ao " + getPortugueseOperation(operation) + ": " + ex.getMessage();
        showError(englishMsg, portugueseMsg);
    }

    // Utility methods for field handling
    public static void setPlaceholder(TextField field, String placeholder) {
        field.setText(placeholder);
        field.setStyle("-fx-text-fill: gray;");
        field.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal && field.getText().equals(placeholder)) {
                field.clear();
                field.setStyle("-fx-text-fill: black;");
            } else if (!newVal && field.getText().isEmpty()) {
                field.setText(placeholder);
                field.setStyle("-fx-text-fill: gray;");
            }
        });
    }

    public static void clearPlaceholder(TextField field, String placeholder) {
        if (field.getText().equals(placeholder)) {
            field.clear();
            field.setStyle("-fx-text-fill: black;");
        }
    }

    public static void restorePlaceholder(TextField field, String placeholder) {
        if (field.getText().isEmpty()) {
            field.setText(placeholder);
            field.setStyle("-fx-text-fill: gray;");
        }
    }

    public static boolean isFieldEmpty(TextField field, String placeholder) {
        String text = field.getText().trim();
        boolean isGray = field.getStyle() != null && field.getStyle().contains("-fx-text-fill: gray");
        return text.isEmpty() || text.equals(placeholder) || isGray;
    }

    public static boolean isPlaceholder(TextField field) {
        return field.getStyle() != null && field.getStyle().contains("-fx-text-fill: gray");
    }

    // Legacy methods for backward compatibility
    public static void showError(String message) {
        showAlert(AlertType.ERROR, "Error / Erro", message);
    }

    public static void showInfo(String message) {
        showAlert(AlertType.INFORMATION, "Info / Informação", message);
    }

    // Validation with backward compatibility
    public static boolean validateField(TextField field, String placeholder, String label) {
        return validateField(field, placeholder, label, label);
    }

    public static boolean validateWithCustomError(String title, String message, TextField field, String placeholder) {
        if (isFieldEmpty(field, placeholder)) {
            showAlert(AlertType.ERROR, title, message);
            return false;
        }
        return true;
    }

    // Core alert methods
    private static void showAlert(AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private static boolean showConfirmationAlert(String title, String message) {
        Alert alert = new Alert(AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        return alert.showAndWait().filter(response -> response == ButtonType.OK).isPresent();
    }

    // Helper method to translate common operations
    private static String getPortugueseOperation(String operation) {
        return switch (operation.toLowerCase()) {
            case "load file", "load" -> "carregar arquivo";
            case "save file", "save" -> "salvar arquivo";
            case "process file", "process" -> "processar arquivo";
            case "extract file", "extract" -> "extrair arquivo";
            case "read file", "read" -> "ler arquivo";
            case "write file", "write" -> "escrever arquivo";
            default -> operation;
        };
    }

    /**
     * Displays analysis results using consistent formatting
     */
    public static void displayAnalysisResults(MantraData mantraData, TextArea resultTextArea) {
        StringBuilder sb = new StringBuilder();
        sb.append(DateFormatUtils.createResultsHeader(mantraData.getTargetDate())).append("\n--\n");

        String word = mantraData.getNameToCount();
        String capitalized = capitalizeFirst(word);

        sb.append("Total '").append(capitalized).append("': ").append(mantraData.getTotalNameCount()).append("\n");
        sb.append("Total 'Fiz': ").append(mantraData.getTotalFizCount()).append("\n");
        sb.append("Total 'Mantra(s)/Rito(s)': ").append(mantraData.getTotalGenericCount()).append("\n");
        sb.append("Total 📿: ").append(mantraData.getTotalFizNumbersSum());

        resultTextArea.setText(sb.toString());
        resultTextArea.setStyle("-fx-text-fill: black;");
    }

    /**
     * Shows success with callback
     */
    public static void showSuccess(String message, Runnable onSuccess) {
        showAlert(AlertType.INFORMATION, "Success / Sucesso", message);
        if (onSuccess != null) {
            onSuccess.run();
        }
    }

    /**
     * Shows success with bilingual message and callback
     */
    public static void showSuccess(String englishMessage, String portugueseMessage, Runnable onSuccess) {
        showSuccess(englishMessage, portugueseMessage);
        if (onSuccess != null) {
            onSuccess.run();
        }
    }

    private static String capitalizeFirst(String input) {
        if (input == null || input.isEmpty()) return input;
        return input.substring(0, 1).toUpperCase() + input.substring(1);
    }
}