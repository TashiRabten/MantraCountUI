package com.example.mantracount;

import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnmappableCharacterException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class FileLoader {

    public static File openFile(Stage stage,
                                TextField pathField,
                                TextArea resultsArea,
                                VBox mismatchesContainer,
                                Label placeholder,
                                File initialDir,
                                MantraData data) throws Exception {

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open Mantra File");
        fileChooser.setInitialDirectory(initialDir);
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("All Supported Files", "*.txt", "*.zip"),
                new FileChooser.ExtensionFilter("Text Files", "*.txt"),
                new FileChooser.ExtensionFilter("Zip Archives", "*.zip")
        );

        File selectedFile = fileChooser.showOpenDialog(stage);
        if (selectedFile == null) return null;

        File fileToRead = selectedFile;
        List<String> lines;

        if (selectedFile.getName().toLowerCase().endsWith(".zip")) {
            File extractedTxt = FileProcessorService.extractFirstTxtFromZip(selectedFile);
            lines = robustReadLines(extractedTxt.toPath());
            data.setFilePath(extractedTxt.getAbsolutePath());
            data.setLines(lines);
            data.setFromZip(true);
            data.setOriginalZipPath(selectedFile.getAbsolutePath());
        } else {
            lines = robustReadLines(selectedFile.toPath());
            data.setFromZip(false);
            data.setOriginalZipPath(null);
        }

        data.setFilePath(fileToRead.getAbsolutePath());
        data.setLines(lines);

        pathField.setText(selectedFile.getAbsolutePath());
        resultsArea.setText("Count Mantras\n(Contar Mantras)");
        resultsArea.setStyle("-fx-text-fill: gray; -fx-font-style: italic;");
        resultsArea.setEditable(false);

        mismatchesContainer.getChildren().clear();
        mismatchesContainer.getChildren().add(placeholder);

        return selectedFile;
    }
    public static List<String> robustReadLines(Path path) throws IOException {
        List<Charset> charsetsToTry = List.of(
                StandardCharsets.UTF_8,
                Charset.forName("windows-1252"),
                Charset.forName("ISO-8859-1")
        );

        for (Charset charset : charsetsToTry) {
            try (BufferedReader reader = Files.newBufferedReader(path, charset)) {
                List<String> lines = new ArrayList<>();
                String line;
                while ((line = reader.readLine()) != null) {
                    lines.add(line);
                }
                System.out.println("✅ Successfully read file using charset: " + charset.name());
                return lines;
            } catch (MalformedInputException | UnmappableCharacterException e) {
                System.err.println("❌ Failed with charset: " + charset.name() + " — " + e.getClass().getSimpleName() + ": " + e.getMessage());
            }
        }

        throw new IOException("❌ Failed to read file: no supported charset worked.");
    }

    }
