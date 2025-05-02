package com.example.mantracount;

public class InputValidator {
    public static ValidationResult validateInputs(String inputDate, String mantraKeyword, String filePath) {
        ValidationResult result = new ValidationResult();

        if (inputDate == null || inputDate.trim().isEmpty()) {
            result.addError("Date cannot be empty");
        } else if (!inputDate.matches("\\d{1,2}/\\d{1,2}/(\\d{2}|\\d{4})")) {
            result.addError("Invalid date format");
        }

        if (mantraKeyword == null || mantraKeyword.trim().isEmpty()) {
            result.addError("Mantra name cannot be empty");
        }

        if (filePath == null || filePath.trim().isEmpty()) {
            result.addError("File path cannot be empty");
        }

        return result;
    }

    public static class ValidationResult {
        private StringBuilder errors = new StringBuilder();
        private boolean hasErrors = false;

        public void addError(String error) {
            if (hasErrors) {
                errors.append("\n");
            }
            errors.append("❌ ").append(error);
            hasErrors = true;
        }

        public boolean isValid() {
            return !hasErrors;
        }

        public String getErrorMessage() {
            return errors.toString();
        }
    }
}