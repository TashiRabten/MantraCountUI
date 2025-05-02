package com.example.mantracount;
import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.stage.FileChooser;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Priority;
import javafx.geometry.Insets;
import javafx.scene.input.KeyCode;  // Added missing import for KeyCode

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;


public class MantraUI extends Application {

    private TextField searchField;
    private Button searchButton;
    private Button prevButton;
    private Button nextButton;
    private CheckBox exactWordCheckBox; // Add field to track the checkbox state
    private int currentSearchIndex = -1;
    private List<Node> searchMatches = new ArrayList<>();
    private String lastSearchQuery = "";
    private boolean isFromZip = false;
    private String originalZipPath;
    private File lastDirectory = new File(System.getProperty("user.home"));
    private String originalFilePath;
    private List<String> originalLines;
    private List<String> mismatchedLines;
    private VBox mismatchesContainer;
    private final Label placeholder = new Label("Mismatch Line\n(Discrep\u00e2ncia de linhas)");
    private List<String> updatedLines;
    private Stage primaryStage;  // Added primaryStage field
    private ScrollPane mismatchesScrollPane;  // Added mismatchesScrollPane field
    private boolean lastExactWordCheckBoxState = true; // Since it's selected by default

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;  // Store the primaryStage

        primaryStage.setTitle("MantraCount");

        // Create VBox here
        VBox vbox = new VBox(10);
        vbox.setPadding(new Insets(20));

        Scene scene = new Scene(vbox, 700, 600);

        // Add CSS styles directly to the scene
        scene.getStylesheets().add("data:text/css,"
                + ".search-highlight {"
                + "    -fx-background-color: #ffff99;"
                + "    -fx-background-radius: 3;"
                + "}");

        primaryStage.setScene(scene);

        TextField dateField = new TextField();
        setPlaceholder(dateField, "Enter start date - MM/DD/YY (Colocar Data Inicial - Mês/Dia/Ano)");

        TextField mantraField = new TextField();
        setPlaceholder(mantraField, "Enter mantra name (Colocar nome do Mantra)");

        TextField pathField = new TextField();
        setPlaceholder(pathField, "Open a file... (Abrir Arquivo...)");
        pathField.setPrefWidth(400);

        Button openFileButton = new Button("Open File");

        TextArea resultsArea = new TextArea();
        resultsArea.setText("Count Mantras\n(Contar Mantras)");
        resultsArea.setStyle("-fx-text-fill: gray;-fx-font-style: italic;");
        resultsArea.setPrefRowCount(6);
        resultsArea.setMinHeight(114);
        resultsArea.setMaxHeight(114);
        VBox.setVgrow(resultsArea, Priority.NEVER);
        resultsArea.setEditable(false);
        resultsArea.setWrapText(true);

        Button processButton = new Button("Count Mantras");
        processButton.setPrefHeight(20);
        processButton.setStyle("-fx-base: #4CAF50; -fx-text-fill: white;");
        // Create a new button for clearing results

        Button clearResultsButton = new Button("Clear Results");
        clearResultsButton.setStyle("-fx-base: #F44336; -fx-text-fill: white;");

        HBox processButtonBox = new HBox(10, processButton, clearResultsButton);
        processButtonBox.setAlignment(Pos.CENTER_LEFT);

        // Set up the event handler
        clearResultsButton.setOnAction(e -> {
            resultsArea.setText("Count Mantras\n(Contar Mantras)");
            resultsArea.setStyle("-fx-text-fill: gray;-fx-font-style: italic;");
            mismatchesContainer.getChildren().clear();
            mismatchesContainer.getChildren().add(placeholder);
            resetSearchState();
        });



        // Add the search controls above the mismatch panel
        HBox searchControls = createSearchControls();

        mismatchesContainer = new VBox(5);
        mismatchesContainer.setPadding(new Insets(5));

        mismatchesScrollPane = new ScrollPane(mismatchesContainer);  // Initialize mismatchesScrollPane
        mismatchesScrollPane.setFitToWidth(true);
        mismatchesScrollPane.setPrefHeight(240);
        mismatchesScrollPane.setMaxHeight(240);   // Also set a maximum height
        mismatchesScrollPane.setStyle("-fx-border-color: #0078D7; -fx-border-width: 2px;");

        placeholder.setStyle("-fx-text-fill: gray; -fx-font-style: italic;");
        mismatchesContainer.getChildren().add(placeholder);


        Button saveButton = new Button("Save Changes");
        saveButton.setStyle("-fx-base: #4CAF50; -fx-text-fill: white;");

        Button cancelButton = new Button("Cancel Changes");
        cancelButton.setStyle("-fx-base: #F44336; -fx-text-fill: white;");

        HBox buttonBox = new HBox(10, saveButton, cancelButton);
        buttonBox.setAlignment(Pos.CENTER_LEFT);
        VBox.setMargin(buttonBox, new Insets(5, 0, 0, 0));  // Add margin to ensure spacing

        openFileButton.setOnAction(event -> openFile(primaryStage, pathField, resultsArea));
        processButton.setOnAction(e -> processFile(dateField, mantraField, pathField, resultsArea));
        saveButton.setOnAction(e -> saveChanges());
        cancelButton.setOnAction(e -> cancelChanges());

        // Populate the vbox with all the UI elements
        vbox.getChildren().addAll(
                dateField,
                mantraField,
                new HBox(10, pathField, openFileButton),
                resultsArea,
                processButtonBox,
                searchControls,  // Add the search controls here
                mismatchesScrollPane,
                buttonBox
        );

        // Load icon using resource stream
        Image icon = new Image(getClass().getResourceAsStream("/icons/BUDA.jpg"));
        primaryStage.getIcons().add(icon);

        setupSearchFeature();  // Add this line to set up the Ctrl+F shortcut
        primaryStage.show();
    }

    private void clearMismatchDisplay() {
        mismatchesContainer.getChildren().clear();
        mismatchesContainer.getChildren().add(placeholder);

        // Clear any search state
        if (searchField != null) {
            searchField.clear();
            clearSearchHighlights();
        }
    }

    private void displayResults(FileProcessorService.ProcessResult result,
                                LocalDate parsedDate, String inputDate, String mantraKeyword,
                                TextArea resultsArea) {
        String formattedStartDate = parsedDate.format(
                (inputDate.length() == 8) ?
                        DateTimeFormatter.ofPattern("MM/dd/yy") :
                        DateTimeFormatter.ofPattern("MM/dd/yyyy")
        );

        resultsArea.setText("\u2714 Results from " + formattedStartDate + ":\n" +
                "--\n"
                + "Total '" + mantraKeyword + "' count: " + result.getTotalMantraKeywordCount() + "\n"
                + "Total 'Mantra(s)' count: " + result.getTotalMantraWordsCount() + "\n"
                + "Total 'Fiz' count: " + result.getTotalFizCount() + "\n"
                + "Sum of mantras: " + result.getTotalFizNumbersSum());
        resultsArea.setStyle("-fx-text-fill: black;");
    }

    private void updateZipFile(String zipPath, String extractedFilePath, List<String> updatedContent) {
        try {
            Path tempZipPath = Files.createTempFile("updated", ".zip");
            String entryName = Paths.get(extractedFilePath).getFileName().toString();

            try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipPath));
                 java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(Files.newOutputStream(tempZipPath))) {

                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    ZipEntry newEntry = new ZipEntry(entry.getName());
                    zos.putNextEntry(newEntry);

                    if (entry.getName().equals(entryName)) {
                        byte[] updatedBytes = String.join("\n", updatedContent).getBytes(StandardCharsets.UTF_8);
                        zos.write(updatedBytes);
                    } else {
                        byte[] buffer = new byte[1024];
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            zos.write(buffer, 0, len);
                        }
                    }
                    zos.closeEntry();
                }
            }
            Files.move(tempZipPath, Paths.get(zipPath), StandardCopyOption.REPLACE_EXISTING);

        } catch (Exception ex) {
            ex.printStackTrace();
            showError("❌ Failed to update zip file.\n❌ Falha ao atualizar arquivo zip.");
        }
    }

    private void cancelChanges() {
        if (mismatchedLines != null) {
            displayMismatchedLines(mismatchedLines);
            showInfo("✔ Changes reverted.\n✔ Alterações revertidas.");
            resetSearchState();
        }
    }

    private void displayMismatchedLines(List<String> mismatchedLines) {
        mismatchesContainer.getChildren().clear();
        if (mismatchedLines.isEmpty()) {
            mismatchesContainer.getChildren().add(placeholder);
            return;
        }

        for (String line : mismatchedLines) {
            int closeBracket = line.indexOf(']');
            int colon = line.indexOf(':', closeBracket);

            if (closeBracket != -1 && colon != -1) {
                String protectedPart = line.substring(0, colon + 1);
                String editablePart = (colon + 1 < line.length()) ? line.substring(colon + 1) : "";

                Label protectedLabel = new Label(protectedPart);
                protectedLabel.setStyle("-fx-font-weight: bold;");

                TextField editableField = new TextField(editablePart);
                HBox.setHgrow(editableField, Priority.ALWAYS);

                HBox lineContainer = new HBox(5, protectedLabel, editableField);
                lineContainer.setAlignment(Pos.CENTER_LEFT);

                mismatchesContainer.getChildren().add(lineContainer);
            } else {
                TextField fullLineField = new TextField(line);
                fullLineField.setPrefWidth(500);
                mismatchesContainer.getChildren().add(fullLineField);
            }
        }
    }

    private void setPlaceholder(TextField field, String placeholder) {
        field.setText(placeholder);
        field.setStyle("-fx-text-fill: gray;");
        field.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal && field.getText().equals(placeholder)) {
                field.clear();
                field.setStyle("-fx-text-fill: black;");
            } else if (!newVal && field.getText().isEmpty()) {
                field.setText(placeholder);
                field.setStyle("-fx-text-fill: gray;");
            }
        });
    }

    private void showError(String message) {
        new Alert(Alert.AlertType.ERROR, message).showAndWait();
    }

    private void showInfo(String message) {
        new Alert(Alert.AlertType.INFORMATION, message).showAndWait();
    }

    private File extractFirstTxt(File zipFile) throws Exception {
        Path tempDir = Files.createTempDirectory("unzipped_chat");
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (!entry.isDirectory() && entry.getName().toLowerCase().endsWith(".txt")) {
                    Path extractedFilePath = tempDir.resolve(Paths.get(entry.getName()).getFileName());
                    Files.copy(zis, extractedFilePath, StandardCopyOption.REPLACE_EXISTING);
                    return extractedFilePath.toFile();
                }
            }
        }
        throw new FileNotFoundException("No .txt found in .zip.\n(Não há .txt no .zip.)");
    }

    private void saveChanges() {
        try {
            if (originalFilePath == null || originalLines == null || mismatchedLines == null) {
                showError("❌ No file loaded.\n❌ Nenhum arquivo carregado.");
                return;
            }

            // Step 1: Extract updated content from UI
            Map<String, String> updatedMismatchMap = extractUpdatedContentFromUI();

            // Step 2: Update the file content
            int updateCount = updateFileContent(updatedMismatchMap);

            // Step 3: Save the updated content
            saveUpdatedContent();

            // Step 4: Show success message
            showInfo("✔ Changes saved successfully.\n✔ " + updateCount + " line(s) updated." +
                    "\n\n✔ Alterações salvas com sucesso.\n✔ " + updateCount + " linha(s) atualizada(s).");
            resetSearchState();
        } catch (Exception ex) {
            ex.printStackTrace();
            showError("❌ Failed to save changes.\n❌ Falha ao salvar alterações.");
        }
    }

    /**
     * Extracts updated content from UI components
     * @return Map of original lines to updated lines
     */
    private Map<String, String> extractUpdatedContentFromUI() {
        Map<String, String> updatedMismatchMap = new HashMap<>();

        for (int i = 0; i < mismatchesContainer.getChildren().size(); i++) {
            Node node = mismatchesContainer.getChildren().get(i);

            // Skip placeholder or if we've processed all mismatched lines
            if (node == placeholder || i >= mismatchedLines.size()) continue;

            String originalLine = mismatchedLines.get(i);
            String updatedLine = extractUpdatedLineFromNode(node);

            if (updatedLine != null) {
                updatedMismatchMap.put(originalLine, updatedLine);
            }
        }

        return updatedMismatchMap;
    }

    /**
     * Extracts the updated line content from a UI node
     * @param node The UI node (HBox or TextField)
     * @return The updated line content
     */
    private String extractUpdatedLineFromNode(Node node) {
        if (node instanceof HBox lineContainer) {
            Label protectedLabel = (Label) lineContainer.getChildren().get(0);
            TextField editableField = (TextField) lineContainer.getChildren().get(1);
            return protectedLabel.getText() + editableField.getText();
        } else if (node instanceof TextField fullLineField) {
            return fullLineField.getText();
        }
        return null;
    }

    /**
     * Updates the file content with the modified lines
     * @param updatedMismatchMap Map of original lines to updated lines
     * @return Number of lines updated
     */
    private int updateFileContent(Map<String, String> updatedMismatchMap) {
        int updateCount = 0;
        List<String> updatedLines = new ArrayList<>(originalLines);

        for (int j = 0; j < updatedLines.size(); j++) {
            String currentLine = updatedLines.get(j);
            if (updatedMismatchMap.containsKey(currentLine)) {
                updatedLines.set(j, updatedMismatchMap.get(currentLine));
                updateCount++;
            }
        }

        // Store the updated lines for saving
        this.updatedLines = updatedLines;
        return updateCount;
    }

    /**
     * Saves the updated content to the file
     * @throws IOException If an I/O error occurs
     */
    private void saveUpdatedContent() throws IOException {
        // Save to the original file
        Files.write(Paths.get(originalFilePath), updatedLines, StandardCharsets.UTF_8,
                StandardOpenOption.TRUNCATE_EXISTING);

        // If the file came from a ZIP, update the ZIP as well
        if (isFromZip && originalZipPath != null) {
            updateZipFile(originalZipPath, originalFilePath, updatedLines);
        }
    }

    private void processFile(TextField dateField, TextField mantraField, TextField pathField, TextArea resultsArea) {
        try {
            // Reset search state at the beginning of processing
            resetSearchState();

            String inputDate = dateField.getText().trim();
            if (inputDate.equals("Enter start date - MM/DD/YY (Colocar Data Inicial - Mês/Dia/Ano)")) {
                inputDate = "";
            }

            String mantraKeyword = mantraField.getText().trim();
            if (mantraKeyword.equals("Enter mantra name (Colocar nome do Mantra)")) {
                mantraKeyword = "";
            }

            String filePath = pathField.getText().trim();
            if (filePath.equals("Open a file... (Abrir Arquivo...)")) {
                filePath = "";
            }


            // Initial null/empty validation
            if (inputDate.isEmpty() || mantraKeyword.isEmpty() || filePath.isEmpty()) {
                StringBuilder errorBuilder = new StringBuilder("❌ Missing required fields:\n❌ Campos obrigatórios ausentes:\n");

                if (inputDate.isEmpty()) errorBuilder.append("- Date / Data\n");
                if (mantraKeyword.isEmpty()) errorBuilder.append("- Mantra\n");
                if (filePath.isEmpty()) errorBuilder.append("- File / Arquivo\n");

                showError(errorBuilder.toString());
                return;
            }

            // Try to parse the date
            LocalDate parsedDate;
            try {
                parsedDate = DateParser.parseDate(inputDate);
            } catch (DateTimeParseException e) {
                showError("❌ Invalid date format: \"" + inputDate + "\"\n❌ Formato de data inválido: \"" + inputDate + "\"");
                return;
            }

            // Load original file lines
            originalFilePath = filePath;
            originalLines = Files.readAllLines(Paths.get(filePath), StandardCharsets.UTF_8);

            // Process file
            FileProcessorService.ProcessResult result =
                    FileProcessorService.processFile(filePath, mantraKeyword, parsedDate);

            // Display results
            displayResults(result, parsedDate, inputDate, mantraKeyword, resultsArea);

            // Show mismatches or clear display
            mismatchedLines = result.getMismatchedLines();
            if (!mismatchedLines.isEmpty()) {
                displayMismatchedLines(mismatchedLines);
            } else {
                clearMismatchDisplay();
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            showError("❌ Unexpected error: " + ex.getMessage() + "\n❌ Erro inesperado: " + ex.getMessage());
        }
    }


    // Add this new method to reset search state
    private void resetSearchState() {
        if (searchField != null) {
            clearSearchHighlights();
            lastSearchQuery = "";
            currentSearchIndex = -1;
            searchMatches.clear();

            // Reset navigation buttons
            if (prevButton != null) prevButton.setDisable(true);
            if (nextButton != null) nextButton.setDisable(true);

            // Reset results label if it exists
            Label resultsLabel = searchButton != null ?
                    (Label) searchButton.getParent().lookup("#searchResultsLabel") : null;
            if (resultsLabel != null) {
                resultsLabel.setText("");
            }
        }
    }

    private void openFile(Stage primaryStage, TextField pathField, TextArea resultsArea) {
        try {
            // Reset search state when opening a new file
            resetSearchState();
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Select a Text or Zip File");
            fileChooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("Text Files and Zip Files", "*.txt", "*.zip"),
                    new FileChooser.ExtensionFilter("All Files", "*.*")
            );
            fileChooser.setInitialDirectory(lastDirectory);

            File selectedFile = fileChooser.showOpenDialog(primaryStage);
            if (selectedFile != null) {
                File fileToUse = selectedFile;

                if (selectedFile.getName().toLowerCase().endsWith(".zip")) {
                    originalZipPath = selectedFile.getAbsolutePath();
                    isFromZip = true;
                    fileToUse = extractFirstTxt(selectedFile);
                } else {
                    isFromZip = false;
                }

                pathField.setText(fileToUse.getAbsolutePath());
                pathField.setStyle("-fx-text-fill: black;");
                lastDirectory = selectedFile.getParentFile();

                resultsArea.setText("Count Mantras\n(Contar Mantras)");
                resultsArea.setStyle("-fx-text-fill: gray;-fx-font-style: italic;");
                mismatchesContainer.getChildren().clear();
                mismatchesContainer.getChildren().add(placeholder);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            showError("\u274c Text file not found.\n\u274c Arquivo de text não encontrado.");
        }
    }

    private HBox createSearchControls() {
        searchField = new TextField();
        setPlaceholder(searchField, "Search words... (Buscar palavras...)");
        searchField.setPrefWidth(200);


        // In your createSearchControls() method, add this listener to the checkbox:
        exactWordCheckBox = new CheckBox("Exact words (Palavras Exatas)");
        exactWordCheckBox.setSelected(true); // Enable exact word search by default
        exactWordCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            // If there's text in the search field, perform a new search
            if (!searchField.getText().trim().isEmpty()) {
                performSearch();
            }
        });
        // Add keyboard shortcut (Ctrl+F) to focus the search field
        searchField.setOnKeyPressed(event -> {
            if (event.isControlDown() && event.getCode() == KeyCode.F) {
                searchField.requestFocus();
            }
        });

        // Add event listener for Enter key
        searchField.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                performSearch();
            }
        });

        searchButton = new Button("Find");
        searchButton.setOnAction(e -> performSearch());

        prevButton = new Button("◀ Prev");
        prevButton.setOnAction(e -> navigateSearch(false));
        prevButton.setDisable(true);

        nextButton = new Button("Next ▶");
        nextButton.setOnAction(e -> navigateSearch(true));
        nextButton.setDisable(true);

        Label searchResults = new Label("");
        searchResults.setId("searchResultsLabel");

        HBox searchControls = new HBox(10, searchField, searchButton, exactWordCheckBox, prevButton, nextButton, searchResults);
        searchControls.setAlignment(Pos.CENTER_LEFT);
        return searchControls;
    }

    // Add this method to perform the search
    private void performSearch() {
        String query = searchField.getText().toLowerCase().trim();

        if (query.isEmpty()) {
            clearSearchHighlights();
            return;
        }

        // If this is a new search (different query or checkbox state changed), reset
        boolean exactWordStateChanged = lastExactWordCheckBoxState != exactWordCheckBox.isSelected();
        if (!query.equals(lastSearchQuery) || exactWordStateChanged) {
            clearSearchHighlights();
            lastSearchQuery = query;
            lastExactWordCheckBoxState = exactWordCheckBox.isSelected(); // Update the last checkbox state
            searchMatches.clear();
            currentSearchIndex = -1;

            // Find all matches
            for (Node node : mismatchesContainer.getChildren()) {
                if (node == placeholder) continue;

                if (nodeContainsText(node, query)) {
                    searchMatches.add(node);
                }
            }

            // Update result label
            Label resultsLabel = (Label) searchButton.getParent().lookup("#searchResultsLabel");
            if (resultsLabel != null) {
                resultsLabel.setText(searchMatches.size() + " matches found");
            }
        }

        // Enable/disable navigation buttons
        boolean hasMatches = !searchMatches.isEmpty();
        prevButton.setDisable(!hasMatches);
        nextButton.setDisable(!hasMatches);

        // Navigate to the first match if we have matches
        if (hasMatches) {
            navigateSearch(true);
        }


        // If this is a new search (different query), reset



        }


    // Helper method to check if a node contains the exact word match for the search text
    private boolean nodeContainsText(Node node, String searchText) {
        if (node instanceof HBox lineContainer) {
            Label protectedLabel = (Label) lineContainer.getChildren().get(0);
            TextField editableField = (TextField) lineContainer.getChildren().get(1);

            return containsExactWord(protectedLabel.getText().toLowerCase(), searchText) ||
                    containsExactWord(editableField.getText().toLowerCase(), searchText);

        } else if (node instanceof TextField fullLineField) {
            return containsExactWord(fullLineField.getText().toLowerCase(), searchText);
        }

        return false;
    }

    /**
     * Checks if the text contains the search query as an exact word
     * @param text The text to search in
     * @param searchQuery The word to search for
     * @return true if the exact word is found, false otherwise
     */
    /**
     * Checks if the text contains the search query as an exact word
     * @param text The text to search in
     * @param searchQuery The word to search for
     * @return true if the exact word is found, false otherwise
     */
    private boolean containsExactWord(String text, String searchQuery) {
        // Only perform exact word match if the checkbox is selected
        if (exactWordCheckBox != null && exactWordCheckBox.isSelected()) {
            // Split text into words and check for exact matches
            String[] words = text.split("\\s+");
            for (String word : words) {
                // Clean word from punctuation for comparison
                String cleanWord = word.replaceAll("[,.;:!?()\\[\\]{}\"']", "");
                if (cleanWord.equalsIgnoreCase(searchQuery)) {
                    return true;
                }
            }
            return false;
        } else {
            // Fall back to contains for partial word matching
            return text.contains(searchQuery);
        }
    }

    // Navigate through search results
    private void navigateSearch(boolean forward) {
        if (searchMatches.isEmpty()) return;

        // Clear previous highlighting
        if (currentSearchIndex >= 0 && currentSearchIndex < searchMatches.size()) {
            Node currentNode = searchMatches.get(currentSearchIndex);
            setNodeHighlight(currentNode, false);
        }

        // Update the index
        if (forward) {
            currentSearchIndex = (currentSearchIndex + 1) % searchMatches.size();
        } else {
            currentSearchIndex = (currentSearchIndex - 1 + searchMatches.size()) % searchMatches.size();
        }

        // Highlight and scroll to the current match
        Node currentNode = searchMatches.get(currentSearchIndex);
        setNodeHighlight(currentNode, true);

        // Ensure the current match is visible in the scroll pane
        mismatchesScrollPane.setVvalue(calculateScrollPosition(currentNode));

        // Update the results label
        Label resultsLabel = (Label) searchButton.getParent().lookup("#searchResultsLabel");
        if (resultsLabel != null) {
            resultsLabel.setText("Match " + (currentSearchIndex + 1) + " of " + searchMatches.size());
        }
    }

    // Calculate the appropriate scroll position to show the node
    private double calculateScrollPosition(Node node) {
        double nodePosition = node.getBoundsInParent().getMinY();
        double containerHeight = mismatchesContainer.getBoundsInLocal().getHeight();
        double viewportHeight = mismatchesScrollPane.getViewportBounds().getHeight();

        return Math.max(0, Math.min(1, (nodePosition - viewportHeight / 2) /
                (containerHeight - viewportHeight)));
    }

    // Set highlight on a node
    private void setNodeHighlight(Node node, boolean highlight) {
        String highlightStyle = "-fx-background-color: #ffff99;";

        if (node instanceof HBox lineContainer) {
            TextField field = (TextField) lineContainer.getChildren().get(1);
            if (highlight) {
                field.setStyle(field.getStyle() + highlightStyle);
            } else {
                field.setStyle(field.getStyle().replace(highlightStyle, ""));
            }
        } else if (node instanceof TextField field) {
            if (highlight) {
                field.setStyle(field.getStyle() + highlightStyle);
            } else {
                field.setStyle(field.getStyle().replace(highlightStyle, ""));
            }
        }
    }

    // Clear all search highlights
    private void clearSearchHighlights() {
        for (Node match : searchMatches) {
            setNodeHighlight(match, false);
        }

        searchMatches.clear();
        currentSearchIndex = -1;
        lastSearchQuery = "";

        prevButton.setDisable(true);
        nextButton.setDisable(true);

        Label resultsLabel = (Label) searchButton.getParent().lookup("#searchResultsLabel");
        if (resultsLabel != null) {
            resultsLabel.setText("");
        }
    }

    // Add this to your scene setup (in the start method)
    private void setupSearchFeature() {
        // Create the global scene keyboard handler for Ctrl+F
        Scene scene = primaryStage.getScene();
        scene.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, event -> {
            if (event.isControlDown() && event.getCode() == KeyCode.F) {
                searchField.requestFocus();
                event.consume();  // Prevent the event from bubbling
            }
        });
    }
}