package com.example.mantracount;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.control.Tooltip;
import javafx.util.Duration;

import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Simple mantra image controller - displays images with synonym support
 */
public class MantraImageController {

    private final Map<String, String> imageDatabase = new HashMap<>();
    private final Map<String, Image> imageCache = new ConcurrentHashMap<>();
    private final ExecutorService imageLoadingExecutor = Executors.newCachedThreadPool();
    private final ImageView imageView;

    private static final double IMAGE_SIZE = 100;

    public MantraImageController() {
        initializeImageDatabase();

        imageView = new ImageView();
        imageView.setFitWidth(IMAGE_SIZE);
        imageView.setFitHeight(IMAGE_SIZE);
        imageView.setPreserveRatio(true);
        imageView.setSmooth(true);

        Tooltip tooltip = new Tooltip("Mantra Image / Imagem do Mantra");
        tooltip.setShowDelay(Duration.millis(500));
        Tooltip.install(imageView, tooltip);

        // Initially hidden
        imageView.setVisible(false);
        imageView.setManaged(false);
    }

    private void initializeImageDatabase() {
        try (InputStream propStream = getClass().getResourceAsStream("/images/mantras/image-config.properties")) {
            if (propStream != null) {
                Properties props = new Properties();
                props.load(propStream);

                for (String key : props.stringPropertyNames()) {
                    String imagePath = props.getProperty(key);
                    imageDatabase.put(key.toLowerCase(), imagePath);
                }

                System.out.println("Loaded " + imageDatabase.size() + " image mappings from properties file");
            } else {
                loadDefaultMappings();
            }
        } catch (Exception e) {
            System.err.println("Error loading image properties: " + e.getMessage());
            loadDefaultMappings();
        }
    }

    private void loadDefaultMappings() {
        // Use SynonymManager to automatically handle all variants
        imageDatabase.put("amitayus", "/images/mantras/amitayus.jpg");
        imageDatabase.put("vajrasattva", "/images/mantras/vajrasattva.jpg");
        imageDatabase.put("tare", "/images/mantras/green_tara.jpg");
        imageDatabase.put("medicina", "/images/mantras/medicine_buddha.jpg");
        imageDatabase.put("avalokiteshvara", "/images/mantras/avalokiteshvara.jpg");
        imageDatabase.put("chenrezig", "/images/mantras/avalokiteshvara.jpg");
        imageDatabase.put("guru", "/images/mantras/tsongkhapa.jpg");
        imageDatabase.put("rinpoche", "/images/mantras/guru_rinpoche.jpg");
        imageDatabase.put("refúgio", "/images/mantras/three_jewels.jpg");
        imageDatabase.put("bodhicitta", "/images/mantras/bodhicitta.jpg");
        imageDatabase.put("bodhisattva", "/images/mantras/bodhicitta.jpg");
        imageDatabase.put("preliminares", "/images/mantras/preliminares.jpg");
        imageDatabase.put("om", "/images/mantras/om_symbol.jpg");
        imageDatabase.put("mantra", "/images/mantras/om_symbol.jpg");
        imageDatabase.put("rito", "/images/mantras/ritual_items.jpg");
        imageDatabase.put("manjushri", "/images/mantras/manjushri.jpg");
        imageDatabase.put("default", "/images/mantras/dharma_wheel.jpg");
    }

    public void updateImage(String mantraName) {
        if (mantraName == null || mantraName.trim().isEmpty()) {
            hideImage();
            return;
        }

        String imagePath = findImagePath(mantraName.toLowerCase().trim());
        if (imagePath != null) {
            loadImageAsync(imagePath);
        } else {
            loadImageAsync(imageDatabase.get("default"));
        }
    }

    private String findImagePath(String mantraName) {
        // Use SynonymManager to find canonical form and check variants
        String canonical = SynonymManager.getCanonicalForm(mantraName);
        if (imageDatabase.containsKey(canonical)) {
            return imageDatabase.get(canonical);
        }

        // Check all variants
        Set<String> variants = SynonymManager.getAllVariants(mantraName);
        for (String variant : variants) {
            if (imageDatabase.containsKey(variant)) {
                return imageDatabase.get(variant);
            }
        }

        // Fallback to contains matching
        for (Map.Entry<String, String> entry : imageDatabase.entrySet()) {
            String key = entry.getKey();
            if (mantraName.contains(key) || key.contains(mantraName)) {
                return entry.getValue();
            }
        }

        return null;
    }

    private void loadImageAsync(String imagePath) {
        if (imagePath == null) {
            hideImage();
            return;
        }

        // Check cache first
        Image cachedImage = imageCache.get(imagePath);
        if (cachedImage != null) {
            showImage(cachedImage);
            return;
        }

        // Load in background
        Task<Image> loadTask = new Task<>() {
            @Override
            protected Image call() throws Exception {
                InputStream imageStream = getClass().getResourceAsStream(imagePath);
                if (imageStream != null) {
                    return new Image(imageStream);
                } else {
                    throw new Exception("Image not found: " + imagePath);
                }
            }
        };

        loadTask.setOnSucceeded(e -> {
            Image image = loadTask.getValue();
            imageCache.put(imagePath, image);
            showImage(image);
        });

        loadTask.setOnFailed(e -> hideImage());
        imageLoadingExecutor.submit(loadTask);
    }

    private void showImage(Image image) {
        Platform.runLater(() -> {
            adjustImageSize(image);
            imageView.setImage(image);
            imageView.setVisible(true);
            imageView.setManaged(true);
        });
    }

    private void adjustImageSize(Image image) {
        double aspectRatio = image.getWidth() / image.getHeight();

        if (aspectRatio > 1.05) { // Wide images
            imageView.setFitWidth(IMAGE_SIZE * 2);
            imageView.setFitHeight(IMAGE_SIZE * 2);
        } else if (aspectRatio < 0.95) { // Tall images
            imageView.setFitWidth(IMAGE_SIZE);
            imageView.setFitHeight(IMAGE_SIZE * 1.15);
        } else { // Square-ish images
            imageView.setFitWidth(IMAGE_SIZE * 1.15);
            imageView.setFitHeight(IMAGE_SIZE * 1.15);
        }
    }

    public void hideImage() {
        Platform.runLater(() -> {
            imageView.setVisible(false);
            imageView.setManaged(false);
            imageView.setImage(null);
        });
    }

    public ImageView getImageView() {
        return imageView;
    }

    public boolean isImageVisible() {
        return imageView.isVisible();
    }

    public void addImageMapping(String mantraName, String imagePath) {
        imageDatabase.put(mantraName.toLowerCase(), imagePath);
    }

    public void shutdown() {
        imageLoadingExecutor.shutdown();
    }
}