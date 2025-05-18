package com.example.mantracount;

import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles the display of mantra data and mismatched lines.
 * Manages the UI for viewing and editing mantra entries.
 */
public class MantrasDisplayController {

    private final Label placeholder;
    private final TextArea resultsArea;
    private final VBox mismatchesContainer;
    private final ScrollPane mismatchesScrollPane;
    private final MantraData mantraData;

    private List<String> mismatchedLines;
    private List<String> originalMismatchedLines = new ArrayList<>();

    /**
     * Creates a new MantrasDisplayController.
     *
     * @param mantraData The data model
     */
    public MantrasDisplayController(MantraData mantraData) {
        this.mantraData = mantraData;

        // Initialize results area - using original size
        resultsArea = new TextArea("Mantra Count / Contagem de Mantras");
        resultsArea.setStyle("-fx-text-fill: gray;");
        resultsArea.setPrefRowCount(6);
        resultsArea.setMinHeight(114);
        resultsArea.setMaxHeight(114);
        resultsArea.setEditable(false);
        resultsArea.setWrapText(true);

        // Initialize mismatches container and placeholder
        placeholder = new Label("Mismatch Line / Discrepância de linhas");
        placeholder.setStyle("-fx-text-fill: gray;");

        mismatchesContainer = new VBox(10);
        mismatchesContainer.setPadding(new javafx.geometry.Insets(10));
        mismatchesContainer.getChildren().add(placeholder);

        mismatchesScrollPane = new ScrollPane(mismatchesContainer);
        mismatchesScrollPane.setFitToWidth(true);
        mismatchesScrollPane.setPrefHeight(240);
        mismatchesScrollPane.setMaxHeight(240);
        mismatchesScrollPane.setStyle("-fx-border-color: #0078D7; -fx-border-width: 2px;");
        VBox.setVgrow(mismatchesScrollPane, Priority.ALWAYS);
    }

    /**
     * Gets the results area.
     * @return The text area for results
     */
    public TextArea getResultsArea() {
        return resultsArea;
    }

    /**
     * Gets the mismatches scroll pane.
     * @return The scroll pane containing mismatched lines
     */
    public ScrollPane getMismatchesScrollPane() {
        return mismatchesScrollPane;
    }

    /**
     * Gets the mismatches container.
     * @return The container for mismatched lines
     */
    public VBox getMismatchesContainer() {
        return mismatchesContainer;
    }

    /**
     * Gets the placeholder label.
     * @return The placeholder label
     */
    public Label getPlaceholder() {
        return placeholder;
    }

    /**
     * Displays the analysis results in the results area.
     */
    public void displayResults() {
        Label emojiLabel = new Label("📿");
        String word = mantraData.getNameToCount();
        String capitalized = capitalizeFirst(word);  // "Vajrasattva"

        String formattedDate = mantraData.getTargetDate().format(DateTimeFormatter.ofPattern("MM/dd/yyyy"));
        resultsArea.setText("✔ Results from / Resultados de " + formattedDate + ":\n--\n" +
                "Total '" + capitalized + "': " + mantraData.getTotalNameCount() + "\n" +
                "Total 'Fiz': " + mantraData.getTotalFizCount() + "\n" +
                "Total 'Mantra(s)/Rito(s)': " + mantraData.getTotalGenericCount() + "\n" +
                "Total " + emojiLabel.getText() + ": " + mantraData.getTotalFizNumbersSum());
        resultsArea.setStyle("-fx-text-fill: black;");
    }

    /**
     * Displays mismatched lines in the mismatches container.
     *
     * @param lines The mismatched lines to display
     */
    public void displayMismatchedLines(List<String> lines) {
        mismatchedLines = lines;
        mismatchesContainer.getChildren().clear();

        if (mismatchedLines == null || mismatchedLines.isEmpty()) {
            mismatchesContainer.getChildren().add(placeholder);
            return;
        }

        for (String line : mismatchedLines) {
            int closeBracket = line.indexOf(']');
            int colon = line.indexOf(':', closeBracket);

            if (closeBracket != -1 && colon != -1) {
                String protectedPart = line.substring(0, colon + 1);
                String editablePart = (colon + 1 < line.length()) ? line.substring(colon + 1) : "";

                Label protectedLabel = new Label(protectedPart);
                protectedLabel.setStyle("-fx-font-weight: bold;");
                protectedLabel.setMinWidth(Region.USE_PREF_SIZE);

                TextField editableField = new TextField(editablePart);
                // Make text field expand to fill available space
                HBox.setHgrow(editableField, Priority.ALWAYS);
                editableField.setMaxWidth(Double.MAX_VALUE);

                HBox lineContainer = new HBox(5, protectedLabel, editableField);
                lineContainer.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                mismatchesContainer.getChildren().add(lineContainer);
            } else {
                TextField fullLineField = new TextField(line);
                // Make text field expand to fill available space
                HBox.setHgrow(fullLineField, Priority.ALWAYS);
                fullLineField.setMaxWidth(Double.MAX_VALUE);

                HBox lineContainer = new HBox(fullLineField);
                lineContainer.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                HBox.setHgrow(lineContainer, Priority.ALWAYS);

                mismatchesContainer.getChildren().add(lineContainer);
            }
        }
    }

    /**
     * Resets the display to its initial state.
     */
    public void resetDisplay() {
        resultsArea.setText("Count Mantras / Contar Mantras");
        resultsArea.setStyle("-fx-text-fill: gray; -fx-font-style: bold;");
        mismatchesContainer.getChildren().clear();
        mismatchesContainer.getChildren().add(placeholder);
        mismatchedLines = null;
        originalMismatchedLines.clear();
    }

    /**
     * Extracts the updated content from the UI.
     *
     * @return A map of original lines to updated lines
     */
    public Map<String, String> extractUpdatedContentFromUI() {
        Map<String, String> updatedMismatchMap = new HashMap<>();
        for (int i = 0; i < mismatchesContainer.getChildren().size(); i++) {
            Node node = mismatchesContainer.getChildren().get(i);
            if (node == placeholder || i >= mismatchedLines.size()) continue;
            String originalLine = mismatchedLines.get(i);
            String updatedLine = extractUpdatedLineFromNode(node);
            if (updatedLine != null) {
                updatedMismatchMap.put(originalLine, updatedLine);
            }
        }
        return updatedMismatchMap;
    }

    /**
     * Extracts updated line content from a node.
     *
     * @param node The node to extract content from
     * @return The updated line content or null if extraction failed
     */
    private String extractUpdatedLineFromNode(Node node) {
        if (node instanceof HBox lineContainer) {
            if (lineContainer.getChildren().size() >= 2) {
                Node firstChild = lineContainer.getChildren().get(0);
                Node secondChild = lineContainer.getChildren().get(1);

                if (firstChild instanceof Label protectedLabel && secondChild instanceof TextField editableField) {
                    return protectedLabel.getText() + editableField.getText();
                }
            }
        } else if (node instanceof TextField fullLineField) {
            return fullLineField.getText();
        }
        return null;
    }

    /**
     * Stores the original mismatched lines for revert functionality.
     */
    public void backupOriginalLines() {
        if (mismatchedLines != null) {
            originalMismatchedLines = new ArrayList<>(mismatchedLines);
        }
    }

    /**
     * Reverts to the original mismatched lines.
     */
    public void revertToOriginalLines() {
        if (originalMismatchedLines != null && !originalMismatchedLines.isEmpty()) {
            // Restore the mismatchedLines to their original state
            mismatchedLines = new ArrayList<>(originalMismatchedLines);
            // Redisplay the original mismatched lines
            displayMismatchedLines(mismatchedLines);
            UIUtils.showInfo("✔ Changes reverted. \n✔ Alterações revertidas.");
        } else {
            UIUtils.showInfo("No changes to revert. \nNão há alterações para reverter.");
        }
    }

    /**
     * Capitalizes the first letter of a string.
     *
     * @param input The string to capitalize
     * @return The capitalized string
     */
    public static String capitalizeFirst(String input) {
        if (input == null || input.isEmpty()) return input;
        return input.substring(0, 1).toUpperCase() + input.substring(1);
    }

    /**
     * Gets the current mismatched lines.
     *
     * @return The list of mismatched lines
     */
    public List<String> getMismatchedLines() {
        return mismatchedLines;
    }
}