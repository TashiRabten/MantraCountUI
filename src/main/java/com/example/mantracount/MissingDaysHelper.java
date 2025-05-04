package com.example.mantracount;

import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.List;

public class MissingDaysHelper {

    // Prepare data for missing days detection
    public static boolean prepareDataForMissingDays(String dateText, String mantraKeyword, String filePath, MantraData mantraData) {
        try {
            if (dateText == null || dateText.isEmpty() || mantraKeyword == null || mantraKeyword.isEmpty() || filePath == null || filePath.isEmpty()) {
                UIUtils.showError("❌ Missing required information.\n❌ Informações necessárias ausentes.");
                return false;
            }

            // Parse the date
            LocalDate targetDate = DateParser.parseDate(dateText);
            mantraData.setTargetDate(targetDate);
            mantraData.setNameToCount(mantraKeyword);

            // Read the file lines
            mantraData.setLines(FileLoader.robustReadLines(Paths.get(filePath)));
            return true;
        } catch (Exception ex) {
            ex.printStackTrace();
            UIUtils.showError("❌ Failed to prepare data for missing days: " + ex.getMessage());
            return false;
        }
    }
}
