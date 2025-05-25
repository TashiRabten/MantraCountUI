package com.example.mantracount;

public class ValidationResult {
    private final StringBuilder errors = new StringBuilder();
    private boolean hasErrors = false;

    public ValidationResult() {}  // default constructor

    public ValidationResult(boolean valid, String errorMessage) {
        if (!valid) {
            // Don't add ❌ prefix here - let UIUtils handle formatting
            this.errors.append(errorMessage);
            this.hasErrors = true;
        }
    }

    // Add bilingual error method
    public void addBilingualError(String englishError, String portugueseError) {
        if (hasErrors) {
            errors.append("\n");
        }
        errors.append(englishError).append("\n").append(portugueseError);
        hasErrors = true;
    }

    public void addError(String error) {
        if (hasErrors) {
            errors.append("\n");
        }
        // Don't add ❌ prefix here - let UIUtils handle formatting
        errors.append(error);
        hasErrors = true;
    }

    public boolean isValid() {
        return !hasErrors;
    }

    public String getErrorMessage() {
        return errors.toString();
    }

    // Method to show errors using UIUtils
    public void showErrors() {
        if (hasErrors) {
            UIUtils.showError(getErrorMessage());
        }
    }
}