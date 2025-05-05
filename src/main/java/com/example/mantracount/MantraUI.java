package com.example.mantracount;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.File;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MantraUI extends Application {

    private TextField searchField;
    private Button searchButton, prevButton, nextButton;
    private CheckBox exactWordCheckBox;
    private List<String> mismatchedLines;
    private VBox mismatchesContainer;
    private final Label placeholder = new Label("Mismatch Line / Discrepância de linhas");
    private ScrollPane mismatchesScrollPane;
    private Stage primaryStage;
    private final MantraData mantraData = new MantraData();
    private String lastSearchQuery = "";  // Store the last search query
    private int currentSearchIndex = -1;  // Store the current index for search navigation
    private List<Node> searchMatches = new ArrayList<>();  // Store search matches
    // Store original mismatched lines to support cancel functionality
    private List<String> originalMismatchedLines = new ArrayList<>();



    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        UpdateChecker.checkForUpdates();
        this.primaryStage = primaryStage;
        primaryStage.setTitle("MantraCount");

        VBox vbox = new VBox(10);
        vbox.setPadding(new Insets(20));
        Scene scene = new Scene(vbox, 700, 600);
        primaryStage.setScene(scene);



        TextField dateField = new TextField();
        UIUtils.setPlaceholder(dateField, "Enter start date - MM/DD/YY / Colocar Data Inicial - MM/DD/AA");
        dateField.setStyle("-fx-font-family: 'Segoe UI';");

        TextField mantraField = new TextField();
        UIUtils.setPlaceholder(mantraField, "Enter mantra name / Colocar nome do Mantra");

        TextField pathField = new TextField();
        UIUtils.setPlaceholder(pathField, "Open a file... / Abrir Arquivo...");
        pathField.setPrefWidth(400);

        Button openFileButton = new Button("Open File / Abrir Arquivo");

        TextArea resultsArea = new TextArea("Mantra Count / Contagem de Mantras");
        resultsArea.setStyle("-fx-text-fill: gray;-fx-font-style: italic;");
        resultsArea.setPrefRowCount(6);
        resultsArea.setMinHeight(114);
        resultsArea.setMaxHeight(114);
        resultsArea.setEditable(false);
        resultsArea.setWrapText(true);

        Button processButton = new Button("Count Mantras / Contar Mantras");
        processButton.setStyle("-fx-base: #4CAF50; -fx-text-fill: white;");

        Button clearResultsButton = new Button("Clear Results / Limpar Resultados");
        clearResultsButton.setStyle("-fx-base: #F44336; -fx-text-fill: white;");

        Button checkMissingDaysButton = new Button("Check Missing Days / Verificar Saltos de Dias");
        checkMissingDaysButton.setStyle("-fx-base: #2196F3; -fx-text-fill: white;");
        checkMissingDaysButton.setDisable(true);

        HBox processBox = new HBox(10, processButton, clearResultsButton, checkMissingDaysButton);
        processBox.setAlignment(Pos.CENTER_LEFT);

        mismatchesContainer = new VBox(10);
        mismatchesContainer.setPadding(new Insets(10));

        mismatchesScrollPane = new ScrollPane(mismatchesContainer);
        mismatchesScrollPane.setFitToWidth(true);
        mismatchesScrollPane.setPrefHeight(240);
        mismatchesScrollPane.setMaxHeight(240);
        mismatchesScrollPane.setStyle("-fx-border-color: #0078D7; -fx-border-width: 2px;");
        VBox.setVgrow(mismatchesScrollPane, Priority.ALWAYS); // ← allow it to grow

        placeholder.setStyle("-fx-text-fill: gray; -fx-font-style: italic;");
        mismatchesContainer.getChildren().add(placeholder);

        Button saveButton = new Button("Save Changes / Salvar Alterações");
        saveButton.setStyle("-fx-base: #4CAF50; -fx-text-fill: white;");

        Button cancelButton = new Button("Cancel Changes / Cancelar Alterações");
        cancelButton.setStyle("-fx-base: #F44336; -fx-text-fill: white;");

        Button updateButton = new Button("\uD83D\uDD04");
        updateButton.setStyle("-fx-font-size: 10px; -fx-background-color: #6A1B9A; -fx-text-fill: white;");
        updateButton.setOnAction(e -> AutoUpdater.checkForUpdates());

        Label updateLabel = new Label(" - Update / Atualizar");
        updateLabel.setStyle("-fx-text-fill: black; -fx-font-size: 12px;");


        HBox buttonBox = new HBox(10, saveButton, cancelButton, updateButton, updateLabel);


        buttonBox.setAlignment(Pos.CENTER_LEFT);
        VBox.setMargin(buttonBox, new Insets(5, 0, 0, 0));

        // Create search interface
        searchField = new TextField();
        UIUtils.setPlaceholder(searchField, "Search... / Buscar...");

        exactWordCheckBox = new CheckBox("Exact word / Palavra exata");
        exactWordCheckBox.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (oldValue != newValue) {  // Only reset if the value actually changed
                resetSearchState();

                // If there's text in the search field, automatically perform a new search
                if (searchField.getText() != null && !searchField.getText().isEmpty()) {
                    searchInMismatches();
                }
            }
        });

        searchButton = new Button("Search / Buscar");
        prevButton = new Button("◀ Prev / Anterior");
        nextButton = new Button("Next / Próximo ▶");

        exactWordCheckBox.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (oldValue != newValue) {  // Only reset if the value actually changed
                // Clear highlights and reset search state without showing any popups
                unhighlightCurrentMatch();
                searchMatches.clear();
                currentSearchIndex = -1;
                prevButton.setDisable(true);
                nextButton.setDisable(true);

                // If there's text in the search field, automatically perform a new search
                // but don't show any notifications if no matches are found
                if (searchField.getText() != null && !searchField.getText().isEmpty()) {
                    searchInMismatchesQuietly();
                }
            }
        });
        HBox searchBox = new HBox(10, searchField, exactWordCheckBox, searchButton, prevButton, nextButton);
        searchBox.setAlignment(Pos.CENTER_LEFT);

        // Set initial state for navigation buttons
        prevButton.setDisable(true);
        nextButton.setDisable(true);

        openFileButton.setOnAction(event -> {
            try {
                File selectedFile = FileLoader.openFile(primaryStage, pathField, resultsArea, mismatchesContainer, placeholder, new File(System.getProperty("user.home")), mantraData);

                if (selectedFile == null) return;

                pathField.setText(selectedFile.getAbsolutePath());
                mantraData.setFilePath(selectedFile.getAbsolutePath());
                mantraData.setLines(FileLoader.robustReadLines(selectedFile.toPath()));
                mantraData.setFromZip(selectedFile.getName().toLowerCase().endsWith(".zip"));
                mantraData.setOriginalZipPath(mantraData.isFromZip() ? selectedFile.getAbsolutePath() : null);

                resultsArea.setText("Count Mantras / Contar Mantras");
                resultsArea.setStyle("-fx-text-fill: gray; -fx-font-style: italic;");
                resultsArea.setEditable(false);

                mismatchesContainer.getChildren().clear();
                mismatchesContainer.getChildren().add(placeholder);
                checkMissingDaysButton.setDisable(true);
                UIUtils.showInfo("✔ File loaded. \n✔ Arquivo carregado.");
            } catch (Exception ex) {
                ex.printStackTrace();
                UIUtils.showError("❌ Failed to load file." + ex.getMessage() + "\n❌ Falha ao carregar arquivo" + ex.getMessage());
            }
        });

        // Correção para o processButton.setOnAction no MantraUI
        processButton.setOnAction(e -> {
            try {
                // Define placeholders for validation
                String datePlaceholder = "Enter start date - MM/DD/YY / Colocar Data Inicial - MM/DD/AA";
                String mantraPlaceholder = "Enter mantra name / Colocar nome do Mantra";
                String pathPlaceholder = "Open a file... / Abrir Arquivo...";

                // Validate date and mantra fields

                if (!UIUtils.validateField(dateField, "❌ Missing or invalid field \n❌ Campo ausente ou inválido",
                        "Please enter the Date\nPor favor, insira a Data", datePlaceholder)) return;

                if (!UIUtils.validateField(mantraField, "❌ Missing or invalid field \n❌ Campo ausente ou inválido",
                        "Please enter the Mantra\nPor favor, insira o Mantra", mantraPlaceholder)) return;

                if (pathField.getText() == null || pathField.getText().trim().isEmpty() ||
                        pathField.getText().equals("Open a file... / Abrir Arquivo...")) {
                    UIUtils.showError("❌ Missing or invalid field. \n❌ Campo ausente ou inválido.",
                            "Please enter the File\nPor favor, insira o Arquivo");
                    return;
                }


                // Preparar dados usando MissingDaysHelper
                boolean ready = MissingDaysHelper.prepareDataForMissingDays(
                        dateField.getText(),
                        mantraField.getText(),
                        pathField.getText(),
                        mantraData
                );

                if (!ready) {
                    return; // MissingDaysHelper já mostrou o erro apropriado
                }

                // Processar o arquivo
                FileProcessorService.processFile(mantraData);

                // Exibir resultados
                displayResults(resultsArea);
                mismatchedLines = mantraData.getDebugLines();
                displayMismatchedLines(mismatchedLines);

                // Verificar dias ausentes
                List<MissingDaysDetector.MissingDayInfo> missingDays =
                        MissingDaysDetector.detectMissingDays(
                                mantraData.getLines(),
                                mantraData.getTargetDate(),
                                mantraData.getNameToCount()
                        );

                // Habilitar/desabilitar o botão de dias ausentes
                checkMissingDaysButton.setDisable(missingDays.isEmpty());

                UIUtils.showInfo("✔Processing completed.\n✔ Processamento concluído.");
            } catch (Exception ex) {
                ex.printStackTrace();
                UIUtils.showError("❌ Erro ao processar arquivo." + ex.getMessage() + "\n❌Error processing file." + ex.getMessage());
            }
        });

        clearResultsButton.setOnAction(e -> {
            resultsArea.setText("Count Mantras / Contar Mantras");
            resultsArea.setStyle("-fx-text-fill: gray; -fx-font-style: italic;");
            mismatchesContainer.getChildren().clear();
            mismatchesContainer.getChildren().add(placeholder);
            mismatchedLines = null;
            originalMismatchedLines.clear();
            resetSearchState();
            checkMissingDaysButton.setDisable(true);
            UIUtils.showInfo("✔ Results cleared. \n✔ Resultados limpos.");
        });

        saveButton.setOnAction(e -> {
            try {
                if (mantraData.getLines() == null || mismatchedLines == null) {
                    UIUtils.showError("No data. / Sem dados.",
                            "No file loaded or processed. \nNenhum arquivo carregado ou processado.");
                    return;
                }

                Map<String, String> updatedMismatchMap = extractUpdatedContentFromUI();
                int updateCount = updateFileContent(updatedMismatchMap);
                FileEditSaver.saveToFile(mantraData.getLines(), mantraData.getFilePath());

                if (mantraData.isFromZip()) {
                    FileEditSaver.updateZipFile(mantraData.getOriginalZipPath(), mantraData.getFilePath(), mantraData.getLines());
                }

                UIUtils.showInfo("✔ Changes saved successfully. \n✔ Alterações salvas com sucesso.\n" +
                        "✔ " + updateCount + " line(s) updated. \n✔ " + updateCount + " linha(s) atualizada(s).");

                // Update original lines after save to support future cancel operations
                originalMismatchedLines = new ArrayList<>(mismatchedLines);
                resetSearchState();

            } catch (Exception ex) {
                ex.printStackTrace();
                UIUtils.showError("❌ Failed to save changes. \n❌ Falha ao salvar alterações.");
            }
        });

        cancelButton.setOnAction(e -> {
            if (originalMismatchedLines != null && !originalMismatchedLines.isEmpty()) {
                // Restore the mismatchedLines to their original state
                mismatchedLines = new ArrayList<>(originalMismatchedLines);
                // Redisplay the original mismatched lines
                displayMismatchedLines(mismatchedLines);
                UIUtils.showInfo("✔ Changes reverted. \n✔ Alterações revertidas.");
                resetSearchState();
            } else {
                UIUtils.showInfo("No changes to revert. \nNão há alterações para reverter.");
            }
        });

        checkMissingDaysButton.setOnAction(e -> {
            try {
                MissingDaysUI missingDaysUI = new MissingDaysUI();
                missingDaysUI.show(primaryStage, mantraData);
            } catch (Exception ex) {
                ex.printStackTrace();
                UIUtils.showError("❌ Error checking missing days." + ex.getMessage() + "\n❌ Erro ao verificar saltos de dias:" + ex.getMessage());
            }
        });

        searchButton.setOnAction(e -> searchInMismatches());
        prevButton.setOnAction(e -> navigateSearch(-1));
        nextButton.setOnAction(e -> navigateSearch(1));

        vbox.getChildren().addAll(
                dateField,
                mantraField,
                new HBox(10, pathField, openFileButton),
                resultsArea,
                processBox,
                searchBox,
                mismatchesScrollPane,
                buttonBox
        );

        primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("/icons/BUDA.jpg")));
        primaryStage.show();

        primaryStage.setOnCloseRequest(event -> {
            // Encerrar o serviço de atualização automática ao fechar o aplicativo
            AutoUpdater.shutdown();
        });}

    private void displayResults(TextArea resultsArea) {
        Label emojiLabel = new Label("📿");
        String word = mantraData.getNameToCount();
        String capitalized = capitalizeFirst(word);  // "Vajrasattva"

        String formattedDate = mantraData.getTargetDate().format(DateTimeFormatter.ofPattern("MM/dd/yyyy"));
        resultsArea.setText("✔ Results from / Resultados de " + formattedDate + ":\n--\n" +
                "Total '" + capitalized + "': " + mantraData.getTotalNameCount() + "\n" +
                "Total 'Mantra(s)': " + mantraData.getTotalMantrasCount() + "\n" +
                "Total 'Fiz': " + mantraData.getTotalFizCount() + "\n" +
                "Total " + emojiLabel.getText() + ": " + mantraData.getTotalFizNumbersSum());
        resultsArea.setStyle("-fx-text-fill: black;");
    }

    public static String capitalizeFirst(String input) {
        if (input == null || input.isEmpty()) return input;
        return input.substring(0, 1).toUpperCase() + input.substring(1);
    }

    private void displayMismatchedLines(List<String> mismatchedLines) {
        mismatchesContainer.getChildren().clear();
        if (mismatchedLines == null || mismatchedLines.isEmpty()) {
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
                protectedLabel.setMinWidth(Region.USE_PREF_SIZE);

                TextField editableField = new TextField(editablePart);
                // Make text field expand to fill available space
                HBox.setHgrow(editableField, Priority.ALWAYS);
                editableField.setMaxWidth(Double.MAX_VALUE);

                HBox lineContainer = new HBox(5, protectedLabel, editableField);
                lineContainer.setAlignment(Pos.CENTER_LEFT);
                mismatchesContainer.getChildren().add(lineContainer);
            } else {
                TextField fullLineField = new TextField(line);
                // Make text field expand to fill available space
                HBox.setHgrow(fullLineField, Priority.ALWAYS);
                fullLineField.setMaxWidth(Double.MAX_VALUE);

                HBox lineContainer = new HBox(fullLineField);
                lineContainer.setAlignment(Pos.CENTER_LEFT);
                HBox.setHgrow(lineContainer, Priority.ALWAYS);

                mismatchesContainer.getChildren().add(lineContainer);
            }
        }
    }

    private Map<String, String> extractUpdatedContentFromUI() {
        Map<String, String> updatedMismatchMap = new HashMap<>();
        for (int i = 0; i < mismatchesContainer.getChildren().size(); i++) {
            Node node = mismatchesContainer.getChildren().get(i);
            if (node == placeholder || i >= mismatchedLines.size()) continue;
            String originalLine = mismatchedLines.get(i);
            String updatedLine = extractUpdatedLineFromNode(node);
            if (updatedLine != null) {
                updatedMismatchMap.put(originalLine, updatedLine);
            }
        }
        return updatedMismatchMap;
    }

    private String extractUpdatedLineFromNode(Node node) {
        if (node instanceof HBox lineContainer) {
            if (lineContainer.getChildren().size() >= 2) {
                Node firstChild = lineContainer.getChildren().get(0);
                Node secondChild = lineContainer.getChildren().get(1);

                if (firstChild instanceof Label protectedLabel && secondChild instanceof TextField editableField) {
                    return protectedLabel.getText() + editableField.getText();
                }
            }
        } else if (node instanceof TextField fullLineField) {
            return fullLineField.getText();
        }
        return null;
    }

    private int updateFileContent(Map<String, String> updatedMismatchMap) {
        int updateCount = 0;
        List<String> originalLines = mantraData.getLines();
        List<String> updatedLines = new ArrayList<>(originalLines);

        for (Map.Entry<String, String> entry : updatedMismatchMap.entrySet()) {
            String originalLine = entry.getKey();
            String updatedLine = entry.getValue();
            for (int i = 0; i < originalLines.size(); i++) {
                if (originalLines.get(i).equals(originalLine)) {
                    updatedLines.set(i, updatedLine);
                    updateCount++;
                }
            }
        }

        mantraData.setLines(updatedLines);
        return updateCount;
    }
    private void searchInMismatches() {
        String query = searchField.getText();
        if (query == null || query.isEmpty() || mismatchesContainer.getChildren().isEmpty() ||
                mismatchesContainer.getChildren().get(0) == placeholder) {
            return;
        }

        // Clear previous search highlights
        unhighlightCurrentMatch();
        searchMatches.clear();

        // Reset search state
        lastSearchQuery = query;
        currentSearchIndex = -1;

        boolean exactWord = exactWordCheckBox.isSelected();

        // Find all matches
        for (Node node : mismatchesContainer.getChildren()) {
            if (node instanceof HBox lineContainer) {
                if (lineContainer.getChildren().size() >= 2 &&
                        lineContainer.getChildren().get(1) instanceof TextField editableField) {
                    if (containsSearch(editableField.getText(), query, exactWord)) {
                        searchMatches.add(lineContainer);
                    }
                }
            } else if (node instanceof TextField fullLineField) {
                if (containsSearch(fullLineField.getText(), query, exactWord)) {
                    searchMatches.add(node);
                }
            }
        }

        // Update navigation buttons state
        updateNavigationButtonState();

        // Navigate to first result if any
        if (!searchMatches.isEmpty()) {
            navigateSearch(1);
        } else {
            UIUtils.showInfo("No matches found. \nNenhuma correspondência encontrada.");
        }
    }
    private void searchInMismatchesQuietly() {
        String query = searchField.getText();
        if (query == null || query.isEmpty() || mismatchesContainer.getChildren().isEmpty() ||
                mismatchesContainer.getChildren().get(0) == placeholder) {
            return;
        }

        // Clear previous search highlights
        unhighlightCurrentMatch();
        searchMatches.clear();

        // Reset search state
        lastSearchQuery = query;
        currentSearchIndex = -1;

        boolean exactWord = exactWordCheckBox.isSelected();

        // Find all matches
        for (Node node : mismatchesContainer.getChildren()) {
            if (node instanceof HBox lineContainer) {
                if (lineContainer.getChildren().size() >= 2 &&
                        lineContainer.getChildren().get(1) instanceof TextField editableField) {
                    if (containsSearch(editableField.getText(), query, exactWord)) {
                        searchMatches.add(lineContainer);
                    }
                }
            } else if (node instanceof TextField fullLineField) {
                if (containsSearch(fullLineField.getText(), query, exactWord)) {
                    searchMatches.add(node);
                }
            }
        }

        // Update navigation buttons state
        updateNavigationButtonState();

        // Navigate to first result if any
        if (!searchMatches.isEmpty()) {
            navigateSearch(1);
        }}

    private boolean containsSearch(String text, String query, boolean exactWord) {
        if (text == null || query == null) return false;

        if (exactWord) {
            // Add word boundary check for exact word match
            // This uses regex word boundaries to ensure we're matching whole words
            String regex = "\\b" + java.util.regex.Pattern.quote(query) + "\\b";
            return java.util.regex.Pattern.compile(regex, java.util.regex.Pattern.CASE_INSENSITIVE)
                    .matcher(text).find();
        } else {
            return text.toLowerCase().contains(query.toLowerCase());
        }
    }

    private void navigateSearch(int direction) {
        if (searchMatches.isEmpty()) {
            return;
        }

        // Unhighlight current match if any
        unhighlightCurrentMatch();

        // Calculate new index
        currentSearchIndex += direction;
        if (currentSearchIndex < 0) {
            currentSearchIndex = searchMatches.size() - 1;
        } else if (currentSearchIndex >= searchMatches.size()) {
            currentSearchIndex = 0;
        }

        // Highlight new match
        highlightCurrentMatch();

        // Scroll to the current match
        Node currentMatch = searchMatches.get(currentSearchIndex);
        mismatchesScrollPane.setVvalue(
                (double) mismatchesContainer.getChildren().indexOf(currentMatch) / mismatchesContainer.getChildren().size()
        );

        // Update navigation buttons state
        updateNavigationButtonState();
    }

    private void highlightCurrentMatch() {
        if (currentSearchIndex >= 0 && currentSearchIndex < searchMatches.size()) {
            Node node = searchMatches.get(currentSearchIndex);

            // Check what type of container we're dealing with
            if (node instanceof HBox lineContainer) {
                // Only highlight the editable part (TextField)
                for (Node child : lineContainer.getChildren()) {
                    if (child instanceof TextField) {
                        child.setStyle("-fx-background-color: #FFFF99;");
                        break;
                    }
                }
            } else if (node instanceof TextField) {
                // Highlight the whole TextField
                node.setStyle("-fx-background-color: #FFFF99;");
            }
        }
    }

    private void unhighlightCurrentMatch() {
        if (currentSearchIndex >= 0 && currentSearchIndex < searchMatches.size()) {
            Node node = searchMatches.get(currentSearchIndex);

            // Check what type of container we're dealing with
            if (node instanceof HBox lineContainer) {
                // Only un-highlight the editable part (TextField)
                for (Node child : lineContainer.getChildren()) {
                    if (child instanceof TextField) {
                        child.setStyle("");
                        break;
                    }
                }
            } else if (node instanceof TextField) {
                // Un-highlight the whole TextField
                node.setStyle("");
            }
        }
    }

    private void resetSearchState() {
        if (searchField != null) {
            lastSearchQuery = "";
            currentSearchIndex = -1;
            searchMatches.clear();
            prevButton.setDisable(true);
            nextButton.setDisable(true);

            // Remove any highlights, but only from TextFields
            for (Node node : mismatchesContainer.getChildren()) {
                if (node instanceof HBox lineContainer) {
                    for (Node child : lineContainer.getChildren()) {
                        if (child instanceof TextField) {
                            child.setStyle("");
                        }
                    }
                } else if (node instanceof TextField) {
                    node.setStyle("");
                }
            }
        }
    }

    private void updateNavigationButtonState() {
        boolean hasMatches = !searchMatches.isEmpty();
        prevButton.setDisable(!hasMatches);
        nextButton.setDisable(!hasMatches);
    }

}