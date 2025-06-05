package com.example.mantracount;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

public class LineEditorHelper {

    public static HBox createProtectedLine(String line, boolean isWarning) {
        CheckBox softDeleteCheck = new CheckBox();
        softDeleteCheck.setTooltip(new Tooltip("Exclude / Excluir"));

        int closeBracket = line.indexOf(']');
        int colon = line.indexOf(':', closeBracket);

        Node editablePart;
        if (closeBracket != -1 && colon != -1 && colon > closeBracket) {
            String prefix = line.substring(0, colon + 1);
            String content = line.substring(colon + 1);
            Label protectedLabel = new Label(prefix);
            protectedLabel.setStyle(UIColorScheme.getBoldTextStyle());
            TextField editableField = new TextField(content);
            UIComponentFactory.applyStandardFieldHeight(editableField);
            editableField.setPromptText("Edit line / Editar linha");
            HBox.setHgrow(editableField, Priority.ALWAYS);
            editablePart = new HBox(UIComponentFactory.STANDARD_SPACING, protectedLabel, editableField);
        } else {
            TextField fullField = new TextField(line);
            UIComponentFactory.applyStandardFieldHeight(fullField);
            fullField.setPromptText("Full editable line / Linha editável");
            if (isWarning) {
                fullField.setStyle(UIColorScheme.getItalicGrayTextStyle());
            }
            HBox.setHgrow(fullField, Priority.ALWAYS);
            editablePart = fullField;
        }

        HBox wrapper = new HBox(UIComponentFactory.BUTTON_SPACING, softDeleteCheck, editablePart);
        wrapper.setAlignment(Pos.CENTER_LEFT);
        return wrapper;
    }

    public static HBox createEditableLine(String originalLine, boolean isWarningLine) {
        CheckBox removeCheck = new CheckBox();
        TextField field = new TextField(originalLine);
        UIComponentFactory.applyStandardFieldHeight(field);
        Button restoreBtn = new Button("Restore / Restaurar");
        restoreBtn.setVisible(false); // Initially hidden

        HBox lineBox = new HBox(UIComponentFactory.BUTTON_SPACING, removeCheck, field, restoreBtn);
        lineBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(field, Priority.ALWAYS);

        if (isWarningLine) {
            field.setStyle(UIColorScheme.getItalicGrayTextStyle());
            field.setEditable(false);  // Lock warning line
        }

        // Store original content for restoring
        final String[] original = { originalLine };

        // Show restore if edited
        field.textProperty().addListener((obs, oldVal, newVal) -> {
            boolean changed = !newVal.equals(original[0]);
            restoreBtn.setVisible(changed || removeCheck.isSelected());
        });

        // Show restore if marked for deletion
        removeCheck.selectedProperty().addListener((obs, wasSelected, isSelected) -> {
            restoreBtn.setVisible(isSelected || !field.getText().equals(original[0]));
        });

        // Restore to original
        restoreBtn.setOnAction(e -> {
            field.setText(original[0]);
            removeCheck.setSelected(false);
            restoreBtn.setVisible(false);
        });

        return lineBox;
    }
}
