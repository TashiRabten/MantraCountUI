package com.example.mantracount;

import javafx.scene.control.TextField;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

public class InputUtils {

    public static ValidationResult validateAndParseInputs(TextField dateField, TextField mantraField, TextField pathField) {
        String inputDate = cleanPlaceholder(dateField, "Enter start date - MM/DD/YY (Colocar Data Inicial - Mês/Dia/Ano)");
        String mantraKeyword = cleanPlaceholder(mantraField, "Enter mantra name (Colocar nome do Mantra)");
        String filePath = cleanPlaceholder(pathField, "Open a file... (Abrir Arquivo...)");

        if (inputDate.isEmpty() || mantraKeyword.isEmpty() || filePath.isEmpty()) {
            StringBuilder sb = new StringBuilder("❌ Missing required fields:\n❌ Campos obrigatórios ausentes:\n");
            if (inputDate.isEmpty()) sb.append("- Date / Data\n");
            if (mantraKeyword.isEmpty()) sb.append("- Mantra\n");
            if (filePath.isEmpty()) sb.append("- File / Arquivo\n");
            return ValidationResult.invalid(sb.toString());
        }

        try {
            LocalDate parsedDate = DateParser.parseDate(inputDate);
            return ValidationResult.valid(inputDate, mantraKeyword, filePath, parsedDate);
        } catch (DateTimeParseException e) {
            return ValidationResult.invalid("❌ Invalid date format: \"" + inputDate + "\"\n❌ Formato de data inválido: \"" + inputDate + "\"");
        }
    }

    private static String cleanPlaceholder(TextField field, String placeholder) {
        String text = field.getText().trim();
        return text.equals(placeholder) ? "" : text;
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
