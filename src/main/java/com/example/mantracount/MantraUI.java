package com.example.mantracount;

import javafx.application.Application;
import javafx.stage.Stage;
import javafx.stage.FileChooser;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.geometry.Insets;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class MantraUI extends Application {

    private java.io.File lastDirectory = new java.io.File(System.getProperty("user.home"));
    private String originalFilePath;
    private List<String> originalLines;
    private List<String> mismatchedLines;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        System.out.println("Starting MantraUI application...");

        try {
            primaryStage.setTitle("MantraCount");

            // Input fields
            TextField dateField = new TextField();
            setPlaceholder(dateField, "Enter start date (MM/DD/YY or MM/DD/YYYY)");

            TextField mantraField = new TextField();
            setPlaceholder(mantraField, "Enter mantra name");

            TextField pathField = new TextField();
            setPlaceholder(pathField, "Select a file...");
            pathField.setPrefWidth(400);

            Button openFileButton = new Button("Open File");
            TextArea resultsArea = new TextArea();
            resultsArea.setEditable(false);
            resultsArea.setWrapText(true);
            resultsArea.setPromptText("Mantra Count");

            Button processButton = new Button("Count Mantras");
            Label editInstructions = new Label("You can edit mismatches below:");
            TextArea mismatchesArea = new TextArea();
            mismatchesArea.setEditable(true);
            mismatchesArea.setWrapText(true);
            mismatchesArea.setPromptText("Mismatches");
            Button saveButton = new Button("Save Changes");

            openFileButton.setOnAction(event -> {
                FileChooser fileChooser = new FileChooser();
                fileChooser.setTitle("Select a Text or Zip File");
                fileChooser.getExtensionFilters().addAll(
                        new FileChooser.ExtensionFilter("Text Files and Zip Files", "*.txt", "*.zip"),
                        new FileChooser.ExtensionFilter("All Files", "*.*")
                );
                fileChooser.setInitialDirectory(lastDirectory);

                File selectedFile = fileChooser.showOpenDialog(primaryStage);
                if (selectedFile != null) {
                    try {
                        if (selectedFile.getName().toLowerCase().endsWith(".zip")) {
                            // If it's a .zip, extract the first .txt file inside
                            selectedFile = extractFirstTxt(selectedFile);
                        }

                        pathField.setText(selectedFile.getAbsolutePath());
                        pathField.setStyle("-fx-text-fill: black;");
                        lastDirectory = selectedFile.getParentFile();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        Alert errorAlert = new Alert(Alert.AlertType.ERROR, "Failed to open or extract file: " + ex.getMessage());
                        errorAlert.showAndWait();
                    }
                }
            });


            processButton.setOnAction(e -> {
                try {
                    String inputDate = dateField.getText().trim();
                    String mantraKeyword = mantraField.getText().trim();
                    String filePath = pathField.getText().trim();

                    if (inputDate.isEmpty() || mantraKeyword.isEmpty() || filePath.isEmpty()
                            || inputDate.startsWith("Enter") || mantraKeyword.startsWith("Enter") || filePath.startsWith("Select")) {
                        Alert warning = new Alert(Alert.AlertType.WARNING, "Please fill all fields before processing.");
                        warning.showAndWait();
                        return;
                    }

                    originalFilePath = filePath;
                    originalLines = Files.readAllLines(Paths.get(filePath), StandardCharsets.UTF_8);

                    DateTimeFormatter formatter;
                    if (inputDate.length() == 8) {
                        formatter = DateTimeFormatter.ofPattern("M/d/yy");
                    } else {
                        formatter = DateTimeFormatter.ofPattern("M/d/yyyy");
                    }
                    LocalDate parsedDate = LocalDate.parse(inputDate, formatter);

                    int totalMantraCount = 0;
                    int totalFizCount = 0;
                    int totalMantrasWordCount = 0;
                    int totalFizNumbersSum = 0;
                    mismatchedLines = new ArrayList<>();

                    for (String line : originalLines) {
                        line = line.trim();
                        if (line.isEmpty()) continue;

                        LocalDate lineDate = null;
                        try {
                            String[] parts = line.split(",", 2);
                            if (parts.length > 0) {
                                String datePart = parts[0].replace("[", "").replace("]", "").trim();
                                if (datePart.length() == 8) {
                                    lineDate = LocalDate.parse(datePart, DateTimeFormatter.ofPattern("M/d/yy"));
                                } else {
                                    lineDate = LocalDate.parse(datePart, DateTimeFormatter.ofPattern("M/d/yyyy"));
                                }
                            }
                        } catch (Exception ex) {
                            // Ignore parse errors
                        }

                        if (lineDate != null && lineDate.isBefore(parsedDate)) {
                            continue;
                        }

                        if (MantraCount.hasApproximateMatch(line, mantraKeyword)) {
                            int mantraCountInLine = MantraCount.countOccurrences(line, mantraKeyword);
                            int fizCountInLine = MantraCount.countOccurrences(line, "fiz");
                            int mantrasWordCountInLine = MantraCount.countOccurrences(line, "mantras");

                            totalMantraCount += mantraCountInLine;
                            totalFizCount += fizCountInLine;
                            totalMantrasWordCount += mantrasWordCountInLine;

                            int fizNumber = MantraCount.extractNumberAfterThirdColon(line);
                            if (fizNumber != -1) {
                                totalFizNumbersSum += fizNumber;
                            }

                            if (fizCountInLine != mantraCountInLine || mantrasWordCountInLine != mantraCountInLine) {
                                mismatchedLines.add(line);
                            }
                        }
                    }

                    String formattedStartDate = parsedDate.format(inputDate.length() == 8
                            ? DateTimeFormatter.ofPattern("MM/dd/yy")
                            : DateTimeFormatter.ofPattern("MM/dd/yyyy"));

                    StringBuilder resultBuilder = new StringBuilder();
                    resultBuilder.append("✔ Results Starting from ").append(formattedStartDate).append(":\n\n");
                    resultBuilder.append("Total ").append(mantraKeyword).append(" count: ").append(totalMantraCount).append("\n");
                    resultBuilder.append("Total 'Fiz' count: ").append(totalFizCount).append("\n");
                    resultBuilder.append("Total 'Mantras' count: ").append(totalMantrasWordCount).append("\n");
                    resultBuilder.append("Sum of mantras: ").append(totalFizNumbersSum).append("\n");

                    resultsArea.setText(resultBuilder.toString());

                    if (!mismatchedLines.isEmpty()) {
                        mismatchesArea.setText(String.join("\n", mismatchedLines));
                    } else {
                        mismatchesArea.setText("No mismatches found.");
                    }

                } catch (Exception ex) {
                    ex.printStackTrace();
                    Alert errorAlert = new Alert(Alert.AlertType.ERROR, "An error occurred: " + ex.getMessage());
                    errorAlert.showAndWait();
                }
            });

            saveButton.setOnAction(e -> {
                try {
                    if (originalFilePath == null || originalLines == null || mismatchedLines == null) {
                        Alert errorAlert = new Alert(Alert.AlertType.ERROR, "No file loaded or processed yet.");
                        errorAlert.showAndWait();
                        return;
                    }

                    List<String> editedMismatches = Arrays.asList(mismatchesArea.getText().split("\\n"));

                    List<String> updatedLines = new ArrayList<>();

                    for (String line : originalLines) {
                        String trimmedLine = line.trim();
                        if (!mismatchedLines.contains(trimmedLine)) {
                            updatedLines.add(line); // keep only good lines
                        }
                    }

                    updatedLines.addAll(editedMismatches); // add all new edits

                    Files.write(Paths.get(originalFilePath), updatedLines, StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING);

                    Alert successAlert = new Alert(Alert.AlertType.INFORMATION, "✔ Changes saved successfully!");
                    successAlert.showAndWait();

                } catch (Exception ex) {
                    ex.printStackTrace();
                    Alert errorAlert = new Alert(Alert.AlertType.ERROR, "Failed to save changes: " + ex.getMessage());
                    errorAlert.showAndWait();
                }
            });

            VBox vbox = new VBox(10,
                    dateField,
                    mantraField,
                    new HBox(10, pathField, openFileButton),
                    resultsArea,
                    processButton,
                    editInstructions,
                    mismatchesArea,
                    saveButton
            );
            vbox.setPadding(new Insets(20));

            Scene scene = new Scene(vbox, 700, 650);
            primaryStage.setScene(scene);
            primaryStage.show();
            System.out.println("Stage shown successfully!");

        } catch (Exception ex) {
            System.err.println("[ERROR] Exception caught during start(Stage stage):");
            ex.printStackTrace();
        }
    }

    private void setPlaceholder(TextField field, String placeholder) {
        field.setText(placeholder);
        field.setStyle("-fx-text-fill: gray;");

        field.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                if (field.getText().equals(placeholder)) {
                    field.clear();
                    field.setStyle("-fx-text-fill: black;");
                }
            } else {
                if (field.getText().isEmpty()) {
                    field.setText(placeholder);
                    field.setStyle("-fx-text-fill: gray;");
                }
            }
        });
    }
    private File extractFirstTxt(File zipFile) throws Exception {
        Path tempDir = Files.createTempDirectory("unzipped_chat");

        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String entryName = entry.getName().toLowerCase();
                if (!entry.isDirectory() && entryName.endsWith(".txt")) {
                    Path extractedFilePath = tempDir.resolve(Paths.get(entry.getName()).getFileName());
                    Files.copy(zis, extractedFilePath, StandardCopyOption.REPLACE_EXISTING);
                    return extractedFilePath.toFile();
                }
            }
        }

        throw new FileNotFoundException("No .txt file found inside the zip archive.");
    }

}
