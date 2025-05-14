package com.example.mantracount;

import java.time.format.DateTimeParseException;



public class InputValidator {
    public static ValidationResult validateInputs(String inputDate, String mantraKeyword, String filePath) {
        if (inputDate == null || inputDate.trim().isEmpty()) {
            return new ValidationResult(false, "❌ Date is required.\n❌ Data é obrigatória.");
        }

        if (mantraKeyword == null || mantraKeyword.trim().isEmpty()) {
            return new ValidationResult(false, "❌ Mantra is required.\n❌ Mantra é obrigatório.");
        }

        if (filePath == null || filePath.trim().isEmpty()) {
            return new ValidationResult(false, "❌ File path is required.\n❌ Caminho do arquivo é obrigatório.");
        }

        // Attempt parsing here to catch format/logical issues early
        try {
            DateParser.parseDate(inputDate);
        } catch (DateTimeParseException e) {
            return new ValidationResult(false,
                    "❌ Invalid date: \"" + inputDate + "\"\n❌ Data inválida: \"" + inputDate + "\"");
        }

        return new ValidationResult(true, "");
    }

}