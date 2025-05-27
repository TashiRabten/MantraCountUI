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
import javafx.util.Duration;

import java.time.LocalDate;
import java.util.List;

/**
 * Main application class for MantraCount with simple image support.
 * Orchestrates the various controllers and UI components.
 */
public class MantraUI extends Application {

    private Stage primaryStage;
    private final MantraData mantraData = new MantraData();

    // Controllers
    private DateRangeController dateRangeController;
    private FileManagementController fileController;
    private MantrasDisplayController displayController;
    private SearchController searchController;

    // UI components
    private Button processButton;
    private Button clearResultsButton;
    private Button checkMissingDaysButton;
    private Button allMantrasButton;
    private Button saveButton;
    private Button cancelButton;
    private TextField mantraField;
    private Button semFizButton;
    private VBox mainContentArea;
    private HBox bottomButtonArea;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        UpdateChecker.checkForUpdates();
        this.primaryStage = primaryStage;
        primaryStage.setTitle("MantraCount");

        // Initialize controllers
        initializeControllers();

        // Create and arrange the UI
        VBox root = createMainLayout();

        // Set up event handlers
        setupEventHandlers();

        // Configure and show the primary stage
        Scene scene = new Scene(root, 710, 405);
        primaryStage.setScene(scene);
        primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("/icons/BUDA.jpg")));

        primaryStage.show();

        Platform.runLater(() -> {
            TitledPane mismatchPanel = displayController.getMismatchesScrollPane();
            mismatchPanel.setPrefHeight(25);
            mismatchPanel.setMinHeight(25);
            mismatchPanel.setMaxHeight(25);
            VBox.setVgrow(mismatchPanel, Priority.NEVER);
        });

        // Shutdown handler
        primaryStage.setOnCloseRequest(event -> {
            AutoUpdater.shutdown();
            displayController.shutdown();
        });


    }

    /**
     * Initializes the controllers.
     */
    private void initializeControllers() {
        // Create controllers
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

        // Connect mismatch panel expansion to window resizing
        displayController.getMismatchesScrollPane().expandedProperty().addListener((obs, wasExpanded, isExpanded) -> {
            adjustWindowSizeForMismatchPanel(isExpanded);
        });

        // Handle window state changes for optimal layout
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

    /**
     * Creates the main layout of the application.
     *
     * @return The root VBox containing all UI components
     */
    private VBox createMainLayout() {
        VBox root = new VBox();
        root.setPadding(new Insets(20));

        // Create main content area
        mainContentArea = new VBox(10);

        // Mantra name field with Portuguese placeholder and English tooltip
        mantraField = new TextField();
        UIUtils.setPlaceholder(mantraField, "Nome do Mantra ou Rito");

        Tooltip mantraFieldTooltip = new Tooltip("Mantra or Rite Name - Enter the name of the mantra or ritual you want to count");
        mantraFieldTooltip.setShowDelay(Duration.millis(300));
        mantraFieldTooltip.setHideDelay(Duration.millis(100));
        Tooltip.install(mantraField, mantraFieldTooltip);

        // Create buttons with Portuguese text and English tooltips
        processButton = createBilingualButton("📿 Contar Mantras", "Count Mantras");
        processButton.setStyle("-fx-base: #4CAF50; -fx-text-fill: white;");
        addHoverEffect(processButton, "#4CAF50");

        clearResultsButton = createBilingualButton("🗑 Limpar", "Clear Results");
        clearResultsButton.setStyle("-fx-base: #F44336; -fx-text-fill: white;");
        addHoverEffect(clearResultsButton, "#F44336");

        checkMissingDaysButton = createBilingualButton("📅 Dias Faltantes", "Check Missing Days");
        checkMissingDaysButton.setStyle("-fx-base: #2196F3; -fx-text-fill: white;");
        checkMissingDaysButton.setDisable(true);
        addHoverEffect(checkMissingDaysButton, "#2196F3");

        allMantrasButton = createBilingualButton("📊 Todos os Mantras", "View All Mantras");
        allMantrasButton.setStyle("-fx-base: #9C27B0; -fx-text-fill: white;");
        allMantrasButton.setDisable(true);
        addHoverEffect(allMantrasButton, "#9C27B0");

        semFizButton = createBilingualButton("⚠ Sem Fiz", "Missing Fiz Analysis");
        semFizButton.setStyle("-fx-base: #FF9800; -fx-text-fill: white;");
        semFizButton.setDisable(true);
        addHoverEffect(semFizButton, "#FF9800");


        // In your MantraUI.java setupEventHandlers() method, replace the semFizButton handler with:

        semFizButton.setOnAction(e -> {
            try {
                MissingFizUI missingFizUI = new MissingFizUI();

                // Create callback to update button state when dialog closes
                Runnable updateButtonCallback = () -> {
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
                };

                missingFizUI.show(primaryStage, mantraData, updateButtonCallback);
            } catch (Exception ex) {
                ex.printStackTrace();
                UIUtils.showBilingualError(
                        "Error in missing fiz analysis: " + ex.getMessage(),
                        "Erro na análise sem fiz: " + ex.getMessage()
                );
            }
        });


        HBox processBox = new HBox(10, processButton, clearResultsButton, checkMissingDaysButton, allMantrasButton, semFizButton);
        processBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        // Create horizontal container for results + image
        HBox resultsWithImage = new HBox(10);
        resultsWithImage.getChildren().addAll(
                displayController.getResultsArea(),
                displayController.getImageController().getImageView()
        );
        HBox.setHgrow(displayController.getResultsArea(), Priority.ALWAYS);

        // Add tooltips to controller components
        addTooltipsToControllerComponents();

        // Add components to main content area
        mainContentArea.getChildren().addAll(
                dateRangeController.getDatePickerContainer(),
                mantraField,
                fileController.getFileControlContainer(),
                resultsWithImage, // Results area + image
                processBox,
                searchController.getSearchContainer(),
                displayController.getMismatchesScrollPane()
        );

        // Create bottom button area
        bottomButtonArea = createBottomButtonArea();

        // Set up layout priorities
        VBox.setVgrow(displayController.getMismatchesScrollPane(), Priority.NEVER);

        // Add both areas to root
        root.getChildren().addAll(mainContentArea, bottomButtonArea);

        return root;
    }

    /**
     * Creates the bottom button area that stays fixed at the bottom.
     */
    private HBox createBottomButtonArea() {
        // Save and cancel buttons
        saveButton = createBilingualButton("💾 Salvar Alterações", "Save Changes");
        saveButton.setStyle("-fx-base: #4CAF50; -fx-text-fill: white;");
        addHoverEffect(saveButton, "#4CAF50");

        cancelButton = createBilingualButton("❌ Cancelar Alterações", "Cancel Changes");
        cancelButton.setStyle("-fx-base: #F44336; -fx-text-fill: white;");
        addHoverEffect(cancelButton, "#F44336");

        Button updateButton = new Button("🔄");
        updateButton.setStyle("-fx-font-size: 10px; -fx-background-color: #6A1B9A; -fx-text-fill: white;");
        Tooltip updateTooltip = new Tooltip("Check for Updates");
        updateTooltip.setShowDelay(Duration.millis(300));
        Tooltip.install(updateButton, updateTooltip);
        updateButton.setOnAction(e -> AutoUpdater.checkForUpdatesManually());
        addHoverEffect(updateButton, "#6A1B9A");

        Label updateLabel = new Label("Atualizar");
        updateLabel.setStyle("-fx-text-fill: black; -fx-font-size: 12px;");

        Tooltip updateLabelTooltip = new Tooltip("Update - Check for application updates");
        updateLabelTooltip.setShowDelay(Duration.millis(300));
        updateLabelTooltip.setHideDelay(Duration.millis(100));
        Tooltip.install(updateLabel, updateLabelTooltip);

        HBox buttonBox = new HBox(10, saveButton, cancelButton, updateButton, updateLabel);
        buttonBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        buttonBox.setPadding(new Insets(10, 0, 0, 0));

        return buttonBox;
    }

    /**
     * Creates a button with Portuguese text and English tooltip
     */
    private Button createBilingualButton(String portugueseText, String englishTooltip) {
        Button button = new Button(portugueseText);

        Tooltip tooltip = new Tooltip(englishTooltip);
        tooltip.setShowDelay(Duration.millis(300));
        tooltip.setHideDelay(Duration.millis(100));
        Tooltip.install(button, tooltip);

        return button;
    }

    /**
     * Adds hover effect to a button
     */
    private void addHoverEffect(Button button, String originalColor) {
        button.setOnMouseEntered(e -> {
            if (!button.isDisabled()) {
                button.setStyle("-fx-base: derive(" + originalColor + ", -15%); -fx-text-fill: white;");
            }
        });
        button.setOnMouseExited(e -> {
            button.setStyle("-fx-base: " + originalColor + "; -fx-text-fill: white;");
        });
    }

    /**
     * Sets up event handlers for UI components.
     */
    private void setupEventHandlers() {
        // Process button
        processButton.setOnAction(e -> processFile());

        // Clear results button
        clearResultsButton.setOnAction(e -> {
            displayController.resetDisplay();
            searchController.resetSearchState();
            checkMissingDaysButton.setDisable(true);
            allMantrasButton.setDisable(true);
            semFizButton.setDisable(true);

        });

        // Save button
        saveButton.setOnAction(e -> {
            if (mantraData.getLines() == null || displayController.getMismatchedLines() == null) {
                UIUtils.showBilingualError("No file loaded or processed", "Nenhum arquivo carregado ou processado");
                return;
            }

            boolean success = fileController.saveChanges(displayController.extractUpdatedContentFromUI());
            if (success) {
                displayController.backupOriginalLines();
                searchController.resetSearchState();
            }
        });

        // Cancel button
        cancelButton.setOnAction(e -> {
            displayController.revertToOriginalLines();
            searchController.resetSearchState();
        });

        checkMissingDaysButton.setOnAction(e -> {
            try {
                MissingDaysUI missingDaysUI = new MissingDaysUI();

                // Create callback to update button state when dialog closes
                Runnable updateButtonCallback = () -> {
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
                };

                missingDaysUI.show(primaryStage, mantraData, updateButtonCallback);
            } catch (Exception ex) {
                ex.printStackTrace();
                UIUtils.showBilingualError(
                        "Error checking missing days: " + ex.getMessage(),
                        "Erro ao verificar dias faltantes: " + ex.getMessage()
                );
            }
        });

        // All mantras button
        allMantrasButton.setOnAction(e -> showAllMantras());
    }

    /**
     * Processes the file and displays results.
     */
    private void processFile() {
        try {
            // Validate inputs
            if (!dateRangeController.validateStartDate()) return;

            String mantraPlaceholder = "Nome do Mantra ou Rito";
            if (!UIUtils.validateField(mantraField, mantraPlaceholder, "Please enter the Mantra / Por favor, insira o Mantra")) return;

            if (!fileController.validateFilePath()) return;

            // Set data
            mantraData.setTargetDate(dateRangeController.getStartDate());
            mantraData.setNameToCount(mantraField.getText().trim());

            // Ensure file is loaded
            if (!fileController.ensureFileLoaded()) return;

            // Reset counts before processing
            mantraData.resetCounts();

            // Process the file - IMPORTANT: This is where the mantra counting happens
            FileProcessorService.processFile(mantraData);

            // Display results (this will also update the image)
            displayController.displayResults();
            displayController.displayMismatchedLines(mantraData.getDebugLines());

            // Adjust window size based on whether there are mismatches
            boolean hasMismatches = mantraData.getDebugLines() != null && !mantraData.getDebugLines().isEmpty();
            adjustWindowSize(hasMismatches);

            // Check for missing days
            List<MissingDaysDetector.MissingDayInfo> missingDays =
                    MissingDaysDetector.detectMissingDays(
                            mantraData.getLines(),
                            mantraData.getTargetDate(),
                            mantraData.getNameToCount()
                    );

            // Enable/disable the missing days button
            checkMissingDaysButton.setDisable(missingDays.isEmpty());

            // Always enable the all mantras button after processing
            allMantrasButton.setDisable(false);
            semFizButton.setDisable(false);
            allMantrasButton.setDisable(false);

// Check if there are missing fiz cases and enable/disable the semFiz button accordingly
            try {
                boolean hasMissingFiz = MissingFizAnalyzer.hasMissingFizLines(
                        mantraData.getLines(),
                        mantraData.getTargetDate(),
                        mantraData.getNameToCount()
                );
                semFizButton.setDisable(!hasMissingFiz);

                // Debug output
                System.out.println("Missing Fiz check: " + hasMissingFiz + " for keyword: " + mantraData.getNameToCount());
            } catch (Exception ex) {
                // If there's an error checking, disable the button
                semFizButton.setDisable(true);
                System.err.println("Error checking missing fiz: " + ex.getMessage());
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            UIUtils.showBilingualError(
                    "Error processing file: " + ex.getMessage(),
                    "Erro ao processar arquivo: " + ex.getMessage()
            );
        }
    }

    /**
     * Shows the All Mantras feature.
     */
    private void showAllMantras() {
        try {
            if (mantraData.getLines() == null || mantraData.getLines().isEmpty()) {
                UIUtils.showBilingualError(
                        "No file loaded. Please load a file first",
                        "Nenhum arquivo carregado. Por favor, carregue um arquivo primeiro"
                );
                return;
            }

            // Get start date - if not set, use earliest date in file or default
            LocalDate startDate = dateRangeController.getStartDate();
            if (startDate == null) {
                // Find earliest date in file as fallback
                for (String line : mantraData.getLines()) {
                    LocalDate lineDate = LineParser.extractDate(line);
                    if (lineDate != null && (startDate == null || lineDate.isBefore(startDate))) {
                        startDate = lineDate;
                    }
                }

                // If still null, use a default like 30 days ago
                if (startDate == null) {
                    startDate = LocalDate.now().minusDays(30);
                }
            }

            // Show the All Mantras UI with just the start date
            AllMantrasUI allMantrasUI = new AllMantrasUI();
            allMantrasUI.show(primaryStage, mantraData, startDate);

        } catch (Exception ex) {
            ex.printStackTrace();
            UIUtils.showBilingualError(
                    "Error showing all mantras: " + ex.getMessage(),
                    "Erro ao mostrar todos os mantras: " + ex.getMessage()
            );
        }
    }

    /**
     * Adds tooltips to controller components after they are created.
     */
    private void addTooltipsToControllerComponents() {
        // Add tooltips to date range controller components
        Platform.runLater(() -> {
            // Find the date picker in the date range controller container
            var dateContainer = dateRangeController.getDatePickerContainer();
            addTooltipsToContainer(dateContainer);

            // Add tooltips to file management controller components
            var fileContainer = fileController.getFileControlContainer();
            addTooltipsToContainer(fileContainer);

            // Add tooltips to search controller components
            var searchContainer = searchController.getSearchContainer();
            addTooltipsToContainer(searchContainer);
        });
    }

    /**
     * Recursively adds tooltips to components in a container.
     */
    private void addTooltipsToContainer(javafx.scene.Parent container) {
        if (container == null) return;

        container.getChildrenUnmodifiable().forEach(node -> {
            // Handle DatePicker
            if (node instanceof DatePicker datePicker) {
                // Translate the placeholder to Portuguese while keeping locale format
                String currentPrompt = datePicker.getPromptText();
                if (currentPrompt != null) {
                    if (currentPrompt.contains("MM/DD/YY") || currentPrompt.contains("MM/dd/yy")) {
                        datePicker.setPromptText("MM/DD/AA"); // US format but Portuguese (Ano)
                    } else if (currentPrompt.contains("DD/MM/YY") || currentPrompt.contains("dd/MM/yy")) {
                        datePicker.setPromptText("DD/MM/AA"); // BR format but Portuguese (Ano)
                    }
                }

                Tooltip dateTooltip = new Tooltip("Start Date - Select the date from which to start counting mantras");
                dateTooltip.setShowDelay(Duration.millis(300));
                dateTooltip.setHideDelay(Duration.millis(100));
                Tooltip.install(datePicker, dateTooltip);
            }

            // Handle Labels
            else if (node instanceof Label label) {
                if (label.getText() != null &&
                        (label.getText().contains("Start Date") ||
                                label.getText().contains("Data Inicial"))) {
                    label.setText("Data Inicial");
                    Tooltip labelTooltip = new Tooltip("Start Date - Select the date from which to start counting mantras");
                    labelTooltip.setShowDelay(Duration.millis(300));
                    labelTooltip.setHideDelay(Duration.millis(100));
                    Tooltip.install(label, labelTooltip);
                }
            }

            // Handle TextFields
            else if (node instanceof TextField textField) {
                String promptText = textField.getPromptText();

                // File field - check for various file-related prompts
                if (promptText != null &&
                        (promptText.contains("Open a file") ||
                                promptText.contains("Abrir Arquivo") ||
                                promptText.contains("file") ||
                                promptText.contains("arquivo") ||
                                promptText.contains("Abrir arquivo"))) {
                    textField.setPromptText("Abrir arquivo...");
                    Tooltip fileFieldTooltip = new Tooltip("Open a file - Click to browse and select your journal/diary file");
                    fileFieldTooltip.setShowDelay(Duration.millis(300));
                    fileFieldTooltip.setHideDelay(Duration.millis(100));
                    Tooltip.install(textField, fileFieldTooltip);
                }
                // Search field
                else if (promptText != null &&
                        (promptText.contains("Search") ||
                                promptText.contains("Buscar"))) {
                    textField.setPromptText("Buscar...");
                    Tooltip searchTooltip = new Tooltip("Search - Enter text to search within mismatch lines");
                    searchTooltip.setShowDelay(Duration.millis(300));
                    searchTooltip.setHideDelay(Duration.millis(100));
                    Tooltip.install(textField, searchTooltip);
                }
            }

            // Handle Buttons
            else if (node instanceof Button button) {
                if (button.getText() != null) {
                    if (button.getText().contains("Open File") ||
                            button.getText().contains("Abrir Arquivo")) {
                        button.setText("Abrir Arquivo");
                        Tooltip buttonTooltip = new Tooltip("Open File - Browse and select your journal/diary file");
                        buttonTooltip.setShowDelay(Duration.millis(300));
                        buttonTooltip.setHideDelay(Duration.millis(100));
                        Tooltip.install(button, buttonTooltip);
                    }
                    else if ((button.getText().contains("Search") || button.getText().contains("Buscar")) &&
                            !button.getText().contains("◀") && !button.getText().contains("▶")) {
                        button.setText("Buscar");
                        Tooltip searchButtonTooltip = new Tooltip("Search - Execute the search in mismatch lines");
                        searchButtonTooltip.setShowDelay(Duration.millis(300));
                        searchButtonTooltip.setHideDelay(Duration.millis(100));
                        Tooltip.install(button, searchButtonTooltip);
                    }
                    else if (button.getText().contains("Prev") || button.getText().contains("◀")) {
                        button.setText("◀ Anterior");
                        Tooltip prevTooltip = new Tooltip("Previous - Go to previous search result");
                        prevTooltip.setShowDelay(Duration.millis(300));
                        prevTooltip.setHideDelay(Duration.millis(100));
                        Tooltip.install(button, prevTooltip);
                    }
                    else if (button.getText().contains("Next") || button.getText().contains("▶")) {
                        button.setText("Próximo ▶");
                        Tooltip nextTooltip = new Tooltip("Next - Go to next search result");
                        nextTooltip.setShowDelay(Duration.millis(300));
                        nextTooltip.setHideDelay(Duration.millis(100));
                        Tooltip.install(button, nextTooltip);
                    }
                }
            }

            // Handle CheckBox
            else if (node instanceof CheckBox checkBox) {
                if (checkBox.getText() != null &&
                        (checkBox.getText().contains("Exact word") ||
                                checkBox.getText().contains("Palavra exata"))) {
                    checkBox.setText("Palavra exata");
                    Tooltip checkboxTooltip = new Tooltip("Exact word - Check to search for exact word matches only");
                    checkboxTooltip.setShowDelay(Duration.millis(300));
                    checkboxTooltip.setHideDelay(Duration.millis(100));
                    Tooltip.install(checkBox, checkboxTooltip);
                }
            }

            // Recursively check child containers
            else if (node instanceof javafx.scene.Parent parent) {
                addTooltipsToContainer(parent);
            }
        });
    }

    /**
     * Adjusts window size when mismatch panel expands/collapses.
     */
    private void adjustWindowSize(boolean hasMismatches) {
        if (hasMismatches) {
            // Expand window to accommodate mismatches
            double currentHeight = primaryStage.getHeight();
            double newHeight = Math.max(currentHeight, 600); // Ensure minimum height for mismatches
            primaryStage.setHeight(newHeight);
        } else {
            // Optionally shrink back to original size
            primaryStage.setHeight(440);
        }
    }

    /**
     * Dynamically adjusts window size based on mismatch panel state
     */
    private void adjustWindowSizeForMismatchPanel(boolean isExpanded) {
        if (isExpanded) {
            // Expand window when mismatch panel opens
            primaryStage.setHeight(Math.max(primaryStage.getHeight(), 600));
        } else {
            // Shrink window when mismatch panel closes
            primaryStage.setHeight(440); // Original compact size
        }
    }
}