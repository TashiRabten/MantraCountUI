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

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Refactored Missing Fiz UI using centralized components and consistent styling.
 * Eliminates code duplication and provides consistent user experience.
 */
public class MissingFizUI {

    private VBox entriesContainer;
    private TextArea summaryArea;
    private ProgressIndicator progressIndicator;
    private MantraData mantraData;
    private List<MissingFizAnalyzer.MissingFizResult> currentResults;
    private Map<String, String> editedLines = new HashMap<>();
    private Button saveBtn;
    private Runnable onCloseCallback;

    public void show(Stage owner, MantraData data) {
        this.show(owner, data, null);
    }

    public void show(Stage owner, MantraData data, Runnable onCloseCallback) {
        this.mantraData = data;
        this.onCloseCallback = onCloseCallback;

        Stage dialog = createDialog(owner);
        VBox root = createMainLayout(dialog);

        dialog.setScene(new Scene(root, 800, 600));
        dialog.show();
    }

    /**
     * Creates the main dialog window
     */
    private Stage createDialog(Stage owner) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(owner);
        dialog.setTitle(StringConstants.MISSING_FIZ_TITLE);
        dialog.getIcons().add(new Image(getClass().getResourceAsStream("/icons/BUDA.jpg")));

        dialog.setOnCloseRequest(e -> {
            if (onCloseCallback != null) {
                onCloseCallback.run();
            }
        });

        return dialog;
    }

    /**
     * Creates the main layout using factory components
     */
    private VBox createMainLayout(Stage dialog) {
        VBox root = new VBox(10);
        root.setPadding(new Insets(15));

        Label header = createHeader();
        progressIndicator = UIComponentFactory.createProgressIndicator();
        summaryArea = createSummaryArea();
        Button analyzeButton = createAnalyzeButton();
        ScrollPane scrollPane = createEntriesScrollPane();
        HBox actions = createActionButtons(dialog);

        root.getChildren().addAll(
                header, summaryArea,
                UIComponentFactory.Layouts.createMainActionLayout(analyzeButton),
                progressIndicator, scrollPane, actions
        );

        return root;
    }

    /**
     * Creates the header using factory
     */
    private Label createHeader() {
        String capitalizedName = MantrasDisplayController.capitalizeFirst(mantraData.getNameToCount());
        return UIComponentFactory.createHeaderLabel(
                "Análise 'Sem Fiz' - " + capitalizedName,
                "Missing Fiz Analysis - Shows lines with mantra patterns but missing action words like 'fiz'"
        );
    }

    /**
     * Creates the summary area using factory
     */
    private TextArea createSummaryArea() {
        return UIComponentFactory.createSummaryArea(
                "Clique em 'Analisar' para encontrar linhas sem palavra 'fiz'"
        );
    }

    /**
     * Creates the analyze button
     */
    private Button createAnalyzeButton() {
        Button analyzeButton = UIComponentFactory.ActionButtons.createAnalyzeButton();
        analyzeButton.setOnAction(e -> analyzeAsync());
        return analyzeButton;
    }

    /**
     * Creates the entries scroll pane
     */
    private ScrollPane createEntriesScrollPane() {
        entriesContainer = new VBox(10);
        ScrollPane scrollPane = UIComponentFactory.createStyledScrollPane(entriesContainer, 300);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        UIComponentFactory.addTooltip(scrollPane,
                "Found Lines - Lines that match the pattern but are missing 'fiz' words. You can edit them here.");

        Label placeholder = UIComponentFactory.createPlaceholderLabel(
                StringConstants.NO_ANALYSIS_PT,
                StringConstants.NO_ANALYSIS_EN
        );
        entriesContainer.getChildren().add(placeholder);

        return scrollPane;
    }

    /**
     * Creates action buttons using factory with dialog alignment
     */
    private HBox createActionButtons(Stage dialog) {
        saveBtn = UIComponentFactory.ActionButtons.createSaveButton();
        saveBtn.setDisable(true);
        saveBtn.setOnAction(e -> saveChanges());

        Button closeBtn = UIComponentFactory.ActionButtons.createCloseButton();
        closeBtn.setOnAction(e -> {
            if (onCloseCallback != null) {
                onCloseCallback.run();
            }
            dialog.close();
        });

        return UIComponentFactory.Layouts.createDialogActionLayout(saveBtn, closeBtn);
    }

    /**
     * Performs analysis asynchronously
     */
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
                UIUtils.showError("Erro na análise: " + ex.getMessage(), "Analysis error: " + ex.getMessage());
            });
            return null;
        });
    }

    /**
     * Displays analysis results
     */
    private void displayResults(List<MissingFizAnalyzer.MissingFizResult> results) {
        updateSummaryArea(results);
        populateEntriesContainer(results);
        saveBtn.setDisable(results.isEmpty());
    }

    /**
     * Updates the summary area with results
     */
    private void updateSummaryArea(List<MissingFizAnalyzer.MissingFizResult> results) {
        String summary = MissingFizAnalyzer.generateSummary(results, mantraData.getNameToCount());
        summaryArea.setText(summary);
        summaryArea.setStyle("-fx-text-fill: black;");
        adjustSummaryAreaHeight(summary);
    }

    /**
     * Automatically adjusts the summary area height based on content
     */
    private void adjustSummaryAreaHeight(String content) {
        if (content == null || content.isEmpty()) {
            summaryArea.setPrefHeight(150);
            return;
        }

        int lineCount = content.split("\n").length;
        double estimatedHeight = Math.max(150, Math.min(350, lineCount * 24 + 30));

        summaryArea.setPrefHeight(estimatedHeight);
        summaryArea.setMaxHeight(estimatedHeight);
    }

    /**
     * Populates the entries container with results
     */
    private void populateEntriesContainer(List<MissingFizAnalyzer.MissingFizResult> results) {
        entriesContainer.getChildren().clear();

        if (results.isEmpty()) {
            Label noResults = UIComponentFactory.createPlaceholderLabel(
                    "✅ Nenhuma linha encontrada com padrão 'mantras/ritos de " +
                            mantraData.getNameToCount() + "' sem palavra de ação.",
                    "No missing fiz lines found - All entries appear to have action words"
            );
            noResults.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
            entriesContainer.getChildren().add(noResults);
            return;
        }

        Label resultsHeader = new Label(StringConstants.FOUND_LINES_PT + " (" + results.size() + "):");
        resultsHeader.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        entriesContainer.getChildren().add(resultsHeader);

        for (MissingFizAnalyzer.MissingFizResult result : results) {
            HBox lineContainer = createEditableLineContainer(result);
            entriesContainer.getChildren().add(lineContainer);
        }
    }

    /**
     * Creates an editable line container for a result
     */
    private HBox createEditableLineContainer(MissingFizAnalyzer.MissingFizResult result) {
        LineParser.LineSplitResult splitResult = LineParser.splitEditablePortion(result.getLine());
        String protectedPart = splitResult.getFixedPrefix();
        String editablePart = splitResult.getEditableSuffix();

        Label infoBadge = createInfoBadge(result);
        Label protectedLabel = createProtectedLabel(protectedPart);
        TextField editableField = createEditableField(editablePart, result);

        HBox lineContainer = new HBox(10, infoBadge, protectedLabel, editableField);
        lineContainer.setAlignment(Pos.CENTER_LEFT);
        lineContainer.setPadding(new Insets(5));
        lineContainer.setStyle("-fx-border-color: #FFCC80; -fx-border-width: 1px; -fx-border-radius: 3px;");

        return lineContainer;
    }

    /**
     * Creates info badge for the line
     */
    private Label createInfoBadge(MissingFizAnalyzer.MissingFizResult result) {
        String dateStr = DateFormatUtils.formatShortDate(result.getDate());
        String badgeText = dateStr + " | " + result.getTotalGenericCount() + " palavras";
        if (result.getExtractedNumber() > 0) {
            badgeText += " | " + result.getExtractedNumber() + " números";
        }

        return UIComponentFactory.createInfoBadge(badgeText,
                "Line info - Date, word count, and extracted numbers");
    }

    /**
     * Creates protected label for non-editable content
     */
    private Label createProtectedLabel(String protectedPart) {
        Label protectedLabel = new Label(protectedPart);
        protectedLabel.setStyle("-fx-font-weight: bold;");
        protectedLabel.setMinWidth(Region.USE_PREF_SIZE);
        UIComponentFactory.addTooltip(protectedLabel, StringConstants.PROTECTED_CONTENT_TOOLTIP);
        return protectedLabel;
    }

    /**
     * Creates editable field for line content
     */
    private TextField createEditableField(String editablePart, MissingFizAnalyzer.MissingFizResult result) {
        TextField editableField = new TextField(editablePart);
        editableField.setPromptText("Editar linha (ex: adicionar 'fiz')");
        HBox.setHgrow(editableField, Priority.ALWAYS);

        UIComponentFactory.addTooltip(editableField,
                "Editable content - You can add 'fiz' or modify this part of the line");

        editableField.textProperty().addListener((obs, oldVal, newVal) -> {
            String protectedPart = LineParser.splitEditablePortion(result.getLine()).getFixedPrefix();
            String newLine = protectedPart + newVal;
            editedLines.put(result.getLine(), newLine);
        });

        return editableField;
    }

    /**
     * Saves changes to file
     */
    private void saveChanges() {
        if (editedLines.isEmpty()) {
            UIUtils.showNoChangesInfo();
            return;
        }

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
            analyzeAsync(); // Re-analyze to show updated results
        }
    }
}