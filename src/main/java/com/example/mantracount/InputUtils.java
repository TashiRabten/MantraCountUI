package com.example.mantracount;

import javafx.scene.control.TextField;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;


public class InputUtils {

    public static ValidationResult validateAndParseInputs(TextField dateField, TextField mantraField, TextField pathField) {
        String inputDate = cleanInputField(dateField, "Enter start date - MM/DD/YY / Colocar Data Inicial - MM/DD/AA");
        String mantraKeyword = cleanInputField(mantraField, "Enter mantra name / Colocar nome do Mantra");
        String filePath = cleanInputField(pathField, "Open a file... / Abrir Arquivo...");

        if (inputDate.isEmpty() || mantraKeyword.isEmpty() || filePath.isEmpty()) {
            StringBuilder sb = new StringBuilder("❌ Missing required fields:\n❌ Campos obrigatórios ausentes:\n");
            if (inputDate.isEmpty()) {
                sb.append("- Date\n");
                sb.append("- Data\n");
            }
            if (mantraKeyword.isEmpty()) {
                sb.append("- Mantra\n");
                sb.append("- Mantra\n");
            }
            if (filePath.isEmpty()) {
                sb.append("- File\n");
                sb.append("- Arquivo\n");
            }
            return ValidationResult.invalid(sb.toString());
        }

        try {
            LocalDate parsedDate = DateParser.parseDate(inputDate);
            return ValidationResult.valid(inputDate, mantraKeyword, filePath, parsedDate);
        } catch (DateTimeParseException e) {
            return ValidationResult.invalid(
                    "❌ Invalid date format: \"" + inputDate + "\"\n" +
                            "❌ Formato de data inválido: \"" + inputDate + "\""
            );
        }
    }

    private static String cleanInputField(TextField field, String placeholder) {
        if (field == null) return "";

        String text = field.getText().trim();

        // Check if it's a placeholder by style (gray text color)
        boolean isGrayText = field.getStyle() != null && field.getStyle().contains("-fx-text-fill: gray");

        // Return empty string if it's a placeholder or matches placeholder text
        if (text.isEmpty() || text.equals(placeholder) || isGrayText) {
            return "";
        }

        return text;
    }

    public record ValidationResult(
            boolean valid,
            String errorMessage,
            String inputDate,
            String mantraKeyword,
            String filePath,
            LocalDate parsedDate
    ) {

        public static ValidationResult valid(String inputDate, String mantraKeyword, String filePath, LocalDate parsedDate) {
            return new ValidationResult(true, null, inputDate, mantraKeyword, filePath, parsedDate);
        }

        public static ValidationResult invalid(String errorMessage) {
            return new ValidationResult(false, errorMessage, null, null, null, null);
        }
    }
}