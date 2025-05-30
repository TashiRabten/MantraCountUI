package com.example.mantracount;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Refactored Missing Days UI using centralized components and consistent styling.
 * Eliminates code duplication and provides consistent user experience.
 */
public class MissingDaysUI {
    private final Map<Integer, String> editedLineIndexes = new HashMap<>();
    private final Set<Integer> removedLineIndexes = new HashSet<>();
    private final Deque<UndoOperation> undoStack = new ArrayDeque<>();
    private final Map<Integer, Integer> originalPositions = new HashMap<>();
    private final Map<Integer, Integer> contextToActualLineMap = new HashMap<>();

    private VBox issuesEditContainer;
    private Button undoButton;
    private ProgressIndicator progressIndicator;
    private ScrollPane scroll;
    private int missingDaysCount = 0;
    private List<String> allLines;
    private MissingDaysDetector.MissingDayInfo currentMissingInfo;
    private Runnable onCloseCallback;

    private static class UndoOperation {
        private final int lineIndex;
        private final String content;
        private final int position;

        public UndoOperation(int lineIndex, String content, int position) {
            this.lineIndex = lineIndex;
            this.content = content;
            this.position = position;
        }

        public int getLineIndex() { return lineIndex; }
        public String getContent() { return content; }
        public int getPosition() { return position; }
    }

    public void show(Stage owner, MantraData data) {
        this.show(owner, data, null);
    }

    public void show(Stage owner, MantraData data, Runnable onCloseCallback) {
        this.onCloseCallback = onCloseCallback;

        Stage dialog = createDialog(owner);
        VBox root = createMainLayout(dialog, data);
        applyThemeColors(root);


        dialog.setScene(new Scene(root, 800, 600));
        dialog.show();

        loadMissingDaysAsync(data);
    }

    private void applyThemeColors(VBox root) {
        // Use the centralized background color
        root.setStyle(UIColorScheme.getMainBackgroundStyle());
    }
    /**
     * Creates the main dialog window
     */
    private Stage createDialog(Stage owner) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(owner);

        String formatInfo = DateParser.getCurrentDateFormat() == DateParser.DateFormat.BR_FORMAT ?
                "BR (DD/MM/AA)" : "US (MM/DD/AA)";
        dialog.setTitle(StringConstants.MISSING_DAYS_TITLE + " [" + formatInfo + "]");

        InputStream stream = getClass().getResourceAsStream("/icons/BUDA.png");
        if (stream != null) {
            System.out.println("Image found!");
            ImageView iconView = new ImageView(new Image(stream));
            iconView.setFitWidth(256);
            iconView.setFitHeight(256);
            dialog.getIcons().add(iconView.getImage());
        } else {
            System.out.println("Image not found: /icons/BUDA.png");
        }

        dialog.setOnCloseRequest(e -> {
            if (onCloseCallback != null) {
                onCloseCallback.run();
            }
        });

        return dialog;
    }

    /**
     * Creates the main layout using factory components
     */
    private VBox createMainLayout(Stage dialog, MantraData data) {
        VBox root = new VBox(10);
        root.setStyle(UIColorScheme.getMainBackgroundStyle());
        root.setPadding(new Insets(15));

        Label header = UIComponentFactory.createHeaderLabel(
                StringConstants.MISSING_DAYS_TITLE.replace("Análise de ", ""),
                "Missing Days Analysis - Shows days where no mantra entries were found"
        );

        progressIndicator = UIComponentFactory.createProgressIndicator();

        ListView<String> missingList = createMissingDaysList(data);

        undoButton = UIComponentFactory.ActionButtons.createUndoButton();
        undoButton.setDisable(true);
        undoButton.setOnAction(e -> undoLast());
        issuesEditContainer = new VBox(0);
        issuesEditContainer.setStyle(UIColorScheme.getResultsAreaStyle());
        issuesEditContainer.setFillWidth(true);


        scroll = UIComponentFactory.createStyledScrollPane(issuesEditContainer);
        scroll.setStyle(UIColorScheme.getResultsAreaStyle());
        scroll.setFitToHeight(true);
        scroll.prefHeightProperty().bind(root.heightProperty().multiply(0.7));

        VBox.setVgrow(scroll, Priority.ALWAYS);

        UIComponentFactory.addTooltip(scroll,
                "Edit Area - Make changes to entries around missing days. Use 'X' to remove entries.");

        HBox actions = createActionButtons(dialog, data);

        root.getChildren().addAll(header, progressIndicator, missingList, undoButton, scroll, actions);

        return root;
    }

    /**
     * Creates the missing days list view
     */
    private ListView<String> createMissingDaysList(MantraData data) {
        ListView<String> missingList = new ListView<>();
        missingList.setStyle(UIColorScheme.getResultsAreaStyle());
        UIComponentFactory.addTooltip(missingList,
                "Missing Days List - Click on a date to see surrounding entries for editing");

        missingList.getSelectionModel().selectedItemProperty().addListener((obs, ov, nv) -> {
            int idx = missingList.getSelectionModel().getSelectedIndex();
            if (idx >= 0) {
                // This will be set when the async operation completes
                if (currentMissingInfo != null) {
                    showEditableLinesAround(data, currentMissingInfo.getDate());
                }
            }
        });

        return missingList;
    }

    /**
     * Creates action buttons using factory with dialog alignment
     */
    private HBox createActionButtons(Stage dialog, MantraData data) {
        Button saveBtn = UIComponentFactory.ActionButtons.createApplyChangesButton();
        saveBtn.setOnAction(e -> applyEditsAsync(data));

        Button closeBtn = UIComponentFactory.ActionButtons.createCloseButton();
        closeBtn.setOnAction(e -> {
            if (onCloseCallback != null) {
                onCloseCallback.run();
            }
            dialog.close();
        });

        return UIComponentFactory.Layouts.createDialogActionLayout(saveBtn, closeBtn);
    }

    /**
     * Loads missing days asynchronously
     */
    private void loadMissingDaysAsync(MantraData data) {
        this.allLines = new ArrayList<>(data.getLines());
        progressIndicator.setVisible(true);

        CompletableFuture.supplyAsync(() ->
                MissingDaysDetector.detectMissingDays(allLines, data.getTargetDate(), data.getNameToCount())
        ).thenAccept(result -> Platform.runLater(() -> {
            List<MissingDaysDetector.MissingDayInfo> missingDays = new ArrayList<>(result);
            missingDaysCount = missingDays.size();

            if (missingDaysCount == 0) {
                UIUtils.showNoMissingDaysSuccess();
                ((Stage) progressIndicator.getScene().getWindow()).close();
                return;
            }

            populateMissingDaysList(missingDays, data);
            progressIndicator.setVisible(false);
        }));
    }

    /**
     * Populates the missing days list
     */
    private void populateMissingDaysList(List<MissingDaysDetector.MissingDayInfo> missingDays, MantraData data) {
        ListView<String> missingList = findMissingListView();
        if (missingList == null) return;

        List<String> items = new ArrayList<>();
        for (MissingDaysDetector.MissingDayInfo info : missingDays) {
            items.add(StringConstants.MISSING_DAY_PT + " " + DateFormatUtils.formatShortDate(info.getDate()));
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
    }

    /**
     * Finds the missing list view in the scene graph
     */
    @SuppressWarnings("unchecked")
    private ListView<String> findMissingListView() {
        if (progressIndicator.getScene() != null) {
            VBox root = (VBox) progressIndicator.getScene().getRoot();
            for (Node child : root.getChildren()) {
                if (child instanceof ListView) {
                    return (ListView<String>) child;
                }
            }
        }
        return null;
    }

    /**
     * Shows editable lines around the missing date
     */
    private void showEditableLinesAround(MantraData data, LocalDate centerDate) {
        editedLineIndexes.clear();
        removedLineIndexes.clear();
        undoStack.clear();
        originalPositions.clear();
        contextToActualLineMap.clear();
        issuesEditContainer.getChildren().clear();
        undoButton.setDisable(true);

        List<Integer> actualLineIndices = findActualLineIndices(data.getLines(), centerDate);
        List<String> contextLines = new ArrayList<>();

        for (int i = 0; i < actualLineIndices.size(); i++) {
            int actualIndex = actualLineIndices.get(i);
            if (actualIndex >= 0 && actualIndex < data.getLines().size()) {
                String line = data.getLines().get(actualIndex);
                contextLines.add(line);
                contextToActualLineMap.put(i, actualIndex);
            }
        }

        for (int i = 0; i < contextLines.size(); i++) {
            String line = contextLines.get(i);
            addEditableLineNode(line, i);
        }
    }

    /**
     * Finds actual line indices in the file
     */
    private List<Integer> findActualLineIndices(List<String> allLines, LocalDate centerDate) {
        List<Integer> actualIndices = new ArrayList<>();

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

        actualIndices.sort(Comparator.naturalOrder());
        return actualIndices;
    }

    /**
     * Adds an editable line node to the container
     */
    private void addEditableLineNode(String lineContent, int index) {
        Node node = createEditableLineNode(lineContent, index);
        issuesEditContainer.getChildren().add(node);
        originalPositions.put(index, issuesEditContainer.getChildren().size() - 1);
    }

    /**
     * Creates an editable line node using factory components
     */
    private Node createEditableLineNode(String lineContent, int index) {
        LineParser.LineSplitResult result = LineParser.splitEditablePortion(lineContent);
        String fixed = result.getFixedPrefix();
        String editable = result.getEditableSuffix();

        TextField editableField = UIComponentFactory.TextFields.createEditLineField(editable);
        editableField.setPromptText("Edite aqui");
        editableField.textProperty().addListener((obs, oldVal, newVal) ->
                editedLineIndexes.put(index, fixed + newVal));

        HBox.setHgrow(editableField, Priority.ALWAYS);
        editableField.setMaxWidth(Double.MAX_VALUE);

        Label fixedLabel = new Label(fixed);
        fixedLabel.setStyle(        UIColorScheme.getFieldLabelStyle()
        /*"-fx-background-color: #f0f0f0; -fx-padding: 4 6 4 6;"*/);
        fixedLabel.setMinWidth(Region.USE_PREF_SIZE);
        UIComponentFactory.addTooltip(fixedLabel, StringConstants.PROTECTED_CONTENT_TOOLTIP);

        Button removeBtn = new Button("X");
        removeBtn.setFocusTraversable(false);
        UIComponentFactory.addTooltip(removeBtn, StringConstants.REMOVE_TOOLTIP);

        HBox row = new HBox(5, removeBtn, fixedLabel, editableField);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setUserData(index);

        VBox box = new VBox(0, row);
        box.setStyle(UIColorScheme.getResultsContainerStyle());
        box.setUserData(index);

        removeBtn.setOnAction(e -> handleRemoveAction(box, index, lineContent));

        return box;
    }

    /**
     * Handles remove button action
     */
    private void handleRemoveAction(VBox box, int index, String lineContent) {
        double currentScrollPosition = scroll.getVvalue();
        int position = issuesEditContainer.getChildren().indexOf(box);

        undoStack.push(new UndoOperation(index, lineContent, position));
        issuesEditContainer.getChildren().remove(box);
        removedLineIndexes.add(index);

        Platform.runLater(() -> {
            undoButton.setDisable(false);
            scroll.setVvalue(currentScrollPosition);
        });
    }

    /**
     * Undoes the last removal operation
     */
    private void undoLast() {
        if (undoStack.isEmpty()) {
            undoButton.setDisable(true);
            return;
        }

        double currentScrollPosition = scroll.getVvalue();
        Node currentFocus = scroll.getScene().getFocusOwner();

        UndoOperation lastOp = undoStack.pop();
        int lastIndex = lastOp.getLineIndex();
        int position = lastOp.getPosition();
        String content = lastOp.getContent();

        removedLineIndexes.remove(lastIndex);
        Node nodeToRestore = createEditableLineNode(content, lastIndex);

        issuesEditContainer.getChildren().add(
                Math.min(position, issuesEditContainer.getChildren().size()),
                nodeToRestore
        );

        boolean wasLastUndo = undoStack.isEmpty();

        Platform.runLater(() -> {
            undoButton.setDisable(wasLastUndo);
            scroll.setVvalue(currentScrollPosition);

            if (currentFocus != null && !wasLastUndo) {
                currentFocus.requestFocus();
            } else if (wasLastUndo) {
                scroll.requestFocus();
            }

            Platform.runLater(() -> scroll.setVvalue(currentScrollPosition));
        });
    }

    /**
     * Applies edits asynchronously
     */
    private void applyEditsAsync(MantraData data) {
        progressIndicator.setVisible(true);

        CompletableFuture.runAsync(() -> {
            try {
                List<String> originalLines = new ArrayList<>(data.getLines());
                List<String> updatedLines = new ArrayList<>(originalLines);

                for (Map.Entry<Integer, String> edit : editedLineIndexes.entrySet()) {
                    int contextIndex = edit.getKey();
                    String editedContent = edit.getValue();

                    Integer actualLineIndex = contextToActualLineMap.get(contextIndex);
                    if (actualLineIndex != null && actualLineIndex >= 0 && actualLineIndex < updatedLines.size()) {
                        updatedLines.set(actualLineIndex, editedContent);
                    }
                }

                if (isContentUnchanged(updatedLines, originalLines)) {
                    Platform.runLater(() -> {
                        progressIndicator.setVisible(false);
                        UIUtils.showNoChangesInfo();
                    });
                    return;
                }

                saveChangesToFile(data, updatedLines);

            } catch (Exception e) {
                Platform.runLater(() -> {
                    progressIndicator.setVisible(false);
                    UIUtils.showError("Error: " + e.getMessage(), "Erro: " + e.getMessage());
                });
            }
        });
    }

    /**
     * Checks if content has changed
     */
    private boolean isContentUnchanged(List<String> updatedLines, List<String> originalLines) {
        if (updatedLines.size() != originalLines.size()) {
            return false;
        }

        for (int i = 0; i < updatedLines.size(); i++) {
            if (!updatedLines.get(i).equals(originalLines.get(i))) {
                return false;
            }
        }

        return true;
    }

    /**
     * Saves changes to file
     */
    private void saveChangesToFile(MantraData data, List<String> updatedLines) {
        try {
            FileEditSaver.saveToFile(updatedLines, data.getFilePath());

            if (data.isFromZip()) {
                FileEditSaver.updateZipFile(
                        data.getOriginalZipPath(),
                        data.getFilePath(),
                        updatedLines,
                        data.getOriginalZipEntryName()
                );
            }

            data.setLines(updatedLines);

            Platform.runLater(() -> {
                progressIndicator.setVisible(false);
                UIUtils.showFileSavedSuccess();

                if (onCloseCallback != null) {
                    onCloseCallback.run();
                }
            });
        } catch (IOException e) {
            Platform.runLater(() -> {
                progressIndicator.setVisible(false);
                UIUtils.showFileSaveError();
            });
        }
    }

    public int getMissingDaysCount() {
        return missingDaysCount;
    }
}