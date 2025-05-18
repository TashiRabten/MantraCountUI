package com.example.mantracount;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class FileEditSaver {

    public static void saveToFile(List<String> lines, String filePath) throws IOException {
        try {
            // Create backup first
            createBackup(filePath);

            // Write the updated content
            Files.write(Paths.get(filePath), lines, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IOException("Failed to save changes: " + e.getMessage(), e);
        }
    }

    private static void createBackup(String filePath) throws IOException {
        Path path = Paths.get(filePath);
        if (Files.exists(path)) {
            String backupPath = filePath + ".bak";
            Files.copy(path, Paths.get(backupPath), StandardCopyOption.REPLACE_EXISTING);
        }
    }


    public static void updateZipFile(String zipFilePath, String extractedFilePath, List<String> updatedLines, String originalEntryName) throws IOException {
        // Create a temporary file for the new ZIP
        Path tempZipPath = Files.createTempFile("updated_", ".zip");

        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFilePath));
             ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(tempZipPath.toFile()))) {

            ZipEntry entry;
            byte[] buffer = new byte[1024];
            boolean fileFound = false;

            while ((entry = zis.getNextEntry()) != null) {
                // Create a new entry for the output ZIP
                ZipEntry newEntry = new ZipEntry(entry.getName());
                zos.putNextEntry(newEntry);

                // If this is the file we're updating - use exact entry name match
                if (originalEntryName != null && entry.getName().equals(originalEntryName)) {
                    // Write the updated content
                    String content = String.join(System.lineSeparator(), updatedLines);
                    zos.write(content.getBytes(StandardCharsets.UTF_8));
                    fileFound = true;
                } else {
                    // Copy the existing content for other files
                    int len;
                    while ((len = zis.read(buffer)) > 0) {
                        zos.write(buffer, 0, len);
                    }
                }

                zos.closeEntry();
                zis.closeEntry();
            }

            // If we didn't find the file in the ZIP, add it with its original path
            if (!fileFound && originalEntryName != null) {
                ZipEntry newEntry = new ZipEntry(originalEntryName);
                zos.putNextEntry(newEntry);
                String content = String.join(System.lineSeparator(), updatedLines);
                zos.write(content.getBytes(StandardCharsets.UTF_8));
                zos.closeEntry();
            }
        }

        // Create backup of original ZIP
        createBackup(zipFilePath);

        // Replace the original ZIP with our updated one
        Files.move(tempZipPath, Paths.get(zipFilePath), StandardCopyOption.REPLACE_EXISTING);
    }

}