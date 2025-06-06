package com.example.mantracount;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.InputStream;

/**
 * Centralized utility class for creating standard dialogs.
 * Eliminates duplication between MissingDaysUI and MissingFizUI.
 */
public final class DialogUtils {

    private DialogUtils() {
        // Utility class - prevent instantiation
    }

    /**
     * Creates a standard modal dialog with icon and close callback.
     * 
     * @param owner The parent window
     * @param title The dialog title
     * @param onCloseCallback Optional callback to run when dialog is closed
     * @return Configured Stage ready for content
     */
    public static Stage createStandardDialog(Stage owner, String title, Runnable onCloseCallback) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(owner);
        dialog.setTitle(title);

        // Load and set the standard BUDA icon
        setStandardIcon(dialog);

        // Set close callback if provided
        if (onCloseCallback != null) {
            dialog.setOnCloseRequest(e -> onCloseCallback.run());
        }

        return dialog;
    }

    /**
     * Sets the standard BUDA icon for a stage/dialog.
     * 
     * @param stage The stage to set the icon for
     */
    public static void setStandardIcon(Stage stage) {
        InputStream stream = DialogUtils.class.getResourceAsStream("/icons/BUDA.png");
        if (stream != null) {
            System.out.println("Image found!");
            ImageView iconView = new ImageView(new Image(stream));
            iconView.setFitWidth(256);
            iconView.setFitHeight(256);
            stage.getIcons().add(iconView.getImage());
        } else {
            System.out.println("Image not found: /icons/BUDA.png");
        }
    }
}