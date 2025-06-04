package com.example.mantracount;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;
import java.io.InputStream;
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
        searchController.adaptToContainerStructure(false);
        BorderPane root = createMainLayout();
        applyThemeColors(root);
        setupEventHandlers();

        Scene scene = new Scene(root, 710, 420);
        primaryStage.setScene(scene);

        InputStream stream = getClass().getResourceAsStream("/icons/BUDA.png");
        if (stream != null) {
            System.out.println("Image found!");
            Image icon = new Image(stream);  // Create image first
            System.out.println("Icon size: " + icon.getWidth() + "x" + icon.getHeight());

            primaryStage.getIcons().add(icon);  // Add the same image object
        } else {
            System.out.println("Image not found: /icons/BUDA.png");
        }


        primaryStage.show();

        Platform.runLater(this::configureMismatchPanel);

        primaryStage.setOnCloseRequest(event -> {
            AutoUpdater.shutdown();
            displayController.shutdown();
        });
    }

    private void applyThemeColors(BorderPane root) {
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

    private BorderPane createMainLayout() {
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(20));
        root.setStyle(UIColorScheme.getMainBackgroundStyle());

        // TOP SECTION: All your content including mismatch panel
        VBox topContent = new VBox(10);
        TitledPane mismatchPanel = displayController.getMismatchesScrollPane();

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

        topContent.getChildren().addAll(
                dateRangeController.getDatePickerContainer(),
                mantraField,
                fileController.getFileControlContainer(),
                resultsWithImage,
                processBox,
                searchController.getSearchContainer(),
                mismatchPanel
        );

        // IMPORTANT: Set VBox to fill available height
        VBox.setVgrow(topContent, Priority.ALWAYS);

        // BOTTOM SECTION: Bottom buttons
        HBox bottomButtonArea = createBottomButtonArea();

        // Set up the BorderPane
        root.setTop(topContent);
        root.setBottom(bottomButtonArea);

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
            UIUtils.showError(StringConstants.ERROR_PROCESSING_FILE_EN + ex.getMessage(),
                    StringConstants.ERROR_PROCESSING_FILE_PT + ex.getMessage());
        }
    }

    private boolean validateInputs() {
        if (!dateRangeController.validateStartDate()) return false;

        // FIXED: Validate mantra field properly
        String mantraText = mantraField.getText();
        if (mantraText == null || mantraText.trim().isEmpty()) {
            UIUtils.showError(StringConstants.MISSING_MANTRA_FIELD_EN,
                    StringConstants.MISSING_MANTRA_FIELD_PT);
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
        TitledPane mismatchPanel = displayController.getMismatchesScrollPane();

        // Set up Mac-specific expansion prevention listener once
        if (System.getProperty("os.name").toLowerCase().contains("mac")) {
            mismatchPanel.expandedProperty().addListener((observable, oldValue, newValue) -> {
                if (primaryStage.isMaximized() && !newValue) {
                    Platform.runLater(() -> mismatchPanel.setExpanded(true));
                }
            });
        }

        primaryStage.maximizedProperty().addListener((obs, wasMaximized, isMaximized) -> {
            mismatchPanel.setExpanded(false);

            Platform.runLater(() -> {
                mismatchPanel.requestLayout();
                mismatchPanel.autosize();
            });

            if (isMaximized) {
                Platform.runLater(() -> {
                    boolean wasExpanded = mismatchPanel.isExpanded(); // Always false due to initial collapse
                    mismatchPanel.setExpanded(true); // Get measurements
                    Platform.runLater(() -> {
                        if (!System.getProperty("os.name").toLowerCase().contains("mac")) {
                            mismatchPanel.setExpanded(wasExpanded); // Windows: false (compact)
                        } else {
                            // Mac: Only expand if we were coming from windowed state
                            if (!wasMaximized) {
                                mismatchPanel.setExpanded(true); // Windowed → Maximized: expand
                            } else {
                                mismatchPanel.setExpanded(false); // Stay collapsed if already maximized
                            }
                        }
                        mismatchPanel.requestLayout();
                        mismatchPanel.autosize();
                    });
                });
            }
        });

        primaryStage.iconifiedProperty().addListener((obs, wasIconified, isIconified) -> {
            if (isIconified) {
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


    private double storedSceneHeight = 0;

    private void adjustWindowSizeForMismatchPanel(boolean isExpanded) {
        TitledPane mismatchPanel = displayController.getMismatchesScrollPane();

        if (isExpanded && primaryStage.isMaximized() || isExpanded && primaryStage.isFullScreen()) {
            Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
            primaryStage.setHeight(screenBounds.getHeight());

            Platform.runLater(() -> {
                Scene scene = primaryStage.getScene();
                BorderPane root = (BorderPane) scene.getRoot();
                VBox topContent = (VBox) root.getTop();

                // Calculate current measurements
                double currentSceneHeight = scene.getHeight();

                double currentTopSectionHeight = 0;
                boolean foundMismatchPanel = false;

                for (Node child : topContent.getChildren()) {
                    if (child == mismatchPanel) {
                        foundMismatchPanel = true;
                        break;
                    }
                    child.autosize();
                    currentTopSectionHeight += child.getBoundsInLocal().getHeight();
                }

                if (foundMismatchPanel) {
                    int componentsAbove = topContent.getChildren().indexOf(mismatchPanel);
                    currentTopSectionHeight += (componentsAbove * 10);
                }

                Node bottomSection = root.getBottom();
                bottomSection.autosize();
                double currentBottomSectionHeight = bottomSection.getBoundsInLocal().getHeight();

                Insets borderPaneInsets = root.getInsets();
                double currentBorderPanePadding = borderPaneInsets.getTop() + borderPaneInsets.getBottom();

                Insets vboxInsets = topContent.getInsets();
                double currentVboxPadding = vboxInsets.getTop() + vboxInsets.getBottom();

                // Store MAX values (keep the best measurements)
                storedSceneHeight = Math.max(storedSceneHeight, currentSceneHeight);

                // Calculate available space using stored MAX values
                double availableForPanel = storedSceneHeight - currentTopSectionHeight - currentBottomSectionHeight - currentBorderPanePadding - currentVboxPadding;

                VBox.setVgrow(mismatchPanel, Priority.ALWAYS);
                mismatchPanel.setPrefHeight(availableForPanel);
                mismatchPanel.setMaxHeight(Double.MAX_VALUE);
                mismatchPanel.setMinHeight(100);
            });

        } else if(isExpanded) {
            double currentSceneHeight = primaryStage.getHeight();
            double baseSceneHeight = 460;
            double availableSpace = Math.max(currentSceneHeight - baseSceneHeight + 25, 170);

            primaryStage.setHeight(Math.max(currentSceneHeight, baseSceneHeight + availableSpace - 25));

            VBox.setVgrow(mismatchPanel, Priority.SOMETIMES);
            mismatchPanel.setPrefHeight(availableSpace);

        } else {
            // Collapsed - but check if we're in a window state transition
            VBox.setVgrow(mismatchPanel, Priority.NEVER);
            mismatchPanel.setPrefHeight(25);

            // Only set height if not in transition
            Platform.runLater(() -> {
                primaryStage.setHeight(460);
            });
        }        }
    }