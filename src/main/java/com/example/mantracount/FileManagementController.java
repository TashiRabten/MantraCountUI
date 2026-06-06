package com.example.mantracount;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

public class FileManagementController {

    private final Stage primaryStage;
    private final TextField pathField;
    private final HBox fileControlContainer;
    private final CheckBox baseDeDadosCheckBox;
    private final MantraData mantraData;
    private final VBox mismatchesContainer;
    private final Label placeholder;
    private final TextArea resultsArea;
    private DateRangeController dateRangeController;
    private List<File> loadedDatabaseFiles; // Track which files were loaded in database mode

    public FileManagementController(Stage primaryStage, MantraData mantraData,
                                    VBox mismatchesContainer, Label placeholder, TextArea resultsArea) {
        this.primaryStage = primaryStage;
        this.mantraData = mantraData;
        this.mismatchesContainer = mismatchesContainer;
        this.placeholder = placeholder;
        this.resultsArea = resultsArea;

        // Create the path field with proper styling - NO PLACEHOLDER initially
        this.pathField = UIComponentFactory.TextFields.createFilePathField();
        this.pathField.setPrefWidth(400);


        Button openFileButton = UIComponentFactory.ActionButtons.createOpenFileButton();
        openFileButton.setOnAction(event -> openFile());

        this.baseDeDadosCheckBox = UIComponentFactory.createBaseDeDadosCheckBox();

        this.fileControlContainer = new HBox(UIComponentFactory.BUTTON_SPACING, pathField, openFileButton, baseDeDadosCheckBox);
        this.fileControlContainer.setAlignment(Pos.CENTER);
        HBox.setHgrow(pathField, Priority.ALWAYS);
    }

    public HBox getFileControlContainer() {
        return fileControlContainer;
    }

    public TextField getPathField() {
        return pathField;
    }

    public CheckBox getBaseDeDadosCheckBox() {
        return baseDeDadosCheckBox;
    }

    public void setDateRangeController(DateRangeController dateRangeController) {
        this.dateRangeController = dateRangeController;
    }

    public boolean openFile() {
        try {
            File selectedFile = FileLoader.openFile(
                    primaryStage,
                    pathField,
                    resultsArea,
                    mismatchesContainer,
                    placeholder,
                    new File(System.getProperty("user.home")),
                    mantraData
            );

            if (selectedFile == null) {
                return false;
            }

            pathField.setText(selectedFile.getAbsolutePath());
            pathField.setStyle(UIColorScheme.getInputFieldStyle()); // Ensure proper styling after text is set
            mantraData.setFilePath(selectedFile.getAbsolutePath());
            mantraData.setFromZip(selectedFile.getName().toLowerCase().endsWith(StringConstants.ZIP_EXTENSION));
            mantraData.setOriginalZipPath(mantraData.isFromZip() ? selectedFile.getAbsolutePath() : null);
            mantraData.setDatabaseMode(false); // Regular file mode

            mismatchesContainer.getChildren().clear();
            mismatchesContainer.getChildren().add(placeholder);

            UIUtils.showInfo("✔ File loaded. \n✔ Arquivo carregado.");
            return true;

        } catch (Exception ex) {
            ex.printStackTrace();
            UIUtils.showError("❌ Failed to load file." + ex.getMessage() + "\n❌ Falha ao carregar arquivo" + ex.getMessage());
            return false;
        }
    }

    public boolean validateFilePath() {
        // If "Base de Dados" is selected, no file path validation needed
        if (baseDeDadosCheckBox.isSelected()) {
            return true;
        }
        
        String text = pathField.getText();
        if (text == null || text.trim().isEmpty()) {
            UIUtils.showError("Missing or invalid field: \nPlease, open the file",
                    "Campo ausente ou inválido:\nPor favor, abra o Arquivo");
            return false;
        }
        return true;
    }

    public boolean ensureFileLoaded() {
        System.out.println("ensureFileLoaded called - Base de Dados checkbox selected: " + baseDeDadosCheckBox.isSelected());
        
        // If "Base de Dados" mode is selected, load from database files
        if (baseDeDadosCheckBox.isSelected()) {
            System.out.println("Loading from database...");
            return loadFromDatabase();
        }
        
        // Regular file loading mode - ensure database mode is disabled and clear old data
        mantraData.setDatabaseMode(false);
        this.loadedDatabaseFiles = null; // Clear database file tracking
        System.out.println("Regular file mode - Database mode flag set to: " + mantraData.isDatabaseMode());
        System.out.println("Path field text: " + pathField.getText());
        System.out.println("Current lines count: " + (mantraData.getLines() != null ? mantraData.getLines().size() : 0));
        
        if ((mantraData.getLines() == null || mantraData.getLines().isEmpty()) &&
                pathField.getText() != null && !pathField.getText().trim().isEmpty()) {
            System.out.println("Loading from specific file...");
            return loadFromSpecificFile();
        }
        
        boolean hasData = mantraData.getLines() != null && !mantraData.getLines().isEmpty();
        System.out.println("ensureFileLoaded returning: " + hasData + " (lines available: " + hasData + ")");
        return hasData;
    }
    
    private boolean loadFromDatabase() {
        try {
            String userHome = System.getProperty("user.home");
            String mantrasPath = userHome + File.separator + "Documents" + File.separator + "MantraCount" + File.separator + "Mantras";
            File mantrasDir = new File(mantrasPath);
            
            if (!mantrasDir.exists()) {
                UIUtils.showError(StringConstants.createBilingualError(
                        StringConstants.DATABASE_DIR_NOT_FOUND_EN,
                        StringConstants.DATABASE_DIR_NOT_FOUND_PT));
                return false;
            }
            
            // Get start date from the date range controller
            LocalDate startDate = getStartDateForDatabase();
            LocalDate endDate = LocalDate.now(); // Always up to today
            
            // Get list of files to load based on date range
            List<File> filesToLoad = getFilesForDateRange(mantrasDir, startDate, endDate);
            
            if (filesToLoad.isEmpty()) {
                UIUtils.showError(StringConstants.createBilingualError(
                        StringConstants.DATABASE_NO_FILES_EN,
                        StringConstants.DATABASE_NO_FILES_PT));
                return false;
            }
            
            List<String> allLines = new ArrayList<>();
            for (File txtFile : filesToLoad) {
                List<String> fileLines = FileLoader.robustReadLines(txtFile.toPath());
                allLines.addAll(fileLines);
                System.out.println("Loaded file: " + txtFile.getName() + " (" + fileLines.size() + " lines)");
            }
            
            // Store the loaded files for save functionality
            this.loadedDatabaseFiles = new ArrayList<>(filesToLoad);
            
            if (allLines.isEmpty()) {
                UIUtils.showError(StringConstants.createBilingualError(
                        StringConstants.DATABASE_NO_ENTRIES_EN,
                        StringConstants.DATABASE_NO_ENTRIES_PT));
                return false;
            }
            
            DateParser.resetDetectedFormat();
            mantraData.setLines(allLines);
            mantraData.setFilePath(mantrasPath); // Set directory path - counting doesn't need a real file
            mantraData.setFromZip(false);
            mantraData.setDatabaseMode(true); // Mark as database mode
            
            System.out.println("Database mode loaded - Directory: " + mantrasPath);
            System.out.println("Database mode flag set to: " + mantraData.isDatabaseMode());
            System.out.println("Total lines loaded: " + allLines.size());
            System.out.println("First few lines for debugging:");
            for (int i = 0; i < Math.min(5, allLines.size()); i++) {
                System.out.println("  Line " + i + ": " + allLines.get(i));
            }
            
            DateParser.detectDateFormat(allLines);
            
            System.out.println("Loaded " + allLines.size() + " lines from " + filesToLoad.size() + " database files");
            UIUtils.showInfo(StringConstants.createBilingualSuccess(
                    StringConstants.DATABASE_LOADED_SUCCESS_EN + " (" + filesToLoad.size() + " files)",
                    StringConstants.DATABASE_LOADED_SUCCESS_PT + " (" + filesToLoad.size() + " arquivos)"));
            return true;
            
        } catch (Exception ex) {
            System.err.println("Error loading from database: " + ex.getMessage());
            ex.printStackTrace();
            UIUtils.showError("❌ Error loading database: " + ex.getMessage() + " / ❌ Erro ao carregar base de dados: " + ex.getMessage());
            return false;
        }
    }
    
    private LocalDate getStartDateForDatabase() {
        if (dateRangeController != null) {
            LocalDate startDate = dateRangeController.getStartDate();
            if (startDate != null) {
                return startDate;
            }
        }
        // Default to start of current month if no date is specified
        return LocalDate.now().withDayOfMonth(1);
    }
    
    private List<File> getFilesForDateRange(File mantrasDir, LocalDate startDate, LocalDate endDate) {
        List<File> filesToLoad = new ArrayList<>();
        
        // Generate list of year-month combinations from start to end date
        LocalDate current = startDate.withDayOfMonth(1); // Start from beginning of start month
        while (!current.isAfter(endDate)) {
            String fileName = current.format(DateTimeFormatter.ofPattern("yyyy-MM")) + ".txt";
            File monthFile = new File(mantrasDir, fileName);
            
            if (monthFile.exists() && monthFile.isFile()) {
                filesToLoad.add(monthFile);
                System.out.println("Found file for " + current.format(DateTimeFormatter.ofPattern("yyyy-MM")) + ": " + fileName);
            }
            
            current = current.plusMonths(1);
        }
        
        return filesToLoad;
    }
    
    private boolean saveDatabaseChanges(Map<String, String> updatedMismatchMap, int updateCount) {
        try {
            // Get the current lines (which includes updates)
            List<String> updatedLines = mantraData.getLines();
            
            // Group lines by their date to determine which file they belong to
            Map<String, List<String>> fileContentMap = groupLinesByMonth(updatedLines);
            
            // Write each group back to its respective file
            for (Map.Entry<String, List<String>> entry : fileContentMap.entrySet()) {
                String monthKey = entry.getKey(); // Format: YYYY-MM
                List<String> linesForMonth = entry.getValue();
                
                String userHome = System.getProperty("user.home");
                String mantrasPath = userHome + File.separator + "Documents" + File.separator + "MantraCount" + File.separator + "Mantras";
                String fileName = monthKey + ".txt";
                String filePath = mantrasPath + File.separator + fileName;
                
                // Write lines to file
                try (java.io.FileWriter writer = new java.io.FileWriter(filePath)) {
                    for (String line : linesForMonth) {
                        writer.write(line + System.lineSeparator());
                    }
                }
                
                System.out.println("Updated database file: " + fileName + " with " + linesForMonth.size() + " lines");
            }
            
            UIUtils.showInfo(StringConstants.createBilingualSuccess(
                    StringConstants.DATABASE_SAVED_SUCCESS_EN + ". " + updateCount + " line(s) updated across " + fileContentMap.size() + " file(s)",
                    StringConstants.DATABASE_SAVED_SUCCESS_PT + ". " + updateCount + " linha(s) atualizada(s) em " + fileContentMap.size() + " arquivo(s)"));
            return true;
            
        } catch (Exception ex) {
            ex.printStackTrace();
            UIUtils.showError(StringConstants.createBilingualError(
                    StringConstants.DATABASE_SAVE_ERROR_EN,
                    StringConstants.DATABASE_SAVE_ERROR_PT));
            return false;
        }
    }
    
    private Map<String, List<String>> groupLinesByMonth(List<String> lines) {
        Map<String, List<String>> fileContentMap = new java.util.HashMap<>();
        
        for (String line : lines) {
            if (line == null || line.trim().isEmpty()) {
                continue;
            }
            
            // Extract date from the line to determine which file it belongs to
            String monthKey = extractMonthFromLine(line);
            if (monthKey != null) {
                fileContentMap.computeIfAbsent(monthKey, k -> new ArrayList<>()).add(line);
            }
        }
        
        return fileContentMap;
    }
    
    private String extractMonthFromLine(String line) {
        try {
            // Extract message content first (remove WhatsApp formatting)
            String messageContent = ParsingUtils.extractMessageContent(line);
            
            // Try to extract date from the original line (before the colon)
            // Handle iPhone format: [MM/dd/yy, time] or [dd/MM/yy, time]
            if (line.startsWith("[")) {
                int commaIndex = line.indexOf(',');
                if (commaIndex > 0) {
                    String datePart = line.substring(1, commaIndex).trim();
                    
                    // Parse the date part
                    java.util.regex.Pattern datePattern = java.util.regex.Pattern.compile("(\\d{1,2})/(\\d{1,2})/(\\d{2,4})");
                    java.util.regex.Matcher matcher = datePattern.matcher(datePart);
                    
                    if (matcher.find()) {
                        String firstNum = matcher.group(1);
                        String secondNum = matcher.group(2);
                        String year = matcher.group(3);
                        
                        // Convert 2-digit year to 4-digit
                        if (year.length() == 2) {
                            int yearInt = Integer.parseInt(year);
                            year = (yearInt < 50) ? "20" + year : "19" + year;
                        }
                        
                        // Determine if it's MM/dd or dd/MM format and extract month
                        DateParser.DateFormat format = DateParser.getCurrentDateFormat();
                        String month;
                        if (format == DateParser.DateFormat.BR_FORMAT) {
                            // dd/MM format
                            month = String.format("%02d", Integer.parseInt(secondNum));
                        } else {
                            // MM/dd format  
                            month = String.format("%02d", Integer.parseInt(firstNum));
                        }
                        
                        return year + "-" + month;
                    }
                }
            }
            
            // Handle Android format: DD/MM/YYYY HH:MM - Name: Message
            java.util.regex.Pattern androidPattern = java.util.regex.Pattern.compile("^(\\d{1,2}/\\d{1,2}/\\d{2,4})\\s+\\d{1,2}:\\d{1,2}\\s+-");
            java.util.regex.Matcher androidMatcher = androidPattern.matcher(line);
            if (androidMatcher.find()) {
                String datePart = androidMatcher.group(1);
                // Similar parsing logic for Android format
                // This is for completeness but iPhone format is more common
            }
            
        } catch (Exception ex) {
            System.err.println("Error extracting month from line: " + line + " - " + ex.getMessage());
        }
        
        // Fallback to current month if extraction fails
        return LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
    }
    
    private boolean loadFromSpecificFile() {
        try {
            System.out.println("Ensuring file loaded from path: " + pathField.getText().trim());
            File file = new File(pathField.getText().trim()).getAbsoluteFile();
            System.out.println("Absolute path: " + file.getAbsolutePath());
            System.out.println("File exists: " + file.exists());

            if (file.exists() && file.isFile()) {
                boolean isZipFile = file.getName().toLowerCase().endsWith(".zip");
                System.out.println("Is ZIP file: " + isZipFile);

                DateParser.resetDetectedFormat();

                mantraData.setFromZip(isZipFile);
                mantraData.setDatabaseMode(false); // Regular file mode

                if (isZipFile) {
                    mantraData.setOriginalZipPath(file.getAbsolutePath());
                    try {
                        FileLoader.ExtractedFileInfo extractInfo = FileLoader.extractFirstTxtFromZip(file);
                        File extractedFile = extractInfo.getExtractedFile();
                        String originalEntryName = extractInfo.getOriginalEntryName();

                        mantraData.setOriginalZipEntryName(originalEntryName);

                        System.out.println("Extracted file: " + extractedFile.getAbsolutePath());
                        System.out.println("Original ZIP entry: " + originalEntryName);

                        List<String> lines = FileLoader.robustReadLines(extractedFile.toPath());
                        mantraData.setLines(lines);
                        mantraData.setFilePath(extractedFile.getAbsolutePath());

                        DateParser.detectDateFormat(lines);

                        System.out.println("Loaded " + lines.size() + " lines from extracted file");

                    } catch (Exception ex) {
                        System.err.println("Error extracting from ZIP: " + ex.getMessage());
                        ex.printStackTrace();
                        UIUtils.showError("❌ Failed to extract from ZIP file. / Falha ao extrair arquivo .zip",
                                ex.getMessage());
                        return false;
                    }
                } else {
                    List<String> lines = FileLoader.robustReadLines(file.toPath());
                    mantraData.setLines(lines);
                    mantraData.setFilePath(file.getAbsolutePath());

                    DateParser.detectDateFormat(lines);

                    System.out.println("Loaded " + lines.size() + " lines from text file");
                }
                return true;
            } else {
                System.err.println("File does not exist or is not a file: " + file.getAbsolutePath());
                UIUtils.showError("❌ File not found / Arquivo não encontrado",
                        "The selected file does not exist. / O arquivo selecionado não existe.");
                return false;
            }
        } catch (Exception ex) {
            System.err.println("Error loading file: " + ex.getMessage());
            ex.printStackTrace();
            UIUtils.showError("❌ Erro ao carregar arquivo: " + ex.getMessage() + " / ❌ Error loading file: " + ex.getMessage());
            return false;
        }
    }

    public boolean saveChanges(Map<String, String> updatedMismatchMap) {
        try {
            if (mantraData.getLines() == null) {
                UIUtils.showError("No data. / Sem dados.",
                        "No file loaded or processed. \nNenhum arquivo carregado ou processado.");
                return false;
            }

            int updateCount = FileUtils.updateFileContent(mantraData, updatedMismatchMap);
            
            // Handle saving based on mode
            if (mantraData.isDatabaseMode()) {
                return saveDatabaseChanges(updatedMismatchMap, updateCount);
            } else {
                FileUtils.saveToFileWithZipHandling(mantraData);
                UIUtils.showInfo("✔ Changes saved successfully. \n✔ Alterações salvas com sucesso.\n" +
                        "✔ " + updateCount + " line(s) updated. \n✔ " + updateCount + " linha(s) atualizada(s).");
                return true;
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            UIUtils.showError("❌ Failed to save changes. \n❌ Falha ao salvar alterações.");
            return false;
        }
    }

}