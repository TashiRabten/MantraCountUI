package com.example.mantracount;

import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;


public class FileLoader {

    // Opens a file chooser and loads the selected file
    public static File openFile(Stage primaryStage, TextField pathField, TextArea resultsArea, VBox mismatchesContainer, Label placeholder, File defaultDirectory, MantraData mantraData) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Text Files", "*.txt", "*.zip"));
        fileChooser.setInitialDirectory(defaultDirectory);
        File selectedFile = fileChooser.showOpenDialog(primaryStage);

        if (selectedFile != null) {
            try {
                pathField.setText(selectedFile.getAbsolutePath());
                mantraData.setFilePath(selectedFile.getAbsolutePath());
                mantraData.setLines(robustReadLines(selectedFile.toPath()));
                mantraData.setFromZip(selectedFile.getName().toLowerCase().endsWith(".zip"));
                mantraData.setOriginalZipPath(
                        mantraData.isFromZip() ? selectedFile.getAbsolutePath() : null
                );

                resultsArea.setText("Count Mantras\n(Contar Mantras)");
                resultsArea.setStyle("-fx-text-fill: gray; -fx-font-style: italic;");
                resultsArea.setEditable(false);

                mismatchesContainer.getChildren().clear();
                mismatchesContainer.getChildren().add(placeholder);

                return selectedFile;
            } catch (IOException e) {
                e.printStackTrace();
                UIUtils.showError("❌ Error reading file: " + e.getMessage());
                return null;
            }
        }
        return null;
    }

    // Read file lines with robust handling for different encodings
    public static List<String> robustReadLines(Path filePath) throws IOException {
        return Files.readAllLines(filePath, StandardCharsets.UTF_8);
    }

    // Additional helper to extract text from .zip files
    public static File extractFirstTxtFromZip(File zipFile) throws Exception {
        Path tempDir = Files.createTempDirectory("unzipped_chat");
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (!entry.isDirectory() && entry.getName().toLowerCase().endsWith(".txt")) {
                    Path extractedFilePath = tempDir.resolve(Paths.get(entry.getName()).getFileName());
                    Files.copy(zis, extractedFilePath);
                    return extractedFilePath.toFile();
                }
            }
        }
        throw new FileNotFoundException("No .txt found in .zip.\n(Não há .txt no .zip.)");
    }
}
