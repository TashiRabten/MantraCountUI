package com.example.mantracount;

import javafx.application.Platform;
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

import java.io.InputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static com.example.mantracount.MantraLineClassifier.hasExplicitMantraRitoWords;

/**
 * Refactored All Mantras UI using centralized components and consistent styling.
 * Eliminates code duplication and provides consistent user experience.
 */
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

        Stage dialog = createDialog(owner);
        VBox root = createMainLayout(dialog);
        applyThemeColors(root);

        dialog.setScene(new Scene(root, 900, 600));
        dialog.show();
    }

    private void applyThemeColors(VBox root) {
        root.setStyle(UIColorScheme.getMainBackgroundStyle());
    }

    /**
     * Creates the main dialog window
     */
    private Stage createDialog(Stage owner) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(owner);
        dialog.setTitle(StringConstants.ALL_MANTRAS_TITLE);

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
        return dialog;
    }

    /**
     * Creates the main layout using factory components
     */
    private VBox createMainLayout(Stage dialog) {
        VBox root = new VBox(10);
        root.setPadding(new Insets(15));

        Label header = createHeader();
        HBox dateBox = createDateSelectionBox();
        HBox loadBox = createLoadButtonBox();
        HBox summaryPanel = createSummaryPanel();
        ScrollPane scrollPane = createEntriesScrollPane();
        Label statsLabel = createStatsLabel();
        HBox actions = createActionButtons(dialog);

        VBox hiddenContainer = new VBox();
        hiddenContainer.setVisible(false);
        hiddenContainer.setPrefHeight(0);
        hiddenContainer.getChildren().add(new Label());

        initializeFileController(dialog, hiddenContainer);
        initializeSearchController();

        root.getChildren().addAll(
                header, dateBox, loadBox, searchController.getSearchContainer(),
                summaryPanel, progressIndicator, scrollPane, statsLabel, actions, hiddenContainer
        );

        return root;
    }

    /**
     * Creates load button box using factory
     */
    private HBox createLoadButtonBox() {
        Button loadButton = UIComponentFactory.ActionButtons.createLoadMantrasButton();
        loadButton.setOnAction(e -> loadMantras());

        HBox loadBox = new HBox(10, loadButton);
        loadBox.setAlignment(Pos.CENTER_LEFT);
        return loadBox;
    }

    private ScrollPane createEntriesScrollPane() {
        entriesContainer = new VBox(0);
        entriesContainer.setStyle(UIColorScheme.getResultsAreaStyle());

        scrollPane = UIComponentFactory.createStyledScrollPane(entriesContainer, 400);
        scrollPane.setFitToHeight(true);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        progressIndicator = UIComponentFactory.createProgressIndicator();

        Label placeholder = UIComponentFactory.createPlaceholderLabel(
                "Nenhum mantra encontrado",
                "No mantras found - Select an end date and click Load"
        );
        entriesContainer.getChildren().add(placeholder);
        placeholder.setAlignment(Pos.CENTER);
        entriesContainer.setAlignment(Pos.CENTER);

        return scrollPane;
    }

    /**
     * Creates stats label
     */
    private Label createStatsLabel() {
        Label statsLabel = new Label(StringConstants.SELECT_END_DATE_PT);
        UIComponentFactory.addTooltip(statsLabel, StringConstants.SELECT_END_DATE_EN);
        return statsLabel;
    }

    /**
     * Creates action buttons using factory with consistent alignment
     */
    private HBox createActionButtons(Stage dialog) {
        Button saveBtn = UIComponentFactory.ActionButtons.createSaveButton();
        saveBtn.setOnAction(e -> saveChanges());

        Button closeBtn = UIComponentFactory.ActionButtons.createCloseButton();
        closeBtn.setOnAction(e -> dialog.close());

        return UIComponentFactory.Layouts.createDialogActionLayout(saveBtn, closeBtn);
    }

    /**
     * Initializes file controller
     */
    private void initializeFileController(Stage dialog, VBox hiddenContainer) {
        fileController = new FileManagementController(
                dialog, mantraData, hiddenContainer, new Label(), new TextArea()
        );
    }

    /**
     * Initializes search controller
     */
    private void initializeSearchController() {
        searchController = new SearchController(entriesContainer, scrollPane);
        searchController.adaptToContainerStructure(true);
    }

    /**
     * Loads mantras for the selected period
     */
    private void loadMantras() {
        LocalDate endDate = endDatePicker.getValue();
        if (endDate == null) {
            UIUtils.showError("Please select an end date", "Por favor, selecione uma data final");
            return;
        }

        if (!UIUtils.validateDateRange(startDate, endDate)) {
            return;
        }

        progressIndicator.setVisible(true);
        loadEntriesAsync(mantraData, endDate);
    }

    /**
     * Loads entries asynchronously
     */
    private void loadEntriesAsync(MantraData data, LocalDate endDate) {
        CompletableFuture.supplyAsync(() -> {
            List<MantraEntry> entries = new ArrayList<>();
            Map<String, Integer> typeCounts = new HashMap<>();
            Map<String, Integer> typeNumbers = new HashMap<>();
            int totalMantras = 0;

            for (String line : data.getLines()) {
                LocalDate lineDate = LineParser.extractDate(line);

                if (lineDate == null || lineDate.isBefore(startDate) || lineDate.isAfter(endDate)) {
                    continue;
                }

                if (containsMantraContent(line)) {
                    String mantraType = extractMantraType(line);
                    int count = extractMantraCount(line);

                    entries.add(new MantraEntry(lineDate, line, mantraType, count));

                    typeCounts.put(mantraType, typeCounts.getOrDefault(mantraType, 0) + 1);
                    typeNumbers.put(mantraType, typeNumbers.getOrDefault(mantraType, 0) + count);

                    totalMantras += (count > 0) ? count : 0;
                }
            }

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

            mantraTypeCounts = typeCounts;
            mantraTypeNumbers = typeNumbers;

            updateSummaryPanel();
            displayEntries(entries);

            Label statsLabel = getStatsLabel();
            if (statsLabel != null) {
                statsLabel.setText(String.format(
                        "Encontrados %d mantras em %d entradas", totalMantras, entryCount
                ));
            }
            progressIndicator.setVisible(false);

        }, Platform::runLater);
    }

    /**
     * Updates the summary panel with mantra type counts
     */
    private void updateSummaryPanel() {
        summaryPanel.getChildren().clear();

        if (mantraTypeCounts.isEmpty()) {
            summaryLabel.setText("Nenhum mantra encontrado");
            summaryPanel.getChildren().add(summaryLabel);
            return;
        }

        List<Map.Entry<String, Integer>> sortedTypes = new ArrayList<>(mantraTypeCounts.entrySet());
        sortedTypes.sort((a, b) -> b.getValue().compareTo(a.getValue()));

        for (Map.Entry<String, Integer> entry : sortedTypes) {
            String type = entry.getKey();
            int lineCount = entry.getValue();
            int totalNumber = mantraTypeNumbers.getOrDefault(type, 0);

            VBox typeBox = createTypeBadge(type, lineCount, totalNumber);
            summaryPanel.getChildren().add(typeBox);
        }

        addTotalBadge();
    }

    /**
     * Sets up hover effect for type badge
     */
    private void setupHoverEffect(VBox typeBox) {
        typeBox.setOnMouseEntered(e ->
                typeBox.setStyle("-fx-background-color: #BBDEFB; -fx-background-radius: 10px;"));
        typeBox.setOnMouseExited(e ->
                typeBox.setStyle("-fx-background-color: #E3F2FD; -fx-background-radius: 10px;"));
    }

    /**
     * Filters entries by mantra type
     */
    private void filterByType(String type) {
        entriesContainer.getChildren().clear();

        List<MantraEntry> filteredEntries = allEntries.stream()
                .filter(entry -> entry.getMantraType().equals(type))
                .collect(java.util.stream.Collectors.toList());

        if (filteredEntries.isEmpty()) {
            Label noResults = UIComponentFactory.createPlaceholderLabel(
                    "Nenhuma entrada para " + type,
                    "No entries found for " + type
            );
            entriesContainer.getChildren().add(noResults);
            return;
        }

        Button showAllBtn = UIComponentFactory.ActionButtons.createShowAllButton();
        showAllBtn.setOnAction(e -> displayEntries(allEntries));
        entriesContainer.getChildren().add(showAllBtn);

        for (MantraEntry entry : filteredEntries) {
            VBox lineEditor = createSearchCompatibleLineEditor(entry);
            entriesContainer.getChildren().add(lineEditor);
        }

        searchController.resetSearchState();
    }

    /**
     * Displays all entries
     */
    private void displayEntries(List<MantraEntry> entries) {
        entriesContainer.getChildren().clear();

        if (entries.isEmpty()) {
            Label placeholder = UIComponentFactory.createPlaceholderLabel(
                    "Nenhum mantra encontrado",
                    "No mantras found - Try adjusting the date range"
            );
            entriesContainer.getChildren().add(placeholder);
            placeholder.setAlignment(Pos.CENTER);
            entriesContainer.setAlignment(Pos.CENTER);
            return;
        }

        for (MantraEntry entry : entries) {
            VBox lineEditor = createSearchCompatibleLineEditor(entry);
            entriesContainer.getChildren().add(lineEditor);
        }

        searchController.resetSearchState();
    }

    /**
     * Saves changes to file
     */
    private void saveChanges() {
        if (entriesContainer.getChildren().isEmpty() ||
                (entriesContainer.getChildren().size() == 1 &&
                        entriesContainer.getChildren().get(0) instanceof Label)) {
            UIUtils.showError("No entries to save. Load entries first.",
                    "Sem entradas para salvar. Carregue as entradas primeiro.");
            return;
        }

        Map<String, String> updatedContentMap = extractUpdatedContentFromUI();

        if (updatedContentMap.isEmpty()) {
            UIUtils.showNoChangesInfo();
            return;
        }

        boolean success = fileController.saveChanges(updatedContentMap);
        if (success) {
            loadMantras();
        }
    }

    private Map<String, String> extractUpdatedContentFromUI() {
        Map<String, String> updatedContent = new HashMap<>();

        for (Node node : entriesContainer.getChildren()) {
            if (node instanceof VBox wrapper && wrapper.getUserData() != null) {  // Check VBox first!
                String originalLine = (String) wrapper.getUserData();

                // Get the HBox inside the VBox
                if (!wrapper.getChildren().isEmpty() && wrapper.getChildren().get(0) instanceof HBox lineContainer) {
                    if (lineContainer.getChildren().size() >= 2) {
                        HBox firstElement = (HBox) lineContainer.getChildren().get(0);
                        Label protectedLabel = (Label) firstElement.getChildren().get(1);
                        TextField editableField = (TextField) lineContainer.getChildren().get(1);

                        String updatedLine = protectedLabel.getText() + editableField.getText();

                        if (!originalLine.equals(updatedLine)) {
                            updatedContent.put(originalLine, updatedLine);
                        }
                    }
                }
            }
        }

        return updatedContent;
    }
    /**
     * Helper methods for content analysis
     */
    /**
     * Helper methods for content analysis
     */
    private boolean containsMantraContent(String line) {
        return MantraLineClassifier.isRelevantForAllMantras(line);
    }

    private String extractMantraType(String line) {
        String lowerCase = line.toLowerCase();

        String[] mantraTypes = {"refúgio", "vajrasattva", "vajrasatva", "refugio", "guru", "bodisatva",
                "bodhisattva", "buda", "buddha", "tare", "tara", "medicina", "preliminares"};

        for (String type : mantraTypes) {
            if (lowerCase.contains(type)) {
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

    /**
     * Helper methods to find UI components
     */
    private Label findHeaderLabel() {
        return null;
    }

    private Label getStatsLabel() {
        return null;
    }

    private Label createHeader() {
        String startDateFormatted = DateFormatUtils.formatShortDate(startDate);
        String endDateFormatted = DateFormatUtils.formatShortDate(LocalDate.now());

        Label header = UIComponentFactory.createHeaderLabel(
                "Todos os Mantras de " + startDateFormatted + " a " + endDateFormatted,
                "All Mantras - Shows all mantras from the selected period"
        );
        header.setStyle(UIColorScheme.getHeaderTitleStyle());
        return header;
    }

    private HBox createDateSelectionBox() {
        LocalDate defaultEndDate = LocalDate.now();
        endDatePicker = new DatePicker(defaultEndDate);
        endDatePicker.setStyle(UIColorScheme.getDatePickerStyle());
        endDatePicker.setPromptText(StringConstants.END_DATE_PT);
        UIComponentFactory.addTooltip(endDatePicker, StringConstants.END_DATE_EN);

        Label endDateLabel = new Label(StringConstants.END_DATE_PT);
        endDateLabel.setStyle(UIColorScheme.getFieldLabelStyle());
        UIComponentFactory.addTooltip(endDateLabel, StringConstants.END_DATE_EN);

        HBox dateBox = new HBox(10, endDatePicker, endDateLabel);
        dateBox.setAlignment(Pos.CENTER_LEFT);

        endDatePicker.valueProperty().addListener((obs, old, newDate) -> {
            if (newDate != null) {
                Label header = findHeaderLabel();
                if (header != null) {
                    String startDateFormatted = DateFormatUtils.formatShortDate(startDate);
                    String endDateFormatted = DateFormatUtils.formatShortDate(newDate);
                    header.setText("Todos os Mantras de " + startDateFormatted + " a " + endDateFormatted);
                }
            }
        });

        return dateBox;
    }

    private HBox createSummaryPanel() {
        summaryPanel = new HBox(15);
        summaryPanel.setPadding(new Insets(10));
        summaryPanel.setStyle(UIColorScheme.getResultsAreaStyle());
        summaryPanel.setAlignment(Pos.CENTER);

        summaryLabel = new Label(StringConstants.LOADING_PT);
        summaryLabel = new Label(StringConstants.LOADING_PT);

        summaryLabel.setStyle(
                "-fx-text-fill: #000000; " +
                        "-fx-border-color: transparent;"
        );
        summaryPanel.getChildren().add(summaryLabel);

        UIComponentFactory.addTooltip(summaryPanel, "Summary - Shows count of each mantra type");

        return summaryPanel;
    }

    private VBox createTypeBadge(String type, int lineCount, int totalNumber) {
        VBox typeBox = new VBox(2);
        typeBox.setAlignment(Pos.CENTER);
        typeBox.setPadding(new Insets(5, 10, 5, 10));
        typeBox.setStyle("-fx-background-color: #E3F2FD; -fx-background-radius: 10px;");

        Label typeLabel = new Label(type);
        typeLabel.setStyle(UIColorScheme.getSectionTitleStyle() + "-fx-font-size: 12px;");

        Label countLabel = new Label(String.format("%d linhas", lineCount));
        countLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #424242;");

        Label numberLabel = new Label(String.format("Total: %d", totalNumber));
        numberLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #616161;");

        typeBox.getChildren().addAll(typeLabel, countLabel, numberLabel);

        UIComponentFactory.addTooltip(typeBox, String.format(
                "%s: %d entries with %d total mantras", type, lineCount, totalNumber
        ));

        typeBox.setOnMouseClicked(e -> filterByType(type));
        typeBox.setCursor(javafx.scene.Cursor.HAND);

        setupHoverEffect(typeBox);

        return typeBox;
    }

    private void addTotalBadge() {
        Separator separator = new Separator(javafx.geometry.Orientation.VERTICAL);
        separator.setPadding(new Insets(0, 5, 0, 5));

        VBox totalBox = new VBox(2);
        totalBox.setAlignment(Pos.CENTER);
        totalBox.setPadding(new Insets(5, 10, 5, 10));
        totalBox.setStyle("-fx-background-color: #C8E6C9; -fx-background-radius: 10px;");

        Label totalLabel = new Label("TOTAL");
        totalLabel.setStyle(UIColorScheme.getSectionTitleStyle() + "-fx-font-size: 12px; -fx-text-fill: #2E7D32;");

        int totalLines = mantraTypeCounts.values().stream().mapToInt(Integer::intValue).sum();
        int totalNumbers = mantraTypeNumbers.values().stream().mapToInt(Integer::intValue).sum();

        Label totalCountLabel = new Label(String.format("%d linhas", totalLines));
        totalCountLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #424242;");

        Label totalNumberLabel = new Label(String.format("Total: %d", totalNumbers));
        totalNumberLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #616161;");

        totalBox.getChildren().addAll(totalLabel, totalCountLabel, totalNumberLabel);

        summaryPanel.getChildren().addAll(separator, totalBox);
    }

    private VBox createSearchCompatibleLineEditor(MantraEntry entry) {
        HBox lineEditor = new HBox(10);
        lineEditor.setPadding(new Insets(5));
        lineEditor.setAlignment(Pos.CENTER);
        lineEditor.setUserData(entry.getLineContent());

        LineParser.LineSplitResult splitResult = LineParser.splitEditablePortion(entry.getLineContent());
        String protectedText = splitResult.getFixedPrefix();
        String editableText = splitResult.getEditableSuffix();

        HBox firstElement = new HBox(10);

        Label typeBadge = UIComponentFactory.createTypeBadge(entry.getMantraType());
        Label protectedLabel = new Label(protectedText);
        UIComponentFactory.addTooltip(protectedLabel, StringConstants.PROTECTED_CONTENT_TOOLTIP);

        firstElement.getChildren().addAll(typeBadge, protectedLabel);

        TextField editableField = UIComponentFactory.TextFields.createEditLineField(editableText);
        editableField.setStyle("-fx-background-color: white; -fx-border-color: " + UIColorScheme.NAVIGATION_COLOR + ";");
        HBox.setHgrow(editableField, Priority.ALWAYS);
        editableField.setPrefWidth(400);

        lineEditor.getChildren().addAll(firstElement, editableField);

        VBox wrapper = new VBox(0, lineEditor);
        wrapper.setStyle(UIColorScheme.getResultsContainerStyle());
        wrapper.setUserData(entry.getLineContent());

        return wrapper;
    }
}