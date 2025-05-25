package com.example.mantracount;

import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

public class UIUtils {

    // Standard bilingual error message
    public static void showBilingualError(String englishMessage, String portugueseMessage) {
        String message = "❌ " + englishMessage + "\n❌ " + portugueseMessage;
        showError("Error / Erro", message);
    }

    // Standard bilingual info message
    public static void showBilingualInfo(String englishMessage, String portugueseMessage) {
        String message = "✔ " + englishMessage + "\n✔ " + portugueseMessage;
        showInfo("Info / Informação", message);
    }

    // Standard bilingual success message
    public static void showBilingualSuccess(String englishMessage, String portugueseMessage) {
        String message = "✔ " + englishMessage + "\n✔ " + portugueseMessage;
        showInfo("Success / Sucesso", message);
    }

    // Standard bilingual confirmation
    public static boolean showBilingualConfirmation(String englishTitle, String portugueseTitle,
                                                    String englishMessage, String portugueseMessage) {
        String title = englishTitle + " / " + portugueseTitle;
        String message = englishMessage + "\n" + portugueseMessage;
        return showConfirmation(title, message);
    }

    public static void displayAnalysisResults(MantraData mantraData, TextArea resultTextArea) {
        StringBuilder sb = new StringBuilder();
        sb.append("✔ Analysis Results / Resultados da Análise:\n\n");
        sb.append("✔ Mantra keyword count: ").append(mantraData.getTotalNameCount()).append("\n");
        sb.append("✔ Fiz count: ").append(mantraData.getTotalFizCount()).append("\n");
        sb.append("✔ Fiz total number: ").append(mantraData.getTotalFizNumbersSum()).append("\n");

        // Combined count for Mantra(s)/Rito(s)
        sb.append("✔ Total Mantra(s)/Rito(s): ").append(mantraData.getTotalGenericCount()).append("\n");

        if (mantraData.hasMismatch()) {
            sb.append("❗ Mismatch detected! / Discrepância detectada!\n");
        } else {
            sb.append("✔ No mismatch detected. / Nenhuma discrepância detectada.\n");
        }

        resultTextArea.setText(sb.toString());
    }

    // NEW: Bilingual field validation (4 parameters)
    public static boolean validateBilingualField(TextField field, String placeholder, String englishLabel, String portugueseLabel) {
        String text = field.getText().trim();
        if (text.isEmpty() || text.equals(placeholder) || isPlaceholder(field)) {
            showBilingualError(
                    "Missing or invalid field: " + englishLabel,
                    "Campo ausente ou inválido: " + portugueseLabel
            );
            return false;
        }
        return true;
    }

    // EXISTING: Single label validation (3 parameters) - keep for backward compatibility
    public static boolean validateField(TextField field, String placeholder, String label) {
        String text = field.getText().trim();
        if (text.isEmpty() || text.equals(placeholder) || isPlaceholder(field)) {
            showBilingualError(
                    "Missing or invalid field: " + label,
                    "Campo ausente ou inválido: " + label
            );
            return false;
        }
        return true;
    }

    // EXISTING: Custom validation with specific title and message
    public static boolean validateWithCustomError(String title, String message, TextField field, String placeholder) {
        String text = field.getText().trim();
        boolean isGray = field.getStyle() != null && field.getStyle().contains("-fx-text-fill: gray");

        if (text.isEmpty() || text.equals(placeholder) || isGray) {
            showError(title, message);
            return false;
        }
        return true;
    }

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

    public static boolean isPlaceholder(TextField field) {
        return field.getStyle() != null && field.getStyle().contains("-fx-text-fill: gray");
    }

    // Enhanced methods with better defaults
    public static void showInfo(String message) {
        showInfo("Info / Informação", message);
    }

    public static void showInfo(String title, String message) {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static void showError(String message) {
        showError("Error / Erro", message);
    }

    public static void showError(String title, String message) {
        Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static void showSuccess(String message, Runnable onSuccess) {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle("Success / Sucesso");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
        onSuccess.run();
    }

    public static boolean showConfirmation(String title, String message) {
        Alert alert = new Alert(AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);

        return alert.showAndWait().filter(response -> response == ButtonType.OK).isPresent();
    }

    // Utility method to create standard bilingual file error messages
    public static void showFileError(String operation, Exception ex) {
        String englishMsg = "Failed to " + operation + ": " + ex.getMessage();
        String portugueseMsg = "Falha ao " + getPortugueseOperation(operation) + ": " + ex.getMessage();
        showBilingualError(englishMsg, portugueseMsg);
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
            default -> operation; // fallback to original
        };
    }
}