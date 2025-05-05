package com.example.mantracount;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class UpdateChecker {

    private static final String CURRENT_VERSION = "3.1.0";
    private static final String GITHUB_RELEASES_API = "https://api.github.com/repos/TashiRabten/MantraCountUI/releases";

    public static void checkForUpdates() {
        new Thread(() -> {
            try {
                URL url = new URL(GITHUB_RELEASES_API);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Accept", "application/vnd.github+json");

                if (conn.getResponseCode() == 200) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String inputLine;
                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }
                    in.close();

                    JSONArray releases = new JSONArray(response.toString());
                    if (releases.length() > 0) {
                        JSONObject latest = releases.getJSONObject(0);
                        String latestVersion = latest.getString("tag_name").replace("v", "");

                        if (isNewerVersion(latestVersion, CURRENT_VERSION)) {
                            String downloadUrl = latest.getString("html_url");
                            Platform.runLater(() -> showUpdateAlert(latestVersion, downloadUrl));
                        }
                    }
                }
                conn.disconnect();
            } catch (Exception e) {
                System.err.println("Update check failed: " + e.getMessage());
            }
        }).start();
    }

    private static boolean isNewerVersion(String latest, String current) {
        String[] latestParts = latest.split("\\.");
        String[] currentParts = current.split("\\.");
        int length = Math.max(latestParts.length, currentParts.length);

        for (int i = 0; i < length; i++) {
            int latestNum = i < latestParts.length ? Integer.parseInt(latestParts[i]) : 0;
            int currentNum = i < currentParts.length ? Integer.parseInt(currentParts[i]) : 0;
            if (latestNum > currentNum) return true;
            if (latestNum < currentNum) return false;
        }
        return false;
    }

    private static void showUpdateAlert(String latestVersion, String url) {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle("Update Available / Atualização Disponível");
        alert.setHeaderText("A new version is available! / Uma nova versão está disponível!");
        alert.setContentText("Latest version: v" + latestVersion +
                "\nYou are using: v" + CURRENT_VERSION +
                "\n\nVisit:\n" + url);
        alert.showAndWait();
    }
}
