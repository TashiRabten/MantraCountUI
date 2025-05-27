package com.example.mantracount;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * UI for analyzing lines that may be missing "fiz" words.
 * Similar structure to MissingDaysUI but for a different purpose.
 */
public class MissingFizUI {

    private VBox entriesContainer;
    private TextArea summaryArea;
    private ProgressIndicator progressIndicator;
    private MantraData mantraData;
    private List<MissingFizAnalyzer.MissingFizResult> currentResults;
    private Map<String, String> editedLines = new HashMap<>();
    private Button saveBtn;
    private Runnable onCloseCallback; // Callback to update main UI button

    public void show(Stage owner, MantraData data) {
        this.show(owner, data, null);
    }

    public void show(Stage owner, MantraData data, Runnable onCloseCallback) {
        this.mantraData = data;
        this.onCloseCallback = onCloseCallback;

        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(owner);
        dialog.setTitle("Análise 'Sem Fiz' - Linhas sem Palavra de Ação");
        dialog.getIcons().add(new Image(getClass().getResourceAsStream("/icons/BUDA.jpg")));

        VBox root = new VBox(10);
        root.setPadding(new Insets(15));

        // Header
        String capitalizedName = MantrasDisplayController.capitalizeFirst(data.getNameToCount());
        Label header = new Label("Análise 'Sem Fiz' - " + capitalizedName);
        header.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        Tooltip headerTooltip = new Tooltip("Missing Fiz Analysis - Shows lines with mantra patterns but missing action words like 'fiz'");
        headerTooltip.setShowDelay(Duration.millis(300));
        headerTooltip.setHideDelay(Duration.millis(100));
        Tooltip.install(header, headerTooltip);

        // Progress indicator
        progressIndicator = new ProgressIndicator();
        progressIndicator.setMaxSize(50, 50);
        progressIndicator.setVisible(false);

        // Summary area
        summaryArea = new TextArea();
        summaryArea.setEditable(false);
        summaryArea.setWrapText(true);
        summaryArea.setPrefRowCount(8); // Start with more rows
        summaryArea.setMinHeight(150);  // Minimum height
        summaryArea.setMaxHeight(300);  // Allow it to grow up to this size
        summaryArea.setText("Clique em 'Analisar' para encontrar linhas sem palavra 'fiz'");
        summaryArea.setStyle("-fx-text-fill: gray;");

        Tooltip summaryTooltip = new Tooltip("Summary - Shows analysis results and statistics");
        summaryTooltip.setShowDelay(Duration.millis(300));
        summaryTooltip.setHideDelay(Duration.millis(100));
        Tooltip.install(summaryArea, summaryTooltip);

        // Analyze button
        Button analyzeButton = new Button("🔍 Analisar");
        analyzeButton.setStyle("-fx-base: #2196F3; -fx-text-fill: white;");
        analyzeButton.setOnAction(e -> analyzeAsync());

        Tooltip analyzeTooltip = new Tooltip("Analyze - Search for lines with mantra patterns but missing 'fiz' words");
        analyzeTooltip.setShowDelay(Duration.millis(300));
        analyzeTooltip.setHideDelay(Duration.millis(100));
        Tooltip.install(analyzeButton, analyzeTooltip);

        HBox analyzeBox = new HBox(10, analyzeButton);
        analyzeBox.setAlignment(Pos.CENTER_LEFT);

        // Container for found entries
        entriesContainer = new VBox(10);
        ScrollPane scrollPane = new ScrollPane(entriesContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(300);
        scrollPane.setStyle("-fx-border-color: #0078D7; -fx-border-width: 1px;");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        Tooltip scrollTooltip = new Tooltip("Found Lines - Lines that match the pattern but are missing 'fiz' words. You can edit them here.");
        scrollTooltip.setShowDelay(Duration.millis(300));
        scrollTooltip.setHideDelay(Duration.millis(100));
        Tooltip.install(scrollPane, scrollTooltip);

        // Initial placeholder
        Label placeholder = new Label("Nenhuma análise realizada ainda");
        placeholder.setStyle("-fx-text-fill: gray; -fx-font-style: italic;");
        Tooltip.install(placeholder, new Tooltip("No analysis performed yet - Click Analyze to start"));
        entriesContainer.getChildren().add(placeholder);

        // Action buttons
        saveBtn = new Button("💾 Salvar Alterações");
        saveBtn.setStyle("-fx-base: #4CAF50; -fx-text-fill: white;");
        saveBtn.setDisable(true); // Initially disabled
        saveBtn.setOnAction(e -> saveChanges());

        Tooltip saveTooltip = new Tooltip("Save Changes - Save any edits made to the found lines");
        saveTooltip.setShowDelay(Duration.millis(300));
        saveTooltip.setHideDelay(Duration.millis(100));
        Tooltip.install(saveBtn, saveTooltip);

        Button closeBtn = new Button("✖ Fechar");
        closeBtn.setStyle("-fx-base: #F44336; -fx-text-fill: white;");
        closeBtn.setOnAction(e -> {
            // Call the callback to update main UI button state
            if (onCloseCallback != null) {
                onCloseCallback.run();
            }
            dialog.close();
        });

        // Also handle window close (X button)
        dialog.setOnCloseRequest(e -> {
            if (onCloseCallback != null) {
                onCloseCallback.run();
            }
        });

        Tooltip closeTooltip = new Tooltip("Close - Close this window");
        closeTooltip.setShowDelay(Duration.millis(300));
        closeTooltip.setHideDelay(Duration.millis(100));
        Tooltip.install(closeBtn, closeTooltip);

        HBox actions = new HBox(10, saveBtn, closeBtn);
        actions.setAlignment(Pos.CENTER_RIGHT);
        actions.setPadding(new Insets(10, 0, 0, 0));

        root.getChildren().addAll(
                header, summaryArea, analyzeBox, progressIndicator, scrollPane, actions
        );

        dialog.setScene(new Scene(root, 800, 600));
        dialog.show();
    }

    private void analyzeAsync() {
        progressIndicator.setVisible(true);
        entriesContainer.getChildren().clear();
        editedLines.clear();

        CompletableFuture.supplyAsync(() ->
                MissingFizAnalyzer.findMissingFizLines(
                        mantraData.getLines(),
                        mantraData.getTargetDate(),
                        mantraData.getNameToCount()
                )
        ).thenAccept(results -> Platform.runLater(() -> {
            currentResults = results;
            displayResults(results);
            progressIndicator.setVisible(false);
        })).exceptionally(ex -> {
            Platform.runLater(() -> {
                progressIndicator.setVisible(false);
                UIUtils.showError("Erro na análise: " + ex.getMessage());
            });
            return null;
        });
    }

    private void displayResults(List<MissingFizAnalyzer.MissingFizResult> results) {
        // Update summary
        String summary = MissingFizAnalyzer.generateSummary(results, mantraData.getNameToCount());
        summaryArea.setText(summary);
        summaryArea.setStyle("-fx-text-fill: black;");

        // Auto-adjust summary area height based on content
        adjustSummaryAreaHeight(summary);

        // Clear and populate entries
        entriesContainer.getChildren().clear();

        if (results.isEmpty()) {
            Label noResults = new Label("✅ Nenhuma linha encontrada com padrão 'mantras/ritos de " +
                    mantraData.getNameToCount() + "' sem palavra de ação.");
            noResults.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
            Tooltip.install(noResults, new Tooltip("No missing fiz lines found - All entries appear to have action words"));
            entriesContainer.getChildren().add(noResults);

            // Disable save button when no results
            saveBtn.setDisable(true);
            return;
        }

        // Enable save button when results are found
        saveBtn.setDisable(false);

        // Add header
        Label resultsHeader = new Label("📋 Linhas encontradas (" + results.size() + "):");
        resultsHeader.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        entriesContainer.getChildren().add(resultsHeader);

        // Add each result as an editable line
        for (MissingFizAnalyzer.MissingFizResult result : results) {
            HBox lineContainer = createEditableLineContainer(result);
            entriesContainer.getChildren().add(lineContainer);
        }
    }

    /**
     * Automatically adjust the summary area height based on content
     */
    private void adjustSummaryAreaHeight(String content) {
        if (content == null || content.isEmpty()) {
            summaryArea.setPrefHeight(150);
            return;
        }

        // Count lines in the content
        int lineCount = content.split("\n").length;

        // Estimate height needed (approximately 22 pixels per line + padding)
        double estimatedHeight = Math.max(150, Math.min(350, lineCount * 24 + 30));

        summaryArea.setPrefHeight(estimatedHeight);
        summaryArea.setMaxHeight(estimatedHeight);
    }

    private HBox createEditableLineContainer(MissingFizAnalyzer.MissingFizResult result) {
        // Split the line using the same logic as other parts of the application
        LineParser.LineSplitResult splitResult = LineParser.splitEditablePortion(result.getLine());
        String protectedPart = splitResult.getFixedPrefix();
        String editablePart = splitResult.getEditableSuffix();

        // Create info badge
        DateTimeFormatter formatter = DateTimeFormatter.ofLocalizedDate(java.time.format.FormatStyle.SHORT);
        String dateStr = result.getDate().format(formatter);
        String badgeText = dateStr + " | " + result.getTotalGenericCount() + " palavras";
        if (result.getExtractedNumber() > 0) {
            badgeText += " | " + result.getExtractedNumber() + " números";
        }

        Label infoBadge = new Label(badgeText);
        infoBadge.setPadding(new Insets(2, 8, 2, 8));
        infoBadge.setStyle("-fx-background-color: #FFE0B2; -fx-background-radius: 4px; " +
                "-fx-font-size: 11px; -fx-text-fill: #E65100;");
        infoBadge.setMinWidth(150);

        Tooltip badgeTooltip = new Tooltip("Line info - Date, word count, and extracted numbers");
        badgeTooltip.setShowDelay(Duration.millis(300));
        badgeTooltip.setHideDelay(Duration.millis(100));
        Tooltip.install(infoBadge, badgeTooltip);

        // Create protected label
        Label protectedLabel = new Label(protectedPart);
        protectedLabel.setStyle("-fx-font-weight: bold;");
        protectedLabel.setMinWidth(Region.USE_PREF_SIZE);

        Tooltip protectedTooltip = new Tooltip("Protected content - Date, time and sender (cannot be edited)");
        protectedTooltip.setShowDelay(Duration.millis(300));
        protectedTooltip.setHideDelay(Duration.millis(100));
        Tooltip.install(protectedLabel, protectedTooltip);

        // Create editable field
        TextField editableField = new TextField(editablePart);
        editableField.setPromptText("Editar linha (ex: adicionar 'fiz')");
        HBox.setHgrow(editableField, Priority.ALWAYS);

        Tooltip editableTooltip = new Tooltip("Editable content - You can add 'fiz' or modify this part of the line");
        editableTooltip.setShowDelay(Duration.millis(300));
        editableTooltip.setHideDelay(Duration.millis(100));
        Tooltip.install(editableField, editableTooltip);

        // Track changes
        editableField.textProperty().addListener((obs, oldVal, newVal) -> {
            String newLine = protectedPart + newVal;
            editedLines.put(result.getLine(), newLine);
        });

        HBox lineContainer = new HBox(10, infoBadge, protectedLabel, editableField);
        lineContainer.setAlignment(Pos.CENTER_LEFT);
        lineContainer.setPadding(new Insets(5));
        lineContainer.setStyle("-fx-border-color: #FFCC80; -fx-border-width: 1px; -fx-border-radius: 3px;");

        return lineContainer;
    }

    private void saveChanges() {
        if (editedLines.isEmpty()) {
            UIUtils.showInfo("Nenhuma alteração para salvar.");
            return;
        }

        // Reuse the existing FileManagementController save logic
        // Create a FileManagementController instance (we need the save functionality)
        FileManagementController fileController = new FileManagementController(
                (Stage) entriesContainer.getScene().getWindow(),
                mantraData,
                new VBox(), // dummy container
                new Label(), // dummy placeholder
                new TextArea() // dummy results area
        );

        boolean success = fileController.saveChanges(editedLines);
        if (success) {
            editedLines.clear();
            // Re-analyze to show updated results
            analyzeAsync();
        }
    }
}