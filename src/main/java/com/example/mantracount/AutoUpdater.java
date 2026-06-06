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
        System.out.println("🔧 Manual update check initiated");
        manualCheck = true;
        checkForUpdates();
    }

    public static void checkForUpdates() {
        System.out.println("🔢 Current version: " + CURRENT_VERSION);

        Task<JSONObject> task = new Task<>() {
            @Override
            protected JSONObject call() throws Exception {
                System.out.println("🌐 Connecting to GitHub API: " + GITHUB_RELEASES_API);
                HttpURLConnection conn = (HttpURLConnection) new URL(GITHUB_RELEASES_API).openConnection();
                conn.setRequestProperty("Accept", "application/vnd.github+json");
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(15000);

                int responseCode = conn.getResponseCode();
                System.out.println("📡 GitHub API response code: " + responseCode);
                if (responseCode != 200)
                    throw new IOException("HTTP " + responseCode);

                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = in.readLine()) != null) sb.append(line);
                in.close();

                System.out.println("📦 GitHub API response received, parsing...");
                JSONArray releases = new JSONArray(sb.toString());
                if (releases.length() == 0) {
                    System.out.println("❌ No releases found in GitHub API response");
                    return null;
                }
                JSONObject latest = releases.getJSONObject(0);
                System.out.println("✅ Latest release found: " + latest.optString("tag_name", "unknown"));
                return latest;
            }
        };

        task.setOnSucceeded(e -> {
            System.out.println("✅ Task succeeded, processing result...");
            JSONObject latest = task.getValue();
            if (latest != null) {
                String tagName = latest.getString("tag_name");
                System.out.println("🏷️ Raw tag name: " + tagName);
                String latestVersion = tagName.startsWith("v.") ? tagName.replace("v.", "") :
                        tagName.startsWith("v") ? tagName.replace("v", "") : tagName;
                System.out.println("🔢 Parsed latest version: " + latestVersion);

                System.out.println("🔍 Comparing versions: Latest=" + latestVersion + " vs Current=" + CURRENT_VERSION);
                if (isNewerVersion(latestVersion, CURRENT_VERSION)) {
                    System.out.println("✅ Update available: " + latestVersion + " > " + CURRENT_VERSION);
                    Platform.runLater(() -> showUpdateDialog(latest));
                } else {
                    System.out.println("ℹ️ No update needed: " + latestVersion + " <= " + CURRENT_VERSION);
                    if (manualCheck) {
                        Platform.runLater(() -> UIUtils.showInfo("✔ App is up-to-date\n✔ Aplicativo está atualizado"));
                    }
                }
            }
            manualCheck = false;
        });

        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            System.err.println("❌ Update check task failed: " + ex.getClass().getSimpleName() + ": " + ex.getMessage());
            ex.printStackTrace();
            if (manualCheck) {
                Platform.runLater(() -> UIUtils.showError(
                        "❌ Connection to Update Failed: " + ex.getMessage() +
                                "\n❌ Conexão de Atualização Falhou: " + ex.getMessage()
                ));
            } else {
                System.err.println(StringConstants.UPDATE_CHECK_FAILED_AUTO + ex.getMessage());
            }
            manualCheck = false;
        });

        executor.submit(task);
    }

    private static void showUpdateDialog(JSONObject release) {
        System.out.println("🚀 Showing update dialog...");
        String latestVersion = extractVersionFromTag(release.optString("tag_name"));
        String url = findInstallerUrl(release);
        String htmlUrl = release.optString("html_url", "https://github.com/TashiRabten/MantraCountUI/releases");
        
        System.out.println("📋 Update dialog data: version=" + latestVersion + ", url=" + url + ", htmlUrl=" + htmlUrl);

        if (latestVersion.isEmpty()) {
            System.err.println(StringConstants.NO_TAG_NAME_FOUND);
            return;
        }

        if (url == null) {
            System.err.println("❌ No installer URL found in release assets");
            UIUtils.showError(
                    StringConstants.NO_INSTALLER_FOUND_EN,
                    StringConstants.NO_INSTALLER_FOUND_DETAILS
            );
            return;
        }

        Stage stage = createUpdateDialogStage();
        VBox root = createDialogContent(release, latestVersion, htmlUrl, url, stage);
        
        stage.setScene(new Scene(root, 500, 450));
        stage.getIcons().add(new Image(AutoUpdater.class.getResourceAsStream(StringConstants.ICON_BUDA_PATH)));
        stage.show();
    }

    private static String extractVersionFromTag(String tagName) {
        String version = tagName.startsWith("v.") ? tagName.replace("v.", "") :
                tagName.startsWith("v") ? tagName.replace("v", "") : tagName;
        return version.trim();
    }

    private static Stage createUpdateDialogStage() {
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle(StringConstants.UPDATE_DIALOG_TITLE);
        return stage;
    }

    private static VBox createDialogContent(JSONObject release, String latestVersion, String htmlUrl, String url, Stage stage) {
        VBox root = new VBox(UIComponentFactory.LARGE_SPACING);
        root.setPadding(new Insets(20));
        root.setAlignment(Pos.CENTER);

        System.out.println("🐛 DEBUG: Creating title with latestVersion: '" + latestVersion + "'");
        System.out.println("🐛 DEBUG: Format string: '" + StringConstants.NEW_VERSION_AVAILABLE + "'");
        Label title = new Label(String.format(StringConstants.NEW_VERSION_AVAILABLE, latestVersion, latestVersion));
        String releaseNotes = extractReleaseNotes(release);
        Hyperlink releaseLink = createReleaseLink(htmlUrl);
        TextArea notes = createNotesArea(releaseNotes);
        
        ProgressBar bar = new ProgressBar(0);
        bar.setVisible(false);
        Label progress = new Label("");
        progress.setVisible(false);

        HBox buttons = createButtons(url, bar, progress, stage);
        
        root.getChildren().addAll(title, notes, releaseLink, bar, progress, buttons);
        return root;
    }

    private static String extractReleaseNotes(JSONObject release) {
        try {
            return release.getString("body");
        } catch (Exception ex) {
            System.err.println("Failed to get release notes: " + ex.getMessage());
            return StringConstants.NO_RELEASE_NOTES;
        }
    }

    private static Hyperlink createReleaseLink(String htmlUrl) {
        Hyperlink releaseLink = new Hyperlink(StringConstants.RELEASE_LINK);
        releaseLink.setOnAction(e -> {
            try {
                Desktop.getDesktop().browse(URI.create(htmlUrl));
            } catch (IOException ex) {
                UIUtils.showError(StringConstants.FAILED_TO_OPEN_BROWSER_EN, StringConstants.FAILED_TO_OPEN_BROWSER_PT);
            }
        });
        return releaseLink;
    }

    private static TextArea createNotesArea(String releaseNotes) {
        TextArea notes = new TextArea(releaseNotes);
        notes.setEditable(false);
        notes.setWrapText(true);
        notes.setPrefHeight(200);
        return notes;
    }

    private static HBox createButtons(String url, ProgressBar bar, Label progress, Stage stage) {
        Button download = new Button(StringConstants.DOWNLOAD_INSTALL_BUTTON);
        Button cancel = new Button(StringConstants.CANCEL_BUTTON);

        HBox buttons = new HBox(UIComponentFactory.BUTTON_SPACING, download, cancel);
        buttons.setAlignment(Pos.CENTER);

        download.setOnAction(e -> handleDownloadAction(url, bar, progress, download, stage));
        cancel.setOnAction(e -> stage.close());
        
        return buttons;
    }

    private static void handleDownloadAction(String url, ProgressBar bar, Label progress, Button download, Stage stage) {
        bar.setVisible(true);
        progress.setVisible(true);
        download.setDisable(true);

        Task<Void> installTask = createInstallTask(url);
        
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
    }

    private static Task<Void> createInstallTask(String url) {
        return new Task<>() {
            @Override
            protected Void call() throws Exception {
                updateMessage(StringConstants.DOWNLOADING_INSTALLER);
                Path userDownloads = downloadInstaller(url);
                Path cleanupScript = createCleanupScript();
                executeCleanupScript(cleanupScript);
                launchInstaller(userDownloads, cleanupScript, this::updateMessage);
                return null;
            }
        };
    }

    private static Path downloadInstaller(String url) throws Exception {
        Path tempDir = Files.createTempDirectory("mantra-update");
        String fileName = url.substring(url.lastIndexOf('/') + 1);
        Path tempOutput = tempDir.resolve(fileName);

        try (InputStream in = new URL(url).openStream()) {
            Files.copy(in, tempOutput, StandardCopyOption.REPLACE_EXISTING);
        }

        Path userDownloads = Paths.get(System.getProperty(StringConstants.USER_HOME_PROPERTY), StringConstants.DOWNLOADS_FOLDER, fileName);
        Files.copy(tempOutput, userDownloads, StandardCopyOption.REPLACE_EXISTING);
        Files.deleteIfExists(tempOutput);
        
        return userDownloads;
    }

    private static Path createCleanupScript() throws Exception {
        Path tempDir = Files.createTempDirectory("mantra-update");
        boolean isWindows = isWindowsOS();
        Path cleanupScript = tempDir.resolve("cleanup" + (isWindows ? ".bat" : ".sh"));
        
        String scriptContent = isWindows ? createWindowsCleanupScript(cleanupScript) : createUnixCleanupScript();
        Files.writeString(cleanupScript, scriptContent);
        
        if (!isWindows) {
            cleanupScript.toFile().setExecutable(true);
        }
        
        return cleanupScript;
    }

    private static String createWindowsCleanupScript(Path cleanupScript) {
        return "@echo off\n" +
               "timeout /t 5 /nobreak > nul 2>&1\n" +
               "start /min cmd /c \"timeout /t 2 /nobreak > nul 2>&1 & del \\\"" +
               cleanupScript.toString() + "\\\"\"\n";
    }

    private static String createUnixCleanupScript() {
        return "#!/bin/bash\n" +
               "sleep 5\n" +
               "(sleep 2; rm \"$0\") &\n";
    }

    private static void executeCleanupScript(Path cleanupScript) throws Exception {
        if (isWindowsOS()) {
            Runtime.getRuntime().exec(new String[]{"cmd", "/c", "start", "/min", "/b", cleanupScript.toString()});
        } else {
            Runtime.getRuntime().exec(cleanupScript.toString());
        }
    }

    private static void launchInstaller(Path userDownloads, Path cleanupScript, MessageUpdater messageUpdater) throws Exception {
        messageUpdater.updateMessage(StringConstants.OPENING_INSTALLER);
        try {
            Desktop.getDesktop().open(userDownloads.toFile());
            executeCleanupInBackground(cleanupScript);
            messageUpdater.updateMessage(StringConstants.INSTALLER_LAUNCHED);
            Thread.sleep(1500);
            Platform.exit();
            System.exit(0);
        } catch (Exception ex) {
            messageUpdater.updateMessage(StringConstants.COULD_NOT_OPEN_INSTALLER);
            tryOpenFileExplorer(userDownloads);
        }
    }

    @FunctionalInterface
    private interface MessageUpdater {
        void updateMessage(String message);
    }

    private static void executeCleanupInBackground(Path cleanupScript) throws Exception {
        if (isWindowsOS()) {
            Runtime.getRuntime().exec("cmd /c start " + cleanupScript.toString());
        } else {
            Runtime.getRuntime().exec(cleanupScript.toString());
        }
    }

    private static void tryOpenFileExplorer(Path userDownloads) {
        try {
            Runtime.getRuntime().exec(new String[]{"open", "-R", userDownloads.toString()});
        } catch (Exception explorerEx) {
            // Failed to open file explorer, continue silently
        }
    }

    private static boolean isWindowsOS() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }

    private static String findInstallerUrl(JSONObject release) {
        JSONArray assets = release.getJSONArray("assets");
        boolean isMac = System.getProperty("os.name").toLowerCase().contains("mac");
        boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");

        String fallbackUrl = null;

        for (int i = 0; i < assets.length(); i++) {
            JSONObject asset = assets.getJSONObject(i);
            String name = asset.getString("name").toLowerCase();

            if (isMac && (name.endsWith(StringConstants.PKG_EXTENSION) || name.endsWith(StringConstants.DMG_EXTENSION))) {
                return asset.getString("browser_download_url");
            }
            if (isWindows && name.endsWith(StringConstants.EXE_EXTENSION)) {
                return asset.getString("browser_download_url");
            }
            if (fallbackUrl == null && (name.endsWith(StringConstants.EXE_EXTENSION) || name.endsWith(StringConstants.DMG_EXTENSION) || name.endsWith(StringConstants.PKG_EXTENSION))) {
                fallbackUrl = asset.getString("browser_download_url");
            }
        }
        return fallbackUrl;
    }

    private static String getCurrentVersion() {
        try (InputStream in = AutoUpdater.class.getResourceAsStream(StringConstants.VERSION_PROPERTIES_PATH)) {
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