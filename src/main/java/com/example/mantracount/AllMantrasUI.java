package com.example.mantracount;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class AllMantrasUI {
    private VBox entriesContainer;
    private ProgressIndicator progressIndicator;
    private MantraData mantraData;
    private LocalDate startDate;
    private DatePicker endDatePicker;
    private List<MantraEntry> allEntries = new ArrayList<>();
    private ScrollPane scrollPane;
    private FileManagementController fileController;
    private SearchController searchController;

    private Label summaryLabel;
    private HBox summaryPanel;
    private Map<String, Integer> mantraTypeCounts = new HashMap<>();
    private Map<String, Integer> mantraTypeNumbers = new HashMap<>();

    // Represents a mantra entry (for data handling)
    public static class MantraEntry {
        private final LocalDate date;
        private final String lineContent;
        private final String mantraType;
        private final int count;

        public MantraEntry(LocalDate date, String lineContent, String mantraType, int count) {
            this.date = date;
            this.lineContent = lineContent;
            this.mantraType = mantraType;
            this.count = count;
        }

        public LocalDate getDate() { return date; }
        public String getLineContent() { return lineContent; }
        public String getMantraType() { return mantraType; }
        public int getCount() { return count; }
    }

    public void show(Stage owner, MantraData data, LocalDate startDate) {
        this.mantraData = data;
        this.startDate = startDate;

        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(owner);
        dialog.setTitle("Todos os Mantras do Período");
        dialog.getIcons().add(new Image(getClass().getResourceAsStream("/icons/BUDA.jpg")));

        summaryPanel = new HBox(15);
        summaryPanel.setPadding(new Insets(10));
        summaryPanel.setAlignment(Pos.CENTER);
        summaryPanel.setStyle("-fx-background-color: #F5F5F5; -fx-border-color: #0078D7; " +
                "-fx-border-width: 2px; -fx-border-radius: 5px; -fx-background-radius: 5px;");

        summaryLabel = new Label("Carregue os mantras para ver o resumo");
        summaryLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: gray;");
        summaryPanel.getChildren().add(summaryLabel);

        Tooltip summaryTooltip = new Tooltip("Summary - Shows count of each mantra type");
        summaryTooltip.setShowDelay(Duration.millis(300));
        Tooltip.install(summaryPanel, summaryTooltip);

        // Create hidden container for FileManagementController
        VBox hiddenContainer = new VBox();
        hiddenContainer.setVisible(false);
        hiddenContainer.setPrefHeight(0);
        hiddenContainer.getChildren().add(new Label());

        // Initialize FileManagementController for save functionality
        fileController = new FileManagementController(
                dialog, mantraData, hiddenContainer, new Label(), new TextArea()
        );

        VBox root = new VBox(10);
        root.setPadding(new Insets(15));

        LocalDate defaultEndDate = LocalDate.now();
        endDatePicker = new DatePicker(defaultEndDate);
        endDatePicker.setPromptText("Data Final");

        Tooltip endDateTooltip = new Tooltip("End Date - Select the final date for the period");
        endDateTooltip.setShowDelay(Duration.millis(300));
        endDateTooltip.setHideDelay(Duration.millis(100));
        Tooltip.install(endDatePicker, endDateTooltip);

        Label endDateLabel = new Label("Data Final:");
        Tooltip.install(endDateLabel, new Tooltip("End Date - Select the final date for the period"));

        HBox dateBox = new HBox(10, endDateLabel, endDatePicker);
        dateBox.setAlignment(Pos.CENTER_LEFT);

        // Load button
        Button loadButton = new Button("Carregar Mantras");
        loadButton.setStyle("-fx-base: #4CAF50; -fx-text-fill: white;");
        loadButton.setOnAction(e -> loadMantras());
        Tooltip.install(loadButton, new Tooltip("Load Mantras - Load all mantras for the selected period"));

        HBox loadBox = new HBox(10, loadButton);
        loadBox.setAlignment(Pos.CENTER_LEFT);

        // Get formatter based on current date format
        DateTimeFormatter localFormatter = (DateParser.getCurrentDateFormat() == DateParser.DateFormat.BR_FORMAT)
                ? DateTimeFormatter.ofPattern("dd/MM/yyyy")
                : DateTimeFormatter.ofPattern("MM/dd/yyyy");

        // Header
        String startDateFormatted = startDate.format(localFormatter);
        String endDateFormatted = defaultEndDate.format(localFormatter);
        Label header = new Label("Todos os Mantras de " + startDateFormatted + " a " + endDateFormatted);
        header.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        Tooltip.install(header, new Tooltip("All Mantras - Shows all mantras from the selected period"));

        // Progress indicator
        progressIndicator = new ProgressIndicator();
        progressIndicator.setMaxSize(50, 50);
        progressIndicator.setVisible(false);

        // Container for entries
        entriesContainer = new VBox(10);
        scrollPane = new ScrollPane(entriesContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(400);
        scrollPane.setStyle("-fx-border-color: #0078D7; -fx-border-width: 1px;");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        // Initial placeholder
        Label placeholder = new Label("Nenhum mantra encontrado");
        placeholder.setStyle("-fx-text-fill: gray; -fx-font-style: italic;");
        Tooltip.install(placeholder, new Tooltip("No mantras found - Select an end date and click Load"));
        entriesContainer.getChildren().add(placeholder);

        // Initialize search controller
        searchController = new SearchController(entriesContainer, scrollPane);
        searchController.adaptToContainerStructure(true);

        // Stats label
        Label statsLabel = new Label("Selecione data final e clique em Carregar");
        Tooltip.install(statsLabel, new Tooltip("Instructions - Select end date and click Load to see mantras"));

        // Action buttons
        Button saveBtn = new Button("💾 Salvar Alterações");
        saveBtn.setStyle("-fx-base: #4CAF50; -fx-text-fill: white;");
        saveBtn.setOnAction(e -> saveChanges());
        Tooltip.install(saveBtn, new Tooltip("Save Changes - Save any edits made to the mantra entries"));

        Button closeBtn = new Button("✖ Fechar");
        closeBtn.setStyle("-fx-base: #F44336; -fx-text-fill: white;");
        closeBtn.setOnAction(e -> dialog.close());
        Tooltip.install(closeBtn, new Tooltip("Close - Close this window"));

        HBox actions = new HBox(10, saveBtn, closeBtn);
        actions.setAlignment(Pos.CENTER_RIGHT);
        actions.setPadding(new Insets(10, 0, 0, 0));

        root.getChildren().addAll(
                header, dateBox, loadBox, searchController.getSearchContainer(),
                summaryPanel,  // Add this
                progressIndicator, scrollPane, statsLabel, actions, hiddenContainer
        );
        // Update header when end date changes
        endDatePicker.valueProperty().addListener((obs, old, newDate) -> {
            if (newDate != null) {
                String newEndDateFormatted = newDate.format(localFormatter);
                header.setText("Todos os Mantras de " + startDateFormatted + " a " + newEndDateFormatted);
            }
        });

        dialog.setScene(new Scene(root, 900, 600));
        dialog.show();
    }

    private void loadMantras() {
        LocalDate endDate = endDatePicker.getValue();
        if (endDate == null) {
            UIUtils.showError("Please select an end date / Por favor, selecione uma data final");
            return;
        }

        if (endDate.isBefore(startDate)) {
            UIUtils.showError("End date cannot be before start date / Data final não pode ser anterior à data inicial");
            return;
        }

        progressIndicator.setVisible(true);
        loadEntriesAsync(mantraData, endDate);
    }

    private void loadEntriesAsync(MantraData data, LocalDate endDate) {
        CompletableFuture.supplyAsync(() -> {
            List<MantraEntry> entries = new ArrayList<>();
            Map<String, Integer> typeCounts = new HashMap<>();
            Map<String, Integer> typeNumbers = new HashMap<>();
            int totalMantras = 0;

            // Analyze each line for mantras
            for (String line : data.getLines()) {
                LocalDate lineDate = LineParser.extractDate(line);

                // Skip if date is missing or outside our range
                if (lineDate == null || lineDate.isBefore(startDate) || lineDate.isAfter(endDate)) {
                    continue;
                }

                // Check if line contains mantra-related content
                if (containsMantraContent(line)) {
                    String mantraType = extractMantraType(line);
                    int count = extractMantraCount(line);

                    entries.add(new MantraEntry(lineDate, line, mantraType, count));

                    // Update type counts
                    typeCounts.put(mantraType, typeCounts.getOrDefault(mantraType, 0) + 1);
                    typeNumbers.put(mantraType, typeNumbers.getOrDefault(mantraType, 0) + count);

                    totalMantras += (count > 0) ? count : 0;
                }
            }

            // Sort by date
            entries.sort(Comparator.comparing(MantraEntry::getDate));
            allEntries = new ArrayList<>(entries);

            return new Object[] { entries, totalMantras, entries.size(), typeCounts, typeNumbers };

        }).thenAcceptAsync(result -> {
            @SuppressWarnings("unchecked")
            List<MantraEntry> entries = (List<MantraEntry>) result[0];
            int totalMantras = (int) result[1];
            int entryCount = (int) result[2];
            @SuppressWarnings("unchecked")
            Map<String, Integer> typeCounts = (Map<String, Integer>) result[3];
            @SuppressWarnings("unchecked")
            Map<String, Integer> typeNumbers = (Map<String, Integer>) result[4];

            // Store the counts
            mantraTypeCounts = typeCounts;
            mantraTypeNumbers = typeNumbers;

            // Update summary panel
            updateSummaryPanel();

            displayEntries(entries);

            // Update status label
            Label statsLabel = getStatsLabel();
            if (statsLabel != null) {
                statsLabel.setText(String.format(
                        "Encontrados %d mantras em %d entradas", totalMantras, entryCount
                ));
            }
            progressIndicator.setVisible(false);

        }, Platform::runLater);
    }

    private void updateSummaryPanel() {
        summaryPanel.getChildren().clear();

        if (mantraTypeCounts.isEmpty()) {
            summaryLabel.setText("Nenhum mantra encontrado");
            summaryPanel.getChildren().add(summaryLabel);
            return;
        }

        // Sort types by count (descending)
        List<Map.Entry<String, Integer>> sortedTypes = new ArrayList<>(mantraTypeCounts.entrySet());
        sortedTypes.sort((a, b) -> b.getValue().compareTo(a.getValue()));

        // Create badges for each type
        for (Map.Entry<String, Integer> entry : sortedTypes) {
            String type = entry.getKey();
            int lineCount = entry.getValue();
            int totalNumber = mantraTypeNumbers.getOrDefault(type, 0);

            VBox typeBox = new VBox(2);
            typeBox.setAlignment(Pos.CENTER);
            typeBox.setPadding(new Insets(5, 10, 5, 10));
            typeBox.setStyle("-fx-background-color: #E3F2FD; -fx-background-radius: 10px;");

            Label typeLabel = new Label(type);
            typeLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #1565C0; -fx-font-size: 12px;");

            Label countLabel = new Label(String.format("%d linhas", lineCount));
            countLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #424242;");

            Label numberLabel = new Label(String.format("Total: %d", totalNumber));
            numberLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #616161;");

            typeBox.getChildren().addAll(typeLabel, countLabel, numberLabel);

            // Add tooltip
            Tooltip typeTooltip = new Tooltip(String.format(
                    "%s: %d entries with %d total mantras", type, lineCount, totalNumber
            ));
            typeTooltip.setShowDelay(Duration.millis(300));
            Tooltip.install(typeBox, typeTooltip);

            // Add click handler to filter by type
            typeBox.setOnMouseClicked(e -> filterByType(type));
            typeBox.setCursor(javafx.scene.Cursor.HAND);

            // Hover effect
            typeBox.setOnMouseEntered(e ->
                    typeBox.setStyle("-fx-background-color: #BBDEFB; -fx-background-radius: 10px;"));
            typeBox.setOnMouseExited(e ->
                    typeBox.setStyle("-fx-background-color: #E3F2FD; -fx-background-radius: 10px;"));

            summaryPanel.getChildren().add(typeBox);
        }

        // Add total at the end
        Separator separator = new Separator(javafx.geometry.Orientation.VERTICAL);
        separator.setPadding(new Insets(0, 5, 0, 5));

        VBox totalBox = new VBox(2);
        totalBox.setAlignment(Pos.CENTER);
        totalBox.setPadding(new Insets(5, 10, 5, 10));
        totalBox.setStyle("-fx-background-color: #C8E6C9; -fx-background-radius: 10px;");

        Label totalLabel = new Label("TOTAL");
        totalLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #2E7D32; -fx-font-size: 12px;");

        int totalLines = mantraTypeCounts.values().stream().mapToInt(Integer::intValue).sum();
        int totalNumbers = mantraTypeNumbers.values().stream().mapToInt(Integer::intValue).sum();

        Label totalCountLabel = new Label(String.format("%d linhas", totalLines));
        totalCountLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #424242;");

        Label totalNumberLabel = new Label(String.format("Total: %d", totalNumbers));
        totalNumberLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #616161;");

        totalBox.getChildren().addAll(totalLabel, totalCountLabel, totalNumberLabel);

        summaryPanel.getChildren().addAll(separator, totalBox);
    }

    private void filterByType(String type) {
        entriesContainer.getChildren().clear();

        List<MantraEntry> filteredEntries = allEntries.stream()
                .filter(entry -> entry.getMantraType().equals(type))
                .collect(java.util.stream.Collectors.toList());

        if (filteredEntries.isEmpty()) {
            Label noResults = new Label("Nenhuma entrada para " + type);
            noResults.setStyle("-fx-text-fill: gray; -fx-font-style: italic;");
            entriesContainer.getChildren().add(noResults);
            return;
        }

        // Add "show all" button
        Button showAllBtn = new Button("← Mostrar Todos");
        showAllBtn.setStyle("-fx-base: #2196F3; -fx-text-fill: white;");
        showAllBtn.setOnAction(e -> displayEntries(allEntries));
        entriesContainer.getChildren().add(showAllBtn);

        // Add filtered entries
        for (MantraEntry entry : filteredEntries) {
            HBox lineEditor = createSearchCompatibleLineEditor(entry);
            entriesContainer.getChildren().add(lineEditor);
        }

        searchController.resetSearchState();
    }

    private void displayEntries(List<MantraEntry> entries) {
        entriesContainer.getChildren().clear();

        if (entries.isEmpty()) {
            Label placeholder = new Label("Nenhum mantra encontrado");
            placeholder.setStyle("-fx-text-fill: gray; -fx-font-style: italic;");
            Tooltip.install(placeholder, new Tooltip("No mantras found - Try adjusting the date range"));
            entriesContainer.getChildren().add(placeholder);
            return;
        }

        for (MantraEntry entry : entries) {
            HBox lineEditor = createSearchCompatibleLineEditor(entry);
            entriesContainer.getChildren().add(lineEditor);
        }

        searchController.resetSearchState();
    }

    private HBox createSearchCompatibleLineEditor(MantraEntry entry) {
        HBox lineEditor = new HBox(10);
        lineEditor.setPadding(new Insets(5));
        lineEditor.setAlignment(Pos.CENTER);
        lineEditor.setUserData(entry.getLineContent());

        // Split line into protected and editable parts
        LineParser.LineSplitResult splitResult = LineParser.splitEditablePortion(entry.getLineContent());
        String protectedText = splitResult.getFixedPrefix();
        String editableText = splitResult.getEditableSuffix();

        // Create first element with mantra type badge and protected text
        HBox firstElement = new HBox(10);

        Label typeBadge = new Label(entry.getMantraType());
        typeBadge.setPadding(new Insets(2, 8, 2, 8));
        typeBadge.setStyle("-fx-background-color: #E3F2FD; -fx-background-radius: 4px; " +
                "-fx-font-weight: bold; -fx-text-fill: #1565C0;");
        typeBadge.setPrefWidth(120);
        Tooltip.install(typeBadge, new Tooltip("Mantra Type - Shows the type of mantra or ritual"));

        Label protectedLabel = new Label(protectedText);
        protectedLabel.setStyle("-fx-font-weight: bold;");
        Tooltip.install(protectedLabel, new Tooltip("Protected content - Date, time and sender name (cannot be edited)"));

        firstElement.getChildren().addAll(typeBadge, protectedLabel);

        // Create editable text field
        TextField editableField = new TextField(editableText);
        editableField.setPromptText("Editar linha");
        HBox.setHgrow(editableField, Priority.ALWAYS);
        editableField.setPrefWidth(400);
        Tooltip.install(editableField, new Tooltip("Edit line - You can modify the content of this mantra entry"));

        lineEditor.getChildren().addAll(firstElement, editableField);
        return lineEditor;
    }

    private Label getStatsLabel() {
        if (scrollPane.getScene() != null) {
            VBox root = (VBox) scrollPane.getScene().getRoot();
            for (int i = 6; i < root.getChildren().size(); i++) {
                if (root.getChildren().get(i) instanceof Label) {
                    return (Label) root.getChildren().get(i);
                }
            }
        }
        return null;
    }

    private void saveChanges() {
        if (entriesContainer.getChildren().isEmpty() ||
                (entriesContainer.getChildren().size() == 1 &&
                        entriesContainer.getChildren().get(0) instanceof Label)) {
            UIUtils.showError("No entries to save. Load entries first. / Sem entradas para salvar. Carregue as entradas primeiro.");
            return;
        }

        Map<String, String> updatedContentMap = extractUpdatedContentFromUI();

        if (updatedContentMap.isEmpty()) {
            UIUtils.showInfo("No changes detected. / Nenhuma alteração detectada.");
            return;
        }

        boolean success = fileController.saveChanges(updatedContentMap);
        if (success) {
            loadMantras(); // Reload entries to refresh the UI
        }
    }

    private Map<String, String> extractUpdatedContentFromUI() {
        Map<String, String> updatedContent = new HashMap<>();

        for (Node node : entriesContainer.getChildren()) {
            if (node instanceof HBox lineContainer && lineContainer.getChildren().size() >= 2) {
                String originalLine = (String) lineContainer.getUserData();
                if (originalLine == null) continue;

                // Extract updated content
                HBox firstElement = (HBox) lineContainer.getChildren().get(0);
                Label protectedLabel = (Label) firstElement.getChildren().get(1);
                TextField editableField = (TextField) lineContainer.getChildren().get(1);

                String updatedLine = protectedLabel.getText() + editableField.getText();

                if (!originalLine.equals(updatedLine)) {
                    updatedContent.put(originalLine, updatedLine);
                }
            }
        }

        return updatedContent;
    }

    private boolean containsMantraContent(String line) {
        String lowerCase = line.toLowerCase();

        // First check: Must contain either "mantra/mantras" or "rito/ritos"
        boolean hasMantraRitoWord = lowerCase.contains("mantra") || lowerCase.contains("mantras") ||
                lowerCase.contains("rito") || lowerCase.contains("ritos");

        if (!hasMantraRitoWord) {
            return false;
        }

        // Second check: Must contain action words using the centralized ActionWordManager
        boolean hasActionWord = ActionWordManager.hasActionWords(line);

        return hasActionWord;
    }

    private String extractMantraType(String line) {
        String lowerCase = line.toLowerCase();

        String[] mantraTypes = {"refúgio", "vajrasattva", "vajrasatva", "refugio", "guru", "bodisatva",
                "bodhisattva", "buda", "buddha", "tare", "tara", "medicina", "preliminares"};

        for (String type : mantraTypes) {
            if (lowerCase.contains(type)) {
                // Use canonical form for display
                String canonical = SynonymManager.getCanonicalForm(type);
                return canonical.substring(0, 1).toUpperCase() + canonical.substring(1);
            }
        }

        if (lowerCase.contains("mantra")) return "Mantra";
        if (lowerCase.contains("rito")) return "Rito";
        return "Desconhecido";
    }

    private int extractMantraCount(String line) {
        return LineAnalyzer.extractNumberAfterThirdColon(line);
    }
}