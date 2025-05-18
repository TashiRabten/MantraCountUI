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
import java.net.URI;
import java.net.URL;
import java.nio.file.*;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AutoUpdater {
    private static boolean manualCheck = false;
    private static final String CURRENT_VERSION = getCurrentVersion();
    private static final String GITHUB_RELEASES_API = "https://api.github.com/repos/TashiRabten/MantraCountUI/releases";
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();

    public static void checkForUpdatesManually() {
        manualCheck = true;
        checkForUpdates();
    }

    public static void checkForUpdates() {
        System.out.println("🔢 Current version: " + CURRENT_VERSION);

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
                String tagName = latest.getString("tag_name");
                String latestVersion = tagName.startsWith("v.") ? tagName.replace("v.", "") :
                        tagName.startsWith("v") ? tagName.replace("v", "") : tagName;

                if (isNewerVersion(latestVersion, CURRENT_VERSION)) {
                    Platform.runLater(() -> showUpdateDialog(latest));
                } else if (manualCheck) {
                    Platform.runLater(() -> UIUtils.showInfo("✔ App is up-to-date\n✔ Aplicativo está atualizado"));
                }
            }
            manualCheck = false;
        });

        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            if (manualCheck) {
                Platform.runLater(() -> UIUtils.showError(
                        "❌ Connection to Update Failed: " + ex.getMessage() +
                                "\n❌ Conexão de Atualização Falhou: " + ex.getMessage()
                ));
            } else {
                System.err.println("⚠️ Auto-update check failed: " + ex.getMessage());
            }
            manualCheck = false;
        });

        executor.submit(task);
    }

    private static void showUpdateDialog(JSONObject release) {
        String tagName = release.optString("tag_name");
        String latestVersion = tagName.startsWith("v.") ? tagName.replace("v.", "") :
                tagName.startsWith("v") ? tagName.replace("v", "") : tagName;
        latestVersion = latestVersion.trim();

        String url = findInstallerUrl(release);
        String htmlUrl = release.optString("html_url", "https://github.com/TashiRabten/MantraCountUI/releases");

        if (latestVersion.isEmpty()) {
            System.err.println("❌ No tag_name found. \n❌ Não encontrou 'tag' de versão.");
            return;
        }

        if (url == null) {
            UIUtils.showError(
                    "No installer found / Instalador não encontrado",
                    "Release does not contain .exe, .dmg or .pkg\nLançamento não contém arquivo .exe, .dmg ou .pkg"
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

        String releaseNotes;
        try {
            releaseNotes = release.getString("body");
        } catch (Exception ex) {
            System.err.println("Failed to get release notes: " + ex.getMessage());
            releaseNotes = "No release notes available / Notas de lançamento não disponíveis";
        }

        Hyperlink releaseLink = new Hyperlink("🔗 Link to manual / Link para o Manual");
        releaseLink.setOnAction(e -> {
            try {
                Desktop.getDesktop().browse(URI.create(htmlUrl));
            } catch (IOException ex) {
                UIUtils.showError("Failed to open browser", "Falha ao abrir o navegador");
            }
        });

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
                    updateMessage("⬇️ Downloading installer...\n⬇️ Baixando instalador...");

                    // Download to temp first
                    Path tempDir = Files.createTempDirectory("mantra-update");
                    String fileName = url.substring(url.lastIndexOf('/') + 1);
                    Path tempOutput = tempDir.resolve(fileName);

                    try (InputStream in = new URL(url).openStream()) {
                        Files.copy(in, tempOutput, StandardCopyOption.REPLACE_EXISTING);
                    }

                    // Copy to downloads folder
                    Path userDownloads = Paths.get(System.getProperty("user.home"), "Downloads", fileName);
                    Files.copy(tempOutput, userDownloads, StandardCopyOption.REPLACE_EXISTING);
                    Files.deleteIfExists(tempOutput);

                    // Create cleanup script
                    Path cleanupScript;
                    String scriptExt;
                    String scriptContent;

                    if (System.getProperty("os.name").toLowerCase().contains("win")) {
                        scriptExt = ".bat";
                        scriptContent =
                                "@echo off\n" +
                                        "echo Waiting for installer to finish...\n" +
                                        "timeout /t 5 /nobreak > nul\n" +
                                        "echo Cleaning up...\n" +
                                        "del \"" + userDownloads.toString() + "\"\n" +
                                        "del \"%~f0\"\n";
                    } else {
                        scriptExt = ".sh";
                        scriptContent =
                                "#!/bin/bash\n" +
                                        "echo \"Waiting for installer to finish...\"\n" +
                                        "sleep 5\n" +
                                        "echo \"Cleaning up...\"\n" +
                                        "rm \"" + userDownloads.toString() + "\"\n" +
                                        "rm \"$0\"\n";
                    }

                    cleanupScript = tempDir.resolve("cleanup" + scriptExt);
                    Files.writeString(cleanupScript, scriptContent);

                    // Make script executable (for Unix systems)
                    if (!System.getProperty("os.name").toLowerCase().contains("win")) {
                        cleanupScript.toFile().setExecutable(true);
                    }

                    updateMessage("🚀 Opening installer...\n🚀 Abrindo instalador...");
                    try {
                        // Launch the installer
                        Desktop.getDesktop().open(userDownloads.toFile());

                        // Execute cleanup script in background
                        if (System.getProperty("os.name").toLowerCase().contains("win")) {
                            Runtime.getRuntime().exec("cmd /c start " + cleanupScript.toString());
                        } else {
                            Runtime.getRuntime().exec(cleanupScript.toString());
                        }

                        updateMessage("✅ Installer launched. Closing app...\n✅ Instalador iniciado. Fechando o aplicativo...");

                        // Give time to see the message
                        Thread.sleep(1500);

                        // Exit the application
                        Platform.exit();
                        System.exit(0);

                    } catch (Exception ex) {
                        updateMessage("❗ Could not open installer automatically.\n❗ Não foi possível abrir o instalador automaticamente.");
                        try {
                            // Try to show the file in explorer/finder
                            Runtime.getRuntime().exec(new String[]{"open", "-R", userDownloads.toString()});
                        } catch (Exception ignored) {}
                    }

                    return null;
                }
            };

            bar.progressProperty().bind(installTask.progressProperty());
            progress.textProperty().bind(installTask.messageProperty());

            installTask.setOnSucceeded(ev -> stage.close());
            installTask.setOnFailed(ev -> {
                Throwable ex = installTask.getException();
                progress.textProperty().unbind();
                progress.setText("❌ Error: " + ex.getMessage() + "\n❌ Erro: " + ex.getMessage());
                download.setDisable(false);
            });

            executor.submit(installTask);
        });

        cancel.setOnAction(e -> stage.close());

        root.getChildren().addAll(title, notes, releaseLink, bar, progress, buttons);
        stage.setScene(new Scene(root, 500, 450));
        stage.getIcons().add(new Image(AutoUpdater.class.getResourceAsStream("/icons/BUDA.jpg")));
        stage.show();
    }

    private static String findInstallerUrl(JSONObject release) {
        JSONArray assets = release.getJSONArray("assets");
        boolean isMac = System.getProperty("os.name").toLowerCase().contains("mac");
        boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");

        String fallbackUrl = null;

        for (int i = 0; i < assets.length(); i++) {
            JSONObject asset = assets.getJSONObject(i);
            String name = asset.getString("name").toLowerCase();

            if (isMac && (name.endsWith(".pkg") || name.endsWith(".dmg"))) {
                return asset.getString("browser_download_url");
            }
            if (isWindows && name.endsWith(".exe")) {
                return asset.getString("browser_download_url");
            }
            if (fallbackUrl == null && (name.endsWith(".exe") || name.endsWith(".dmg") || name.endsWith(".pkg"))) {
                fallbackUrl = asset.getString("browser_download_url");
            }
        }
        return fallbackUrl;
    }

    private static String getCurrentVersion() {
        try (InputStream in = AutoUpdater.class.getResourceAsStream("/version.properties")) {
            if (in == null) {
                System.err.println("❌ version.properties not found in resources.");
                return "0.0.0";
            }
            Properties props = new Properties();
            props.load(in);
            return props.getProperty("version", "0.0.0").trim();
        } catch (IOException e) {
            System.err.println("❌ Could not load version from properties: " + e.getMessage());
            return "0.0.0";
        }
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