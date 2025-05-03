package com.example.mantracount;

public class ValidationResult {
    private final StringBuilder errors = new StringBuilder();
    private boolean hasErrors = false;

    public ValidationResult() {}  // default constructor

    public ValidationResult(boolean valid, String errorMessage) {
        if (!valid) {
            this.errors.append("❌ ").append(errorMessage);
            this.hasErrors = true;
        }
    }

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
