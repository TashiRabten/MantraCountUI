package com.example.mantracount;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

public class FileManagementController {

    private final Stage primaryStage;
    private final TextField pathField;
    private final Button openFileButton;
    private final HBox fileControlContainer;
    private final MantraData mantraData;
    private final VBox mismatchesContainer;
    private final Label placeholder;
    private final TextArea resultsArea;

    public FileManagementController(Stage primaryStage, MantraData mantraData,
                                    VBox mismatchesContainer, Label placeholder, TextArea resultsArea) {
        this.primaryStage = primaryStage;
        this.mantraData = mantraData;
        this.mismatchesContainer = mismatchesContainer;
        this.placeholder = placeholder;
        this.resultsArea = resultsArea;

        // Create the path field with proper styling - NO PLACEHOLDER initially
        this.pathField = UIComponentFactory.TextFields.createFilePathField();
        this.pathField.setPrefWidth(400);


        this.openFileButton = UIComponentFactory.ActionButtons.createOpenFileButton();
        this.openFileButton.setOnAction(event -> openFile());

        this.fileControlContainer = new HBox(10, pathField, openFileButton);
        this.fileControlContainer.setAlignment(Pos.CENTER);
        HBox.setHgrow(pathField, Priority.ALWAYS);
    }

    public HBox getFileControlContainer() {
        return fileControlContainer;
    }

    public TextField getPathField() {
        return pathField;
    }

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
            pathField.setStyle(UIColorScheme.getInputFieldStyle()); // Ensure proper styling after text is set
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

    public boolean validateFilePath() {
        String text = pathField.getText();
        if (text == null || text.trim().isEmpty()) {
            UIUtils.showError("Missing or invalid field: \nPlease, open the file",
                    "Campo ausente ou inválido:\nPor favor, abra o Arquivo");
            return false;
        }
        return true;
    }

    public boolean ensureFileLoaded() {
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

                    DateParser.resetDetectedFormat();

                    mantraData.setFromZip(isZipFile);

                    if (isZipFile) {
                        mantraData.setOriginalZipPath(file.getAbsolutePath());
                        try {
                            FileLoader.ExtractedFileInfo extractInfo = FileLoader.extractFirstTxtFromZip(file);
                            File extractedFile = extractInfo.getExtractedFile();
                            String originalEntryName = extractInfo.getOriginalEntryName();

                            mantraData.setOriginalZipEntryName(originalEntryName);

                            System.out.println("Extracted file: " + extractedFile.getAbsolutePath());
                            System.out.println("Original ZIP entry: " + originalEntryName);

                            List<String> lines = FileLoader.robustReadLines(extractedFile.toPath());
                            mantraData.setLines(lines);
                            mantraData.setFilePath(extractedFile.getAbsolutePath());

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