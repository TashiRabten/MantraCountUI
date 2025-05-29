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

public class UIComponentFactory {

    private static final ButtonImageUtils buttonImageUtils = new ButtonImageUtils();
    private static final Duration TOOLTIP_SHOW_DELAY = Duration.millis(300);
    private static final Duration TOOLTIP_HIDE_DELAY = Duration.millis(100);

    public static final String SAVE_ACTION_COLOR = UIColorScheme.SAVE_ACTION_COLOR;
    public static final String CANCEL_ACTION_COLOR = UIColorScheme.CANCEL_ACTION_COLOR;
    public static final String PROCESS_ACTION_COLOR = UIColorScheme.PROCESS_ACTION_COLOR;
    public static final String NAVIGATION_COLOR = UIColorScheme.NAVIGATION_COLOR;
    public static final String FEATURE_MISSING_DAYS_COLOR = UIColorScheme.FEATURE_MISSING_DAYS_COLOR;
    public static final String FEATURE_ALL_MANTRAS_COLOR = UIColorScheme.FEATURE_ALL_MANTRAS_COLOR;
    public static final String FEATURE_SEM_FIZ_COLOR = UIColorScheme.FEATURE_SEM_FIZ_COLOR;
    public static final String UPDATE_COLOR = UIColorScheme.UPDATE_COLOR;
    public static final String UNDO_COLOR = UIColorScheme.UNDO_COLOR;

    public enum ButtonAlignment {
        LEFT, CENTER, RIGHT
    }

    public enum TextAreaState {
        NORMAL, PLACEHOLDER, SUCCESS, ERROR, INFO
    }

    public static Button createButton(String text, String tooltip) {
        Button button = new Button(text);
        addTooltip(button, tooltip);
        return button;
    }

    public static class ActionButtons {

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

        public static Button createOpenFileButton() {
            return createStyledButton(StringConstants.OPEN_FILE_PT,
                    StringConstants.OPEN_FILE_EN,
                    NAVIGATION_COLOR, null);
        }
    }

    public static class TextFields {

        public static TextField createTextField(String placeholder, String tooltip) {
            TextField field = new TextField();
            field.setPromptText(placeholder);
            addTooltip(field, tooltip);
            field.setStyle(UIColorScheme.getInputFieldStyle());
            addInputFieldFocusEffect(field);
            return field;
        }

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
            field.setStyle(UIColorScheme.getInputFieldStyle());
            addInputFieldFocusEffect(field);
            return field;
        }
    }

    public static class DatePickers {

        public static DatePicker createStartDatePicker() {
            DatePicker datePicker = new DatePicker();
            datePicker.setEditable(true);
            datePicker.setStyle(UIColorScheme.getDatePickerStyle());
            datePicker.focusedProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal) {
                    datePicker.setStyle(UIColorScheme.getDatePickerFocusedStyle());
                } else {
                    datePicker.setStyle(UIColorScheme.getDatePickerStyle());
                }
            });
            datePicker.setPromptText(StringConstants.START_DATE_LABEL_PT);
            addTooltip(datePicker, StringConstants.START_DATE_TOOLTIP_EN);
            return datePicker;
        }

        public static DatePicker createEndDatePicker() {
            DatePicker datePicker = new DatePicker();
            datePicker.setEditable(true);
            datePicker.setStyle(UIColorScheme.getDatePickerStyle());
            datePicker.focusedProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal) {
                    datePicker.setStyle(UIColorScheme.getDatePickerFocusedStyle());
                } else {
                    datePicker.setStyle(UIColorScheme.getDatePickerStyle());
                }
            });
            datePicker.setPromptText(StringConstants.END_DATE_LABEL_PT);
            addTooltip(datePicker, StringConstants.END_DATE_TOOLTIP_EN);
            return datePicker;
        }
    }

    public static class Layouts {

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

        public static HBox createMainActionLayout(javafx.scene.Node... nodes) {
            return createButtonLayout(ButtonAlignment.LEFT, nodes);
        }

        public static HBox createDialogActionLayout(javafx.scene.Node... nodes) {
            return createButtonLayout(ButtonAlignment.RIGHT, nodes);
        }

        public static HBox createSearchContainer(TextField searchField, CheckBox exactWordCheckBox,
                                                 Button searchButton, Button prevButton, Button nextButton) {
            HBox searchContainer = new HBox(10, searchField, exactWordCheckBox, searchButton, prevButton, nextButton);
            searchContainer.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            return searchContainer;
        }
    }

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
        TextArea resultsArea = new TextArea();
        resultsArea.setText("Contagem de Mantras");
        resultsArea.setPrefRowCount(6);
        resultsArea.setMinHeight(114);
        resultsArea.setMaxHeight(114);
        resultsArea.setEditable(false);
        resultsArea.setWrapText(true);

        // Use black text for results area
        resultsArea.setStyle(UIColorScheme.getResultsAreaStyle());


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

        // Use black text for summary area
        summaryArea.setStyle(UIColorScheme.getResultsContainerStyle()
        );

        addTooltip(summaryArea, "Summary - Shows analysis results and statistics");
        return summaryArea;
    }

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

    public static ScrollPane createStyledScrollPane(VBox content, double prefHeight) {
        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(prefHeight);

        // Force the proper background color for scroll panes
        scrollPane.setStyle(
                "-fx-background: " + UIColorScheme.INPUT_BACKGROUND + "; " +
                        "-fx-control-inner-background: " + UIColorScheme.INPUT_BACKGROUND + "; " +
                        "-fx-background-color: " + UIColorScheme.INPUT_BACKGROUND + "; " +
                        "-fx-border-color: " + UIColorScheme.BORDER_ACCENT + "; " +
                        "-fx-border-width: 2px; " +
                        "-fx-border-radius: 4px;"
        );

        return scrollPane;
    }

    public static ScrollPane createStyledScrollPane(VBox content) {
        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);

        // Force the proper background color
        scrollPane.setStyle(
                "-fx-background: " + UIColorScheme.INPUT_BACKGROUND + "; " +
                        "-fx-control-inner-background: " + UIColorScheme.INPUT_BACKGROUND + "; " +
                        "-fx-background-color: " + UIColorScheme.INPUT_BACKGROUND + "; " +
                        "-fx-border-color: #0078D7; " +
                        "-fx-border-width: 1px;"
        );

        return scrollPane;
    }

    public static Button createStyledButton(String text, String tooltip, String color, String iconKey) {
        Button button = createButton(text, tooltip);
        if (color != null) {
            String simpleStyle = String.format(
                    "-fx-base: %s; -fx-text-fill: white;",
                    color
            );
            button.setStyle(simpleStyle);
            addHoverEffect(button, color);
        }
        if (iconKey != null) {
            buttonImageUtils.assignButtonIcon(button, iconKey, buttonImageUtils.imageIni());
        }
        return button;
    }

    public static ProgressIndicator createProgressIndicator() {
        ProgressIndicator progress = new ProgressIndicator();
        progress.setMaxSize(50, 50);
        progress.setVisible(false);
        addTooltip(progress, StringConstants.PROGRESS_TOOLTIP);
        return progress;
    }

    public static CheckBox createExactWordCheckBox() {
        CheckBox checkBox = new CheckBox("Palavra exata");
        checkBox.setStyle("-fx-text-fill: #000000;");
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

    public static HBox createEditableLineContainer(String originalLine) {
        LineParser.LineSplitResult splitResult = LineParser.splitEditablePortion(originalLine);
        String protectedPart = splitResult.getFixedPrefix();
        String editablePart = splitResult.getEditableSuffix();

        Label protectedLabel = new Label(protectedPart);
        protectedLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #000000;");
        addTooltip(protectedLabel, StringConstants.PROTECTED_CONTENT_TOOLTIP);

        TextField editableField = TextFields.createEditLineField(editablePart);
        HBox.setHgrow(editableField, Priority.ALWAYS);

        HBox container = new HBox(5, protectedLabel, editableField);
        container.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        return container;
    }

    public static void setTextAreaState(TextArea textArea, TextAreaState state, String content) {
        textArea.setText(content);

        // Base style with forced background color
        String baseStyle = String.format(
                "-fx-control-inner-background: %s; " +
                        "-fx-background-color: %s; " +
                        "-fx-border-color: %s; " +
                        "-fx-border-width: 1px; " +
                        "-fx-border-radius: 3px; " +
                        "-fx-background-radius: 3px;",
                UIColorScheme.RESULTS_BACKGROUND, UIColorScheme.RESULTS_BACKGROUND, UIColorScheme.BORDER_DEFAULT
        );

        switch (state) {
            case NORMAL -> textArea.setStyle(baseStyle +
                    "-fx-text-fill: #000000;");
            case PLACEHOLDER -> textArea.setStyle(baseStyle +
                    "-fx-text-fill: #000000; -fx-font-style: normal;");
            case SUCCESS -> textArea.setStyle(baseStyle +
                    String.format("-fx-text-fill: %s;", UIColorScheme.TEXT_SUCCESS));
            case ERROR -> textArea.setStyle(baseStyle +
                    String.format("-fx-text-fill: %s;", UIColorScheme.TEXT_ERROR));
            case INFO -> textArea.setStyle(baseStyle +
                    String.format("-fx-text-fill: %s;", UIColorScheme.TEXT_INFO));
        }
    }

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
}