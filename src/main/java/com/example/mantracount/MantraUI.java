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
 * Main application class for MantraCount.
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
    private VBox mainContentArea; // New: separate content area that can grow
    private HBox bottomButtonArea; // New: fixed bottom button area

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
            // Force the collapsed state by triggering the listener logic
            mismatchPanel.setPrefHeight(25);
            mismatchPanel.setMinHeight(25);
            mismatchPanel.setMaxHeight(25);
            VBox.setVgrow(mismatchPanel, Priority.NEVER);
        });

        // Shutdown handler
        primaryStage.setOnCloseRequest(event -> AutoUpdater.shutdown());
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
        // Fixed: Pass the TitledPane instead of ScrollPane to SearchController
        searchController = new SearchController(
                displayController.getMismatchesContainer(),
                displayController.getMismatchesScrollPane()
        );

        // Connect mismatch panel expansion to window resizing
        displayController.getMismatchesScrollPane().expandedProperty().addListener((obs, wasExpanded, isExpanded) -> {
            adjustWindowSizeForMismatchPanel(isExpanded);
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

        // Create main content area that can grow
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

        HBox processBox = new HBox(10, processButton, clearResultsButton, checkMissingDaysButton, allMantrasButton);
        processBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        // Add tooltips to controller components
        addTooltipsToControllerComponents();

        // Add components to main content area
        mainContentArea.getChildren().addAll(
                dateRangeController.getDatePickerContainer(),
                mantraField,
                fileController.getFileControlContainer(),
                displayController.getResultsArea(),
                processBox,
                searchController.getSearchContainer(),
                displayController.getMismatchesScrollPane() // This TitledPane will expand when needed
        );

        // Create bottom button area
        bottomButtonArea = createBottomButtonArea();

        // Set up layout priorities
        VBox.setVgrow(displayController.getMismatchesScrollPane(), Priority.ALWAYS); // Allow mismatch panel to grow

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

        Label updateLabel = new Label(" - Atualizar");
        updateLabel.setStyle("-fx-text-fill: black; -fx-font-size: 12px;");

        Tooltip updateLabelTooltip = new Tooltip("Update - Check for application updates");
        updateLabelTooltip.setShowDelay(Duration.millis(300));
        updateLabelTooltip.setHideDelay(Duration.millis(100));
        Tooltip.install(updateLabel, updateLabelTooltip);

        HBox buttonBox = new HBox(10, saveButton, cancelButton, updateButton, updateLabel);
        buttonBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        buttonBox.setPadding(new Insets(10, 0, 0, 0)); // Add some top padding

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
            // Optionally resize window back to original size when cleared
            adjustWindowSize(false);
        });

        // Save button
        saveButton.setOnAction(e -> {
            if (mantraData.getLines() == null || displayController.getMismatchedLines() == null) {
                UIUtils.showError("Nenhum arquivo carregado ou processado.");
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

        // Missing days button
        checkMissingDaysButton.setOnAction(e -> {
            try {
                MissingDaysUI missingDaysUI = new MissingDaysUI();
                missingDaysUI.show(primaryStage, mantraData);
            } catch (Exception ex) {
                ex.printStackTrace();
                UIUtils.showError("❌ Erro ao verificar dias faltantes: " + ex.getMessage());
            }
        });

        // All mantras button
        allMantrasButton.setOnAction(e -> showAllMantras());
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
     * Processes the file and displays results.
     */
    private void processFile() {
        try {
            // Validate inputs
            if (!dateRangeController.validateStartDate()) return;

            String mantraPlaceholder = "Nome do Mantra ou Rito";
            if (!UIUtils.validateField(mantraField, "Por favor, insira o Mantra", mantraPlaceholder)) return;

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

            // Display results
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

        } catch (Exception ex) {
            ex.printStackTrace();
            UIUtils.showError("❌ Erro ao processar arquivo: " + ex.getMessage());
        }
    }

    /**
     * Shows the All Mantras feature.
     */
    private void showAllMantras() {
        try {
            if (mantraData.getLines() == null || mantraData.getLines().isEmpty()) {
                UIUtils.showError("Nenhum arquivo carregado. Por favor, carregue um arquivo primeiro.");
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
            UIUtils.showError("❌ Erro ao mostrar todos os mantras: " + ex.getMessage());
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
                // Date field - translate to Portuguese while preserving locale format
                else if (promptText != null &&
                        (promptText.contains("MM/DD/YY") || promptText.contains("MM/dd/yy") ||
                                promptText.contains("DD/MM/YY") || promptText.contains("dd/MM/yy") ||
                                promptText.contains("Mês/Dia/Ano") ||
                                promptText.contains("DD/MM/AAAA") ||
                                promptText.toLowerCase().contains("date"))) {
                    // Translate to Portuguese while keeping the correct format for locale
                    if (promptText.contains("MM/DD") || promptText.contains("MM/dd")) {
                        textField.setPromptText("MM/DD/AA"); // US format in Portuguese
                    } else if (promptText.contains("DD/MM") || promptText.contains("dd/MM")) {
                        textField.setPromptText("DD/MM/AA"); // BR format in Portuguese
                    } else {
                        textField.setPromptText("Data"); // Generic Portuguese if format unclear
                    }

                    Tooltip dateFieldTooltip = new Tooltip("Date - Enter date in your system's date format");
                    dateFieldTooltip.setShowDelay(Duration.millis(300));
                    dateFieldTooltip.setHideDelay(Duration.millis(100));
                    Tooltip.install(textField, dateFieldTooltip);
                }
                // If no prompt text but this could be a file field based on context
                else if (promptText == null || promptText.isEmpty()) {
                    // Check if this text field is likely for file input based on parent structure
                    javafx.scene.Parent parent = textField.getParent();
                    if (parent != null) {
                        // Look for nearby button or label that suggests this is for file selection
                        boolean isFileField = parent.getChildrenUnmodifiable().stream()
                                .anyMatch(sibling ->
                                        (sibling instanceof Button button &&
                                                button.getText() != null &&
                                                (button.getText().contains("Open") ||
                                                        button.getText().contains("File") ||
                                                        button.getText().contains("Abrir") ||
                                                        button.getText().contains("Arquivo"))) ||
                                                (sibling instanceof Label label &&
                                                        label.getText() != null &&
                                                        (label.getText().toLowerCase().contains("file") ||
                                                                label.getText().toLowerCase().contains("arquivo")))
                                );

                        boolean isSearchField = parent.getChildrenUnmodifiable().stream()
                                .anyMatch(sibling ->
                                        (sibling instanceof Button button &&
                                                button.getText() != null &&
                                                (button.getText().contains("Search") ||
                                                        button.getText().contains("Buscar"))) ||
                                                (sibling instanceof CheckBox checkBox &&
                                                        checkBox.getText() != null &&
                                                        (checkBox.getText().contains("Exact") ||
                                                                checkBox.getText().contains("Palavra")))
                                );

                        if (isFileField) {
                            textField.setPromptText("Abrir arquivo...");
                            Tooltip fileFieldTooltip = new Tooltip("Open a file - Click to browse and select your journal/diary file");
                            fileFieldTooltip.setShowDelay(Duration.millis(300));
                            fileFieldTooltip.setHideDelay(Duration.millis(100));
                            Tooltip.install(textField, fileFieldTooltip);
                        } else if (isSearchField) {
                            textField.setPromptText("Buscar...");
                            Tooltip searchTooltip = new Tooltip("Search - Enter text to search within mismatch lines");
                            searchTooltip.setShowDelay(Duration.millis(300));
                            searchTooltip.setHideDelay(Duration.millis(100));
                            Tooltip.install(textField, searchTooltip);
                        }
                    }
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
                    // Additional search-related buttons that might not contain "Search"
                    else if (button.getText().equals("🔍") ||
                            (button.getText().length() <= 3 && !button.getText().contains("🔄"))) {
                        // This could be a search icon button
                        Tooltip searchIconTooltip = new Tooltip("Search - Execute the search in mismatch lines");
                        searchIconTooltip.setShowDelay(Duration.millis(300));
                        searchIconTooltip.setHideDelay(Duration.millis(100));
                        Tooltip.install(button, searchIconTooltip);
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