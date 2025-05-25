package com.example.mantracount;

import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles the display of mantra data and mismatched lines with expandable panel.
 * Manages the UI for viewing and editing mantra entries.
 */
public class MantrasDisplayController {

    private final Label placeholder;
    private final TextArea resultsArea;
    private final VBox mismatchesContainer;
    private final TitledPane mismatchTitledPane; // Changed to TitledPane for expandable functionality
    private final ScrollPane mismatchesScrollPane;

    private final MantraData mantraData;

    private List<String> mismatchedLines;
    private List<String> originalMismatchedLines = new ArrayList<>();

    /**
     * Creates a new MantrasDisplayController with expandable mismatch panel.
     *
     * @param mantraData The data model
     */
    public MantrasDisplayController(MantraData mantraData) {
        this.mantraData = mantraData;

        // Initialize results area - using original size with Portuguese text and English tooltip
        resultsArea = new TextArea("Contagem de Mantras");
        resultsArea.setStyle("-fx-text-fill: gray;");
        resultsArea.setPrefRowCount(6);
        resultsArea.setMinHeight(114);
        resultsArea.setMaxHeight(114);
        resultsArea.setEditable(false);
        resultsArea.setWrapText(true);

        // Add English tooltip to results area
        Tooltip resultsTooltip = new Tooltip("Mantra Count - Shows the counting results");
        resultsTooltip.setShowDelay(Duration.millis(300));
        resultsTooltip.setHideDelay(Duration.millis(100));
        Tooltip.install(resultsArea, resultsTooltip);

        // Initialize mismatches container and placeholder with Portuguese text and English tooltip
        placeholder = new Label("Nenhuma discrepância encontrada");
        placeholder.setStyle("-fx-text-fill: gray;");

        Tooltip placeholderTooltip = new Tooltip("No mismatches found");
        placeholderTooltip.setShowDelay(Duration.millis(300));
        placeholderTooltip.setHideDelay(Duration.millis(100));
        Tooltip.install(placeholder, placeholderTooltip);

        mismatchesContainer = new VBox(10);
        mismatchesContainer.setPadding(new javafx.geometry.Insets(10));
        mismatchesContainer.getChildren().add(placeholder);

        // Create scroll pane for the container - allow it to grow when expanded
        mismatchesScrollPane = new ScrollPane(mismatchesContainer);
        mismatchesScrollPane.setFitToWidth(true);
        mismatchesScrollPane.setPrefHeight(120);
        mismatchesScrollPane.setMinHeight(120);
        mismatchesScrollPane.setStyle("-fx-border-color: #0078D7; -fx-border-width: 2px;");

        // Add English tooltip to scroll pane
        Tooltip scrollTooltip = new Tooltip("Mismatch Lines Container - Shows lines that need attention");
        scrollTooltip.setShowDelay(Duration.millis(300));
        scrollTooltip.setHideDelay(Duration.millis(100));
        Tooltip.install(mismatchesScrollPane, scrollTooltip);

        // Create expandable titled pane with Portuguese text and English tooltip
        mismatchTitledPane = new TitledPane();
        mismatchTitledPane.setText("Discrepância de linhas");
        mismatchTitledPane.setContent(mismatchesScrollPane);
        mismatchTitledPane.setExpanded(false);
        mismatchTitledPane.setCollapsible(true);

        // Add English tooltip to titled pane
        Tooltip titledPaneTooltip = new Tooltip("Mismatch Lines - Click to expand/collapse. Shows lines requiring attention or confirmation.");
        titledPaneTooltip.setShowDelay(Duration.millis(300));
        titledPaneTooltip.setHideDelay(Duration.millis(100));
        Tooltip.install(mismatchTitledPane, titledPaneTooltip);

        // Set initial collapsed height
        mismatchTitledPane.setPrefHeight(50);
        mismatchTitledPane.setMinHeight(50);

        // Add listener to handle expansion/collapse
        mismatchTitledPane.expandedProperty().addListener((obs, wasExpanded, isExpanded) -> {
            if (isExpanded) {
                // When expanded, allow growth
                mismatchTitledPane.setPrefHeight(230);
                mismatchTitledPane.setMaxHeight(Region.USE_COMPUTED_SIZE);
                mismatchesScrollPane.setPrefHeight(200);
                mismatchesScrollPane.setMaxHeight(Region.USE_COMPUTED_SIZE);
                VBox.setVgrow(mismatchTitledPane, Priority.ALWAYS);
            } else {
                // When collapsed, minimize everything
                mismatchTitledPane.setPrefHeight(25);
                mismatchTitledPane.setMaxHeight(25);
                mismatchTitledPane.setMinHeight(25);
                VBox.setVgrow(mismatchTitledPane, Priority.NEVER);
            }
        });
    }

    /**
     * Gets the results area.
     * @return The text area for results
     */
    public TextArea getResultsArea() {
        return resultsArea;
    }

    /**
     * Gets the mismatches scroll pane (now wrapped in TitledPane).
     * @return The titled pane containing the scroll pane with mismatched lines
     */
    public TitledPane getMismatchesScrollPane() {
        return mismatchTitledPane;
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
     * Displays the analysis results in the results area - clean version without mismatch info.
     */
    public void displayResults() {
        String word = mantraData.getNameToCount();
        String capitalized = capitalizeFirst(word);

        // Use locale-sensitive date formatting instead of hardcoded MM/dd/yyyy
        String formattedDate = mantraData.getTargetDate().format(DateTimeFormatter.ofLocalizedDate(java.time.format.FormatStyle.SHORT));

        StringBuilder results = new StringBuilder();
        results.append("✔ Resultados de ").append(formattedDate).append(":\n--\n");
        results.append("Total '").append(capitalized).append("': ").append(mantraData.getTotalNameCount()).append("\n");
        results.append("Total 'Fiz': ").append(mantraData.getTotalFizCount()).append("\n");
        results.append("Total 'Mantra(s)/Rito(s)': ").append(mantraData.getTotalGenericCount()).append("\n");
        results.append("Total 📿: ").append(mantraData.getTotalFizNumbersSum());

        resultsArea.setText(results.toString());
        resultsArea.setStyle("-fx-text-fill: black;");
    }

    /**
     * Displays mismatched lines in the expandable container.
     *
     * @param lines The mismatched lines to display
     */
    public void displayMismatchedLines(List<String> lines) {
        mismatchedLines = lines;
        mismatchesContainer.getChildren().clear();

        if (mismatchedLines == null || mismatchedLines.isEmpty()) {
            // Keep the TitledPane visible but show "no mismatches" message
            mismatchTitledPane.setVisible(true);
            mismatchTitledPane.setManaged(true);
            mismatchTitledPane.setText("✅ Não há discrepância de linhas");
            mismatchTitledPane.setExpanded(false);

            // Update tooltip for no mismatches case
            Tooltip noMismatchTooltip = new Tooltip("No Mismatch Lines - All lines processed successfully");
            noMismatchTooltip.setShowDelay(Duration.millis(300));
            noMismatchTooltip.setHideDelay(Duration.millis(100));
            Tooltip.install(mismatchTitledPane, noMismatchTooltip);

            // Apply collapsed height settings to ensure no gap
            mismatchTitledPane.setPrefHeight(25);
            mismatchTitledPane.setMinHeight(25);
            mismatchTitledPane.setMaxHeight(25);
            VBox.setVgrow(mismatchTitledPane, Priority.NEVER);

            Label noIssuesLabel = new Label("✅ Nenhuma discrepância encontrada");
            noIssuesLabel.setStyle("-fx-text-fill: green; -fx-font-style: italic;");

            Tooltip noIssuesLabelTooltip = new Tooltip("No mismatches found - All entries are correct");
            noIssuesLabelTooltip.setShowDelay(Duration.millis(300));
            noIssuesLabelTooltip.setHideDelay(Duration.millis(100));
            Tooltip.install(noIssuesLabel, noIssuesLabelTooltip);

            mismatchesContainer.getChildren().add(noIssuesLabel);
            return;
        }

        // Show and configure the TitledPane when mismatches found
        mismatchTitledPane.setVisible(true);
        mismatchTitledPane.setManaged(true);
        mismatchTitledPane.setText("⚠ Linhas Requerendo Atenção (" + mismatchedLines.size() + ")");
        mismatchTitledPane.setExpanded(true);

        // Update tooltip for mismatches found case
        Tooltip mismatchFoundTooltip = new Tooltip("Lines Requiring Attention (" + mismatchedLines.size() + ") - Click to collapse. Edit the fields to fix mismatches.");
        mismatchFoundTooltip.setShowDelay(Duration.millis(300));
        mismatchFoundTooltip.setHideDelay(Duration.millis(100));
        Tooltip.install(mismatchTitledPane, mismatchFoundTooltip);

        for (String line : mismatchedLines) {
            int closeBracket = line.indexOf(']');
            int colon = line.indexOf(':', closeBracket);

            if (closeBracket != -1 && colon != -1) {
                String protectedPart = line.substring(0, colon + 1);
                String editablePart = (colon + 1 < line.length()) ? line.substring(colon + 1) : "";

                Label protectedLabel = new Label(protectedPart);
                protectedLabel.setStyle("-fx-font-weight: bold;");
                protectedLabel.setMinWidth(Region.USE_PREF_SIZE);

                // Add tooltip to protected label
                Tooltip protectedTooltip = new Tooltip("Protected part - Date and time (cannot be edited)");
                protectedTooltip.setShowDelay(Duration.millis(300));
                protectedTooltip.setHideDelay(Duration.millis(100));
                Tooltip.install(protectedLabel, protectedTooltip);

                TextField editableField = new TextField(editablePart);
                HBox.setHgrow(editableField, Priority.ALWAYS);
                editableField.setMaxWidth(Double.MAX_VALUE);

                // Add tooltip to editable field
                Tooltip editableTooltip = new Tooltip("Editable content - You can modify this text to fix the mismatch");
                editableTooltip.setShowDelay(Duration.millis(300));
                editableTooltip.setHideDelay(Duration.millis(100));
                Tooltip.install(editableField, editableTooltip);

                HBox lineContainer = new HBox(5, protectedLabel, editableField);
                lineContainer.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                mismatchesContainer.getChildren().add(lineContainer);
            } else {
                TextField fullLineField = new TextField(line);
                HBox.setHgrow(fullLineField, Priority.ALWAYS);
                fullLineField.setMaxWidth(Double.MAX_VALUE);

                // Add tooltip to full line field
                Tooltip fullLineTooltip = new Tooltip("Full line edit - You can modify this entire line");
                fullLineTooltip.setShowDelay(Duration.millis(300));
                fullLineTooltip.setHideDelay(Duration.millis(100));
                Tooltip.install(fullLineField, fullLineTooltip);

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
        resultsArea.setText("Contar Mantras");
        resultsArea.setStyle("-fx-text-fill: gray; -fx-font-style: bold;");

        mismatchesContainer.getChildren().clear();
        mismatchesContainer.getChildren().add(placeholder);

        // Reset titled pane to collapsed state
        mismatchTitledPane.setText("Discrepância de linhas");
        mismatchTitledPane.setExpanded(false);

        // Reset tooltip to original
        Tooltip resetTooltip = new Tooltip("Mismatch Lines - Click to expand/collapse. Shows lines requiring attention or confirmation.");
        resetTooltip.setShowDelay(Duration.millis(300));
        resetTooltip.setHideDelay(Duration.millis(100));
        Tooltip.install(mismatchTitledPane, resetTooltip);

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
            UIUtils.showInfo("✔ Alterações revertidas.");
        } else {
            UIUtils.showInfo("Não há alterações para reverter.");
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