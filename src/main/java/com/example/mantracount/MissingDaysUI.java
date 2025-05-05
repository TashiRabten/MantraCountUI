package com.example.mantracount;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class MissingDaysUI {
    private final Map<Integer, String> editedLineIndexes = new HashMap<>();
    private final Set<Integer> removedLineIndexes = new HashSet<>();
    private final Deque<UndoOperation> undoStack = new ArrayDeque<>();
    private final Map<Integer, Integer> originalPositions = new HashMap<>();
    private VBox issuesEditContainer;
    private Button undoButton;
    private ProgressIndicator progressIndicator;
    private int missingDaysCount = 0;
    private List<String> allLines;
    private MissingDaysDetector.MissingDayInfo currentMissingInfo;
    private Map<Integer, Integer> contextToActualLineMap = new HashMap<>(); // Maps context indices to actual file line indices

    // New class to track undo operations with positions
    private static class UndoOperation {
        private final int lineIndex;
        private final String content;
        private final int position;

        public UndoOperation(int lineIndex, String content, int position) {
            this.lineIndex = lineIndex;
            this.content = content;
            this.position = position;
        }

        public int getLineIndex() {
            return lineIndex;
        }

        public String getContent() {
            return content;
        }

        public int getPosition() {
            return position;
        }
    }

    public void show(Stage owner, MantraData data) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(owner);
        dialog.setTitle("Missing Days Analysis / Análise de Saltos de Dias");
        dialog.getIcons().add(new Image(getClass().getResourceAsStream("/icons/BUDA.jpg")));


        VBox root = new VBox(10);
        root.setPadding(new Insets(15));

        Label header = new Label("Missing Days Analysis / Análise de Saltos de Dias");
        header.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");


        progressIndicator = new ProgressIndicator();
        progressIndicator.setMaxSize(50, 50);
        progressIndicator.setVisible(false);

        ListView<String> missingList = new ListView<>();

        issuesEditContainer = new VBox(10);
        ScrollPane scroll = new ScrollPane(issuesEditContainer);
        scroll.setFitToWidth(true);
        scroll.prefHeightProperty().bind(root.heightProperty().multiply(0.7));
        scroll.setStyle("-fx-border-color: #0078D7; -fx-border-width: 1px;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        undoButton = new Button("\u21A9 Undo Last Removal / Desfazer Remoção");
        undoButton.setStyle("-fx-base: #2196F3; -fx-text-fill: white;");
        undoButton.setDisable(true);
        undoButton.setOnAction(e -> undoLast());

        Button saveBtn = new Button("\u2714 Apply Edits / Aplicar Alterações");
        saveBtn.setStyle("-fx-base: #4CAF50; -fx-text-fill: white;");
        saveBtn.setOnAction(e -> applyEditsAsync(data));

        Button closeBtn = new Button("\u2716 Close / Fechar");
        closeBtn.setStyle("-fx-base: #F44336; -fx-text-fill: white;");
        closeBtn.setOnAction(e -> dialog.close());

        HBox actions = new HBox(10, saveBtn, closeBtn);
        actions.setAlignment(Pos.CENTER_RIGHT);
        actions.setPadding(new Insets(10, 0, 0, 0));

        root.getChildren().addAll(header, progressIndicator, missingList, undoButton, scroll, actions);
        dialog.setScene(new Scene(root, 800, 600));
        dialog.show();

        this.allLines = new ArrayList<>(data.getLines());
        progressIndicator.setVisible(true);
        CompletableFuture.supplyAsync(() ->
                MissingDaysDetector.detectMissingDays(allLines, data.getTargetDate(), data.getNameToCount())
        ).thenAccept(result -> Platform.runLater(() -> {
            List<MissingDaysDetector.MissingDayInfo> missingDays = new ArrayList<>(result);
            missingDaysCount = missingDays.size();

            if (missingDaysCount == 0) {
                UIUtils.showInfo("\u2714 No missing days found. \nNenhum salto de dia encontrado.");
                dialog.close();
                return;
            }

            List<String> items = new ArrayList<>();
            for (MissingDaysDetector.MissingDayInfo info : missingDays) {
                items.add("Missing: " + info.getDate());
            }

            missingList.setItems(FXCollections.observableArrayList(items));
            missingList.getSelectionModel().selectedItemProperty().addListener((obs, ov, nv) -> {
                int idx = missingList.getSelectionModel().getSelectedIndex();
                if (idx >= 0) {
                    currentMissingInfo = missingDays.get(idx);
                    showEditableLinesAround(data, currentMissingInfo.getDate());
                }
            });
            missingList.getSelectionModel().selectFirst();
            progressIndicator.setVisible(false);
        }));
    }

    private void showEditableLinesAround(MantraData data, LocalDate centerDate) {
        editedLineIndexes.clear();
        removedLineIndexes.clear();
        undoStack.clear();
        originalPositions.clear();
        contextToActualLineMap.clear(); // Reset the context to actual line mapping
        issuesEditContainer.getChildren().clear();
        undoButton.setDisable(true);

        // Find the actual line indices for the context lines
        List<Integer> actualLineIndices = findActualLineIndices(data.getLines(), centerDate);

        // Get context lines and track their actual file positions
        List<String> contextLines = new ArrayList<>();
        for (int i = 0; i < actualLineIndices.size(); i++) {
            int actualIndex = actualLineIndices.get(i);
            if (actualIndex >= 0 && actualIndex < data.getLines().size()) {
                String line = data.getLines().get(actualIndex);
                contextLines.add(line);
                // Store mapping from context index to actual file index
                contextToActualLineMap.put(i, actualIndex);
            }
        }

        // Add the editable lines to the UI
        for (int i = 0; i < contextLines.size(); i++) {
            String line = contextLines.get(i);
            addEditableLineNode(line, i);
        }
    }

    // Helper method to find the actual line indices in the file
    private List<Integer> findActualLineIndices(List<String> allLines, LocalDate centerDate) {
        List<Integer> actualIndices = new ArrayList<>();

        // Use the same logic as LineAnalyzer to identify relevant dates
        List<LocalDate> datesToInclude = Arrays.asList(
                centerDate.minusDays(1),
                centerDate,
                centerDate.plusDays(1),
                centerDate.plusDays(2)
        );

        for (int i = 0; i < allLines.size(); i++) {
            String line = allLines.get(i);
            LocalDate date = LineParser.extractDate(line);
            if (date != null && datesToInclude.contains(date)) {
                actualIndices.add(i);
            }
        }

        // Sort the indices to ensure they appear in the correct order
        actualIndices.sort(Comparator.naturalOrder());
        return actualIndices;
    }

    private void applyEditsAsync(MantraData data) {
        progressIndicator.setVisible(true);
        CompletableFuture.runAsync(() -> {
            try {
                List<String> originalLines = new ArrayList<>(data.getLines());
                List<String> updatedLines = new ArrayList<>(originalLines);

                // Only apply edits (do NOT delete any lines)
                for (Map.Entry<Integer, String> edit : editedLineIndexes.entrySet()) {
                    int contextIndex = edit.getKey();
                    String editedContent = edit.getValue();

                    Integer actualLineIndex = contextToActualLineMap.get(contextIndex);
                    if (actualLineIndex != null && actualLineIndex >= 0 && actualLineIndex < updatedLines.size()) {
                        updatedLines.set(actualLineIndex, editedContent);
                    }
                }

                // Check if anything changed
                boolean changesMade = false;
                if (updatedLines.size() != originalLines.size()) {
                    changesMade = true;
                } else {
                    for (int i = 0; i < updatedLines.size(); i++) {
                        if (!updatedLines.get(i).equals(originalLines.get(i))) {
                            changesMade = true;
                            break;
                        }
                    }
                }

                if (!changesMade) {
                    Platform.runLater(() -> {
                        progressIndicator.setVisible(false);
                        UIUtils.showError("\u274C No changes to save. \nNenhuma alteração para salvar.");
                    });
                    return;
                }

                // Save to file
                try {
                    FileEditSaver.saveToFile(updatedLines, data.getFilePath());

                    if (data.isFromZip()) {
                        FileEditSaver.updateZipFile(data.getOriginalZipPath(), data.getFilePath(), updatedLines);
                    }

                    data.setLines(updatedLines);

                    Platform.runLater(() -> {
                        progressIndicator.setVisible(false);
                        UIUtils.showInfo("\u2714 Changes saved successfully! \n Alterações salvas com sucesso!");
                    });
                } catch (IOException e) {
                    Platform.runLater(() -> {
                        progressIndicator.setVisible(false);
                        UIUtils.showError("\u274C Failed to save: " + e.getMessage() + "\nFalha ao salvar: " + e.getMessage());
                    });
                }
            } catch (Exception e) {
                Platform.runLater(() -> {
                    progressIndicator.setVisible(false);
                    UIUtils.showError("\u274C Error: " + e.getMessage() + "\nErro: " + e.getMessage());
                });
            }
        });
    }


    private void addEditableLineNode(String lineContent, int index) {
        Node node = createEditableLineNode(lineContent, index);
        issuesEditContainer.getChildren().add(node);
        originalPositions.put(index, issuesEditContainer.getChildren().size() - 1);
    }

    private Node createEditableLineNode(String lineContent, int index) {
        LineParser.LineSplitResult result = LineParser.splitEditablePortion(lineContent);
        String fixed = result.getFixedPrefix();
        String editable = result.getEditableSuffix();

        TextField editableField = new TextField(editable);
        editableField.setPromptText("Edit here / Edite aqui");
        editableField.textProperty().addListener((obs, oldVal, newVal) ->
                editedLineIndexes.put(index, fixed + newVal));

        // Make text field expand to fill available space
        HBox.setHgrow(editableField, Priority.ALWAYS);
        editableField.setMaxWidth(Double.MAX_VALUE);

        Label fixedLabel = new Label(fixed);
        fixedLabel.setStyle("-fx-background-color: #f0f0f0; -fx-padding: 4 6 4 6;");
        fixedLabel.setMinWidth(Region.USE_PREF_SIZE);

        Button removeBtn = new Button("X");
        removeBtn.setOnAction(e -> {
            // Get the VBox containing this row
            Node parentNode = removeBtn.getParent().getParent();

            // Find the position in the container
            int position = issuesEditContainer.getChildren().indexOf(parentNode);

            // Create an undo operation with position information
            undoStack.push(new UndoOperation(index, lineContent, position));

            // Remove the node and mark the line as removed
            issuesEditContainer.getChildren().remove(parentNode);
            removedLineIndexes.add(index);

            undoButton.setDisable(false);
        });

        HBox row = new HBox(5, removeBtn, fixedLabel, editableField);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setUserData(index); // Set user data for the row

        VBox box = new VBox(3, row);
        box.setPadding(new Insets(5));
        box.setStyle("-fx-border-color: #ccc; -fx-border-radius: 3px;");
        box.setUserData(index); // Set user data for the container too

        return box;
    }

    private void undoLast() {
        if (undoStack.isEmpty()) {
            undoButton.setDisable(true);
            return;
        }

        // Get the last removed operation with position information
        UndoOperation lastOp = undoStack.pop();
        int lastIndex = lastOp.getLineIndex();
        int position = lastOp.getPosition();
        String content = lastOp.getContent();

        // Remove from removed set
        removedLineIndexes.remove(lastIndex);

        // Create a new node
        Node nodeToRestore = createEditableLineNode(content, lastIndex);

        // Insert at the exact position it was removed from
        issuesEditContainer.getChildren().add(
                Math.min(position, issuesEditContainer.getChildren().size()),
                nodeToRestore
        );

        undoButton.setDisable(undoStack.isEmpty());
    }

    public int getMissingDaysCount() {
        return missingDaysCount;
    }
}