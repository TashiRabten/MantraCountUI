package com.example.mantracount;

import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;


public class UIUtils {


    public static void displayAnalysisResults(MantraData mantraData, TextArea resultTextArea) {
        StringBuilder sb = new StringBuilder();
        sb.append("Analysis Results:\n\n");


        // Example: Appending mantra counts and additional results
        sb.append("Mantra keyword count: ").append(mantraData.getTotalNameCount()).append("\n");
        sb.append("Fiz count: ").append(mantraData.getTotalFizCount()).append("\n");
        sb.append("Fiz number: ").append(mantraData.getTotalFizNumbersSum()).append("\n");
        sb.append("Total mantras: ").append(mantraData.getTotalMantrasCount()).append("\n");

        if (mantraData.hasMismatch()) {
            sb.append("Mismatch detected!\n");
        } else {
            sb.append("No mismatch detected.\n");
        }

        // Display in the resultTextArea
        resultTextArea.setText(sb.toString());
    }

    // Show an information message to the user
    public static void showInfo(String message) {
        Alert infoAlert = new Alert(AlertType.INFORMATION);
        infoAlert.setTitle("Information");
        infoAlert.setHeaderText(null);
        infoAlert.setContentText(message);
        infoAlert.showAndWait();
    }


    // Show an error message to the user
    public static void showError(String message) {
        Alert errorAlert = new Alert(AlertType.ERROR);
        errorAlert.setTitle("Error");
        errorAlert.setHeaderText(null);
        errorAlert.setContentText(message);
        errorAlert.showAndWait();
    }

    // Set the placeholder text in a text field with dynamic behavior
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

    // Show a success message with a specific callback
    public static void showSuccess(String message, Runnable onSuccess) {
        Alert successAlert = new Alert(AlertType.INFORMATION);
        successAlert.setTitle("Success");
        successAlert.setHeaderText(null);
        successAlert.setContentText(message);
        successAlert.showAndWait();
        onSuccess.run();
    }
}
