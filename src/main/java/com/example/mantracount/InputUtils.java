package com.example.mantracount;

import javafx.scene.control.TextField;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/**
 * Utility class for validating and parsing input fields.
 * Classe utilitária para validar e analisar campos de entrada.
 */
public class InputUtils {

    /**
     * Validates and parses input fields for date, mantra name, and file path.
     * Valida e analisa campos de entrada para data, nome do mantra e caminho do arquivo.
     *
     * @param dateField Field containing the date / Campo contendo a data
     * @param mantraField Field containing the mantra name / Campo contendo o nome do mantra
     * @param pathField Field containing the file path / Campo contendo o caminho do arquivo
     * @return ValidationResult object with results / Objeto ValidationResult com os resultados
     */
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

    /**
     * Cleans an input field by checking both placeholder text and gray text style.
     * Limpa um campo de entrada verificando tanto o texto do placeholder quanto o estilo de texto cinza.
     *
     * @param field The field to clean / O campo a ser limpo
     * @param placeholder The placeholder text / O texto do placeholder
     * @return The cleaned text or empty string if it's a placeholder / O texto limpo ou string vazia se for um placeholder
     */
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

    /**
     * Record class to represent validation results.
     * Classe de registro para representar resultados da validação.
     */
    public record ValidationResult(
            boolean valid,
            String errorMessage,
            String inputDate,
            String mantraKeyword,
            String filePath,
            LocalDate parsedDate
    ) {
        /**
         * Creates a valid validation result.
         * Cria um resultado de validação válido.
         */
        public static ValidationResult valid(String inputDate, String mantraKeyword, String filePath, LocalDate parsedDate) {
            return new ValidationResult(true, null, inputDate, mantraKeyword, filePath, parsedDate);
        }

        /**
         * Creates an invalid validation result with an error message.
         * Cria um resultado de validação inválido com uma mensagem de erro.
         */
        public static ValidationResult invalid(String errorMessage) {
            return new ValidationResult(false, errorMessage, null, null, null, null);
        }
    }
}