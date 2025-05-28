package com.example.mantracount;

import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

/**
 * Handles file operations for the Mantra application.
 * This class manages opening, saving, and processing files.
 */
public class FileManagementController {

    private final Stage primaryStage;
    private final TextField pathField;
    private final Button openFileButton;
    private final HBox fileControlContainer;
    private final MantraData mantraData;
    private final VBox mismatchesContainer;
    private final Label placeholder;
    private final TextArea resultsArea;

    /**
     * Creates a new FileManagementController.
     *
     * @param primaryStage The primary stage of the application
     * @param mantraData The data model to update with file contents
     * @param mismatchesContainer The container for mismatched lines
     * @param placeholder The placeholder label for the mismatches container
     * @param resultsArea The text area to display results
     */
    public FileManagementController(Stage primaryStage, MantraData mantraData,
                                    VBox mismatchesContainer, Label placeholder, TextArea resultsArea) {
        this.primaryStage = primaryStage;
        this.mantraData = mantraData;
        this.mismatchesContainer = mismatchesContainer;
        this.placeholder = placeholder;
        this.resultsArea = resultsArea;

        // Initialize file path field with Portuguese placeholder and English tooltip
        pathField = new TextField();
        UIUtils.setPlaceholder(pathField, "Abrir arquivo...");
        pathField.setPrefWidth(400);

        Tooltip fileFieldTooltip = new Tooltip("Open a file - Click to browse and select your journal/diary file");
        fileFieldTooltip.setShowDelay(Duration.millis(300));
        fileFieldTooltip.setHideDelay(Duration.millis(100));
        Tooltip.install(pathField, fileFieldTooltip);

        // Initialize open button with Portuguese text and English tooltip
        openFileButton = UIComponentFactory.ActionButtons.createOpenFileButton();
        openFileButton.setOnAction(event -> openFile());

        Tooltip buttonTooltip = new Tooltip("Open File - Browse and select your journal/diary file");
        buttonTooltip.setShowDelay(Duration.millis(300));
        buttonTooltip.setHideDelay(Duration.millis(100));
        Tooltip.install(openFileButton, buttonTooltip);

        fileControlContainer = new HBox(10, pathField, openFileButton);
        HBox.setHgrow(pathField, Priority.ALWAYS);
    }

    /**
     * Gets the file control UI container.
     * @return The HBox containing the file path field and open button
     */
    public HBox getFileControlContainer() {
        return fileControlContainer;
    }

    /**
     * Gets the file path field.
     * @return The TextField containing the file path
     */
    public TextField getPathField() {
        return pathField;
    }

    /**
     * Opens a file using the file chooser.
     * @return true if a file was successfully opened, false otherwise
     */
    public boolean openFile() {
        try {
            File selectedFile = FileLoader.openFile(
                    primaryStage,
                    pathField,
                    resultsArea,
                    mismatchesContainer,
                    placeholder,
                    new File(System.getProperty("user.home")),
                    mantraData
            );

            if (selectedFile == null) {
                return false;
            }

            pathField.setText(selectedFile.getAbsolutePath());
            mantraData.setFilePath(selectedFile.getAbsolutePath());
            mantraData.setFromZip(selectedFile.getName().toLowerCase().endsWith(".zip"));
            mantraData.setOriginalZipPath(mantraData.isFromZip() ? selectedFile.getAbsolutePath() : null);

            mismatchesContainer.getChildren().clear();
            mismatchesContainer.getChildren().add(placeholder);

            UIUtils.showInfo("✔ File loaded. \n✔ Arquivo carregado.");
            return true;

        } catch (Exception ex) {
            ex.printStackTrace();
            UIUtils.showError("❌ Failed to load file." + ex.getMessage() + "\n❌ Falha ao carregar arquivo" + ex.getMessage());
            return false;
        }
    }

    /**
     * Validates the file path field.
     * @return true if valid, false otherwise
     */
    public boolean validateFilePath() {
        if (pathField.getText() == null || pathField.getText().trim().isEmpty() ||
                pathField.getText().equals("Abrir arquivo...")) {
            UIUtils.showError("Missing or invalid field: \nPlease, open the file",
                    "Campo ausente ou inválido:\nPor favor, abra o Arquivo");
            return false;
        }
        return true;
    }

    /**
     * Ensures the file is loaded if a path is specified but not yet loaded.
     * @return true if successful, false otherwise
     */
    public boolean ensureFileLoaded() {
        // If file path is set but lines aren't loaded
        if ((mantraData.getLines() == null || mantraData.getLines().isEmpty()) &&
                pathField.getText() != null && !pathField.getText().trim().isEmpty()) {
            try {
                System.out.println("Ensuring file loaded from path: " + pathField.getText().trim());
                File file = new File(pathField.getText().trim()).getAbsoluteFile();
                System.out.println("Absolute path: " + file.getAbsolutePath());
                System.out.println("File exists: " + file.exists());

                if (file.exists() && file.isFile()) {
                    boolean isZipFile = file.getName().toLowerCase().endsWith(".zip");
                    System.out.println("Is ZIP file: " + isZipFile);

                    // Reset date format detection for new file
                    DateParser.resetDetectedFormat();

                    mantraData.setFromZip(isZipFile);

                    if (isZipFile) {
                        mantraData.setOriginalZipPath(file.getAbsolutePath());
                        try {
                            FileLoader.ExtractedFileInfo extractInfo = FileLoader.extractFirstTxtFromZip(file);
                            File extractedFile = extractInfo.getExtractedFile();
                            String originalEntryName = extractInfo.getOriginalEntryName();

                            // Save the original entry name
                            mantraData.setOriginalZipEntryName(originalEntryName);

                            System.out.println("Extracted file: " + extractedFile.getAbsolutePath());
                            System.out.println("Original ZIP entry: " + originalEntryName);

                            List<String> lines = FileLoader.robustReadLines(extractedFile.toPath());
                            mantraData.setLines(lines);
                            mantraData.setFilePath(extractedFile.getAbsolutePath());

                            // Detect date format from extracted file
                            DateParser.detectDateFormat(lines);

                            System.out.println("Loaded " + lines.size() + " lines from extracted file");

                        } catch (Exception ex) {
                            System.err.println("Error extracting from ZIP: " + ex.getMessage());
                            ex.printStackTrace();
                            UIUtils.showError("❌ Failed to extract from ZIP file. / Falha ao extrair arquivo .zip",
                                    ex.getMessage());
                            return false;
                        }
                    } else {
                        List<String> lines = FileLoader.robustReadLines(file.toPath());
                        mantraData.setLines(lines);
                        mantraData.setFilePath(file.getAbsolutePath());

                        // Detect date format from text file
                        DateParser.detectDateFormat(lines);

                        System.out.println("Loaded " + lines.size() + " lines from text file");
                    }
                    return true;
                } else {
                    System.err.println("File does not exist or is not a file: " + file.getAbsolutePath());
                    UIUtils.showError("❌ File not found / Arquivo não encontrado",
                            "The selected file does not exist. / O arquivo selecionado não existe.");
                    return false;
                }
            } catch (Exception ex) {
                System.err.println("Error loading file: " + ex.getMessage());
                ex.printStackTrace();
                UIUtils.showError("❌ Erro ao carregar arquivo: " + ex.getMessage() + " / ❌ Error loading file: " + ex.getMessage());
                return false;
            }
        }
        return mantraData.getLines() != null && !mantraData.getLines().isEmpty();
    }

    /**
     * Saves changes to the file.
     * @param updatedMismatchMap The map of original to updated mismatched lines
     * @return true if successful, false otherwise
     */
    public boolean saveChanges(Map<String, String> updatedMismatchMap) {
        try {
            if (mantraData.getLines() == null) {
                UIUtils.showError("No data. / Sem dados.",
                        "No file loaded or processed. \nNenhum arquivo carregado ou processado.");
                return false;
            }

            int updateCount = updateFileContent(updatedMismatchMap);
            FileEditSaver.saveToFile(mantraData.getLines(), mantraData.getFilePath());

            if (mantraData.isFromZip()) {
                FileEditSaver.updateZipFile(
                        mantraData.getOriginalZipPath(),
                        mantraData.getFilePath(),
                        mantraData.getLines(),
                        mantraData.getOriginalZipEntryName()
                );
            }
            UIUtils.showInfo("✔ Changes saved successfully. \n✔ Alterações salvas com sucesso.\n" +
                    "✔ " + updateCount + " line(s) updated. \n✔ " + updateCount + " linha(s) atualizada(s).");
            return true;

        } catch (Exception ex) {
            ex.printStackTrace();
            UIUtils.showError("❌ Failed to save changes. \n❌ Falha ao salvar alterações.");
            return false;
        }
    }

    /**
     * Updates the file content with the edited lines.
     * @param updatedMismatchMap The map of original to updated mismatched lines
     * @return The number of lines updated
     */
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
}