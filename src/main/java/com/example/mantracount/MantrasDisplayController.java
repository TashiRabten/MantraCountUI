package com.example.mantracount;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MantrasDisplayController {

    private final Label placeholder;
    private final TextArea resultsArea;
    private final VBox mismatchesContainer;
    private final TitledPane mismatchTitledPane;
    private final ScrollPane mismatchesScrollPane;
    private final MantraImageController imageController;
    private final MantraData mantraData;

    private List<String> mismatchedLines;
    private List<String> originalMismatchedLines = new ArrayList<>();

    public MantrasDisplayController(MantraData mantraData) {
        this.mantraData = mantraData;
        this.imageController = new MantraImageController();

        this.resultsArea = UIComponentFactory.createResultsArea();
        this.placeholder = createPlaceholderLabel();
        this.mismatchesContainer = new VBox(10);
        this.mismatchesContainer.setStyle(
                "-fx-background-color: " + UIColorScheme.RESULTS_BACKGROUND + "; " +
                        "-fx-border-color: " + UIColorScheme.RESULTS_BACKGROUND + ";"
        );
        this.mismatchesContainer.setPadding(new javafx.geometry.Insets(10));
        this.mismatchesContainer.getChildren().add(placeholder);

        this.mismatchesScrollPane = UIComponentFactory.createStyledScrollPane(mismatchesContainer, 120);
        this.mismatchesScrollPane.setStyle(UIColorScheme.getMismatchedAreaStyle());
        this.mismatchTitledPane = createMismatchTitledPane();

        setupTitledPaneListener();
    }

    private Label createPlaceholderLabel() {
        return UIComponentFactory.createPlaceholderLabel(
                StringConstants.NO_MISMATCHES_PT,
                StringConstants.NO_MISMATCHES_EN
        );
    }

    private TitledPane createMismatchTitledPane() {
        TitledPane titledPane = new TitledPane();
        titledPane.setText(StringConstants.MISMATCH_LINES_PT);
        titledPane.setContent(mismatchesScrollPane);
        titledPane.setExpanded(false);
        titledPane.setCollapsible(true);

        // Use CSS styling that targets the internal structure
        titledPane.setStyle(UIColorScheme.getMismatchedTitleDropdownStyle());

        UIComponentFactory.addTooltip(titledPane, StringConstants.MISMATCH_LINES_EN);
        titledPane.setPrefHeight(50);
        titledPane.setMinHeight(50);

        // Apply styling after the component is fully created
        Platform.runLater(() -> {
            titledPane.applyCss();
            titledPane.layout();

            // Now safely lookup and style the title
            javafx.scene.Node titleRegion = titledPane.lookup(".title");
            if (titleRegion != null) {
                titleRegion.setStyle(
                        "-fx-background-color: " + UIColorScheme.NAVIGATION_COLOR + "; " +
                                "-fx-text-fill: white; "
                );
            }
        });

        return titledPane;
    }

    private void setupTitledPaneListener() {
        mismatchTitledPane.expandedProperty().addListener((obs, wasExpanded, isExpanded) -> {
            if (isExpanded) {
                mismatchTitledPane.setPrefHeight(230);
                mismatchTitledPane.setMaxHeight(Region.USE_COMPUTED_SIZE);
                mismatchesScrollPane.setPrefHeight(200);
                mismatchesScrollPane.setMaxHeight(Region.USE_COMPUTED_SIZE);
                VBox.setVgrow(mismatchTitledPane, Priority.ALWAYS);
            } else {
                mismatchTitledPane.setPrefHeight(25);
                mismatchTitledPane.setMaxHeight(25);
                mismatchTitledPane.setMinHeight(25);
                VBox.setVgrow(mismatchTitledPane, Priority.NEVER);
            }
        });
    }

    public void displayResults() {
        String word = mantraData.getNameToCount();
        String capitalized = capitalizeFirst(word);

        StringBuilder results = new StringBuilder();
        results.append(DateFormatUtils.createResultsHeader(mantraData.getTargetDate())).append("\n--\n");
        results.append("Total '").append(capitalized).append("': ").append(mantraData.getTotalNameCount()).append("\n");
        results.append("Total 'Fiz': ").append(mantraData.getTotalFizCount()).append("\n");
        results.append("Total 'Mantra(s)/Rito(s)': ").append(mantraData.getTotalGenericCount()).append("\n");
        results.append("Total 📿: ").append(mantraData.getTotalFizNumbersSum());

        UIComponentFactory.setTextAreaState(resultsArea, UIComponentFactory.TextAreaState.NORMAL, results.toString());

        imageController.updateImage(word);
    }

    public void displayMismatchedLines(List<String> lines) {
        mismatchedLines = lines;
        mismatchesContainer.getChildren().clear();

        if (mismatchedLines == null || mismatchedLines.isEmpty()) {
            displayNoMismatches();
            return;
        }

        displayMismatchesFound(lines);
    }

    private void displayNoMismatches() {
        mismatchTitledPane.setVisible(true);
        mismatchTitledPane.setManaged(true);
        mismatchTitledPane.setText(StringConstants.NO_MISMATCH_LINES_PT);
        mismatchTitledPane.setExpanded(false);

        UIComponentFactory.addTooltip(mismatchTitledPane,
                "No Mismatch Lines - All lines processed successfully");

        configureTitledPaneForNoMismatches();

        Label noIssuesLabel = UIComponentFactory.createPlaceholderLabel(
                StringConstants.NO_MISMATCHES_PT,
                "No mismatches found - All entries are correct"
        );
        noIssuesLabel.setStyle("-fx-text-fill: green; -fx-font-style: normal;");

        mismatchesContainer.getChildren().add(noIssuesLabel);
    }

    private void displayMismatchesFound(List<String> lines) {
        mismatchTitledPane.setVisible(true);
        mismatchTitledPane.setManaged(true);
        mismatchTitledPane.setText(StringConstants.LINES_REQUIRING_ATTENTION_PT + " (" + lines.size() + ")");
        mismatchTitledPane.setExpanded(true);

        UIComponentFactory.addTooltip(mismatchTitledPane,
                "Lines Requiring Attention (" + lines.size() + ") - Click to collapse. Edit the fields to fix mismatches.");

        for (String line : lines) {
            HBox lineContainer = createEditableLineContainer(line);
            mismatchesContainer.getChildren().add(lineContainer);
        }
    }

    private HBox createEditableLineContainer(String line) {
        LineParser.LineSplitResult splitResult = LineParser.splitEditablePortion(line);
        String protectedPart = splitResult.getFixedPrefix();
        String editablePart = splitResult.getEditableSuffix();

        if (!protectedPart.isEmpty()) {
            return createProtectedEditableStructure(protectedPart, editablePart);
        } else {
            return createFullEditableStructure(line);
        }
    }

    private HBox createProtectedEditableStructure(String protectedPart, String editablePart) {
        Label protectedLabel = new Label(protectedPart);
        protectedLabel.setStyle("-fx-font-weight: bold;");
        protectedLabel.setMinWidth(Region.USE_PREF_SIZE);
        UIComponentFactory.addTooltip(protectedLabel, StringConstants.PROTECTED_CONTENT_TOOLTIP);

        TextField editableField = UIComponentFactory.TextFields.createEditLineField(editablePart);
        HBox.setHgrow(editableField, Priority.ALWAYS);
        editableField.setMaxWidth(Double.MAX_VALUE);

        HBox lineContainer = new HBox(5, protectedLabel, editableField);
        lineContainer.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        return lineContainer;
    }

    private HBox createFullEditableStructure(String line) {
        TextField fullLineField = new TextField(line);
        fullLineField.setStyle(UIColorScheme.getInputFieldStyle());
        HBox.setHgrow(fullLineField, Priority.ALWAYS);
        fullLineField.setMaxWidth(Double.MAX_VALUE);

        UIComponentFactory.addTooltip(fullLineField, "Full line edit - You can modify this entire line");

        HBox lineContainer = new HBox(fullLineField);
        lineContainer.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        HBox.setHgrow(lineContainer, Priority.ALWAYS);

        return lineContainer;
    }

    private void configureTitledPaneForNoMismatches() {
        mismatchTitledPane.setPrefHeight(25);
        mismatchTitledPane.setMinHeight(25);
        mismatchTitledPane.setMaxHeight(25);
        VBox.setVgrow(mismatchTitledPane, Priority.NEVER);
    }

    public void resetDisplay() {
        UIComponentFactory.setTextAreaState(resultsArea, UIComponentFactory.TextAreaState.PLACEHOLDER,
                StringConstants.MANTRA_COUNT_RESULT_PT);

        mismatchesContainer.getChildren().clear();
        mismatchesContainer.getChildren().add(placeholder);

        mismatchTitledPane.setText(StringConstants.MISMATCH_LINES_PT);
        mismatchTitledPane.setExpanded(false);

        UIComponentFactory.addTooltip(mismatchTitledPane, StringConstants.MISMATCH_LINES_EN);

        mismatchedLines = null;
        originalMismatchedLines.clear();

        imageController.hideImage();
    }

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

    private String extractUpdatedLineFromNode(Node node) {
        if (node instanceof HBox lineContainer) {
            if (lineContainer.getChildren().size() >= 2) {
                Node firstChild = lineContainer.getChildren().get(0);
                Node secondChild = lineContainer.getChildren().get(1);

                if (firstChild instanceof Label protectedLabel && secondChild instanceof TextField editableField) {
                    return protectedLabel.getText() + editableField.getText();
                }
            } else if (lineContainer.getChildren().size() == 1) {
                Node child = lineContainer.getChildren().get(0);
                if (child instanceof TextField fullLineField) {
                    return fullLineField.getText();
                }
            }
        } else if (node instanceof TextField fullLineField) {
            return fullLineField.getText();
        }

        return null;
    }

    public void backupOriginalLines() {
        if (mismatchedLines != null) {
            originalMismatchedLines = new ArrayList<>(mismatchedLines);
        }
    }

    public void revertToOriginalLines() {
        if (originalMismatchedLines != null && !originalMismatchedLines.isEmpty()) {
            mismatchedLines = new ArrayList<>(originalMismatchedLines);
            displayMismatchedLines(mismatchedLines);
            UIUtils.showChangesRevertedSuccess();
        } else {
            UIUtils.showInfo("No changes to revert", "Não há alterações para reverter");
        }
    }

    public static String capitalizeFirst(String input) {
        if (input == null || input.isEmpty()) return input;
        return input.substring(0, 1).toUpperCase() + input.substring(1);
    }

    public TextArea getResultsArea() { return resultsArea; }
    public TitledPane getMismatchesScrollPane() { return mismatchTitledPane; }
    public VBox getMismatchesContainer() { return mismatchesContainer; }
    public Label getPlaceholder() { return placeholder; }
    public MantraImageController getImageController() { return imageController; }
    public List<String> getMismatchedLines() { return mismatchedLines; }

    public void shutdown() {
        imageController.shutdown();
    }
}