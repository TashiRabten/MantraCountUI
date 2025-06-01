package com.example.mantracount;

import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ButtonImageUtils {
    Properties imageMap = new Properties();

    public Properties imageIni(){

        try (InputStream input = getClass().getResourceAsStream("/images/button-config.properties")) {
            if (input != null) {
                imageMap.load(input);
            } else {
                System.err.println("Image Propoerty file not found.");
            }
        } catch (IOException e) {
            System.err.println("Failed to load button image configuration: " + e.getMessage());
        }
        return imageMap;
    }

    public void assignButtonIcon(Button button, String key, Properties imageMap) {
        String imagePath = imageMap.getProperty(key, imageMap.getProperty("default"));
        try (InputStream stream = getClass().getResourceAsStream(imagePath)) {
            if (stream != null) {
                Image image = new Image(stream);
                ImageView icon = new ImageView(image);
                icon.setFitWidth(16);
                icon.setFitHeight(16);
                button.setGraphic(icon);
            } else {
                System.err.println("Icon not found for key: " + key + " (path: " + imagePath + ")");
            }
        } catch (Exception e) {
            System.err.println("Unexpected error loading icon for key: " + key + " - " + e.getMessage());
        }
    }
}