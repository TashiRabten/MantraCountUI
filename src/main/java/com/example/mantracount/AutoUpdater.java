package com.example.mantracount;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.json.JSONArray;
import org.json.JSONObject;

import java.awt.Desktop;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AutoUpdater {

    private static final String CURRENT_VERSION = "3.1.0";
    private static final String GITHUB_RELEASES_API = "https://api.github.com/repos/TashiRabten/MantraCountUI/releases";
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();

    public static void checkForUpdates() {
        Task<JSONObject> task = new Task<>() {
            @Override
            protected JSONObject call() throws Exception {
                HttpURLConnection conn = (HttpURLConnection) new URL(GITHUB_RELEASES_API).openConnection();
                conn.setRequestProperty("Accept", "application/vnd.github+json");
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(15000);

                if (conn.getResponseCode() != 200)
                    throw new IOException("HTTP " + conn.getResponseCode());

                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = in.readLine()) != null) sb.append(line);
                in.close();

                JSONArray releases = new JSONArray(sb.toString());
                return releases.length() > 0 ? releases.getJSONObject(0) : null;
            }
        };

        task.setOnSucceeded(e -> {
            JSONObject latest = task.getValue();
            if (latest != null) {
                // Handle both "v." prefix and "v" prefix
                String tagName = latest.getString("tag_name");
                String latestVersion = tagName.startsWith("v.") ? tagName.replace("v.", "") :
                        tagName.startsWith("v") ? tagName.replace("v", "") : tagName;

                if (isNewerVersion(latestVersion, CURRENT_VERSION)) {
                    Platform.runLater(() -> showUpdateDialog(latest));
                }
            }
        });

        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            Platform.runLater(() -> UIUtils.showError(
                    "Update Check Failed: " + ex.getMessage() +"\nVerificação de Atualização Falhou: " + ex.getMessage()
            ));
        });

        executor.submit(task);
    }

    private static void showUpdateDialog(JSONObject release) {
        // Handle both "v." prefix and "v" prefix
        String tagName = release.optString("tag_name");
        String latestVersion = tagName.startsWith("v.") ? tagName.replace("v.", "") :
                tagName.startsWith("v") ? tagName.replace("v", "") : tagName;
        latestVersion = latestVersion.trim();

        String url = findInstallerUrl(release);

        if (latestVersion.isEmpty()) {
            System.err.println("❌ No tag_name found. \n❌ Não encontrou 'tag' de versão.");
            System.err.println("Raw tag_name: " + tagName);
            return;
        }

        if (url == null) {
            UIUtils.showError(
                    "No installer found / Instalador não encontrado",
                    "Release does not contain .exe or .dmg\nLançamento não contém arquivo .exe ou .dmg"
            );
            return;
        }

        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Update Available / Atualização Disponível");

        VBox root = new VBox(10);
        root.setPadding(new Insets(20));
        root.setAlignment(Pos.CENTER);

        Label title = new Label("🔄 A new version (" + latestVersion + ") is available!\n🔄 Uma nova versão (" + latestVersion + ") está disponível!");

        // Use getJSONObject("body") only if it exists
        String releaseNotes = "";
        try {
            releaseNotes = release.getString("body");
        } catch (Exception ex) {
            System.err.println("Failed to get release notes: " + ex.getMessage());
            releaseNotes = "No release notes available / Notas de lançamento não disponíveis";
        }

        TextArea notes = new TextArea(releaseNotes);
        notes.setEditable(false);
        notes.setWrapText(true);
        notes.setPrefHeight(200);

        ProgressBar bar = new ProgressBar(0);
        bar.setVisible(false);
        Label progress = new Label("");
        progress.setVisible(false);

        Button download = new Button("💾 Download & Install / Baixar e Instalar");
        Button cancel = new Button("❌ Cancel / Cancelar");

        HBox buttons = new HBox(10, download, cancel);
        buttons.setAlignment(Pos.CENTER);

        download.setOnAction(e -> {
            bar.setVisible(true);
            progress.setVisible(true);
            download.setDisable(true);

            Task<Void> installTask = new Task<>() {
                @Override
                protected Void call() throws Exception {
                    updateMessage("⬇️ Downloading installer...\nBaixando instalador...");

                    Path tempDir = Files.createTempDirectory("mantra-update");
                    String fileName = url.substring(url.lastIndexOf('/') + 1);
                    Path output = tempDir.resolve(fileName);

                    try (InputStream in = new URL(url).openStream()) {
                        Files.copy(in, output, StandardCopyOption.REPLACE_EXISTING);
                    }

                    updateMessage("🚀 Opening installer...\nAbrindo instalador...");
                    Desktop.getDesktop().open(output.toFile());
                    updateMessage("✅ Installer launched. Close this app to continue.\nInstalador iniciado. Feche este aplicativo para continuar.");
                    return null;
                }
            };

            bar.progressProperty().bind(installTask.progressProperty());
            progress.textProperty().bind(installTask.messageProperty());

            installTask.setOnSucceeded(ev -> stage.close());
            installTask.setOnFailed(ev -> {
                Throwable ex = installTask.getException();
                progress.textProperty().unbind();
                progress.setText("❌ Error: " + ex.getMessage() + "\nErro: " + ex.getMessage());
                download.setDisable(false);
            });

            executor.submit(installTask);
        });

        cancel.setOnAction(e -> stage.close());

        root.getChildren().addAll(title, notes, bar, progress, buttons);
        stage.setScene(new Scene(root, 500, 400));
        stage.getIcons().add(new Image(AutoUpdater.class.getResourceAsStream("/icons/BUDA.jpg")));
        stage.show();
    }

    private static String findInstallerUrl(JSONObject release) {
        JSONArray assets = release.getJSONArray("assets");
        for (int i = 0; i < assets.length(); i++) {
            String name = assets.getJSONObject(i).getString("name").toLowerCase();
            if (name.endsWith(".exe") || name.endsWith(".dmg") || name.endsWith(".pkg")) {
                return assets.getJSONObject(i).getString("browser_download_url");
            }
        }
        return null;
    }

    private static boolean isNewerVersion(String latest, String current) {
        if (latest == null || latest.isBlank() || current == null || current.isBlank()) {
            System.err.println("⚠️ Version string is blank. Skipping update check.\n⚠️ String de versão está em branco. Pulando verificação de atualização.");
            return false;
        }

        String[] lv = latest.split("\\.");
        String[] cv = current.split("\\.");

        try {
            for (int i = 0; i < Math.max(lv.length, cv.length); i++) {
                int l = i < lv.length ? Integer.parseInt(lv[i].trim()) : 0;
                int c = i < cv.length ? Integer.parseInt(cv[i].trim()) : 0;
                if (l > c) return true;
                if (l < c) return false;
            }
        } catch (NumberFormatException e) {
            System.err.println("🚫 Invalid version format: " + latest + " or " + current + "\n🚫 Formato de versão inválido: " + latest + " ou " + current);
            return false;
        }

        return false;
    }

    public static void shutdown() {
        executor.shutdown();
    }
}