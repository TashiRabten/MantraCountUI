package com.example.mantracount;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.InputStream;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Refactored Missing Fiz UI using centralized components and consistent styling.
 * Fixed to match MissingDaysUI blue background styling.
 */
public class MissingFizUI {

    private VBox entriesContainer;
    private Label summaryArea; // CHANGED: From TextArea to Label
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

        InputStream stream = getClass().getResourceAsStream("/icons/BUDA.png");
        if (stream != null) {
            System.out.println("Image found!");
            ImageView iconView = new ImageView(new Image(stream));
            iconView.setFitWidth(256);
            iconView.setFitHeight(256);
            dialog.getIcons().add(iconView.getImage());
        } else {
            System.out.println("Image not found: /icons/BUDA.png");
        }

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
        VBox root = new VBox(UIComponentFactory.LARGE_SPACING);
        root.setPadding(new Insets(15));

        applyThemeColors(root);

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

    private void applyThemeColors(VBox root) {
        root.setStyle(UIColorScheme.getMainBackgroundStyle());
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
     * Creates the summary area using Label instead of TextArea for proper centering
     */
    private Label createSummaryArea() {
        // FIXED: Create Label instead of TextArea but keep all the same method calls
        Label summaryArea = new Label("Clique em 'Analisar' para encontrar linhas sem palavra 'fiz'");
        summaryArea.setWrapText(true);
        summaryArea.setPrefHeight(150);
        summaryArea.setMinHeight(150);
        summaryArea.setMaxHeight(300);

        // Apply styling with centering and padding
        summaryArea.setStyle(UIColorScheme.getSummaryContainerStyle());

        summaryArea.setAlignment(Pos.CENTER);
        summaryArea.setMaxWidth(Double.MAX_VALUE);

        UIComponentFactory.addTooltip(summaryArea, "Summary - Shows analysis results and statistics");
        return summaryArea;
    }

    /**
     * Updates the summary area with results
     */
    private void updateSummaryArea(List<MissingFizAnalyzer.MissingFizResult> results) {
        String summary = MissingFizAnalyzer.generateSummary(results, mantraData.getNameToCount());
        summaryArea.setText(summary);

        // FIXED: Different styling based on content with padding
        if (results.isEmpty() || summary.contains("Clique em")) {
            // For initial state or empty results - center the text
            summaryArea.setStyle(
                    UIColorScheme.getResultsAreaStyle() +
                            "-fx-alignment: center; " +
                            "-fx-text-alignment: center; " +
                            "-fx-padding: 10px;" // Add padding
            );
            summaryArea.setAlignment(Pos.CENTER);
        } else {
            // For actual results - left align for readability
            summaryArea.setStyle(
                    UIColorScheme.getResultsAreaStyle() +
                            "-fx-alignment: top-left; " +
                            "-fx-text-alignment: left; " +
                            "-fx-padding: 10px;" // Add padding for results too
            );
            summaryArea.setAlignment(Pos.TOP_LEFT);
        }

        adjustSummaryAreaHeight(summary);
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
     * Creates the entries scroll pane with proper centering
     */
    private ScrollPane createEntriesScrollPane() {
        entriesContainer = new VBox(UIComponentFactory.NO_SPACING); // No spacing like MissingDaysUI
        entriesContainer.setStyle(UIColorScheme.getResultsAreaStyle()); // Blue background
        entriesContainer.setFillWidth(true);

        // FIXED: Set alignment to center the content vertically and horizontally
        entriesContainer.setAlignment(Pos.CENTER);

        ScrollPane scrollPane = UIComponentFactory.createStyledScrollPane(entriesContainer, 300);
        scrollPane.setStyle(UIColorScheme.getResultsAreaStyle()); // Blue background
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        UIComponentFactory.addTooltip(scrollPane,
                "Found Lines - Lines that match the pattern but are missing 'fiz' words. You can edit them here.");

        Label placeholder = UIComponentFactory.createPlaceholderLabel(
                StringConstants.NO_ANALYSIS_PT,
                StringConstants.NO_ANALYSIS_EN
        );

        // FIXED: Ensure the label itself is centered and takes full width
        placeholder.setAlignment(Pos.CENTER);
        placeholder.setMaxWidth(Double.MAX_VALUE);

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

        // FIXED: Maintain blue background after clearing
        entriesContainer.setStyle(UIColorScheme.getResultsAreaStyle());

        if (results.isEmpty()) {
            // FIXED: Set container alignment to center
            entriesContainer.setAlignment(Pos.CENTER);

            Label noResults = UIComponentFactory.createPlaceholderLabel(
                    "✅ Nenhuma linha encontrada com padrão 'mantras/ritos de " +
                            mantraData.getNameToCount() + "' sem palavra de ação.",
                    "No missing fiz lines found - All entries appear to have action words"
            );

            // FIXED: Style with green color, center alignment and padding
            noResults.setStyle(
                    UIColorScheme.getPlaceholderLabelStyle() +
                            "-fx-text-fill: green; " +
                            "-fx-font-weight: bold; " +
                            "-fx-padding: 10px;" // Add padding
            );

            // FIXED: Ensure the label is centered and takes full width
            noResults.setAlignment(Pos.CENTER);
            noResults.setMaxWidth(Double.MAX_VALUE);

            entriesContainer.getChildren().add(noResults);
            return;
        }

        // FIXED: Reset alignment to TOP_LEFT when showing actual results
        entriesContainer.setAlignment(Pos.TOP_LEFT);

        Label resultsHeader = new Label(StringConstants.FOUND_LINES_PT + " (" + results.size() + "):");
        resultsHeader.setStyle(UIColorScheme.getResultsHeaderStyle());
        entriesContainer.getChildren().add(resultsHeader);

        for (MissingFizAnalyzer.MissingFizResult result : results) {
            VBox lineContainer = createEditableLineContainer(result);
            entriesContainer.getChildren().add(lineContainer);
        }

        // FIXED: Ensure blue background is maintained throughout
        entriesContainer.setStyle(UIColorScheme.getResultsAreaStyle());
    }

    /**
     * Creates an editable line container for a result - matches MissingDaysUI style
     */
    private VBox createEditableLineContainer(MissingFizAnalyzer.MissingFizResult result) {
        LineParser.LineSplitResult splitResult = LineParser.splitEditablePortion(result.getLine());
        String protectedPart = splitResult.getFixedPrefix();
        String editablePart = splitResult.getEditableSuffix();

        Label infoBadge = createInfoBadge(result);
        Label protectedLabel = createProtectedLabel(protectedPart);
        TextField editableField = createEditableField(editablePart, result);

        HBox lineContent = new HBox(UIComponentFactory.STANDARD_SPACING, infoBadge, protectedLabel, editableField);
        lineContent.setAlignment(Pos.CENTER_LEFT);
        lineContent.setPadding(new Insets(5));

        // Wrap in VBox with white background like MissingDaysUI
        VBox lineContainer = new VBox(UIComponentFactory.NO_SPACING, lineContent);
        lineContainer.setStyle(UIColorScheme.getResultsContainerStyle()); // White background
        lineContainer.setUserData(result.getLine());

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
        protectedLabel.setStyle(UIColorScheme.getFieldLabelStyle());
        protectedLabel.setMinWidth(Region.USE_PREF_SIZE);
        UIComponentFactory.addTooltip(protectedLabel, StringConstants.PROTECTED_CONTENT_TOOLTIP);
        return protectedLabel;
    }

    /**
     * Creates editable field for line content with white background
     */
    private TextField createEditableField(String editablePart, MissingFizAnalyzer.MissingFizResult result) {
        TextField editableField = UIComponentFactory.TextFields.createEditLineField(editablePart);
        editableField.setPromptText("Editar linha (ex: adicionar 'fiz')");
        editableField.setStyle(UIColorScheme.getEditableFieldStyle()); // Ensure white background
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