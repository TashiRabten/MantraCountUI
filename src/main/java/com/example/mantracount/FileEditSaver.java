package com.example.mantracount;

import javafx.stage.Stage;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class FileEditSaver {
    private final Map<String, String> editedLines = new HashMap<>();  // Maps original lines to their updated content
    private final List<String> removedLines = new ArrayList<>();      // Keeps track of removed lines
    private final Map<String, Integer> linePositions = new HashMap<>(); // Keeps track of line positions (optional, for undo logic)
    static Runnable onSuccess;
    // Save edits to the file
    public static void saveToFile(List<String> lines, String filePath) throws IOException {
        Files.write(Paths.get(filePath), lines);
    }

    // Save edits to a file and handle ZIP extraction
    public static void saveEdits(MantraData mantraData, List<String> updatedLines) throws IOException {
        saveToFile(updatedLines, mantraData.getFilePath());
        onSuccess.run();  // On success, run success logic (like UI notification)
    }

    // Update a .zip file with the new file content
    public static void updateZipFile(String zipFilePath, String txtFilePath, List<String> updatedLines) throws IOException {
        Path tempDir = Files.createTempDirectory("zipUpdate");

        // Extract the original zip and update its content
        try (ZipFile zipFile = new ZipFile(zipFilePath)) {
            ZipEntry txtFileEntry = zipFile.getEntry(new File(txtFilePath).getName());
            Path tempTxtFile = tempDir.resolve(txtFileEntry.getName());

            // Extract the original txt file
            try (InputStream is = zipFile.getInputStream(txtFileEntry);
                 OutputStream os = Files.newOutputStream(tempTxtFile)) {
                byte[] buffer = new byte[1024];
                int length;
                while ((length = is.read(buffer)) != -1) {
                    os.write(buffer, 0, length);
                }
            }

            // Update the content
            saveToFile(updatedLines, tempTxtFile.toString());

            // Now, write the updated file back to the ZIP
            try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFilePath))) {
                // Add other entries from the original zip, including the updated txt file
                for (Enumeration<? extends ZipEntry> entries = zipFile.entries(); entries.hasMoreElements(); ) {
                    ZipEntry entry = entries.nextElement();

                    // If the entry is the txt file, replace it with the updated one
                    if (entry.getName().equals(txtFileEntry.getName())) {
                        zos.putNextEntry(new ZipEntry(txtFileEntry.getName()));
                        Files.copy(tempTxtFile, zos);
                    } else {
                        // Copy other entries unchanged
                        zos.putNextEntry(entry);
                        try (InputStream is = zipFile.getInputStream(entry)) {
                            byte[] buffer = new byte[1024];
                            int length;
                            while ((length = is.read(buffer)) != -1) {
                                zos.write(buffer, 0, length);
                            }
                        }
                    }
                    zos.closeEntry();
                }
            }
        }
    }
    private void saveEdits(MantraData data, Stage dialog) {
        try {
            List<String> lines = new ArrayList<>(data.getLines());

            // Update the lines with the edited content
            for (Map.Entry<String, String> entry : editedLines.entrySet()) {
                for (int i = 0; i < lines.size(); i++) {
                    if (lines.get(i).equals(entry.getKey())) {
                        lines.set(i, entry.getValue());
                        break;
                    }
                }
            }

            // Soft-delete lines visually removed
            lines.removeIf(removedLines::contains);

            // Delegate the saving process to FileEditSaver
            FileEditSaver.saveEdits(data, lines);

            data.setLines(lines);
            UIUtils.showInfo("✔ Changes saved.");
        } catch (Exception e) {
            UIUtils.showError("❌ Failed to save changes: " + e.getMessage());
        }
    }


}



