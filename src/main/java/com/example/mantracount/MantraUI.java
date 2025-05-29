package com.example.mantracount;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.util.List;

public class MantraUI extends Application {

    private Stage primaryStage;
    private final MantraData mantraData = new MantraData();

    private DateRangeController dateRangeController;
    private FileManagementController fileController;
    private MantrasDisplayController displayController;
    private SearchController searchController;

    private Button processButton;
    private Button clearResultsButton;
    private Button checkMissingDaysButton;
    private Button allMantrasButton;
    private Button saveButton;
    private Button cancelButton;
    private Button updateButton;
    private Button semFizButton;
    private TextField mantraField;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        UpdateChecker.checkForUpdates();
        this.primaryStage = primaryStage;
        primaryStage.setTitle("MantraCount");

        initializeControllers();
        VBox root = createMainLayout();
        applyThemeColors(root);
        setupEventHandlers();

        Scene scene = new Scene(root, 710, 420);
        primaryStage.setScene(scene);
        primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("/icons/BUDA.jpg")));
        primaryStage.show();

        Platform.runLater(this::configureMismatchPanel);

        primaryStage.setOnCloseRequest(event -> {
            AutoUpdater.shutdown();
            displayController.shutdown();
        });
    }

    private void applyThemeColors(VBox root) {
        root.setStyle(UIColorScheme.getMainBackgroundStyle());
    }

    private void initializeControllers() {
        dateRangeController = new DateRangeController();
        displayController = new MantrasDisplayController(mantraData);
        fileController = new FileManagementController(
                primaryStage,
                mantraData,
                displayController.getMismatchesContainer(),
                displayController.getPlaceholder(),
                displayController.getResultsArea()
        );

        searchController = new SearchController(
                displayController.getMismatchesContainer(),
                displayController.getMismatchesScrollPane()
        );

        setupMismatchPanelListener();
        setupWindowStateListeners();
    }

    private VBox createMainLayout() {
        VBox root = new VBox();
        root.setPadding(new Insets(20));
        root.setStyle(UIColorScheme.getMainBackgroundStyle());

        VBox mainContentArea = new VBox(10);

        mantraField = UIComponentFactory.TextFields.createMantraField();

        createActionButtons();

        HBox processBox = new HBox(10, processButton, clearResultsButton,
                checkMissingDaysButton, allMantrasButton, semFizButton);
        processBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        HBox resultsWithImage = new HBox(10);
        resultsWithImage.getChildren().addAll(
                displayController.getResultsArea(),
                displayController.getImageController().getImageView()
        );
        HBox.setHgrow(displayController.getResultsArea(), Priority.ALWAYS);

        mainContentArea.getChildren().addAll(
                dateRangeController.getDatePickerContainer(),
                mantraField,
                fileController.getFileControlContainer(),
                resultsWithImage,
                processBox,
                searchController.getSearchContainer(),
                displayController.getMismatchesScrollPane()
        );

        HBox bottomButtonArea = createBottomButtonArea();
        VBox.setVgrow(displayController.getMismatchesScrollPane(), Priority.NEVER);
        root.getChildren().addAll(mainContentArea, bottomButtonArea);

        return root;
    }

    private HBox createBottomButtonArea() {
        saveButton = UIComponentFactory.ActionButtons.createSaveButton();
        cancelButton = UIComponentFactory.ActionButtons.createCancelButton();
        updateButton = UIComponentFactory.ActionButtons.createUpdateButton();

        Label updateLabel = new Label(StringConstants.UPDATE_LABEL_PT);
        updateLabel.setStyle(UIColorScheme.getFieldLabelStyle());
        UIComponentFactory.addTooltip(updateLabel, StringConstants.UPDATE_TOOLTIP_EN);

        return UIComponentFactory.Layouts.createMainActionLayout(saveButton, cancelButton, updateButton, updateLabel);
    }

    private void createActionButtons() {
        processButton = UIComponentFactory.ActionButtons.createProcessButton();
        clearResultsButton = UIComponentFactory.ActionButtons.createClearButton();
        checkMissingDaysButton = UIComponentFactory.ActionButtons.createMissingDaysButton();
        checkMissingDaysButton.setDisable(true);

        allMantrasButton = UIComponentFactory.ActionButtons.createAllMantrasButton();
        allMantrasButton.setDisable(true);

        semFizButton = UIComponentFactory.ActionButtons.createSemFizButton();
        semFizButton.setDisable(true);
    }

    private void setupEventHandlers() {
        processButton.setOnAction(e -> processFile());
        clearResultsButton.setOnAction(e -> clearResults());
        saveButton.setOnAction(e -> saveChanges());
        cancelButton.setOnAction(e -> cancelChanges());
        updateButton.setOnAction(e -> AutoUpdater.checkForUpdatesManually());
        checkMissingDaysButton.setOnAction(e -> showMissingDays());
        allMantrasButton.setOnAction(e -> showAllMantras());
        semFizButton.setOnAction(e -> showSemFizAnalysis());
    }

    private void processFile() {
        try {
            if (!validateInputs()) return;

            setMantraData();

            if (!fileController.ensureFileLoaded()) return;

            mantraData.resetCounts();
            FileProcessorService.processFile(mantraData);

            displayController.displayResults();
            displayController.displayMismatchedLines(mantraData.getDebugLines());

            boolean hasMismatches = mantraData.getDebugLines() != null && !mantraData.getDebugLines().isEmpty();
            adjustWindowSize(hasMismatches);

            updateButtonStates();

        } catch (Exception ex) {
            ex.printStackTrace();
            UIUtils.showError("Error processing file: " + ex.getMessage(),
                    "Erro ao processar arquivo: " + ex.getMessage());
        }
    }

    private boolean validateInputs() {
        if (!dateRangeController.validateStartDate()) return false;

        // FIXED: Validate mantra field properly
        String mantraText = mantraField.getText();
        if (mantraText == null || mantraText.trim().isEmpty()) {
            UIUtils.showError("Missing or invalid field: Mantra name",
                    "Campo ausente ou inválido: Nome do mantra");
            return false;
        }

        if (!fileController.validateFilePath()) return false;
        return true;
    }

    private void setMantraData() {
        mantraData.setTargetDate(dateRangeController.getStartDate());
        mantraData.setNameToCount(mantraField.getText().trim());
    }

    private void updateButtonStates() {
        try {
            List<MissingDaysDetector.MissingDayInfo> missingDays =
                    MissingDaysDetector.detectMissingDays(
                            mantraData.getLines(),
                            mantraData.getTargetDate(),
                            mantraData.getNameToCount()
                    );

            checkMissingDaysButton.setDisable(missingDays.isEmpty());
            allMantrasButton.setDisable(false);

            boolean hasMissingFiz = MissingFizAnalyzer.hasMissingFizLines(
                    mantraData.getLines(),
                    mantraData.getTargetDate(),
                    mantraData.getNameToCount()
            );
            semFizButton.setDisable(!hasMissingFiz);

        } catch (Exception ex) {
            checkMissingDaysButton.setDisable(true);
            semFizButton.setDisable(true);
            System.err.println("Error updating button states: " + ex.getMessage());
        }
    }

    private void clearResults() {
        displayController.resetDisplay();
        searchController.resetSearchState();
        checkMissingDaysButton.setDisable(true);
        allMantrasButton.setDisable(true);
        semFizButton.setDisable(true);
    }

    private void saveChanges() {
        if (mantraData.getLines() == null || displayController.getMismatchedLines() == null) {
            UIUtils.showError("No file loaded or processed", "Nenhum arquivo carregado ou processado");
            return;
        }

        boolean success = fileController.saveChanges(displayController.extractUpdatedContentFromUI());
        if (success) {
            displayController.backupOriginalLines();
            searchController.resetSearchState();
        }
    }

    private void cancelChanges() {
        displayController.revertToOriginalLines();
        searchController.resetSearchState();
    }

    private void showMissingDays() {
        try {
            MissingDaysUI missingDaysUI = new MissingDaysUI();
            Runnable updateButtonCallback = this::updateMissingDaysButtonState;
            missingDaysUI.show(primaryStage, mantraData, updateButtonCallback);
        } catch (Exception ex) {
            ex.printStackTrace();
            UIUtils.showError("Error checking missing days: " + ex.getMessage(),
                    "Erro ao verificar dias faltantes: " + ex.getMessage());
        }
    }

    private void showAllMantras() {
        try {
            if (mantraData.getLines() == null || mantraData.getLines().isEmpty()) {
                UIUtils.showError("No file loaded. Please load a file first",
                        "Nenhum arquivo carregado. Por favor, carregue um arquivo primeiro");
                return;
            }

            LocalDate startDate = dateRangeController.getStartDate();
            if (startDate == null) {
                startDate = findEarliestDateInFile();
                if (startDate == null) {
                    startDate = LocalDate.now().minusDays(30);
                }
            }

            AllMantrasUI allMantrasUI = new AllMantrasUI();
            allMantrasUI.show(primaryStage, mantraData, startDate);

        } catch (Exception ex) {
            ex.printStackTrace();
            UIUtils.showError("Error showing all mantras: " + ex.getMessage(),
                    "Erro ao mostrar todos os mantras: " + ex.getMessage());
        }
    }

    private void showSemFizAnalysis() {
        try {
            MissingFizUI missingFizUI = new MissingFizUI();
            Runnable updateButtonCallback = this::updateSemFizButtonState;
            missingFizUI.show(primaryStage, mantraData, updateButtonCallback);
        } catch (Exception ex) {
            ex.printStackTrace();
            UIUtils.showError("Error in missing fiz analysis: " + ex.getMessage(),
                    "Erro na análise sem fiz: " + ex.getMessage());
        }
    }

    private void updateMissingDaysButtonState() {
        try {
            List<MissingDaysDetector.MissingDayInfo> missingDays =
                    MissingDaysDetector.detectMissingDays(
                            mantraData.getLines(),
                            mantraData.getTargetDate(),
                            mantraData.getNameToCount()
                    );
            Platform.runLater(() -> checkMissingDaysButton.setDisable(missingDays.isEmpty()));
        } catch (Exception ex) {
            Platform.runLater(() -> checkMissingDaysButton.setDisable(true));
        }
    }

    private void updateSemFizButtonState() {
        try {
            boolean hasMissingFiz = MissingFizAnalyzer.hasMissingFizLines(
                    mantraData.getLines(),
                    mantraData.getTargetDate(),
                    mantraData.getNameToCount()
            );
            Platform.runLater(() -> semFizButton.setDisable(!hasMissingFiz));
        } catch (Exception ex) {
            Platform.runLater(() -> semFizButton.setDisable(true));
        }
    }

    private LocalDate findEarliestDateInFile() {
        LocalDate earliestDate = null;
        for (String line : mantraData.getLines()) {
            LocalDate lineDate = LineParser.extractDate(line);
            if (lineDate != null && (earliestDate == null || lineDate.isBefore(earliestDate))) {
                earliestDate = lineDate;
            }
        }
        return earliestDate;
    }

    private void setupMismatchPanelListener() {
        displayController.getMismatchesScrollPane().expandedProperty().addListener(
                (obs, wasExpanded, isExpanded) -> adjustWindowSizeForMismatchPanel(isExpanded));
    }

    private void setupWindowStateListeners() {
        primaryStage.maximizedProperty().addListener((obs, wasMaximized, isMaximized) -> {
            TitledPane mismatchPanel = displayController.getMismatchesScrollPane();
            mismatchPanel.setExpanded(false);

            Platform.runLater(() -> {
                mismatchPanel.requestLayout();
                mismatchPanel.autosize();
            });

            if (isMaximized) {
                Platform.runLater(() -> {
                    boolean wasExpanded = mismatchPanel.isExpanded();
                    mismatchPanel.setExpanded(true);
                    Platform.runLater(() -> {
                        mismatchPanel.setExpanded(wasExpanded);
                        mismatchPanel.requestLayout();
                        mismatchPanel.autosize();
                    });
                });
            }
        });

        primaryStage.iconifiedProperty().addListener((obs, wasIconified, isIconified) -> {
            if (isIconified) {
                TitledPane mismatchPanel = displayController.getMismatchesScrollPane();
                mismatchPanel.setExpanded(false);

                Platform.runLater(() -> {
                    mismatchPanel.requestLayout();
                    mismatchPanel.autosize();
                });
            }
        });
    }

    private void configureMismatchPanel() {
        TitledPane mismatchPanel = displayController.getMismatchesScrollPane();
        mismatchPanel.setPrefHeight(25);
        mismatchPanel.setMinHeight(25);
        mismatchPanel.setMaxHeight(25);
        VBox.setVgrow(mismatchPanel, Priority.NEVER);
    }







    private void adjustWindowSize(boolean hasMismatches) {
        if (hasMismatches) {
            double currentHeight = primaryStage.getHeight();
            double newHeight = Math.max(currentHeight, 580);
            primaryStage.setHeight(newHeight);
        } else {
            primaryStage.setHeight(460);
        }
    }


    private void adjustWindowSizeForMismatchPanel(boolean isExpanded) {
        TitledPane mismatchPanel = displayController.getMismatchesScrollPane();

        if (isExpanded && primaryStage.isMaximized()) {
            primaryStage.setHeight(Math.max(primaryStage.getHeight(), 780));
            mismatchPanel.setPrefHeight(250);
            mismatchPanel.setMaxHeight(250);
            mismatchPanel.setMinHeight(250);
        } else if(isExpanded) {
            mismatchPanel.setPrefHeight(170);
            mismatchPanel.setMaxHeight(170);
            mismatchPanel.setMinHeight(170);
            primaryStage.setHeight(Math.max(primaryStage.getHeight(), 600));
        } else {
            primaryStage.setHeight(460);
        }
    }
}