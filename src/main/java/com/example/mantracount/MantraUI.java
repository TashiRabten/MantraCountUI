package com.example.mantracount;


import javafx.scene.Node;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class MantraUI extends Application {
    private TextField searchField;
    private Button searchButton, prevButton, nextButton;
    private CheckBox exactWordCheckBox;
    private List<Node> searchMatches = new ArrayList<>();
    private int currentSearchIndex = -1;
    private String lastSearchQuery = "";
    private boolean lastExactWordCheckBoxState = true;

    private VBox mismatchesContainer;
    private final Label placeholder = new Label("Mismatch Line\n(Discrepância de linhas)");
    private ScrollPane mismatchesScrollPane;

    private List<String> mismatchedLines;
    private Stage primaryStage;

    private final MantraData mantraData = new MantraData();

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        primaryStage.setTitle("MantraCount");

        VBox vbox = new VBox(10);
        vbox.setPadding(new Insets(20));
        Scene scene = new Scene(vbox, 700, 600);
        scene.getStylesheets().add("data:text/css,.search-highlight {-fx-background-color: #ffff99;-fx-background-radius: 3;}");
        primaryStage.setScene(scene);

        TextField dateField = new TextField();
        UIUtils.setPlaceholder(dateField, "Enter start date - MM/DD/YY (Colocar Data Inicial - Mês/Dia/Ano)");

        TextField mantraField = new TextField();
        UIUtils.setPlaceholder(mantraField, "Enter mantra name (Colocar nome do Mantra)");

        TextField pathField = new TextField();
        UIUtils.setPlaceholder(pathField, "Open a file... (Abrir Arquivo...)");
        pathField.setPrefWidth(400);

        Button openFileButton = new Button("Open File");
        openFileButton.setStyle("-fx-background-color: #0078D7; -fx-text-fill: white;");

        TextArea resultsArea = new TextArea("Count Mantras\n(Contar Mantras)");
        resultsArea.setStyle("-fx-text-fill: gray;-fx-font-style: italic;");
        resultsArea.setPrefRowCount(6);
        resultsArea.setMinHeight(114);
        resultsArea.setMaxHeight(114);
        VBox.setVgrow(resultsArea, Priority.NEVER);
        resultsArea.setEditable(false);
        resultsArea.setWrapText(true);



        Button processButton = new Button("Count Mantras/Contar Mantras");
        processButton.setPrefHeight(20);
        processButton.setStyle("-fx-base: #4CAF50; -fx-text-fill: white;");
        // Create a new button for clearing results

        Button clearResultsButton = new Button("Clear Results/Limpar Resultados");
        clearResultsButton.setStyle("-fx-base: #F44336; -fx-text-fill: white;");

        Button checkMissingDaysButton = new Button("Check Missing Days/Checar Saltos de Dias");
        checkMissingDaysButton.setStyle("-fx-base: #2196F3; -fx-text-fill: white;");
        checkMissingDaysButton.setDisable(true);


        HBox processBox = new HBox(10, processButton, clearResultsButton, checkMissingDaysButton);
        processBox.setAlignment(Pos.CENTER_LEFT);

        HBox searchControls = createSearchControls();

        mismatchesContainer = new VBox(5);
        mismatchesContainer.setPadding(new Insets(5));

        mismatchesScrollPane = new ScrollPane(mismatchesContainer);
        mismatchesScrollPane.setFitToWidth(true);
        mismatchesScrollPane.setPrefHeight(240);
        mismatchesScrollPane.setMaxHeight(240);
        mismatchesScrollPane.setStyle("-fx-border-color: #0078D7; -fx-border-width: 2px;");

        placeholder.setStyle("-fx-text-fill: gray; -fx-font-style: italic;");
        mismatchesContainer.getChildren().add(placeholder);

        Button saveButton = new Button("Save Changes/Salvar Alterações");
        saveButton.setStyle("-fx-base: #4CAF50; -fx-text-fill: white;");

        Button cancelButton = new Button("Cancel Changes/Cancelar Alterações");
        cancelButton.setStyle("-fx-base: #F44336; -fx-text-fill: white;");

        HBox buttonBox = new HBox(10, saveButton, cancelButton);
        buttonBox.setAlignment(Pos.CENTER_LEFT);
        VBox.setMargin(buttonBox, new Insets(5, 0, 0, 0));

        openFileButton.setOnAction(event -> {
            try {
                File selectedFile = FileLoader.openFile(primaryStage, pathField, resultsArea,
                        mismatchesContainer, placeholder,
                        new File(System.getProperty("user.home")), mantraData);

                if (selectedFile == null) return;

                pathField.setText(selectedFile.getAbsolutePath());

                mantraData.setFilePath(selectedFile.getAbsolutePath());
                mantraData.setLines(FileLoader.robustReadLines(selectedFile.toPath()));
                mantraData.setFromZip(selectedFile.getName().toLowerCase().endsWith(".zip"));
                mantraData.setOriginalZipPath(
                        mantraData.isFromZip() ? selectedFile.getAbsolutePath() : null
                );

                resultsArea.setText("Count Mantras\n(Contar Mantras)");
                resultsArea.setStyle("-fx-text-fill: gray; -fx-font-style: italic;");
                resultsArea.setEditable(false);

                mismatchesContainer.getChildren().clear();
                mismatchesContainer.getChildren().add(placeholder);
                checkMissingDaysButton.setDisable(true);
                UIUtils.showInfo("✔ File loaded.\n✔ Arquivo carregado.");
            } catch (Exception ex) {
                ex.printStackTrace();
                UIUtils.showError("❌ Failed to load file: " + ex.getMessage() +
                        "\n❌ Falha ao carregar o arquivo: " + ex.getMessage());
            }
        });


        processButton.setOnAction(e -> {
            boolean ready = MissingDaysHelper.prepareDataForMissingDays(
                    dateField.getText(),
                    mantraField.getText(),
                    pathField.getText(),
                    mantraData
            );
            if (!ready) return;

            // ✅ Se for .zip, extrair o .txt
            if (mantraData.getFilePath().toLowerCase().endsWith(".zip")) {
                try {
                        File extracted = FileProcessorService.extractFirstTxtFromZip(new File(mantraData.getFilePath()));
                        mantraData.setFromZip(true);
                        mantraData.setOriginalZipPath(mantraData.getFilePath());  // Keep zip path for writing back
                        mantraData.setFilePath(extracted.getAbsolutePath());      // Update to txt path
                        mantraData.setLines(FileLoader.robustReadLines(extracted.toPath())); // ✅ Fix: Read actual .txt

                } catch (Exception ex) {
                    ex.printStackTrace();
                    UIUtils.showError("❌ Falha ao extrair arquivo ZIP.\n❌ Failed to extract .zip file.");
                    return;
                }
            }

            try {
                MantraCount.processFile(mantraData); // ✅ Wrapper que chama FileProcessorService

                displayResults(resultsArea); // mostra resumo
                this.mismatchedLines = mantraData.getDebugLines(); // linhas com erro
                displayMismatchedLines(mismatchedLines); // mostra na UI

                checkMissingDaysButton.setDisable(mismatchedLines.isEmpty());
            } catch (Exception ex) {
                ex.printStackTrace();
                UIUtils.showError("❌ Erro ao processar o arquivo.\n❌ Error processing file: " + ex.getMessage());
            }
        });




        saveButton.setOnAction(e -> {
            try {
                if (mantraData.getLines() == null || mismatchedLines == null) {
                    UIUtils.showError("❌ No file loaded.\n❌ Nenhum arquivo carregado.");
                    return;
                }

                // Extract updated mismatch lines from UI
                Map<String, String> updatedMismatchMap = extractUpdatedContentFromUI();
                int updateCount = updateFileContent(updatedMismatchMap);

                // Save to file
                FileEditSaver.saveToFile(mantraData.getLines(), mantraData.getFilePath());

                // If from ZIP, update ZIP file too
                if (mantraData.isFromZip()) {
                    FileEditSaver.updateZipFile(
                            mantraData.getOriginalZipPath(),
                            mantraData.getFilePath(),
                            mantraData.getLines()
                    );
                }

                UIUtils.showInfo("✔ Changes saved successfully.\n✔ " + updateCount + " line(s) updated." +
                        "\n\n✔ Alterações salvas com sucesso.\n✔ " + updateCount + " linha(s) atualizada(s).");

                resetSearchState();

            } catch (Exception ex) {
                ex.printStackTrace();
                UIUtils.showError("❌ Failed to save changes.\n❌ Falha ao salvar alterações:\n" + ex.getMessage());
            }
        });


        cancelButton.setOnAction(e -> {
            if (mantraData.getDebugLines() != null) {
                displayMismatchedLines(mantraData.getDebugLines()); // reset UI to original mismatches
                UIUtils.showInfo("✔ Changes reverted.\n✔ Alterações desfeitas.");
                resetSearchState();
                checkMissingDaysButton.setDisable(true); // disable until reprocessed
            }
        });



        checkMissingDaysButton.setOnAction(e -> {
            try {
                LocalDate parsedDate = mantraData.getTargetDate();
                String mantraKeyword = mantraData.getNameToCount();
                List<String> lines = mantraData.getLines();

                if (parsedDate == null || mantraKeyword == null || lines == null) {
                    UIUtils.showError("❌ Missing data to check for missing days.\n❌ Dados ausentes para checar dias faltantes.");
                    return;
                }

                MissingDaysUI missingDaysUI = new MissingDaysUI();
                missingDaysUI.show(primaryStage, mantraData);


            } catch (Exception ex) {
                ex.printStackTrace();
                UIUtils.showError("❌ Error checking missing days:\n❌ Erro ao verificar dias faltantes:\n" + ex.getMessage());
            }
        });

        vbox.getChildren().addAll(dateField, mantraField, new HBox(10, pathField, openFileButton), resultsArea, processBox, searchControls, mismatchesScrollPane, buttonBox);

        primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("/icons/BUDA.jpg")));
        setupSearchFeature();
        primaryStage.show();
    }
    private List<String> extractEditedLines() {
        List<String> updatedLines = new ArrayList<>();

        for (Node node : mismatchesContainer.getChildren()) {
            if (node == placeholder) continue;

            if (node instanceof HBox lineContainer) {
                Label protectedLabel = (Label) lineContainer.getChildren().get(0);
                TextField editableField = (TextField) lineContainer.getChildren().get(1);
                updatedLines.add(protectedLabel.getText() + editableField.getText());
            } else if (node instanceof TextField fullLineField) {
                updatedLines.add(fullLineField.getText());
            }
        }

        return updatedLines;
    }


    private void clearMismatchDisplay() {
        mismatchesContainer.getChildren().clear();
        mismatchesContainer.getChildren().add(placeholder);
    }

    private void setupSearchFeature() {
        Scene scene = primaryStage.getScene();
        scene.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, event -> {
            if (event.isControlDown() && event.getCode() == KeyCode.F) {
                searchField.requestFocus();
                event.consume();
            }
        });
    }

    private void resetSearchState() {
        if (searchField != null) {
            clearSearchHighlights();
            lastSearchQuery = "";
            currentSearchIndex = -1;
            searchMatches.clear();
            if (prevButton != null) prevButton.setDisable(true);
            if (nextButton != null) nextButton.setDisable(true);
            Label resultsLabel = (Label) searchButton.getParent().lookup("#searchResultsLabel");
            if (resultsLabel != null) {
                resultsLabel.setText("");
            }
        }
    }

    private void displayResults(TextArea resultsArea) {
        String formattedDate = mantraData.getTargetDate().format(DateTimeFormatter.ofPattern("MM/dd/yyyy"));
        resultsArea.setText("✔ Results from " + formattedDate + ":\n--\n" +
                "Total '" + mantraData.getNameToCount() + "' count: " + mantraData.getTotalNameCount() + "\n" +
                "Total 'Mantra(s)' count: " + mantraData.getTotalMantrasCount() + "\n" +
                "Total 'Fiz' count: " + mantraData.getTotalFizCount() + "\n" +
                "Sum of mantras: " + mantraData.getTotalFizNumbersSum());
        resultsArea.setStyle("-fx-text-fill: black;");
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
    private void saveUpdatedContent() throws IOException {
        FileEditSaver.saveEdits(mantraData, extractEditedLines(), () -> UIUtils.showInfo("✔ File saved."));

        if (mantraData.isFromZip()) {
            FileEditSaver.updateZipFile(
                    mantraData.getOriginalZipPath(),
                    mantraData.getFilePath(),
                    extractEditedLines()
            );
        }
    }

    private void saveChanges() {
        try {
            if (mantraData.getLines() == null || mismatchedLines == null) {
                showError("❌ No file loaded.\n❌ Nenhum arquivo carregado.");
                return;
            }

            Map<String, String> updatedMismatchMap = extractUpdatedContentFromUI();
            int updateCount = updateFileContent(updatedMismatchMap);

            FileEditSaver.saveToFile(mantraData.getLines(), mantraData.getFilePath());

            if (mantraData.isFromZip()) {
                FileEditSaver.updateZipFile(mantraData.getOriginalZipPath(), mantraData.getFilePath(), mantraData.getLines());
            }

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
        List<String> originalLines = mantraData.getLines();
        List<String> updatedLines = new ArrayList<>(originalLines);

        Map<String, List<Integer>> lineIndexMap = new HashMap<>();
        for (int i = 0; i < originalLines.size(); i++) {
            String line = originalLines.get(i);
            lineIndexMap.computeIfAbsent(line, k -> new ArrayList<>()).add(i);
        }

        for (Map.Entry<String, String> entry : updatedMismatchMap.entrySet()) {
            String originalLine = entry.getKey();
            String updatedLine = entry.getValue();

            List<Integer> indexes = lineIndexMap.get(originalLine);
            if (indexes != null) {
                for (int idx : indexes) {
                    updatedLines.set(idx, updatedLine);
                    updateCount++;
                }
            }
        }

        mantraData.setLines(updatedLines);
        return updateCount;
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
    private void checkMissingDays(TextField dateField, TextField mantraField, TextField pathField) {
        try {
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

            // Validar entradas
            ValidationResult validation = InputValidator.validateInputs(inputDate, mantraKeyword, filePath);
            if (!validation.isValid()) {
                showError(validation.getErrorMessage());
                return;
            }

            // Analisar a data
            LocalDate parsedDate = DateParser.parseDate(inputDate);

            // Atualizar mantraData antes de passar para MissingDaysUI
            mantraData.setTargetDate(parsedDate);
            mantraData.setNameToCount(mantraKeyword);
            mantraData.setLines(Files.readAllLines(Paths.get(filePath), StandardCharsets.UTF_8));

            // Abrir a janela de análise de dias faltantes
            MissingDaysUI missingDaysUI = new MissingDaysUI();
            missingDaysUI.show(primaryStage, mantraData);

            // Mostrar contagem de dias faltantes em uma mensagem após análise
            int missingCount = missingDaysUI.getMissingDaysCount();
            if (missingCount > 0) {
                showInfo("Found " + missingCount + " missing days in sequence.\n" +
                        "Encontrados " + missingCount + " dias faltantes na sequência.");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            showError("❌ Error checking missing days: " + ex.getMessage() +
                    "\n❌ Erro ao verificar dias faltantes: " + ex.getMessage());
        }


        }
    private HBox createSearchControls() {
        searchField = new TextField();
        setPlaceholder(searchField, "Search words... (Buscar palavras...)");
        searchField.setPrefWidth(200);

        exactWordCheckBox = new CheckBox("Exact words (Palavras Exatas)");
        exactWordCheckBox.setSelected(true);
        exactWordCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (!searchField.getText().trim().isEmpty()) {
                performSearch();
            }
        });

        searchField.setOnKeyPressed(event -> {
            if (event.isControlDown() && event.getCode() == KeyCode.F) {
                searchField.requestFocus();
            } else if (event.getCode() == KeyCode.ENTER) {
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

    }