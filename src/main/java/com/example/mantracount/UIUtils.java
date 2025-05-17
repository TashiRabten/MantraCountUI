package com.example.mantracount;

import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

public class UIUtils {

    public static void displayAnalysisResults(MantraData mantraData, TextArea resultTextArea) {
        StringBuilder sb = new StringBuilder();
        sb.append("✔ Analysis Results / Resultados da Análise:\n\n");
        sb.append("✔ Mantra keyword count: ").append(mantraData.getTotalNameCount()).append("\n");
        sb.append("✔ Fiz count: ").append(mantraData.getTotalFizCount()).append("\n");
        sb.append("✔ Fiz total number: ").append(mantraData.getTotalFizNumbersSum()).append("\n");
        sb.append("✔ Total mantras: ").append(mantraData.getTotalMantrasCount()).append("\n");

        if (mantraData.hasMismatch()) {
            sb.append("❗ Mismatch detected! / Discrepância detectada!\n");
        } else {
            sb.append("✔ No mismatch detected. / Nenhuma discrepância detectada.\n");
        }

        resultTextArea.setText(sb.toString());
    }

    public static boolean validateField(TextField field, String placeholder, String label) {
        String text = field.getText().trim();
        if (text.isEmpty() || text.equals(placeholder) || isPlaceholder(field)) {
            showError(
                    "❌ Missing or invalid field / ❌ Campo ausente ou inválido",
                    "Error: " + label + "\nErro: " + label
            );
            return false;
        }
        return true;
    }
    public static boolean validateField(TextField field, String title, String message, String placeholder) {
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
        return field.getStyle().contains("-fx-text-fill: gray");
    }

    public static void showInfo(String message) {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle("Info / Informação");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static void showError(String message) {
        Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle("Error / Erro");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
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

}
