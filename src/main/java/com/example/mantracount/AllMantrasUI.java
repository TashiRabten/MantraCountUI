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
    private List<String> originalLines = new ArrayList<>();
    private ScrollPane scrollPane;
    private FileManagementController fileController;
    private SearchController searchController;

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

        public LocalDate getDate() {
            return date;
        }

        public String getLineContent() {
            return lineContent;
        }

        public String getMantraType() {
            return mantraType;
        }

        public int getCount() {
            return count;
        }
    }

    public void show(Stage owner, MantraData data, LocalDate startDate) {
        this.mantraData = data;
        this.startDate = startDate;



        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(owner);
        dialog.setTitle("All Mantras from Period / Todos os Mantras do Período");
        dialog.getIcons().add(new Image(getClass().getResourceAsStream("/icons/BUDA.jpg")));

        // Create a hidden container for FileManagementController
        VBox hiddenContainer = new VBox();
        hiddenContainer.setVisible(false);
        hiddenContainer.setPrefHeight(0);

        Label hiddenPlaceholder = new Label();
        hiddenContainer.getChildren().add(hiddenPlaceholder);

        TextArea hiddenResultsArea = new TextArea();

        // Initialize FileManagementController for save functionality
        fileController = new FileManagementController(
                dialog,
                mantraData,
                hiddenContainer,
                hiddenPlaceholder,
                hiddenResultsArea
        );



        VBox root = new VBox(10);
        root.setPadding(new Insets(15));

        LocalDate defaultEndDate = LocalDate.now();
        endDatePicker = new DatePicker(defaultEndDate);
        endDatePicker.setPromptText("End Date / Data Final");
        HBox dateBox = new HBox(10, new Label("End Date / Data Final:"), endDatePicker);
        dateBox.setAlignment(Pos.CENTER_LEFT);

        // Load button
        Button loadButton = new Button("Load Mantras / Carregar Mantras");
        loadButton.setStyle("-fx-base: #4CAF50; -fx-text-fill: white;");
        loadButton.setOnAction(e -> loadMantras());

        HBox loadBox = new HBox(10, loadButton);
        loadBox.setAlignment(Pos.CENTER_LEFT);

        // Get formatters for both languages
        DateTimeFormatter usFormatter = DateTimeFormatter.ofPattern("MM/dd/yyyy");
        DateTimeFormatter localFormatter =
                (DateParser.getCurrentDateFormat() == DateParser.DateFormat.BR_FORMAT)
                        ? DateTimeFormatter.ofPattern("dd/MM/yyyy")
                        : usFormatter;

// Format the dates for English and Portuguese parts separately
        String startDateUS = startDate.format(usFormatter);
        String endDateUS = defaultEndDate.format(usFormatter);
        String startDateLocal = startDate.format(localFormatter);
        String endDateLocal = defaultEndDate.format(localFormatter);

// Header with different formats for different languages
        Label header = new Label("All Mantras from " + startDateUS + " to " + endDateUS +
                " / Todos os Mantras de " + startDateLocal + " a " + endDateLocal);
        header.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

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

        // Placeholder for empty container
        Label placeholder = new Label("No mantras found / Nenhum mantra encontrado");
        placeholder.setStyle("-fx-text-fill: gray; -fx-font-style: italic;");
        entriesContainer.getChildren().add(placeholder);


        searchController = new SearchController(entriesContainer, scrollPane);
        searchController.adaptToContainerStructure(true); // Tell it we're using AllMantrasUI structure
        // Stats at the bottom
        Label statsLabel = new Label("Select end date and click Load / Selecione data final e clique em Carregar");

        // Save and Close buttons
        Button saveBtn = new Button("💾 Save Changes / Salvar Alterações");
        saveBtn.setStyle("-fx-base: #4CAF50; -fx-text-fill: white;");
        saveBtn.setOnAction(e -> saveChanges());

        Button closeBtn = new Button("\u2716 Close / Fechar");
        closeBtn.setStyle("-fx-base: #F44336; -fx-text-fill: white;");
        closeBtn.setOnAction(e -> dialog.close());

        HBox actions = new HBox(10, saveBtn, closeBtn);
        actions.setAlignment(Pos.CENTER_RIGHT);
        actions.setPadding(new Insets(10, 0, 0, 0));

        root.getChildren().addAll(
                header,
                dateBox,
                loadBox,
                searchController.getSearchContainer(),
                progressIndicator,
                scrollPane,
                statsLabel,
                actions,
                hiddenContainer  // Add the hidden container
        );

        // Update header when end date changes
        endDatePicker.valueProperty().addListener((obs, old, newDate) -> {
            if (newDate != null) {
                header.setText("All Mantras from " +
                        formatDate(startDate) + " to " + formatDate(newDate) +
                        " / Todos os Mantras de " + formatDate(startDate) + " a " + formatDate(newDate));
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
            int totalMantras = 0;

            // Store original lines for reference
            originalLines = new ArrayList<>(data.getLines());

            // Analyze each line for mantras
            for (String line : data.getLines()) {
                LocalDate lineDate = LineParser.extractDate(line);

                // Skip if date is missing or outside our range
                if (lineDate == null || lineDate.isBefore(startDate) || lineDate.isAfter(endDate)) {
                    continue;
                }

                // Check if line contains mantra-related content
                if (containsMantraContent(line)) {
                    // Extract mantra type and count
                    String mantraType = extractMantraType(line);
                    int count = extractMantraCount(line);

                    entries.add(new MantraEntry(lineDate, line, mantraType, count));
                    totalMantras += (count > 0) ? count : 0;
                }
            }

            // Sort by date
            entries.sort(Comparator.comparing(MantraEntry::getDate));

            // Save all entries for filtering
            allEntries = new ArrayList<>(entries);

            // Return results with statistics
            return new Object[] { entries, totalMantras, entries.size() };

        }).thenAcceptAsync(result -> {
            @SuppressWarnings("unchecked")
            List<MantraEntry> entries = (List<MantraEntry>) result[0];
            int totalMantras = (int) result[1];
            int entryCount = (int) result[2];

            // Display entries
            displayEntries(entries);

            // Update status label
            Label statsLabel = getStatsLabel();
            if (statsLabel != null) {
                statsLabel.setText(String.format(
                        "Found %d mantras in %d entries / Encontrados %d mantras em %d entradas",
                        totalMantras, entryCount
                ));
            }
            progressIndicator.setVisible(false);

        }, Platform::runLater);
    }

    private void displayEntries(List<MantraEntry> entries) {
        entriesContainer.getChildren().clear();

        if (entries.isEmpty()) {
            Label placeholder = new Label("No mantras found / Nenhum mantra encontrado");
            placeholder.setStyle("-fx-text-fill: gray; -fx-font-style: italic;");
            entriesContainer.getChildren().add(placeholder);
            return;
        }

        for (MantraEntry entry : entries) {
            // Create structure that's EXACTLY compatible with SearchController
            HBox lineEditor = createSearchCompatibleLineEditor(entry);
            entriesContainer.getChildren().add(lineEditor);
        }

        // Reset search state
        searchController.resetSearchState();
    }

    private HBox createSearchCompatibleLineEditor(MantraEntry entry) {
        // CRITICAL: Structure must exactly match what SearchController expects

        // Create the HBox container with padding and alignment
        HBox lineEditor = new HBox(10);
        lineEditor.setPadding(new Insets(5));
        lineEditor.setAlignment(Pos.CENTER);

        // Store original line as user data for reference
        lineEditor.setUserData(entry.getLineContent());

        // Split line into protected and editable parts
        String protectedText = entry.getLineContent();
        String editableText = "";

        int closeBracket = entry.getLineContent().indexOf(']');
        int colon = entry.getLineContent().indexOf(':', closeBracket);

        if (closeBracket != -1 && colon != -1 && colon > closeBracket) {
            protectedText = entry.getLineContent().substring(0, colon + 1);
            editableText = entry.getLineContent().substring(colon + 1);
        }

        // Create combined label with mantra type and protected text
        HBox firstElement = new HBox(10);

        // Add mantra type badge
        Label typeBadge = new Label(entry.getMantraType());
        typeBadge.setPadding(new Insets(2, 8, 2, 8));
        typeBadge.setStyle("-fx-background-color: #E3F2FD; -fx-background-radius: 4px; " +
                "-fx-font-weight: bold; -fx-text-fill: #1565C0;");
        typeBadge.setPrefWidth(120);

        Label protectedLabel = new Label(protectedText);
        protectedLabel.setStyle("-fx-font-weight: bold;");

        firstElement.getChildren().addAll(typeBadge, protectedLabel);

        // CRITICAL: SearchController looks for a TextField as the SECOND direct child
        // Create editable text field - this must be the second child
        TextField editableField = new TextField(editableText);
        editableField.setPromptText("Edit line / Editar linha");
        HBox.setHgrow(editableField, Priority.ALWAYS);
        editableField.setPrefWidth(400);  // Make it bigger

        // First add the combined badge + protected part
        lineEditor.getChildren().add(firstElement);

        // Then add the editable field as the SECOND child - this is critical for SearchController
        lineEditor.getChildren().add(editableField);

        return lineEditor;
    }

    private Label getStatsLabel() {
        // Find the stats label in the scene
        if (scrollPane.getScene() != null) {
            VBox root = (VBox) scrollPane.getScene().getRoot();
            for (int i = 0; i < root.getChildren().size(); i++) {
                if (root.getChildren().get(i) instanceof Label && i > 5) { // Likely the stats label
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

        // Extract updated content from UI
        Map<String, String> updatedContentMap = extractUpdatedContentFromUI();

        if (updatedContentMap.isEmpty()) {
            UIUtils.showInfo("No changes detected. / Nenhuma alteração detectada.");
            return;
        }

        // Use FileManagementController to save changes - this reuses the same logic
        boolean success = fileController.saveChanges(updatedContentMap);

        if (success) {
            // Reload entries to refresh the UI
            loadMantras();
        }
    }

    private Map<String, String> extractUpdatedContentFromUI() {
        Map<String, String> updatedContent = new HashMap<>();

        for (Node node : entriesContainer.getChildren()) {
            if (node instanceof HBox lineContainer) {
                // Extract the original line from user data
                String originalLine = (String) lineContainer.getUserData();
                if (originalLine == null) continue;

                // Extract the updated content - adapted to our updated structure
                if (lineContainer.getChildren().size() >= 2) {
                    // First child should be the HBox with badge and protected part
                    HBox firstElement = (HBox) lineContainer.getChildren().get(0);
                    Label protectedLabel = (Label) firstElement.getChildren().get(1);

                    // Second child should be the TextField
                    TextField editableField = (TextField) lineContainer.getChildren().get(1);

                    String updatedLine = protectedLabel.getText() + editableField.getText();

                    // Check if content changed
                    if (!originalLine.equals(updatedLine)) {
                        updatedContent.put(originalLine, updatedLine);
                    }
                }
            }
        }

        return updatedContent;
    }

    private boolean containsMantraContent(String line) {
        String lowerCase = line.toLowerCase();
        // Check for common indicators of mantra entries
        return (lowerCase.contains("mantra") || lowerCase.contains("mantras")) &&
                (lowerCase.contains("fiz") || lowerCase.contains("recitei") ||
                        lowerCase.contains("fez") || lowerCase.contains("faz"));
    }

    private String extractMantraType(String line) {
        String lowerCase = line.toLowerCase();

        // Common mantra types - expand based on your needs
        String[] mantraTypes = {"refúgio", "vajrasattva", "refug", "guru", "boddhisattva",
                "bodhisattva", "buda", "buddha", "tara", "medicine", "medicina"};

        for (String type : mantraTypes) {
            if (lowerCase.contains(type)) {
                // Capitalize first letter for display
                return type.substring(0, 1).toUpperCase() + type.substring(1);
            }
        }

        // If no specific type found
        if (lowerCase.contains("mantra")) {
            return "Mantra";
        }

        return "Unknown / Desconhecido";
    }

    private int extractMantraCount(String line) {
        // First try with LineAnalyzer's existing methods
        int extractedNumber = LineAnalyzer.extractNumberAfterThirdColon(line);
        if (extractedNumber > 0) {
            return extractedNumber;
        }

        // Fallback to simple number extraction after "fiz" or similar words
        String lowerCase = line.toLowerCase();
        String[] countIndicators = {"fiz", "recitei", "fez", "faz"};

        for (String indicator : countIndicators) {
            int position = lowerCase.indexOf(indicator);
            if (position >= 0) {
                // Look for a number after the indicator
                String afterIndicator = lowerCase.substring(position + indicator.length());
                return extractFirstNumber(afterIndicator);
            }
        }

        return 0;
    }

    private int extractFirstNumber(String text) {
        StringBuilder numberBuilder = new StringBuilder();
        boolean foundDigit = false;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);

            if (Character.isDigit(c)) {
                numberBuilder.append(c);
                foundDigit = true;
            } else if (foundDigit) {
                // Stop after the first sequence of digits
                break;
            }
        }

        if (numberBuilder.length() > 0) {
            try {
                return Integer.parseInt(numberBuilder.toString());
            } catch (NumberFormatException e) {
                return 0;
            }
        }

        return 0;
    }

    private String formatDate(LocalDate date) {
        if (date == null) return "";
        return DateParser.formatDate(date, false);
    }


}