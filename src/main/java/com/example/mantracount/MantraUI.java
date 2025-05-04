package com.example.mantracount;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.File;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MantraUI extends Application {

    private TextField searchField;
    private Button searchButton, prevButton, nextButton;
    private CheckBox exactWordCheckBox;
    private List<String> mismatchedLines;
    private VBox mismatchesContainer;
    private final Label placeholder = new Label("Mismatch Line\n(Discrepância de linhas)");
    private ScrollPane mismatchesScrollPane;
    private Stage primaryStage;
    private final MantraData mantraData = new MantraData();
    private String lastSearchQuery = "";  // Store the last search query
    private int currentSearchIndex = -1;  // Store the current index for search navigation
    private List<Node> searchMatches = new ArrayList<>();  // Store search matches


    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        primaryStage.setTitle("MantraCount");

        VBox vbox = new VBox(10);
        vbox.setPadding(new Insets(20));
        Scene scene = new Scene(vbox, 700, 600);
        primaryStage.setScene(scene);

        TextField dateField = new TextField();
        UIUtils.setPlaceholder(dateField, "Enter start date - MM/DD/YY");

        TextField mantraField = new TextField();
        UIUtils.setPlaceholder(mantraField, "Enter mantra name");

        TextField pathField = new TextField();
        UIUtils.setPlaceholder(pathField, "Open a file...");
        pathField.setPrefWidth(400);

        Button openFileButton = new Button("Open File");

        TextArea resultsArea = new TextArea("Count Mantras");
        resultsArea.setStyle("-fx-text-fill: gray;-fx-font-style: italic;");
        resultsArea.setPrefRowCount(6);
        resultsArea.setMinHeight(114);
        resultsArea.setMaxHeight(114);
        resultsArea.setEditable(false);
        resultsArea.setWrapText(true);

        Button processButton = new Button("Count Mantras");
        processButton.setStyle("-fx-base: #4CAF50; -fx-text-fill: white;");

        Button clearResultsButton = new Button("Clear Results");
        clearResultsButton.setStyle("-fx-base: #F44336; -fx-text-fill: white;");

        Button checkMissingDaysButton = new Button("Check Missing Days");
        checkMissingDaysButton.setStyle("-fx-base: #2196F3; -fx-text-fill: white;");
        checkMissingDaysButton.setDisable(true);

        HBox processBox = new HBox(10, processButton, clearResultsButton, checkMissingDaysButton);
        processBox.setAlignment(Pos.CENTER_LEFT);

        mismatchesContainer = new VBox(5);
        mismatchesContainer.setPadding(new Insets(5));

        mismatchesScrollPane = new ScrollPane(mismatchesContainer);
        mismatchesScrollPane.setFitToWidth(true);
        mismatchesScrollPane.setPrefHeight(240);
        mismatchesScrollPane.setMaxHeight(240);
        mismatchesScrollPane.setStyle("-fx-border-color: #0078D7; -fx-border-width: 2px;");

        placeholder.setStyle("-fx-text-fill: gray; -fx-font-style: italic;");
        mismatchesContainer.getChildren().add(placeholder);

        Button saveButton = new Button("Save Changes");
        saveButton.setStyle("-fx-base: #4CAF50; -fx-text-fill: white;");

        Button cancelButton = new Button("Cancel Changes");
        cancelButton.setStyle("-fx-base: #F44336; -fx-text-fill: white;");

        HBox buttonBox = new HBox(10, saveButton, cancelButton);
        buttonBox.setAlignment(Pos.CENTER_LEFT);
        VBox.setMargin(buttonBox, new Insets(5, 0, 0, 0));

        openFileButton.setOnAction(event -> {
            try {
                File selectedFile = FileLoader.openFile(primaryStage, pathField, resultsArea, mismatchesContainer, placeholder, new File(System.getProperty("user.home")), mantraData);

                if (selectedFile == null) return;

                pathField.setText(selectedFile.getAbsolutePath());
                mantraData.setFilePath(selectedFile.getAbsolutePath());
                mantraData.setLines(FileLoader.robustReadLines(selectedFile.toPath()));
                mantraData.setFromZip(selectedFile.getName().toLowerCase().endsWith(".zip"));
                mantraData.setOriginalZipPath(mantraData.isFromZip() ? selectedFile.getAbsolutePath() : null);

                resultsArea.setText("Count Mantras");
                resultsArea.setStyle("-fx-text-fill: gray; -fx-font-style: italic;");
                resultsArea.setEditable(false);

                mismatchesContainer.getChildren().clear();
                mismatchesContainer.getChildren().add(placeholder);
                checkMissingDaysButton.setDisable(true);
                UIUtils.showInfo("✔ File loaded.");
            } catch (Exception ex) {
                ex.printStackTrace();
                UIUtils.showError("❌ Failed to load file: " + ex.getMessage());
            }
        });

        processButton.setOnAction(e -> {
            boolean ready = MissingDaysHelper.prepareDataForMissingDays(dateField.getText(), mantraField.getText(), pathField.getText(), mantraData);
            if (!ready) return;

            // Extract .txt from .zip if necessary
            if (mantraData.getFilePath().toLowerCase().endsWith(".zip")) {
                try {
                    File extracted = FileProcessorService.extractFirstTxtFromZip(new File(mantraData.getFilePath()));
                    mantraData.setFromZip(true);
                    mantraData.setOriginalZipPath(mantraData.getFilePath());
                    mantraData.setFilePath(extracted.getAbsolutePath());
                    mantraData.setLines(FileLoader.robustReadLines(extracted.toPath()));
                } catch (Exception ex) {
                    ex.printStackTrace();
                    UIUtils.showError("❌ Failed to extract .zip file.");
                    return;
                }
            }

            try {
                FileProcessorService.processFile(mantraData);
                displayResults(resultsArea);
                mismatchedLines = mantraData.getDebugLines();
                displayMismatchedLines(mismatchedLines);
                checkMissingDaysButton.setDisable(mismatchedLines.isEmpty());
            } catch (Exception ex) {
                ex.printStackTrace();
                UIUtils.showError("❌ Error processing file: " + ex.getMessage());
            }
        });

        saveButton.setOnAction(e -> {
            try {
                if (mantraData.getLines() == null || mismatchedLines == null) {
                    UIUtils.showError("❌ No file loaded.");
                    return;
                }

                Map<String, String> updatedMismatchMap = extractUpdatedContentFromUI();
                int updateCount = updateFileContent(updatedMismatchMap);
                FileEditSaver.saveToFile(mantraData.getLines(), mantraData.getFilePath());

                if (mantraData.isFromZip()) {
                    FileEditSaver.updateZipFile(mantraData.getOriginalZipPath(), mantraData.getFilePath(), mantraData.getLines());
                }

                UIUtils.showInfo("✔ Changes saved successfully.\n✔ " + updateCount + " line(s) updated.");
                resetSearchState();

            } catch (Exception ex) {
                ex.printStackTrace();
                UIUtils.showError("❌ Failed to save changes.");
            }
        });

        cancelButton.setOnAction(e -> {
            if (mantraData.getDebugLines() != null) {
                displayMismatchedLines(mantraData.getDebugLines());
                UIUtils.showInfo("✔ Changes reverted.");
                resetSearchState();
                checkMissingDaysButton.setDisable(true);
            }
        });

        checkMissingDaysButton.setOnAction(e -> {
            try {
                MissingDaysUI missingDaysUI = new MissingDaysUI();
                missingDaysUI.show(primaryStage, mantraData);
            } catch (Exception ex) {
                ex.printStackTrace();
                UIUtils.showError("❌ Error checking missing days: " + ex.getMessage());
            }
        });

        vbox.getChildren().addAll(dateField, mantraField, new HBox(10, pathField, openFileButton), resultsArea, processBox, mismatchesScrollPane, buttonBox);
        primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("/icons/BUDA.jpg")));
        primaryStage.show();
    }

    private void displayResults(TextArea resultsArea) {
        String formattedDate = mantraData.getTargetDate().format(DateTimeFormatter.ofPattern("MM/dd/yyyy"));
        resultsArea.setText("✔ Results from " + formattedDate + ":\n--\n" +
                "Total '" + mantraData.getNameToCount() + "' count: " + mantraData.getTotalNameCount() + "\n" +
                "Total 'Mantra(s)' count: " + mantraData.getTotalMantrasCount() + "\n" +
                "Total 'Fiz' count: " + mantraData.getTotalFizCount() + "\n" +
                "Sum of mantras: " + mantraData.getTotalFizNumbersSum());
        resultsArea.setStyle("-fx-text-fill: black;");
    }

    private void displayMismatchedLines(List<String> mismatchedLines) {
        mismatchesContainer.getChildren().clear();
        if (mismatchedLines.isEmpty()) {
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
                TextField editableField = new TextField(editablePart);
                HBox lineContainer = new HBox(5, protectedLabel, editableField);
                lineContainer.setAlignment(Pos.CENTER_LEFT);
                mismatchesContainer.getChildren().add(lineContainer);
            } else {
                TextField fullLineField = new TextField(line);
                fullLineField.setPrefWidth(500);
                mismatchesContainer.getChildren().add(fullLineField);
            }
        }
    }

    private Map<String, String> extractUpdatedContentFromUI() {
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
            Label protectedLabel = (Label) lineContainer.getChildren().get(0);
            TextField editableField = (TextField) lineContainer.getChildren().get(1);
            return protectedLabel.getText() + editableField.getText();
        } else if (node instanceof TextField fullLineField) {
            return fullLineField.getText();
        }
        return null;
    }

    private int updateFileContent(Map<String, String> updatedMismatchMap) {
        int updateCount = 0;
        List<String> originalLines = mantraData.getLines();
        List<String> updatedLines = new ArrayList<>(originalLines);

        for (Map.Entry<String, String> entry : updatedMismatchMap.entrySet()) {
            String originalLine = entry.getKey();
            String updatedLine = entry.getValue();
            for (int i = 0; i < originalLines.size(); i++) {
                if (originalLines.get(i).equals(originalLine)) {
                    updatedLines.set(i, updatedLine);
                    updateCount++;
                }
            }
        }

        mantraData.setLines(updatedLines);
        return updateCount;
    }

    private void resetSearchState() {
        if (searchField != null) {
            searchField.clear();
            lastSearchQuery = "";
            currentSearchIndex = -1;
            searchMatches.clear();
        }
    }
}
