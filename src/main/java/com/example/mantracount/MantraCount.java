/*
package com.example.mantracount;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class MantraCount extends Application {
    private static final String APP_TITLE = "Mantra Count v2.0";

    private TextField dateField;
    private TextField mantraKeywordField;
    private TextField filePathField;
    private Button analyzeButton;
    private Button browseButton;
    private Button missingDaysButton;
    private Label statusLabel;
    private ProgressIndicator progressIndicator;
    private TextArea resultTextArea;
    private CheckBox showAllDatesCheckBox;
    private MantraData mantraData;

    @Override
    public void start(Stage primaryStage) {
        mantraData = new MantraData();

        primaryStage.setTitle(APP_TITLE);

        dateField = new TextField(LineParser.formatDate(LocalDate.now()));
        mantraKeywordField = new TextField("mantra");
        filePathField = new TextField();
        analyzeButton = new Button("Analyze / Analisar");
        browseButton = new Button("Browse / Navegar");
        missingDaysButton = new Button("Missing Days / Dias Faltantes");
        statusLabel = new Label("Ready / Pronto");
        progressIndicator = new ProgressIndicator(0);
        resultTextArea = new TextArea();
        showAllDatesCheckBox = new CheckBox("Show All Dates / Mostrar Todas as Datas");

        progressIndicator.setMaxSize(20, 20);
        progressIndicator.setVisible(false);
        missingDaysButton.setDisable(true);
        missingDaysButton.setStyle("-fx-base: #e0e0e0;");
        analyzeButton.setStyle("-fx-base: #4CAF50; -fx-text-fill: white;");
        browseButton.setStyle("-fx-base: #2196F3; -fx-text-fill: white;");

        resultTextArea.setEditable(false);
        resultTextArea.setWrapText(true);

        VBox root = new VBox(10);
        root.setPadding(new Insets(20));

        Label dateLabel = new Label("Start Date / Data Inicial:");
        Label mantraKeywordLabel = new Label("Keyword / Palavra-chave:");
        Label filePathLabel = new Label("File Path / Caminho do Arquivo:");

        GridPane formGrid = new GridPane();
        formGrid.setHgap(10);
        formGrid.setVgap(10);

        formGrid.add(dateLabel, 0, 0);
        formGrid.add(dateField, 1, 0);
        formGrid.add(mantraKeywordLabel, 0, 1);
        formGrid.add(mantraKeywordField, 1, 1);
        formGrid.add(filePathLabel, 0, 2);

        HBox filePathBox = new HBox(10, filePathField, browseButton);
        HBox.setHgrow(filePathField, Priority.ALWAYS);
        formGrid.add(filePathBox, 1, 2);

        HBox actionBox = new HBox(10, analyzeButton, showAllDatesCheckBox, missingDaysButton, progressIndicator, statusLabel);
        actionBox.setAlignment(Pos.CENTER_LEFT);

        root.getChildren().addAll(formGrid, actionBox, resultTextArea);
        VBox.setVgrow(resultTextArea, Priority.ALWAYS);

        browseButton.setOnAction(e -> browseFile(primaryStage));
        analyzeButton.setOnAction(e -> handleAnalyze());
        missingDaysButton.setOnAction(e -> new MissingDaysUI().show(primaryStage, mantraData));

        Scene scene = new Scene(root, 800, 700);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void browseFile(Stage stage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select File / Selecionar Arquivo");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Text Files", "*.txt"),
                new FileChooser.ExtensionFilter("Zip Files", "*.zip"),
                new FileChooser.ExtensionFilter("All Files", "*.*")
        );
        File file = fileChooser.showOpenDialog(stage);
        if (file != null) {
            filePathField.setText(file.getAbsolutePath());
        }
    }

    private void handleAnalyze() {
        missingDaysButton.setDisable(true);
        missingDaysButton.setStyle("-fx-base: #e0e0e0;");
        progressIndicator.setVisible(true);
        statusLabel.setText("Processing... / Processando...");

        String dateText = dateField.getText().trim();
        String mantraKeyword = mantraKeywordField.getText().trim();
        String filePath = filePathField.getText().trim();

        ValidationResult result = InputValidator.validateInputs(dateText, mantraKeyword, filePath);
        if (!result.isValid()) {
            updateStatus(result.getErrorMessage(), true);
            return;
        }

        LocalDate parsedDate = DateParser.parseDate(dateText);
        mantraData.setTargetDate(parsedDate);
        mantraData.setNameToCount(mantraKeyword);
        mantraData.setFilePath(filePath);
        mantraData.setFromZip(filePath.toLowerCase().endsWith(".zip"));

        boolean showAllDates = showAllDatesCheckBox.isSelected();

        CompletableFuture
                .runAsync(() -> {
                    try {
                        FileProcessorService.processFile(mantraData); // only uses mantraData
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .thenRunAsync(() -> {
                    UIUtils.displayAnalysisResults(mantraData, resultTextArea);
                    checkForMissingDays();
                }, Platform::runLater)
                .exceptionally(ex -> {
                    Platform.runLater(() -> updateStatus("Error: " + ex.getMessage(), true));
                    return null;
                });
    }

    private void checkForMissingDays() {
        if (mantraData.getTargetDate() == null || mantraData.getLines() == null || mantraData.getLines().isEmpty()) {
            missingDaysButton.setDisable(true);
            missingDaysButton.setStyle("-fx-base: #e0e0e0;");
            return;
        }

        CompletableFuture.supplyAsync(() -> {
            try {
                List<MissingDaysDetector.MissingDayInfo> missingDays = MissingDaysDetector.detectMissingDays(
                        mantraData.getLines(), mantraData.getTargetDate(), mantraData.getNameToCount()
                );
                return !missingDays.isEmpty();
            } catch (Exception ex) {
                return false;
            }
        }).thenAcceptAsync(hasMissingDays -> {
            missingDaysButton.setDisable(false);
            if (hasMissingDays) {
                missingDaysButton.setStyle("-fx-base: #2196F3; -fx-text-fill: white;");
            } else {
                missingDaysButton.setStyle("-fx-base: #e0e0e0;");
            }
        }, Platform::runLater);
    }

    private void updateStatus(String message, boolean isError) {
        statusLabel.setText(message);
        statusLabel.setTextFill(isError ? Color.RED : Color.BLACK);
        progressIndicator.setVisible(false);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
*/
