package com.example.mantracount;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

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
        Scene scene = new Scene(root, 710, 600);
        primaryStage.setScene(scene);
        primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("/icons/BUDA.jpg")));
        primaryStage.show();

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
        searchController = new SearchController(
                displayController.getMismatchesContainer(),
                displayController.getMismatchesScrollPane()
        );
    }

    /**
     * Creates the main layout of the application.
     *
     * @return The root VBox containing all UI components
     */
    private VBox createMainLayout() {
        VBox root = new VBox(10);
        root.setPadding(new Insets(20));

        // Mantra name field
        mantraField = new TextField();
        UIUtils.setPlaceholder(mantraField, "Enter mantra name / Colocar nome do Mantra");

        // Process buttons
        processButton = new Button("Count Mantras / Contar Mantras");
        processButton.setStyle("-fx-base: #4CAF50; -fx-text-fill: white;");

        clearResultsButton = new Button("Clear / Limpar");
        clearResultsButton.setStyle("-fx-base: #F44336; -fx-text-fill: white;");

        checkMissingDaysButton = new Button("Missing Days / Saltos de Dias");
        checkMissingDaysButton.setStyle("-fx-base: #2196F3; -fx-text-fill: white;");
        checkMissingDaysButton.setDisable(true);

        allMantrasButton = new Button("All Mantras / Todos os Mantras");
        allMantrasButton.setStyle("-fx-base: #9C27B0; -fx-text-fill: white;");
        allMantrasButton.setDisable(true);

        HBox processBox = new HBox(10, processButton, clearResultsButton, checkMissingDaysButton, allMantrasButton);
        processBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        // Save and cancel buttons
        saveButton = new Button("Save Changes / Salvar Alterações");
        saveButton.setStyle("-fx-base: #4CAF50; -fx-text-fill: white;");

        cancelButton = new Button("Cancel Changes / Cancelar Alterações");
        cancelButton.setStyle("-fx-base: #F44336; -fx-text-fill: white;");

        Button updateButton = new Button("\uD83D\uDD04");
        updateButton.setStyle("-fx-font-size: 10px; -fx-background-color: #6A1B9A; -fx-text-fill: white;");
        updateButton.setOnAction(e -> AutoUpdater.checkForUpdatesManually());

        Label updateLabel = new Label(" - Update / Atualizar");
        updateLabel.setStyle("-fx-text-fill: black; -fx-font-size: 12px;");

        HBox buttonBox = new HBox(10, saveButton, cancelButton, updateButton, updateLabel);
        buttonBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        VBox.setMargin(buttonBox, new Insets(5, 0, 0, 0));

        // Add all components to the root layout
        root.getChildren().addAll(
                dateRangeController.getDatePickerContainer(),
                mantraField,
                fileController.getFileControlContainer(),
                displayController.getResultsArea(),
                processBox,
                searchController.getSearchContainer(),
                displayController.getMismatchesScrollPane(),
                buttonBox
        );

        return root;
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
        });

        // Save button
        saveButton.setOnAction(e -> {
            if (mantraData.getLines() == null || displayController.getMismatchedLines() == null) {
                UIUtils.showError("No file loaded or processed. \nNenhum arquivo carregado ou processado.");
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
                UIUtils.showError("❌ Error checking missing days: " + ex.getMessage());
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

            String mantraPlaceholder = "Enter mantra name / Colocar nome do Mantra";
            if (!UIUtils.validateField(mantraField, "Please enter the Mantra\nPor favor, insira o Mantra", mantraPlaceholder)) return;

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
            UIUtils.showError("❌ Error processing file: " + ex.getMessage());
        }
    }

    /**
     * Shows the All Mantras feature.
     */
    private void showAllMantras() {
        try {
            if (mantraData.getLines() == null || mantraData.getLines().isEmpty()) {
                UIUtils.showError("No file loaded. Please load a file first. \nNenhum arquivo carregado. Por favor, carregue um arquivo primeiro.");
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
            UIUtils.showError("❌ Error showing all mantras: " + ex.getMessage());
        }
    }
}