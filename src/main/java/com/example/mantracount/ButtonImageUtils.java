package com.example.mantracount;

import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class ButtonImageUtils {

    private static final Map<String, Image> imageCache = new HashMap<>();
    private static Properties cachedImageMap;

    public Properties imageIni() {
        if (cachedImageMap == null) {
            cachedImageMap = new Properties();
            try (InputStream input = getClass().getResourceAsStream("/images/button-config.properties")) {
                if (input != null) {
                    cachedImageMap.load(input);
                } else {
                    System.err.println("Image property file not found.");
                }
            } catch (IOException e) {
                System.err.println("Failed to load button image configuration: " + e.getMessage());
            }
        }
        return cachedImageMap;
    }

    public void assignButtonIcon(Button button, String key, Properties imageMap) {
        String imagePath = imageMap.getProperty(key, imageMap.getProperty("default"));
        Image image = imageCache.get(imagePath);
        if (image == null) {
            try (InputStream stream = getClass().getResourceAsStream(imagePath)) {
                if (stream != null) {
                    image = new Image(stream);
                    imageCache.put(imagePath, image);
                } else {
                    System.err.println("Icon not found for key: " + key + " (path: " + imagePath + ")");
                    return;
                }
            } catch (Exception e) {
                System.err.println("Unexpected error loading icon for key: " + key + " - " + e.getMessage());
                return;
            }
        }
        ImageView icon = new ImageView(image);
        icon.setFitWidth(16);
        icon.setFitHeight(16);
        icon.setSmooth(true);
        icon.setPreserveRatio(true);
        button.setGraphic(icon);
    }
}