package com.example.mantracount;

import javafx.concurrent.Task;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.util.Duration;
import javafx.scene.control.Tooltip;

import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Simple mantra image controller - just displays images without changing UI dimensions.
 * Minimal footprint, no statistics, no configuration dialogs.
 */
public class MantraImageController {

    private final Map<String, String> imageDatabase = new HashMap<>();
    private final Map<String, Image> imageCache = new ConcurrentHashMap<>();
    private final ExecutorService imageLoadingExecutor = Executors.newCachedThreadPool();

    private final ImageView imageView;

    // Fixed dimensions to not interfere with existing layout
    private static final double IMAGE_SIZE = 100;

    public MantraImageController() {
        initializeImageDatabase();

        // Simple image view with fixed size
        imageView = new ImageView();
        imageView.setFitWidth(IMAGE_SIZE);
        imageView.setFitHeight(IMAGE_SIZE);
        imageView.setPreserveRatio(true);
        imageView.setSmooth(true);


        // Simple tooltip
        Tooltip tooltip = new Tooltip("Mantra Image / Imagem do Mantra");
        tooltip.setShowDelay(Duration.millis(500));
        Tooltip.install(imageView, tooltip);

        // Initially hidden
        imageView.setVisible(false);
        imageView.setManaged(false);
    }

    private void adjustImageSize(Image image) {
        double aspectRatio = image.getWidth() / image.getHeight();

        System.out.println("Image aspect ratio: " + aspectRatio); // Debug line

        if (aspectRatio > 1.05) { // Wide images
            System.out.println("Using WIDE sizing"); // Debug line
            imageView.setFitWidth(IMAGE_SIZE * 2);
            imageView.setFitHeight(IMAGE_SIZE * 2);
        } else if (aspectRatio < 0.95) { // Tall images
            System.out.println("Using TALL sizing"); // Debug line
            imageView.setFitWidth(IMAGE_SIZE * 1.0);
            imageView.setFitHeight(IMAGE_SIZE * 1.15);
        } else { // Square-ish image
            System.out.println("Using SQUARE sizing"); // Debug line
            imageView.setFitWidth(IMAGE_SIZE * 1.15);
            imageView.setFitHeight(IMAGE_SIZE * 1.15);
        }
    }


    /**
     * Initialize image mapping from properties file
     */

    private void initializeImageDatabase() {
        try (InputStream propStream = getClass().getResourceAsStream("/images/mantras/image-config.properties")) {
            if (propStream != null) {
                Properties props = new Properties();
                props.load(propStream);

                // Load all properties into imageDatabase
                for (String key : props.stringPropertyNames()) {
                    String imagePath = props.getProperty(key);
                    imageDatabase.put(key.toLowerCase(), imagePath);
                }

                System.out.println("Loaded " + imageDatabase.size() + " image mappings from properties file");
            } else {
                // Fallback to hardcoded if properties file not found
                loadDefaultMappings();
            }
        } catch (Exception e) {
            System.err.println("Error loading image properties: " + e.getMessage());
            loadDefaultMappings();
        }
    }

    /**
     * Initialize simple image mapping
     */
    private void loadDefaultMappings()  {
        // Core mappings - just path strings
        imageDatabase.put("vajrasattva", "/images/mantras/vajrasattva.jpg");
        imageDatabase.put("tara", "/images/mantras/green_tara.jpg");
        imageDatabase.put("tare", "/images/mantras/green_tara.jpg");
        imageDatabase.put("medicina", "/images/mantras/medicine_buddha.jpg");
        imageDatabase.put("avalokiteshvara", "/images/mantras/avalokiteshvara.jpg");
        imageDatabase.put("chenrezig", "/images/mantras/avalokiteshvara.jpg");
        imageDatabase.put("guru", "/images/mantras/tsongkhapa.jpg");
        imageDatabase.put("rinpoche", "/images/mantras/guru_rinpoche.jpg");
        imageDatabase.put("refugio", "/images/mantras/three_jewels.jpg");
        imageDatabase.put("refúgio", "/images/mantras/three_jewels.jpg");
        imageDatabase.put("bodhicitta", "/images/mantras/bodhicitta.jpg");
        imageDatabase.put("bodisatva", "/images/mantras/bodhicitta.jpg");
        imageDatabase.put("bodhisattva", "/images/mantras/bodhicitta.jpg");
        imageDatabase.put("preliminares", "/images/mantras/preliminares.jpg");
        imageDatabase.put("om", "/images/mantras/om_symbol.jpg");
        imageDatabase.put("mantra", "/images/mantras/om_symbol.jpg");
        imageDatabase.put("rito", "/images/mantras/ritual_items.jpg");
        imageDatabase.put("ritual", "/images/mantras/ritual_items.jpg");

        // Default fallback
        imageDatabase.put("default", "/images/mantras/dharma_wheel.jpg");
    }

    /**
     * Update image for mantra name - simple matching
     */
    public void updateImage(String mantraName) {
        if (mantraName == null || mantraName.trim().isEmpty()) {
            hideImage();
            return;
        }

        String imagePath = findImagePath(mantraName.toLowerCase().trim());
        if (imagePath != null) {
            loadImageAsync(imagePath);
        } else {
            // Load default
            loadImageAsync(imageDatabase.get("default"));
        }
    }

    /**
     * Simple image path finding
     */
    private String findImagePath(String mantraName) {
        // Exact match first
        if (imageDatabase.containsKey(mantraName)) {
            return imageDatabase.get(mantraName);
        }

        // Simple contains matching
        for (Map.Entry<String, String> entry : imageDatabase.entrySet()) {
            String key = entry.getKey();
            if (mantraName.contains(key) || key.contains(mantraName)) {
                return entry.getValue();
            }
        }

        return null;
    }

    /**
     * Load image asynchronously
     */
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

    /**
     * Show image
     */
    private void showImage(Image image) {
        javafx.application.Platform.runLater(() -> {
            adjustImageSize(image);
            imageView.setImage(image);
            imageView.setVisible(true);
            imageView.setManaged(true);
        });
    }

    /**
     * Hide image
     */
    public void hideImage() {
        javafx.application.Platform.runLater(() -> {
            imageView.setVisible(false);
            imageView.setManaged(false);
            imageView.setImage(null);
        });
    }

    /**
     * Get the image view for adding to UI
     */
    public ImageView getImageView() {
        return imageView;
    }

    /**
     * Check if image is visible
     */
    public boolean isImageVisible() {
        return imageView.isVisible();
    }

    /**
     * Add custom mapping
     */
    public void addImageMapping(String mantraName, String imagePath) {
        imageDatabase.put(mantraName.toLowerCase(), imagePath);
    }

    /**
     * Shutdown executor
     */
    public void shutdown() {
        imageLoadingExecutor.shutdown();
    }
}