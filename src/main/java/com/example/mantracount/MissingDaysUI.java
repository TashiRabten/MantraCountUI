package com.example.mantracount;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import java.time.LocalDate;
import java.util.*;
import com.example.mantracount.MissingDaysDetector.MissingDayInfo;


public class MissingDaysUI {
    private final Map<String, String> editedLines = new HashMap<>();
    private final List<String> removedLines = new ArrayList<>();
    private final Map<String, Integer> linePositions = new HashMap<>();
    private VBox editableContainer;
    private Button undoButton;

    public void show(Stage owner, MantraData data) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(owner);
        dialog.setTitle("Missing Days Analysis / Análise de Dias Faltantes");

        VBox root = new VBox(10);
        root.setPadding(new Insets(20));

        Label infoLabel = new Label("Select a missing day to review issues:\nSelecione um dia faltante para revisar problemas:");

        List<MissingDaysDetector.MissingDayInfo> missingDays = MissingDaysDetector.detectMissingDays(
                data.getLines(), data.getTargetDate(), data.getNameToCount()
        );

        missingDaysCount = missingDays.size();

        ListView<String> missingList = new ListView<>();
        for (MissingDaysDetector.MissingDayInfo m : missingDays) {
            missingList.getItems().add(m.toString());
        }

        editableContainer = new VBox(5);
        ScrollPane scroll = new ScrollPane(editableContainer);
        scroll.setFitToWidth(true);
        scroll.setPrefHeight(280);

        undoButton = new Button("↩ Undo Last Removal");
        undoButton.setDisable(true);
        undoButton.setOnAction(e -> undoLast());

        Button saveBtn = new Button("Apply Edits / Aplicar");
        saveBtn.setStyle("-fx-base: #4CAF50; -fx-text-fill: white;");
        saveBtn.setOnAction(e -> saveEdits(data, dialog));

        Button closeBtn = new Button("Cancel / Cancelar");
        closeBtn.setOnAction(e -> dialog.close());

        HBox actions = new HBox(10, saveBtn, closeBtn);
        actions.setAlignment(Pos.CENTER_RIGHT);

        missingList.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            int idx = missingList.getSelectionModel().getSelectedIndex();
            if (idx >= 0 && idx < missingDays.size()) {
                showEditableLines(data, missingDays.get(idx).getDate());
            }
        });

        root.getChildren().addAll(infoLabel, missingList, undoButton, scroll, actions);
        if (missingDaysCount > 0) {
            UIUtils.showInfo("❗ Found " + missingDaysCount + " missing day(s).\n❗ Encontrado(s) " + missingDaysCount + " dia(s) faltante(s).");
        } else {
            UIUtils.showInfo("✔ No missing days detected.\n✔ Nenhum dia faltando detectado.");
        }
        dialog.setScene(new Scene(root, 700, 500));
        dialog.showAndWait();
    }

    private void showEditableLines(MantraData data, LocalDate date) {
        editableContainer.getChildren().clear();
        List<String> suspects = MissingDaysDetector.findPotentialIssues(data.getLines(), date, data.getNameToCount());

        if (suspects.isEmpty()) {
            editableContainer.getChildren().add(new Label("No suspicious lines found / Nenhuma linha suspeita encontrada."));
            return;
        }

        linePositions.clear();
        for (int i = 0; i < suspects.size(); i++) {
            String line = suspects.get(i);
            linePositions.put(line, i);
            addEditableLine(line);
        }
    }

    private void addEditableLine(String line) {
        VBox box = new VBox(3);
        box.setStyle("-fx-border-color: lightgray; -fx-padding: 5;");
        box.setUserData(line);

        LineParser.LineSplitResult split = LineParser.splitEditablePortion(line);

        Button removeBtn = new Button("X");
        removeBtn.setOnAction(e -> {
            removedLines.add(line);
            editableContainer.getChildren().remove(box);
            undoButton.setDisable(false);
        });

        Label fixed = new Label(split.getFixedPrefix());
        TextField editable = new TextField(split.getEditableSuffix());

        // ✅ Initialize editedLines immediately
        editedLines.put(line, fixed.getText() + editable.getText());

        editable.textProperty().addListener((obs, ov, nv) -> editedLines.put(line, fixed.getText() + nv));

        HBox row = new HBox(5, removeBtn, fixed, editable);
        row.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(editable, Priority.ALWAYS);
        box.getChildren().addAll(row, new Label("Original: " + line));

        editableContainer.getChildren().add(box);
    }

    private void undoLast() {
        if (removedLines.isEmpty()) return;
        String last = removedLines.remove(removedLines.size() - 1);
        addEditableLine(last);
        editableContainer.getChildren().remove(editableContainer.getChildren().size() - 1);
        int pos = linePositions.getOrDefault(last, editableContainer.getChildren().size());
        editableContainer.getChildren().add(pos, editableContainer.getChildren().remove(editableContainer.getChildren().size() - 1));
        if (removedLines.isEmpty()) undoButton.setDisable(true);
    }

    private void saveEdits(MantraData data, Stage dialog) {
        try {
            List<String> lines = new ArrayList<>(data.getLines());
            for (Map.Entry<String, String> entry : editedLines.entrySet()) {
                for (int i = 0; i < lines.size(); i++) {
                    if (lines.get(i).equals(entry.getKey())) {
                        lines.set(i, entry.getValue());
                        break;
                    }
                }
            }

            // Soft-delete lines visually removed
            lines.removeIf(removedLines::contains);

            FileEditSaver.saveToFile(lines, data.getFilePath());
            if (data.isFromZip()) {
                FileEditSaver.updateZipFile(data.getOriginalZipPath(), data.getFilePath(), lines);
            }

            data.setLines(lines);
            UIUtils.showInfo("✔ Changes saved.");
        } catch (Exception e) {
            UIUtils.showError("❌ Failed to save changes: " + e.getMessage());
        }
    }

    private int missingDaysCount = 0;

    public int getMissingDaysCount() {
        return missingDaysCount;
    }
}
