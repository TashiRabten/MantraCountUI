package com.example.mantracount;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/**
 * Flexible UI component factory that can adapt to different UI contexts.
 * Provides both standardized components and customizable alternatives.
 */
public class UIComponentFactory {

    private static final ButtonImageUtils buttonImageUtils = new ButtonImageUtils();


    // Standard tooltip timing
    private static final Duration TOOLTIP_SHOW_DELAY = Duration.millis(300);
    private static final Duration TOOLTIP_HIDE_DELAY = Duration.millis(100);

    // Consistent color groups by function
    public static final String SAVE_ACTION_COLOR = "#BFAF7C";      // Save, Apply, Salvar Alterações
    public static final String CANCEL_ACTION_COLOR = "#D16A5F";    // Cancel, Close, Fechar, Cancelar, Clear
    public static final String PROCESS_ACTION_COLOR = "#BFAF7C";   // Process, Analyze, Load, Contar, Carregar, Analisar
    public static final String NAVIGATION_COLOR = "#3E7EBE";       // Search, Previous, Next, Navigation buttons
    public static final String FEATURE_MISSING_DAYS_COLOR = "#3E7EBE";
    public static final String FEATURE_ALL_MANTRAS_COLOR = "#7F5DA3";
    public static final String FEATURE_SEM_FIZ_COLOR = "#E08232";
    public static final String UPDATE_COLOR = "#8C6D98";
    public static final String UNDO_COLOR = "#3E7EBE";

    /**
     * Button alignment options for different UI contexts
     */
    public enum ButtonAlignment {
        LEFT, CENTER, RIGHT
    }

    /**
     * Creates a basic button with text and tooltip
     */
    public static Button createButton(String text, String tooltip) {
        Button button = new Button(text);
        addTooltip(button, tooltip);
        return button;
    }


    /**
     * Comprehensive action buttons for ALL UIs with consistent colors and icons
     */
    public static class ActionButtons {

        // === SAVE/APPLY ACTION GROUP (Same color #BFAF7C, save icon) ===
        public static Button createSaveButton() {
            return createStyledButton(StringConstants.SAVE_CHANGES_PT,
                    StringConstants.SAVE_CHANGES_EN,
                    SAVE_ACTION_COLOR, "save");
        }

        public static Button createApplyChangesButton() {
            return createStyledButton(StringConstants.APPLY_CHANGES_PT,
                    StringConstants.APPLY_CHANGES_EN,
                    SAVE_ACTION_COLOR, "save");
        }

        // === CANCEL/CLOSE ACTION GROUP (Same color #D16A5F, cancel icon) ===
        public static Button createCloseButton() {
            return createStyledButton(StringConstants.CLOSE_PT,
                    StringConstants.CLOSE_EN,
                    CANCEL_ACTION_COLOR, "cancel");
        }

        public static Button createCancelButton() {
            return createStyledButton(StringConstants.CANCEL_PT,
                    StringConstants.CANCEL_EN,
                    CANCEL_ACTION_COLOR, "cancel");
        }

        public static Button createClearButton() {
            return createStyledButton(StringConstants.CLEAR_RESULTS_PT,
                    StringConstants.CLEAR_RESULTS_EN,
                    CANCEL_ACTION_COLOR, "broom");
        }

        // === PROCESS/ANALYZE/LOAD ACTION GROUP (Same color #BFAF7C, mala icon) ===
        public static Button createProcessButton() {
            return createStyledButton(StringConstants.PROCESS_MANTRAS_PT,
                    StringConstants.PROCESS_MANTRAS_EN,
                    PROCESS_ACTION_COLOR, "mala");
        }

        public static Button createAnalyzeButton() {
            return createStyledButton(StringConstants.ANALYZE_PT,
                    StringConstants.ANALYZE_EN,
                    PROCESS_ACTION_COLOR, "mala");
        }

        public static Button createLoadMantrasButton() {
            return createStyledButton(StringConstants.LOAD_MANTRAS_PT,
                    StringConstants.LOAD_MANTRAS_EN,
                    PROCESS_ACTION_COLOR, "mala");
        }

        // === FEATURE BUTTONS (Keep their unique colors for visual distinction) ===
        public static Button createMissingDaysButton() {
            return createStyledButton(StringConstants.MISSING_DAYS_PT,
                    StringConstants.MISSING_DAYS_EN,
                    FEATURE_MISSING_DAYS_COLOR, "calendar");
        }

        public static Button createAllMantrasButton() {
            return createStyledButton(StringConstants.ALL_MANTRAS_PT,
                    StringConstants.ALL_MANTRAS_EN,
                    FEATURE_ALL_MANTRAS_COLOR, "wheel");
        }

        public static Button createSemFizButton() {
            return createStyledButton(StringConstants.SEM_FIZ_PT,
                    StringConstants.SEM_FIZ_EN,
                    FEATURE_SEM_FIZ_COLOR, "lotus");
        }

        // === NAVIGATION/SEARCH GROUP (Same color #2196F3) ===
        public static Button createSearchButton() {
            return createStyledButton(StringConstants.SEARCH_PT,
                    StringConstants.SEARCH_EN,
                    NAVIGATION_COLOR, null);
        }

        public static Button createPreviousButton() {
            return createStyledButton(StringConstants.PREVIOUS_WITH_ARROW_PT,
                    StringConstants.PREVIOUS_EN,
                    NAVIGATION_COLOR, null);
        }

        public static Button createNextButton() {
            return createStyledButton(StringConstants.NEXT_WITH_ARROW_PT,
                    StringConstants.NEXT_EN,
                    NAVIGATION_COLOR, null);
        }

        public static Button createShowAllButton() {
            return createStyledButton(StringConstants.SHOW_ALL_PT,
                    StringConstants.SHOW_ALL_EN,
                    NAVIGATION_COLOR, null);
        }

        // === SPECIAL ACTION BUTTONS ===
        public static Button createUndoButton() {
            return createStyledButton(StringConstants.UNDO_WITH_ARROW_PT,
                    StringConstants.UNDO_EN,
                    UNDO_COLOR, null);
        }

        public static Button createUpdateButton() {
            Button button = createStyledButton("", StringConstants.UPDATE_TOOLTIP_EN,
                    UPDATE_COLOR, "update");
            button.setStyle("-fx-font-size: 10px; -fx-background-color: " + UPDATE_COLOR + "; -fx-text-fill: white;");
            return button;
        }

        // === FILE OPERATIONS ===
        public static Button createOpenFileButton() {
            return createStyledButton(StringConstants.OPEN_FILE_PT,
                    StringConstants.OPEN_FILE_EN,
                    NAVIGATION_COLOR, null);
        }
    }
    public static class TextFields {
        public static TextField createTextField(String placeholder, String tooltip) {
            TextField field = new TextField();
            UIUtils.setPlaceholder(field, placeholder);
            addTooltip(field, tooltip);

            // Use the centralized styling from UIColorScheme
            field.setStyle(UIColorScheme.getInputFieldStyle());

            // Use centralized focus effect
            addInputFieldFocusEffect(field);

            return field;
        }

        // Update all existing TextField methods to use consistent styling
        public static TextField createMantraField() {
            return createTextField(StringConstants.MANTRA_NAME_PLACEHOLDER_PT,
                    StringConstants.MANTRA_NAME_TOOLTIP_EN);
        }

        public static TextField createSearchField() {
            return createTextField(StringConstants.SEARCH_PLACEHOLDER_PT,
                    StringConstants.SEARCH_TOOLTIP_EN);
        }

        public static TextField createEditLineField(String content) {
            TextField field = new TextField(content);
            field.setPromptText(StringConstants.EDIT_LINE_PLACEHOLDER_PT);
            addTooltip(field, StringConstants.EDIT_LINE_TOOLTIP_EN);

            // Apply consistent styling
            field.setStyle(UIColorScheme.getInputFieldStyle());
            addInputFieldFocusEffect(field);

            return field;
        }

    }

    public static class DatePickers {
        public static DatePicker createStartDatePicker() {
            DatePicker datePicker = new DatePicker();
            datePicker.setEditable(true);

            // Apply consistent input field styling
            datePicker.setStyle(UIColorScheme.getInputFieldStyle());

            // Add focus effect for DatePicker
            datePicker.focusedProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal) {
                    datePicker.setStyle(UIColorScheme.getInputFieldFocusedStyle());
                } else {
                    datePicker.setStyle(UIColorScheme.getInputFieldStyle());
                }
            });

            datePicker.setPromptText(StringConstants.START_DATE_LABEL_PT);
            addTooltip(datePicker, StringConstants.START_DATE_TOOLTIP_EN);

            return datePicker;
        }

        public static DatePicker createEndDatePicker() {
            DatePicker datePicker = new DatePicker();
            datePicker.setEditable(true);

            // Apply consistent input field styling
            datePicker.setStyle(UIColorScheme.getInputFieldStyle());

            // Add focus effect for DatePicker
            datePicker.focusedProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal) {
                    datePicker.setStyle(UIColorScheme.getInputFieldFocusedStyle());
                } else {
                    datePicker.setStyle(UIColorScheme.getInputFieldStyle());
                }
            });

            datePicker.setPromptText(StringConstants.END_DATE_LABEL_PT);
            addTooltip(datePicker, StringConstants.END_DATE_TOOLTIP_EN);

            return datePicker;
        }
    }
    // Add focus effect helper
    private static void addInputFieldFocusEffect(TextField field) {
        field.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                field.setStyle(UIColorScheme.getInputFieldFocusedStyle());
            } else {
                field.setStyle(UIColorScheme.getInputFieldStyle());
            }
        });
    }

    public static TextArea createResultsArea() {
        TextArea resultsArea = new TextArea("Contagem de Mantras");
        resultsArea.setPrefRowCount(6);
        resultsArea.setMinHeight(114);
        resultsArea.setMaxHeight(114);
        resultsArea.setEditable(false);
        resultsArea.setWrapText(true);

        // Use simpler styling closer to the original
        resultsArea.setStyle(
                "-fx-background-color: " + UIColorScheme.RESULTS_BACKGROUND + "; " +
                        "-fx-text-fill: " + UIColorScheme.TEXT_PLACEHOLDER + "; " +
                        "-fx-font-style: normal; " +  // Remove italic
                        "-fx-border-color: " + UIColorScheme.BORDER_DEFAULT + "; " +
                        "-fx-border-width: 1px;"
        );

        addTooltip(resultsArea, "Mantra Count - Shows the counting results");
        return resultsArea;
    }

    public static TextArea createSummaryArea(String initialText) {
        TextArea summaryArea = new TextArea();
        summaryArea.setEditable(false);
        summaryArea.setWrapText(true);
        summaryArea.setPrefRowCount(8);
        summaryArea.setMinHeight(150);
        summaryArea.setMaxHeight(300);
        summaryArea.setText(initialText);

        // Keep it simple like the old version
        summaryArea.setStyle(
                "-fx-background-color: " + UIColorScheme.RESULTS_BACKGROUND + "; " +
                        "-fx-text-fill: " + UIColorScheme.TEXT_PRIMARY + "; " +  // Use primary text color
                        "-fx-border-color: " + UIColorScheme.BORDER_DEFAULT + "; " +
                        "-fx-border-width: 1px;"
        );

        addTooltip(summaryArea, "Summary - Shows analysis results and statistics");
        return summaryArea;
    }

    // Update label methods
    public static Label createHeaderLabel(String text, String englishTooltip) {
        Label header = new Label(text);
        header.setStyle(UIColorScheme.getHeaderLabelStyle());
        if (englishTooltip != null) {
            addTooltip(header, englishTooltip);
        }
        return header;
    }

    public static Label createPlaceholderLabel(String text, String englishTooltip) {
        Label placeholder = new Label(text);
        placeholder.setStyle(UIColorScheme.getPlaceholderLabelStyle());
        if (englishTooltip != null) {
            addTooltip(placeholder, englishTooltip);
        }
        return placeholder;
    }

    // Update scroll pane method
    public static ScrollPane createStyledScrollPane(VBox content, double prefHeight) {
        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(prefHeight);

        // Apply harmonious styling
        scrollPane.setStyle(UIColorScheme.getScrollPaneStyle());

        return scrollPane;
    }

    public static Button createStyledButton(String text, String tooltip, String color, String iconKey) {
        Button button = createButton(text, tooltip);

        if (color != null) {
            // Keep it simple like the old version, but with slight enhancement
            String simpleStyle = String.format(
                    "-fx-base: %s; -fx-text-fill: white;",
                    color
            );
            button.setStyle(simpleStyle);

            // Use the original hover effect
            addHoverEffect(button, color);
        }

        if (iconKey != null) {
            buttonImageUtils.assignButtonIcon(button, iconKey, buttonImageUtils.imageIni());
        }

        return button;
    }

    // Enhanced hover effect with border highlight
    private static void addEnhancedHoverEffect(Button button, String originalColor) {
        button.setOnMouseEntered(e -> {
            if (!button.isDisabled()) {
                String hoverStyle = String.format(
                        "-fx-base: derive(%s, -15%%); -fx-text-fill: white; " +
                                "-fx-border-color: %s; -fx-border-width: 2px; -fx-border-radius: 3px;",
                        originalColor, UIColorScheme.BORDER_FOCUSED
                );
                button.setStyle(hoverStyle);
            }
        });
        button.setOnMouseExited(e -> {
            String normalStyle = String.format(
                    "-fx-base: %s; -fx-text-fill: white; " +
                            "-fx-border-color: %s; -fx-border-width: 1px; -fx-border-radius: 3px;",
                    originalColor, UIColorScheme.BORDER_DEFAULT
            );
            button.setStyle(normalStyle);
        });

        // Handle disabled state
        button.disabledProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                String disabledStyle = String.format(
                        "-fx-base: %s; -fx-text-fill: %s; " +
                                "-fx-border-color: %s; -fx-border-width: 1px; -fx-border-radius: 3px;",
                        UIColorScheme.BUTTON_DISABLED, UIColorScheme.BUTTON_DISABLED_TEXT, UIColorScheme.ACCENT_LIGHT
                );
                button.setStyle(disabledStyle);
            }
        });
    }

    /**
     * Layout factory methods with flexible alignment
     */
    public static class Layouts {

        /**
         * Creates button layout with specified alignment
         */
        public static HBox createButtonLayout(ButtonAlignment alignment, javafx.scene.Node... nodes) {
            HBox layout = new HBox(10, nodes);

            switch (alignment) {
                case LEFT -> layout.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                case CENTER -> layout.setAlignment(javafx.geometry.Pos.CENTER);
                case RIGHT -> layout.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
            }

            layout.setPadding(new javafx.geometry.Insets(10, 0, 0, 0));
            return layout;
        }

        /**
         * Creates main app action button layout (left-aligned for main UI)
         */
        public static HBox createMainActionLayout(javafx.scene.Node... nodes) {
            return createButtonLayout(ButtonAlignment.LEFT, nodes);
        }

        /**
         * Creates dialog action button layout (right-aligned for dialogs)
         */
        public static HBox createDialogActionLayout(javafx.scene.Node... nodes) {
            return createButtonLayout(ButtonAlignment.RIGHT, nodes);
        }

        /**
         * Creates search container layout
         */
        public static HBox createSearchContainer(TextField searchField, CheckBox exactWordCheckBox,
                                                 Button searchButton, Button prevButton, Button nextButton) {
            HBox searchContainer = new HBox(10, searchField, exactWordCheckBox, searchButton, prevButton, nextButton);
            searchContainer.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            return searchContainer;
        }
    }

    /**
     * Component factory methods
     */
    public static ProgressIndicator createProgressIndicator() {
        ProgressIndicator progress = new ProgressIndicator();
        progress.setMaxSize(50, 50);
        progress.setVisible(false);
        addTooltip(progress, StringConstants.PROGRESS_TOOLTIP);
        return progress;
    }

    public static ScrollPane createStyledScrollPane(VBox content) {
        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-border-color: #0078D7; -fx-border-width: 1px;");
        return scrollPane;
    }


    public static CheckBox createExactWordCheckBox() {
        CheckBox checkBox = new CheckBox("Palavra exata");
        addTooltip(checkBox, "Exact word - Check to search for exact word matches only");
        return checkBox;
    }

    public static Label createInfoBadge(String text, String englishTooltip) {
        Label badge = new Label(text);
        badge.setPadding(new javafx.geometry.Insets(2, 8, 2, 8));
        badge.setStyle("-fx-background-color: #FFE0B2; -fx-background-radius: 4px; " +
                "-fx-font-size: 11px; -fx-text-fill: #E65100;");
        badge.setMinWidth(150);
        if (englishTooltip != null) {
            addTooltip(badge, englishTooltip);
        }
        return badge;
    }

    public static Label createTypeBadge(String type) {
        Label badge = new Label(type);
        badge.setPadding(new javafx.geometry.Insets(2, 8, 2, 8));
        badge.setStyle("-fx-background-color: #E3F2FD; -fx-background-radius: 4px; " +
                "-fx-font-weight: bold; -fx-text-fill: #1565C0;");
        badge.setPrefWidth(120);
        addTooltip(badge, "Mantra Type - Shows the type of mantra or ritual");
        return badge;
    }

    /**
     * Editable line creation for different contexts
     */
    public static HBox createEditableLineContainer(String originalLine) {
        LineParser.LineSplitResult splitResult = LineParser.splitEditablePortion(originalLine);
        String protectedPart = splitResult.getFixedPrefix();
        String editablePart = splitResult.getEditableSuffix();

        Label protectedLabel = new Label(protectedPart);
        protectedLabel.setStyle("-fx-font-weight: bold;");
        addTooltip(protectedLabel, StringConstants.PROTECTED_CONTENT_TOOLTIP);

        TextField editableField = TextFields.createEditLineField(editablePart);
        HBox.setHgrow(editableField, Priority.ALWAYS);

        HBox container = new HBox(5, protectedLabel, editableField);
        container.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        return container;
    }

    /**
     * Tooltip management
     */
    public static void addTooltip(Control control, String tooltipText) {
        if (tooltipText != null && !tooltipText.isEmpty()) {
            Tooltip tooltip = new Tooltip(tooltipText);
            tooltip.setShowDelay(TOOLTIP_SHOW_DELAY);
            tooltip.setHideDelay(TOOLTIP_HIDE_DELAY);
            Tooltip.install(control, tooltip);
        }
    }

    public static void addTooltip(javafx.scene.Node node, String tooltipText) {
        if (tooltipText != null && !tooltipText.isEmpty()) {
            Tooltip tooltip = new Tooltip(tooltipText);
            tooltip.setShowDelay(TOOLTIP_SHOW_DELAY);
            tooltip.setHideDelay(TOOLTIP_HIDE_DELAY);
            Tooltip.install(node, tooltip);
        }
    }

    public static void addHoverEffect(Button button, String originalColor) {
        button.setOnMouseEntered(e -> {
            if (!button.isDisabled()) {
                button.setStyle("-fx-base: derive(" + originalColor + ", -15%); -fx-text-fill: white;");
            }
        });
        button.setOnMouseExited(e -> {
            button.setStyle("-fx-base: " + originalColor + "; -fx-text-fill: white;");
        });
    }

    // Backward compatibility - deprecated methods
    @Deprecated
    public static HBox createActionButtonLayout(javafx.scene.Node... nodes) {
        return Layouts.createMainActionLayout(nodes);
    }

    @Deprecated
    public static HBox createSearchContainer(TextField searchField, CheckBox exactWordCheckBox,
                                             Button searchButton, Button prevButton, Button nextButton) {
        return Layouts.createSearchContainer(searchField, exactWordCheckBox, searchButton, prevButton, nextButton);
    }
    public static void setTextAreaState(TextArea textArea, TextAreaState state, String content) {
        textArea.setText(content);

        String baseStyle = UIColorScheme.getTextAreaStyle();
        switch (state) {
            case NORMAL -> textArea.setStyle(baseStyle);
            case PLACEHOLDER -> textArea.setStyle(baseStyle +
                    String.format("-fx-text-fill: %s; -fx-font-style: italic;", UIColorScheme.TEXT_PLACEHOLDER));
            case SUCCESS -> textArea.setStyle(baseStyle +
                    String.format("-fx-text-fill: %s;", UIColorScheme.TEXT_SUCCESS));
            case ERROR -> textArea.setStyle(baseStyle +
                    String.format("-fx-text-fill: %s;", UIColorScheme.TEXT_ERROR));
            case INFO -> textArea.setStyle(baseStyle +
                    String.format("-fx-text-fill: %s;", UIColorScheme.TEXT_INFO));
        }
    }

    public enum TextAreaState {
        NORMAL, PLACEHOLDER, SUCCESS, ERROR, INFO
    }

    public static class HeaderComponents {
        /**
         * Creates header input fields with proper styling
         */
        public static TextField createHeaderTextField(String placeholder, String tooltip) {
            TextField field = new TextField();
            field.setPromptText(placeholder);

            // Use the light blue input styling instead of white
            field.setStyle(UIColorScheme.getInputFieldStyle());

            // Add focus effect
            field.focusedProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal) {
                    field.setStyle(UIColorScheme.getInputFieldFocusedStyle());
                } else {
                    field.setStyle(UIColorScheme.getInputFieldStyle());
                }
            });

            if (tooltip != null) {
                addTooltip(field, tooltip);
            }

            return field;
        }

        /**
         * Creates a styled ComboBox for header menus
         */
        public static <T> ComboBox<T> createHeaderComboBox() {
            ComboBox<T> comboBox = new ComboBox<>();
            comboBox.setStyle(UIColorScheme.getMenuItemStyle());

            // Add hover and selection effects
            comboBox.setOnMouseEntered(e ->
                    comboBox.setStyle(UIColorScheme.getMenuItemHoverStyle())
            );
            comboBox.setOnMouseExited(e ->
                    comboBox.setStyle(UIColorScheme.getMenuItemStyle())
            );

            return comboBox;
        }

        /**
         * Creates a header container with proper background and borders
         */
        public static VBox createHeaderContainer(Node... children) {
            VBox headerContainer = new VBox(10);
            headerContainer.getChildren().addAll(children);
            headerContainer.setStyle(UIColorScheme.getHeaderContainerStyle());
            headerContainer.setAlignment(Pos.CENTER_LEFT);
            return headerContainer;
        }

        /**
         * Creates a horizontal header layout
         */
        public static HBox createHeaderRow(Node... children) {
            HBox headerRow = new HBox(15);
            headerRow.getChildren().addAll(children);
            headerRow.setAlignment(Pos.CENTER_LEFT);
            headerRow.setPadding(new Insets(5, 0, 5, 0));
            return headerRow;
        }
    }

}