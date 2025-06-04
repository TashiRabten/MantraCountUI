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
    public static File openFile(Stage primaryStage, TextField pathField, TextArea resultsArea,
                                VBox mismatchesContainer, Label placeholder, File defaultDirectory,
                                MantraData mantraData) {
        FileChooser fileChooser = new FileChooser();

        // Create separate extension filters for better visibility
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter(StringConstants.ALL_SUPPORTED_FILES_EN, "*" + StringConstants.TXT_EXTENSION, "*" + StringConstants.ZIP_EXTENSION),
                new FileChooser.ExtensionFilter(StringConstants.TEXT_FILES_EN, "*" + StringConstants.TXT_EXTENSION),
                new FileChooser.ExtensionFilter(StringConstants.ZIP_FILES_EN, "*" + StringConstants.ZIP_EXTENSION)
        );

        // Set initial directory, fallback to user home if not found
        if (defaultDirectory != null && defaultDirectory.exists() && defaultDirectory.isDirectory()) {
            fileChooser.setInitialDirectory(defaultDirectory);
        } else {
            fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));
        }

        File selectedFile = fileChooser.showOpenDialog(primaryStage);

        if (selectedFile != null) {
            try {
                pathField.setText(selectedFile.getAbsolutePath());
                mantraData.setFilePath(selectedFile.getAbsolutePath());

                // Reset date format detection for new file
                DateParser.resetDetectedFormat();

                boolean isZipFile = selectedFile.getName().toLowerCase().endsWith(StringConstants.ZIP_EXTENSION);
                mantraData.setFromZip(isZipFile);
                mantraData.setOriginalZipPath(isZipFile ? selectedFile.getAbsolutePath() : null);

                if (isZipFile) {
                    // If zip file, extract first txt and read lines
                    ExtractedFileInfo extractInfo = extractFirstTxtFromZip(selectedFile);
                    File extractedFile = extractInfo.getExtractedFile();

                    // Store the original ZIP entry name
                    mantraData.setOriginalZipEntryName(extractInfo.getOriginalEntryName());

                    List<String> fileLines = robustReadLines(extractedFile.toPath());
                    mantraData.setLines(fileLines);

                    // Detect date format
                    DateParser.detectDateFormat(fileLines);

                    // Keep original zip path but set file path to extracted file
                    mantraData.setFilePath(extractedFile.getAbsolutePath());
                } else {
                    // If regular txt file, read lines directly
                    List<String> fileLines = robustReadLines(selectedFile.toPath());
                    mantraData.setLines(fileLines);

                    // Detect date format
                    DateParser.detectDateFormat(fileLines);
                }


                mismatchesContainer.getChildren().clear();
                mismatchesContainer.getChildren().add(placeholder);

                return selectedFile;
            } catch (IOException e) {
                e.printStackTrace();
                UIUtils.showError("❌ Error reading file: " + e.getMessage());
                return null;
            } catch (Exception e) {
                e.printStackTrace();
                UIUtils.showError("❌ Error processing file: " + e.getMessage());
                return null;
            }
        }
        return null;
    }

    // Read file lines with robust handling for different encodings
    public static List<String> robustReadLines(Path filePath) throws IOException {
        try {
            return Files.readAllLines(filePath, StandardCharsets.UTF_8);
        } catch (IOException e) {
            // Try with different encoding if UTF-8 fails
            try {
                return Files.readAllLines(filePath, StandardCharsets.ISO_8859_1);
            } catch (IOException e2) {
                throw new IOException("Failed to read file with UTF-8 and ISO-8859-1 encodings", e);
            }
        }
    }

    // Extract text from .zip files
    public static ExtractedFileInfo extractFirstTxtFromZip(File zipFile) throws Exception {
        Path tempDir = Files.createTempDirectory("mantracount_temp");
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String entryName = entry.getName().toLowerCase();
                if (!entry.isDirectory() && entryName.endsWith(StringConstants.TXT_EXTENSION)) {
                    // Save the original entry name with original case
                    String originalEntryName = entry.getName();

                    // Get just the filename part
                    String fileName = Paths.get(entry.getName()).getFileName().toString();
                    Path extractedFilePath = tempDir.resolve(fileName);

                    // Copy the file content
                    Files.copy(zis, extractedFilePath);

                    // Add shutdown hook to clean up temp files
                    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                        try {
                            Files.deleteIfExists(extractedFilePath);
                            Files.deleteIfExists(tempDir);
                        } catch (IOException e) {
                            // Silent cleanup
                        }
                    }));

                    return new ExtractedFileInfo(extractedFilePath.toFile(), originalEntryName);
                }
            }
        }
        throw new FileNotFoundException("No .txt file found in the zip archive.\n(Não há arquivo .txt no arquivo zip.)");
    }
    public static class ExtractedFileInfo {
        private final File extractedFile;
        private final String originalEntryName;

        public ExtractedFileInfo(File extractedFile, String originalEntryName) {
            this.extractedFile = extractedFile;
            this.originalEntryName = originalEntryName;
        }

        public File getExtractedFile() { return extractedFile; }
        public String getOriginalEntryName() { return originalEntryName; }
    }


}