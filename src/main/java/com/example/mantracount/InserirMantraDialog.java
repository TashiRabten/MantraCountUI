package com.example.mantracount;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class InserirMantraDialog {
    
    private Stage dialog;
    private DatePicker datePicker;
    private CheckBox rangeToggle;
    private DatePicker endDatePicker;
    private Label endDateLabel;
    private TextField mantraNameField;
    private ComboBox<String> typeComboBox;
    private TextField amountField;
    private ComboBox<String> userNameComboBox;
    private Button inserirButton;
    private Button cancelButton;
    
    public InserirMantraDialog(Stage owner) {
        createDialog(owner);
    }
    
    private void createDialog(Stage owner) {
        dialog = new Stage();
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.initOwner(owner);
        dialog.setTitle("Inserir Mantra");
        dialog.setResizable(false);
        
        VBox root = new VBox(UIComponentFactory.LARGE_SPACING);
        root.setPadding(new Insets(20));
        root.setStyle(UIColorScheme.getMainBackgroundStyle());
        
        // Header
        Label headerLabel = UIComponentFactory.createHeaderLabel("Inserir Novo Mantra", "Insert New Mantra");
        
        // Form fields
        GridPane formGrid = createFormGrid();
        
        // Buttons
        HBox buttonBox = createButtonBox();
        
        root.getChildren().addAll(headerLabel, formGrid, buttonBox);

        Scene scene = new Scene(root, 420, 320);
        dialog.setScene(scene);

        InputStream stream = getClass().getResourceAsStream("/icons/BUDA.png");
        if (stream != null) {
            System.out.println("Image found!");
            Image icon = new Image(stream);  // Create image first
            System.out.println("Icon size: " + icon.getWidth() + "x" + icon.getHeight());

            dialog.getIcons().add(icon);  // Add the same image object
        } else {
            System.out.println("Image not found: /icons/BUDA.png");
        }
        
        setupEventHandlers();
    }
    
    private GridPane createFormGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(UIComponentFactory.STANDARD_SPACING);
        grid.setVgap(UIComponentFactory.STANDARD_SPACING);
        
        // Set column constraints to ensure proper width distribution
        javafx.scene.layout.ColumnConstraints labelColumn = new javafx.scene.layout.ColumnConstraints();
        labelColumn.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
        labelColumn.setPrefWidth(javafx.scene.layout.Region.USE_COMPUTED_SIZE);
        
        javafx.scene.layout.ColumnConstraints fieldColumn = new javafx.scene.layout.ColumnConstraints();
        fieldColumn.setHgrow(javafx.scene.layout.Priority.ALWAYS);
        fieldColumn.setMinWidth(220);
        
        grid.getColumnConstraints().addAll(labelColumn, fieldColumn);
        
        // Date picker
        Label dateLabel = new Label("Data:");
        dateLabel.setStyle(UIColorScheme.getFieldLabelStyleSmall());
        datePicker = UIComponentFactory.DatePickers.createStartDatePicker();
        datePicker.setValue(LocalDate.now());
        datePicker.setMinHeight(UIComponentFactory.FIELD_HEIGHT_2);
        datePicker.setPrefWidth(220);
        datePicker.setMinWidth(220);

        // Always use focused style - override any focus switching
        datePicker.setStyle(UIColorScheme.getDatePickerFocusedStyle());

        // Add listener to maintain focused style regardless of actual focus state
        datePicker.focusedProperty().addListener((obs, oldVal, newVal) -> {
            javafx.application.Platform.runLater(() -> {
                datePicker.setStyle(UIColorScheme.getDatePickerFocusedStyle());
            });
        });

        // Range toggle
        rangeToggle = new CheckBox("Intervalo de Datas");
        rangeToggle.setStyle(UIColorScheme.getFieldLabelStyleSmall());
        UIComponentFactory.addTooltip(rangeToggle, "Enable to insert mantras for a date range");

        // End date picker (initially hidden)
        endDateLabel = new Label("Data Final:");
        endDateLabel.setStyle(UIColorScheme.getFieldLabelStyleSmall());
        endDateLabel.setVisible(false);
        endDateLabel.setManaged(false);

        endDatePicker = UIComponentFactory.DatePickers.createStartDatePicker();
        endDatePicker.setValue(LocalDate.now());
        endDatePicker.setMinHeight(UIComponentFactory.FIELD_HEIGHT_2);
        endDatePicker.setPrefWidth(220);
        endDatePicker.setMinWidth(220);
        endDatePicker.setStyle(UIColorScheme.getDatePickerFocusedStyle());
        endDatePicker.setVisible(false);
        endDatePicker.setManaged(false);

        // Add listener to maintain focused style
        endDatePicker.focusedProperty().addListener((obs, oldVal, newVal) -> {
            javafx.application.Platform.runLater(() -> {
                endDatePicker.setStyle(UIColorScheme.getDatePickerFocusedStyle());
            });
        });

        // Set up visibility binding for range fields
        rangeToggle.selectedProperty().addListener((obs, oldVal, newVal) -> {
            endDateLabel.setVisible(newVal);
            endDateLabel.setManaged(newVal);
            endDatePicker.setVisible(newVal);
            endDatePicker.setManaged(newVal);

            // Adjust dialog height based on range toggle
            if (newVal) {
                dialog.setHeight(360);
            } else {
                dialog.setHeight(320);
            }
        });

        // Mantra name field
        Label mantraLabel = new Label("Nome do Mantra:");
        mantraLabel.setStyle(UIColorScheme.getFieldLabelStyleSmall());
        mantraNameField = UIComponentFactory.TextFields.createTextField("Ex: Om Mani Padme Hum", "Enter the mantra name");
        mantraNameField.setStyle(UIColorScheme.getInputFieldStyleSmall());
        mantraNameField.setMinHeight(UIComponentFactory.FIELD_HEIGHT_2);
        
        // Add input filter to accept only letters and spaces
        mantraNameField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("[a-zA-ZáàâãéêíóôõúüçÁÀÂÃÉÊÍÓÔÕÚÜÇ\\s]*")) {
                mantraNameField.setText(newValue.replaceAll("[^a-zA-ZáàâãéêíóôõúüçÁÀÂÃÉÊÍÓÔÕÚÜÇ\\s]", ""));
            }
        });
        // Type dropdown
        Label typeLabel = new Label("Tipo:");
        typeLabel.setStyle(UIColorScheme.getFieldLabelStyleSmall());
        typeComboBox = new ComboBox<>();
        typeComboBox.getItems().addAll("Mantra", "Rito", "Prece");
        typeComboBox.setValue("Mantra");
        typeComboBox.setStyle(UIColorScheme.getComboBoxStyleSmall() + "; -fx-pref-row-count: 3;");
        typeComboBox.setPrefWidth(200);
        typeComboBox.setPrefHeight(UIComponentFactory.FIELD_HEIGHT_2);

        typeComboBox.setEditable(false);
        
        // Amount field
        Label amountLabel = new Label("Quantidade:");
        amountLabel.setStyle(UIColorScheme.getFieldLabelStyleSmall());
        amountField = UIComponentFactory.TextFields.createTextField("Ex: 108", "Enter the number of mantras");
        amountField.setStyle(UIColorScheme.getInputFieldStyleSmall());
        amountField.setMinHeight(UIComponentFactory.FIELD_HEIGHT_2);
        
        // Add input filter to accept only integers
        amountField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*")) {
                amountField.setText(newValue.replaceAll("[^\\d]", ""));
            }
        });
        
        // User name field
        Label userNameLabel = new Label(StringConstants.USER_NAME_LABEL_PT);
        userNameLabel.setStyle(UIColorScheme.getFieldLabelStyleSmall());
        userNameComboBox = new ComboBox<>();
        userNameComboBox.setEditable(true);
        userNameComboBox.setStyle(UIColorScheme.getComboBoxStyleSmall());
        userNameComboBox.setPrefWidth(220);
        userNameComboBox.setPrefHeight(UIComponentFactory.FIELD_HEIGHT_2);
        userNameComboBox.setPromptText(StringConstants.USER_NAME_PLACEHOLDER_PT);
        UIComponentFactory.addTooltip(userNameComboBox, StringConstants.USER_NAME_TOOLTIP_EN);
        
        // Load previous names and set default
        loadUserNames();
        
        // Add input filter to accept only letters and spaces
        userNameComboBox.getEditor().textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("[a-zA-ZáàâãéêíóôõúüçÁÀÂÃÉÊÍÓÔÕÚÜÇ\\s]*")) {
                userNameComboBox.getEditor().setText(newValue.replaceAll("[^a-zA-ZáàâãéêíóôõúüçÁÀÂÃÉÊÍÓÔÕÚÜÇ\\s]", ""));
            }
        });
        // Add to grid - User name at top for better visibility
        grid.add(userNameLabel, 0, 0);
        grid.add(userNameComboBox, 1, 0);
        grid.add(dateLabel, 0, 1);
        grid.add(datePicker, 1, 1);
        grid.add(rangeToggle, 1, 2);
        grid.add(endDateLabel, 0, 3);
        grid.add(endDatePicker, 1, 3);
        grid.add(mantraLabel, 0, 4);
        grid.add(mantraNameField, 1, 4);
        grid.add(typeLabel, 0, 5);
        grid.add(typeComboBox, 1, 5);
        grid.add(amountLabel, 0, 6);
        grid.add(amountField, 1, 6);

        return grid;
    }
    
    private HBox createButtonBox() {
        inserirButton = UIComponentFactory.ActionButtons.createSaveButton();
        inserirButton.setText("Inserir");
        UIComponentFactory.addTooltip(inserirButton, "Insert the mantra entry");
        
        cancelButton = UIComponentFactory.ActionButtons.createCloseButton();
        
        return UIComponentFactory.Layouts.createDialogActionLayout(inserirButton, cancelButton);
    }
    
    private void setupEventHandlers() {
        inserirButton.setOnAction(e -> handleInserir());
        cancelButton.setOnAction(e -> dialog.close());
    }
    
    private void handleInserir() {
        if (!validateInputs()) {
            return;
        }

        try {
            int entriesInserted = 0;

            if (rangeToggle.isSelected()) {
                // Range mode: insert entries for each date in the range
                LocalDate startDate = datePicker.getValue();
                LocalDate endDate = endDatePicker.getValue();

                LocalDate currentDate = startDate;
                while (!currentDate.isAfter(endDate)) {
                    String whatsappLine = createWhatsAppLine(currentDate);
                    writeToFile(whatsappLine, currentDate);
                    entriesInserted++;
                    currentDate = currentDate.plusDays(1);
                }
            } else {
                // Single date mode: insert one entry
                String whatsappLine = createWhatsAppLine(datePicker.getValue());
                writeToFile(whatsappLine, datePicker.getValue());
                entriesInserted = 1;
            }

            // Save the user name for future use
            saveUserName(getUserName());

            // Show success message
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Sucesso");
            alert.setHeaderText(null);
            String message = entriesInserted == 1
                ? "\uD83D\uDCFF Mantra successfully inserted! \n\uD83D\uDCFF Mantra inserido com sucesso!"
                : "\uD83D\uDCFF " + entriesInserted + " mantras successfully inserted! \n\uD83D\uDCFF " + entriesInserted + " mantras inseridos com sucesso!";
            alert.setContentText(message);
            alert.showAndWait();

        } catch (Exception ex) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erro");
            alert.setHeaderText(null);
            alert.setContentText("❌ Error inserting mantra: " + ex.getMessage() + "\n❌ Erro ao inserir mantra: " + ex.getMessage());
            alert.showAndWait();
        }
    }
    
    private boolean validateInputs() {
        if (datePicker.getValue() == null) {
            showError("📅 Please select a date\n📅 Por favor, selecione uma data");
            return false;
        }

        // Validate end date if range mode is enabled
        if (rangeToggle.isSelected()) {
            if (endDatePicker.getValue() == null) {
                showError("📅 Please select an end date\n📅 Por favor, selecione uma data final");
                return false;
            }

            if (endDatePicker.getValue().isBefore(datePicker.getValue())) {
                showError("📅 End date must be after or equal to start date\n📅 Data final deve ser posterior ou igual à data inicial");
                return false;
            }
        }

        if (mantraNameField.getText() == null || mantraNameField.getText().trim().isEmpty()) {
            showError("📝 Please enter the mantra name\n📝 Por favor, insira o nome do mantra");
            return false;
        }

        if (typeComboBox.getValue() == null) {
            showError("🏷️ Please select the type\n🏷️ Por favor, selecione o tipo");
            return false;
        }

        try {
            String amountText = amountField.getText().trim();
            if (amountText.isEmpty()) {
                showError("🔢 Please enter the amount\n🔢 Por favor, insira a quantidade");
                return false;
            }
            int amount = Integer.parseInt(amountText);
            if (amount <= 0) {
                showError("⚠️ Please enter a valid amount greater than zero\n⚠️ Por favor, insira uma quantidade válida maior que zero");
                return false;
            }
        } catch (NumberFormatException e) {
            showError("🔢 Please enter a valid number\n🔢 Por favor, insira um número válido");
            return false;
        }

        return true;
    }
    
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Validation Error / Erro de Validação");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    private String createWhatsAppLine(LocalDate date) {
        String mantraName = mantraNameField.getText().trim();
        String type = typeComboBox.getValue();
        int amount = Integer.parseInt(amountField.getText().trim());
        String userName = getUserName();

        // Use locale-aware date formatting based on system default
        DateParser.DateFormat dateFormat = DateParser.getCurrentDateFormat();
        String datePattern = (dateFormat == DateParser.DateFormat.BR_FORMAT) ? "dd/MM/yy" : "MM/dd/yy";
        String formattedDate = date.format(DateTimeFormatter.ofPattern(datePattern));

        // Always use current time to prevent duplicates
        LocalTime timeToUse = LocalTime.now();

        // Format time based on locale - Brazilian uses 24-hour, US uses 12-hour AM/PM
        String timePattern = (dateFormat == DateParser.DateFormat.BR_FORMAT) ? "HH:mm:ss" : "hh:mm:ss a";
        String formattedTime = timeToUse.format(DateTimeFormatter.ofPattern(timePattern));

        // Determine singular/plural form
        String typeText = (amount == 1) ? type : getPlural(type);

        // Create iPhone WhatsApp format: [date, time] UserName: Fiz amount typeText de mantraName
        return String.format("[%s, %s] %s: Fiz %d %s de %s",
                           formattedDate, formattedTime, userName, amount, typeText, mantraName);
    }
    
    private String getUserName() {
        String userName = userNameComboBox.getEditor().getText();
        if (userName == null || userName.trim().isEmpty()) {
            userName = userNameComboBox.getValue();
        }
        if (userName == null || userName.trim().isEmpty()) {
            userName = "Mantrika";
        }
        return userName.trim();
    }
    
    private String getPlural(String type) {
        switch (type) {
            case "mantra":
                return "mantras";
            case "rito":
                return "ritos";
            case "prece":
                return "preces";
            default:
                return type + "s";
        }
    }
    
    private void writeToFile(String content, LocalDate date) throws IOException {
        // Create MantraCount directory if it doesn't exist
        String userHome = System.getProperty("user.home");
        String documentsPath = userHome + File.separator + "Documents" + File.separator + "MantraCount";
        File mantraCountDir = new File(documentsPath);

        if (!mantraCountDir.exists()) {
            mantraCountDir.mkdirs();
        }

        // Create Mantras subdirectory if it doesn't exist
        String mantrasPath = documentsPath + File.separator + "Mantras";
        File mantrasDir = new File(mantrasPath);

        if (!mantrasDir.exists()) {
            mantrasDir.mkdirs();
        }

        // Create filename based on selected date
        String filename = date.format(DateTimeFormatter.ofPattern("yyyy-MM")) + ".txt";
        String filePath = mantrasPath + File.separator + filename;
        File targetFile = new File(filePath);

        // Read existing content if file exists
        List<String> existingLines = new ArrayList<>();
        if (targetFile.exists()) {
            try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.FileReader(targetFile))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    existingLines.add(line);
                }
            }
        }

        // Insert the new content in chronological order
        insertInChronologicalOrder(existingLines, content, date);

        // Write all content back to file
        try (FileWriter writer = new FileWriter(filePath, false)) { // false = overwrite
            for (String line : existingLines) {
                writer.write(line + System.lineSeparator());
            }
        }
    }
    
    private void insertInChronologicalOrder(List<String> existingLines, String newContent, LocalDate newDate) {
        // Extract time from the new content
        LocalTime newTime = extractTimeFromContent(newContent);

        // Find the correct insertion position
        int insertPosition = existingLines.size(); // Default to end

        for (int i = 0; i < existingLines.size(); i++) {
            String existingLine = existingLines.get(i);

            // Extract date and time from existing line
            LocalDate existingDate = LineParser.extractDate(existingLine);
            LocalTime existingTime = extractTimeFromContent(existingLine);

            if (existingDate != null && existingTime != null) {
                // Compare dates first
                if (newDate.isBefore(existingDate)) {
                    insertPosition = i;
                    break;
                } else if (newDate.equals(existingDate)) {
                    // Same date, compare times
                    if (newTime.isBefore(existingTime) || newTime.equals(existingTime)) {
                        insertPosition = i;
                        // If times are equal, insert after (i+1)
                        if (newTime.equals(existingTime)) {
                            insertPosition = i + 1;
                        }
                        break;
                    }
                }
            }
        }

        // Insert at the determined position
        existingLines.add(insertPosition, newContent);
    }
    
    private void loadUserNames() {
        try {
            String userHome = System.getProperty("user.home");
            String configPath = userHome + File.separator + "Documents" + File.separator + "MantraCount" + File.separator + "user-names.txt";
            File configFile = new File(configPath);
            
            userNameComboBox.getItems().clear();
            
            if (configFile.exists()) {
                try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.FileReader(configFile))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        line = line.trim();
                        if (!line.isEmpty() && !userNameComboBox.getItems().contains(line)) {
                            userNameComboBox.getItems().add(line);
                        }
                    }
                }
            }
            
            // Add Mantrika to items if not already present
            if (!userNameComboBox.getItems().contains("Mantrika")) {
                userNameComboBox.getItems().add("Mantrika");
            }
            
            // Set default value - use most recent name or Mantrika
            if (!userNameComboBox.getItems().isEmpty()) {
                userNameComboBox.setValue(userNameComboBox.getItems().get(0));
            } else {
                userNameComboBox.setValue("Mantrika");
            }
            
        } catch (Exception e) {
            // Default fallback
            userNameComboBox.getItems().clear();
            userNameComboBox.getItems().add("Mantrika");
            userNameComboBox.setValue("Mantrika");
        }
    }
    
    private void saveUserName(String userName) {
        if (userName == null || userName.trim().isEmpty()) {
            return;
        }
        
        userName = userName.trim();
        
        try {
            String userHome = System.getProperty("user.home");
            String mantrasPath = userHome + File.separator + "Documents" + File.separator + "MantraCount";
            File mantrasDir = new File(mantrasPath);
            
            if (!mantrasDir.exists()) {
                mantrasDir.mkdirs();
            }
            
            String configPath = mantrasPath + File.separator + "user-names.txt";
            File configFile = new File(configPath);
            
            // Read existing names
            List<String> existingNames = new ArrayList<>();
            if (configFile.exists()) {
                try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.FileReader(configFile))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        line = line.trim();
                        if (!line.isEmpty()) {
                            existingNames.add(line);
                        }
                    }
                }
            }
            
            // Add new name at the beginning if not already present
            if (!existingNames.contains(userName)) {
                existingNames.add(0, userName);
                
                // Keep only the last 10 names
                if (existingNames.size() > 10) {
                    existingNames = existingNames.subList(0, 10);
                }
                
                // Write back to file
                try (java.io.FileWriter writer = new java.io.FileWriter(configPath)) {
                    for (String name : existingNames) {
                        writer.write(name + System.lineSeparator());
                    }
                }
            } else if (!existingNames.get(0).equals(userName)) {
                // Move existing name to the top
                existingNames.remove(userName);
                existingNames.add(0, userName);
                
                // Write back to file
                try (java.io.FileWriter writer = new java.io.FileWriter(configPath)) {
                    for (String name : existingNames) {
                        writer.write(name + System.lineSeparator());
                    }
                }
            }
            
        } catch (Exception e) {
            System.err.println("Error saving user name: " + e.getMessage());
        }
    }
    
    private LocalTime extractTimeFromContent(String content) {
        try {
            // Extract time from WhatsApp format: [DD/MM/yy, HH:mm:ss] or [MM/dd/yy, HH:mm:ss AM/PM]
            if (content.startsWith("[")) {
                int commaIndex = content.indexOf(',');
                int closeBracketIndex = content.indexOf(']');
                if (commaIndex > 0 && closeBracketIndex > commaIndex) {
                    String timePart = content.substring(commaIndex + 1, closeBracketIndex).trim();
                    
                    // Try to parse 24-hour format first (Brazilian format)
                    try {
                        DateTimeFormatter timeFormatter24 = DateTimeFormatter.ofPattern("HH:mm:ss");
                        return LocalTime.parse(timePart, timeFormatter24);
                    } catch (Exception e1) {
                        // Try 12-hour format with AM/PM (US format)
                        try {
                            DateTimeFormatter timeFormatter12 = DateTimeFormatter.ofPattern("h:mm:ss a", Locale.ENGLISH);
                            return LocalTime.parse(timePart, timeFormatter12);
                        } catch (Exception e2) {
                            // If both fail, continue to default
                        }
                    }
                }
            }
        } catch (Exception e) {
            // If parsing fails, return noon as default
        }
        return LocalTime.of(12, 0); // Default to noon
    }
    
    public void show() {
        dialog.showAndWait();
    }
}