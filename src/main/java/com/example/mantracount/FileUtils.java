package com.example.mantracount;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Centralized utility class for file operations.
 * Eliminates duplication between FileManagementController and LineEditingUtils.
 */
public final class FileUtils {

    private FileUtils() {
        // Utility class - prevent instantiation
    }

    /**
     * Updates lines in MantraData based on a map of original to updated lines.
     * 
     * @param mantraData The MantraData to update
     * @param originalToUpdated Map of original lines to their updated versions
     * @return Number of lines updated
     */
    public static int updateFileContent(MantraData mantraData, Map<String, String> originalToUpdated) {
        List<String> originalLines = mantraData.getLines();
        List<String> updatedLines = new ArrayList<>(originalLines);
        int updateCount = 0;

        for (Map.Entry<String, String> entry : originalToUpdated.entrySet()) {
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

    /**
     * Saves MantraData to file, handling both regular files and zip files.
     * 
     * @param mantraData The MantraData to save
     * @return true if save was successful, false otherwise
     */
    public static boolean saveToFileWithZipHandling(MantraData mantraData) {
        try {
            // Save to file
            FileEditSaver.saveToFile(mantraData.getLines(), mantraData.getFilePath());

            // Handle zip files
            if (mantraData.isFromZip()) {
                FileEditSaver.updateZipFile(
                        mantraData.getOriginalZipPath(),
                        mantraData.getFilePath(),
                        mantraData.getLines(),
                        mantraData.getOriginalZipEntryName()
                );
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}