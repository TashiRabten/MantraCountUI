package com.example.mantracount;

import javafx.scene.control.Alert;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class FileEditSaver {

    public static List<String> applyEdits(List<String> originalLines,
                                          Map<String, String> edits) {
        List<String> updatedLines = new ArrayList<>(originalLines);

        for (Map.Entry<String, String> entry : edits.entrySet()) {
            String original = entry.getKey().trim();
            String edited = entry.getValue();

            for (int i = 0; i < updatedLines.size(); i++) {
                if (updatedLines.get(i).trim().equals(original)) {
                    updatedLines.set(i, edited);
                    break;
                }
            }
        }

        return updatedLines;
    }

    public static void saveEdits(MantraData mantraData, List<String> updatedLines, Runnable onSuccess) {
        try {
            if (mantraData.isFromZip()) {
                new FileEditSaver().updateZipFile(mantraData.getOriginalZipPath(),
                        mantraData.getFilePath(),
                        updatedLines);
            } else {
                saveToFile(updatedLines, mantraData.getFilePath());
            }
            onSuccess.run();
        } catch (IOException e) {
            e.printStackTrace();
            showError("\u274C Error saving file\n\u274C Erro ao salvar arquivo: " + e.getMessage());
        }
    }

    public static void saveToFile(List<String> lines, String filePath) throws IOException {
        Files.write(Paths.get(filePath), lines, StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING);
    }

    public static void updateZipFile(String zipPath, String extractedFilePath, List<String> updatedContent) throws IOException {
        Path tempZipPath = Files.createTempFile("updated", ".zip");
        String entryName = Paths.get(extractedFilePath).getFileName().toString();

        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipPath));
             ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(tempZipPath))) {

            ZipEntry entry;
            byte[] buffer = new byte[1024];

            while ((entry = zis.getNextEntry()) != null) {
                ZipEntry newEntry = new ZipEntry(entry.getName());
                zos.putNextEntry(newEntry);

                if (entry.getName().equals(entryName)) {
                    byte[] updatedBytes = String.join("\n", updatedContent).getBytes(StandardCharsets.UTF_8);
                    zos.write(updatedBytes);
                } else {
                    int len;
                    while ((len = zis.read(buffer)) > 0) {
                        zos.write(buffer, 0, len);
                    }
                }
                zos.closeEntry();
            }
        }

        Files.move(tempZipPath, Paths.get(zipPath), StandardCopyOption.REPLACE_EXISTING);
    }

    private static void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText("Error");
        alert.setContentText(message);
        alert.showAndWait();
    }
}