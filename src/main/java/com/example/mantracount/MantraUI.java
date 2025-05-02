package com.example.mantracount;
import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.stage.FileChooser;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Priority;
import javafx.geometry.Insets;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class MantraUI extends Application {
    private boolean isFromZip = false;
    private String originalZipPath;
    private File lastDirectory = new File(System.getProperty("user.home"));
    private String originalFilePath;
    private List<String> originalLines;
    private List<String> mismatchedLines;
    private VBox mismatchesContainer;
    private final Label placeholder = new Label("Mismatch Line\n(Discrep\u00e2ncia de linhas)");

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {

        primaryStage.setTitle("MantraCount");

        // Load icon using resource stream
        Image icon = new Image(getClass().getResourceAsStream("/icons/BUDA.jpg"));
        primaryStage.getIcons().add(icon);

        StackPane root = new StackPane(); // Your actual UI root
        root.setStyle("-fx-background-color: #ffffff;");
        primaryStage.setScene(new Scene(root, 300, 250));
        primaryStage.show();

        TextField dateField = new TextField();
        setPlaceholder(dateField, "Enter start date (MM/DD/YY or MM/DD/YYYY) (Colocar Data Inicial (Mês/Dia/Ano))");

        TextField mantraField = new TextField();
        setPlaceholder(mantraField, "Enter mantra name (Colocar nome do Mantra)");

        TextField pathField = new TextField();
        setPlaceholder(pathField, "Open a file... (Abrir Arquivo...)");
        pathField.setPrefWidth(400);

        Button openFileButton = new Button("Open File");

        TextArea resultsArea = new TextArea();
        resultsArea.setText("Count Mantras\n(Contar Mantras)");
        resultsArea.setStyle("-fx-text-fill: gray;-fx-font-style: italic;");
        resultsArea.setPrefRowCount(6);
        resultsArea.setMinHeight(114);
        resultsArea.setMaxHeight(114);
        VBox.setVgrow(resultsArea, Priority.NEVER);
        resultsArea.setEditable(false);
        resultsArea.setWrapText(true);

        Button processButton = new Button("Count Mantras");
        processButton.setPrefHeight(20);
        processButton.setStyle("-fx-base: #4CAF50; -fx-text-fill: white;");
        // Create a new button for clearing results

        Button clearResultsButton = new Button("Clear Results");
        clearResultsButton.setStyle("-fx-base: #F44336; -fx-text-fill: white;");

        HBox processButtonBox = new HBox(10, processButton, clearResultsButton);
        processButtonBox.setAlignment(Pos.CENTER_LEFT);

        // Set up the event handler
        clearResultsButton.setOnAction(e -> {
            resultsArea.setText("Count Mantras\n(Contar Mantras)");
            resultsArea.setStyle("-fx-text-fill: gray;-fx-font-style: italic;");
            mismatchesContainer.getChildren().clear();
            mismatchesContainer.getChildren().add(placeholder);
        });

        Label editInstructions = new Label("You can edit mismatches below (only text after the colon):");
        editInstructions.setStyle("-fx-font-weight: bold;");

        mismatchesContainer = new VBox(5);
        mismatchesContainer.setPadding(new Insets(5));

        ScrollPane mismatchesScrollPane = new ScrollPane(mismatchesContainer);
        mismatchesScrollPane.setFitToWidth(true);
        mismatchesScrollPane.setPrefHeight(240);
        mismatchesScrollPane.setMaxHeight(240);   // Also set a maximum height
        mismatchesScrollPane.setStyle("-fx-border-color: #0078D7; -fx-border-width: 2px;");

        placeholder.setStyle("-fx-text-fill: gray; -fx-font-style: italic;");
        mismatchesContainer.getChildren().add(placeholder);

        Label helpText = new Label("\u26a0 Note: You cannot modify or delete the date or name parts.\n\u26a0 Nota: N\u00e3o \u00e9 poss\u00edvel modificar ou excluir a data ou nome.");
        helpText.setStyle("-fx-text-fill: #707070;");

        Button saveButton = new Button("Save Changes");
        saveButton.setStyle("-fx-base: #4CAF50; -fx-text-fill: white;");

        Button cancelButton = new Button("Cancel Changes");
        cancelButton.setStyle("-fx-base: #F44336; -fx-text-fill: white;");

        HBox buttonBox = new HBox(10, saveButton, cancelButton);
        buttonBox.setAlignment(Pos.CENTER_LEFT);
        VBox.setMargin(buttonBox, new Insets(5, 0, 0, 0));  // Add margin to ensure spacing

        openFileButton.setOnAction(event -> openFile(primaryStage, pathField, resultsArea));
        processButton.setOnAction(e -> processFile(dateField, mantraField, pathField, resultsArea));
        saveButton.setOnAction(e -> saveChanges());
        cancelButton.setOnAction(e -> cancelChanges());

        VBox vbox = new VBox(10,
                dateField,
                mantraField,
                new HBox(10, pathField, openFileButton),
                resultsArea,
                processButtonBox,    // Use the HBox with both buttons
                editInstructions,
                mismatchesScrollPane,
                helpText,
                buttonBox
        );

        vbox.setPadding(new Insets(20));

        Scene scene = new Scene(vbox, 700, 650);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void clearMismatchDisplay() {
        mismatchesContainer.getChildren().clear();
        mismatchesContainer.getChildren().add(placeholder);
    }

    private void displayResults(FileProcessorService.ProcessResult result,
                                LocalDate parsedDate, String inputDate, String mantraKeyword,
                                TextArea resultsArea) {
        String formattedStartDate = parsedDate.format(
                (inputDate.length() == 8) ?
                        DateTimeFormatter.ofPattern("MM/dd/yy") :
                        DateTimeFormatter.ofPattern("MM/dd/yyyy")
        );

        resultsArea.setText("\u2714 Results from " + formattedStartDate + ":\n\n"
                + "Total '" + mantraKeyword + "' count: " + result.getTotalMantraKeywordCount() + "\n"
                + "Total 'Mantra(s)' count: " + result.getTotalMantraWordsCount() + "\n"
                + "Total 'Fiz' count: " + result.getTotalFizCount() + "\n"
                + "Sum of mantras: " + result.getTotalFizNumbersSum());
        resultsArea.setStyle("-fx-text-fill: black;");
    }

    private void updateZipFile(String zipPath, String extractedFilePath, List<String> updatedContent) {
        try {
            Path tempZipPath = Files.createTempFile("updated", ".zip");
            String entryName = Paths.get(extractedFilePath).getFileName().toString();

            try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipPath));
                 java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(Files.newOutputStream(tempZipPath))) {

                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    ZipEntry newEntry = new ZipEntry(entry.getName());
                    zos.putNextEntry(newEntry);

                    if (entry.getName().equals(entryName)) {
                        byte[] updatedBytes = String.join("\n", updatedContent).getBytes(StandardCharsets.UTF_8);
                        zos.write(updatedBytes);
                    } else {
                        byte[] buffer = new byte[1024];
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            zos.write(buffer, 0, len);
                        }
                    }
                    zos.closeEntry();
                }
            }
            Files.move(tempZipPath, Paths.get(zipPath), StandardCopyOption.REPLACE_EXISTING);

        } catch (Exception ex) {
            ex.printStackTrace();
            showError("❌ Failed to update zip file.\n❌ Falha ao atualizar arquivo zip.");
        }
    }

    private void cancelChanges() {
        if (mismatchedLines != null) {
            displayMismatchedLines(mismatchedLines);
            showInfo("✔ Changes reverted.\n✔ Alterações revertidas.");
        }
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
                HBox.setHgrow(editableField, Priority.ALWAYS);

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

    private void setPlaceholder(TextField field, String placeholder) {
        field.setText(placeholder);
        field.setStyle("-fx-text-fill: gray;");
        field.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal && field.getText().equals(placeholder)) {
                field.clear();
                field.setStyle("-fx-text-fill: black;");
            } else if (!newVal && field.getText().isEmpty()) {
                field.setText(placeholder);
                field.setStyle("-fx-text-fill: gray;");
            }
        });
    }

    private void showError(String message) {
        new Alert(Alert.AlertType.ERROR, message).showAndWait();
    }

    private void showInfo(String message) {
        new Alert(Alert.AlertType.INFORMATION, message).showAndWait();
    }

    private File extractFirstTxt(File zipFile) throws Exception {
        Path tempDir = Files.createTempDirectory("unzipped_chat");
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (!entry.isDirectory() && entry.getName().toLowerCase().endsWith(".txt")) {
                    Path extractedFilePath = tempDir.resolve(Paths.get(entry.getName()).getFileName());
                    Files.copy(zis, extractedFilePath, StandardCopyOption.REPLACE_EXISTING);
                    return extractedFilePath.toFile();
                }
            }
        }
        throw new FileNotFoundException("No .txt found in .zip.\n(Não há .txt no .zip.)");
    }

    private void saveChanges() {
        try {
            if (originalFilePath == null || originalLines == null || mismatchedLines == null) {
                showError("❌ No file loaded.\n❌ Nenhum arquivo carregado.");
                return;
            }

            List<String> updatedLines = new ArrayList<>(originalLines);
            Map<String, String> updatedMismatchMap = new HashMap<>();
            int updateCount = 0;

            for (int i = 0; i < mismatchesContainer.getChildren().size(); i++) {
                if (mismatchesContainer.getChildren().get(i) == placeholder) continue;
                if (i >= mismatchedLines.size()) break;

                String originalLine = mismatchedLines.get(i);
                Node node = mismatchesContainer.getChildren().get(i);

                if (node instanceof HBox lineContainer) {

                    Label protectedLabel = (Label) lineContainer.getChildren().get(0);
                    TextField editableField = (TextField) lineContainer.getChildren().get(1);

                    String updatedLine = protectedLabel.getText() + editableField.getText();
                    updatedMismatchMap.put(originalLine, updatedLine);

                } else if (node instanceof TextField fullLineField) {
                    updatedMismatchMap.put(originalLine, fullLineField.getText());
                }
            }

            for (int j = 0; j < updatedLines.size(); j++) {
                String currentLine = updatedLines.get(j);
                if (updatedMismatchMap.containsKey(currentLine)) {
                    updatedLines.set(j, updatedMismatchMap.get(currentLine));
                    updateCount++;
                }
            }

            Files.write(Paths.get(originalFilePath), updatedLines, StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING);

            if (isFromZip && originalZipPath != null) {
                updateZipFile(originalZipPath, originalFilePath, updatedLines);
            }

            showInfo("✔ Changes saved successfully.\n✔ " + updateCount + " line(s) updated.\n\n✔ Alterações salvas com sucesso.\n✔ " + updateCount + " linha(s) atualizada(s).");

        } catch (Exception ex) {
            ex.printStackTrace();
            showError("❌ Failed to save changes.\n❌ Falha ao salvar alterações.");
        }
    }

    private void processFile(TextField dateField, TextField mantraField, TextField pathField, TextArea resultsArea) {
        try {
            String inputDate = dateField.getText().trim();
            String mantraKeyword = mantraField.getText().trim();
            String filePath = pathField.getText().trim();

            InputValidator.ValidationResult validationResult =
                    InputValidator.validateInputs(inputDate, mantraKeyword, filePath);

            if (!validationResult.isValid()) {
                showError(validationResult.getErrorMessage());
                return;
            }

            LocalDate parsedDate = DateParser.parseDate(inputDate);

            originalFilePath = filePath;
            originalLines = Files.readAllLines(Paths.get(filePath), StandardCharsets.UTF_8);

            FileProcessorService.ProcessResult result =
                    FileProcessorService.processFile(filePath, mantraKeyword, parsedDate);

            displayResults(result, parsedDate, inputDate, mantraKeyword, resultsArea);
            mismatchedLines = result.getMismatchedLines();

            if (!mismatchedLines.isEmpty()) {
                displayMismatchedLines(mismatchedLines);
            } else {
                clearMismatchDisplay();
            }

        } catch (DateTimeParseException e) {
            showError("❌ Invalid date format.\n❌ Formato de data inválido.");
        } catch (Exception ex) {
            ex.printStackTrace();
            showError("❌ Unexpected error: " + ex.getMessage() + "\n❌ Erro inesperado: " + ex.getMessage());
        }
    }

    private void openFile(Stage primaryStage, TextField pathField, TextArea resultsArea) {
        try {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Select a Text or Zip File");
            fileChooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("Text Files and Zip Files", "*.txt", "*.zip"),
                    new FileChooser.ExtensionFilter("All Files", "*.*")
            );
            fileChooser.setInitialDirectory(lastDirectory);

            File selectedFile = fileChooser.showOpenDialog(primaryStage);
            if (selectedFile != null) {
                File fileToUse = selectedFile;

                if (selectedFile.getName().toLowerCase().endsWith(".zip")) {
                    originalZipPath = selectedFile.getAbsolutePath();
                    isFromZip = true;
                    fileToUse = extractFirstTxt(selectedFile);
                } else {
                    isFromZip = false;
                }

                pathField.setText(fileToUse.getAbsolutePath());
                pathField.setStyle("-fx-text-fill: black;");
                lastDirectory = selectedFile.getParentFile();

                resultsArea.setText("Count Mantras\n(Contar Mantras)");
                resultsArea.setStyle("-fx-text-fill: gray;-fx-font-style: italic;");
                mismatchesContainer.getChildren().clear();
                mismatchesContainer.getChildren().add(placeholder);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            showError("\u274c File error.\n\u274c Erro de arquivo.");
        }
    }
}
