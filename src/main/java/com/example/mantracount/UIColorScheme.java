package com.example.mantracount;

public class UIColorScheme {
    // Background colors
    public static final String INPUT_BACKGROUND = "#B8D0E8";         // Light blue for input fields
    public static final String INPUT_HEADER_BG = "#C8DCF0";          // Very light blue for headers
    public static final String RESULTS_BACKGROUND = "#A8C4DC";       // Medium light blue for results
    public static final String MAIN_BACKGROUND = "#88A9CC";          // Your main blue

    // Border colors
    public static final String BORDER_DEFAULT = "#B8C8D8";
    public static final String BORDER_FOCUSED = "#5A7A9A";
    public static final String BORDER_ACCENT = "#5A7A9A";

    // Text colors
    public static final String TEXT_PRIMARY = "#2C3E50";
    public static final String TEXT_HEADER = "#34495E";
    public static final String TEXT_PLACEHOLDER = "#7A8B9C";
    public static final String TEXT_SUCCESS = "#27AE60";
    public static final String TEXT_ERROR = "#E74C3C";
    public static final String TEXT_INFO = "#5DADE2";

    // Button states
    public static final String BUTTON_DISABLED = "#D5DDE5";
    public static final String BUTTON_DISABLED_TEXT = "#7A8B9C";

    // Accent colors
    public static final String ACCENT_LIGHT = "#A8B8C8";
    public static final String HOVER_ACCENT = "#7A9ABF";

    // === NEW HEADER COLOR PALETTE ===

    // Title/Header Colors (Dark contrasts for readability)
    public static final String HEADER_TITLE_PRIMARY = "#2C3E50";    // Deep charcoal blue - excellent contrast
    public static final String HEADER_TITLE_SECONDARY = "#34495E";  // Slightly lighter charcoal
    public static final String HEADER_SUBTITLE = "#4A5F7A";        // Medium blue-gray for subtitles

    // Text Fill-in Fields (Input backgrounds that work with blue-gray)
    public static final String INPUT_HEADER_ALT_BG = "#F8FAFB";     // Very light blue-white
    public static final String INPUT_PLACEHOLDER_TEXT = "#6B7B8A";  // Medium gray-blue for placeholders

    // Menu Colors (Gray menu options)
    public static final String MENU_BACKGROUND = "#E8EDF2";         // Light gray-blue for menu backgrounds
    public static final String MENU_ITEM_DEFAULT = "#5A6B7A";       // Medium gray-blue for menu items
    public static final String MENU_ITEM_HOVER = "#4A5A69";         // Darker on hover
    public static final String MENU_ITEM_SELECTED = "#FFFFFF";      // White text when selected
    public static final String MENU_SELECTED_BG = "#5A7A9A";        // Darker blue-gray for selected background

    // Accent Colors that work with your blue-gray theme
    public static final String ACCENT_WARM = "#D4A574";             // Warm beige/tan - complementary
    public static final String ACCENT_CORAL = "#E08A7B";            // Soft coral - warm complement
    public static final String ACCENT_CREAM = "#F5F2E8";            // Warm cream for highlights

    // Border Colors for Header Elements
    public static final String HEADER_BORDER_LIGHT = "#C8D4E0";     // Light blue-gray border
    public static final String HEADER_BORDER_MEDIUM = "#A8B8C8";    // Medium border
    public static final String HEADER_BORDER_DARK = "#7A8B9C";      // Darker border for definition

    public static String getHeaderSubtitleStyle() {
        return String.format(
                "-fx-font-size: 14px; " +
                        "-fx-font-weight: normal; " +
                        "-fx-text-fill: %s;",
                HEADER_SUBTITLE
        );
    }

    public static String getHeaderInputFieldStyle() {
        return String.format(
                "-fx-background-color: %s; " +
                        "-fx-border-color: %s; " +
                        "-fx-border-width: 1px; " +
                        "-fx-border-radius: 4px; " +
                        "-fx-background-radius: 4px; " +
                        "-fx-text-fill: %s; " +
                        "-fx-prompt-text-fill: %s; " +
                        "-fx-padding: 6px 8px;",
                INPUT_HEADER_BG, HEADER_BORDER_MEDIUM, HEADER_TITLE_PRIMARY, INPUT_PLACEHOLDER_TEXT
        );
    }

    public static String getHeaderInputFocusedStyle() {
        return getHeaderInputFieldStyle()
                .replace(HEADER_BORDER_MEDIUM, HEADER_BORDER_DARK)
                .replace("1px", "2px");
    }

    // ADD these methods to your UIColorScheme.java

    // Main background
    public static String getMainBackgroundStyle() {
        return "-fx-background-color: " + MAIN_BACKGROUND + ";";
    }

    // Section containers (light background with border)
    public static String getSectionContainerStyle() {
        return String.format(
                "-fx-background-color: %s; " +
                        "-fx-border-color: %s; " +
                        "-fx-border-width: 1px; " +
                        "-fx-border-radius: 5px; " +
                        "-fx-background-radius: 5px;",
                INPUT_BACKGROUND, BORDER_DEFAULT
        );
    }

    // Section subtitles (smaller, lighter)
    public static String getSectionSubtitleStyle() {
        return String.format(
                "-fx-font-size: 12px; " +
                        "-fx-text-fill: %s;",
                TEXT_PLACEHOLDER
        );
    }

    public static String getMenuItemStyle() {
        return String.format(
                "-fx-background-color: %s; " +
                        "-fx-text-fill: %s; " +
                        "-fx-padding: 8px 12px; " +
                        "-fx-border-radius: 3px; " +
                        "-fx-background-radius: 3px;",
                MENU_BACKGROUND, MENU_ITEM_DEFAULT
        );
    }

    public static String getMenuItemHoverStyle() {
        return String.format(
                "-fx-background-color: %s; " +
                        "-fx-text-fill: %s; " +
                        "-fx-padding: 8px 12px; " +
                        "-fx-border-radius: 3px; " +
                        "-fx-background-radius: 3px;",
                HEADER_BORDER_MEDIUM, MENU_ITEM_HOVER
        );
    }

    public static String getMenuItemSelectedStyle() {
        return String.format(
                "-fx-background-color: %s; " +
                        "-fx-text-fill: %s; " +
                        "-fx-padding: 8px 12px; " +
                        "-fx-border-radius: 3px; " +
                        "-fx-background-radius: 3px;",
                MENU_SELECTED_BG, MENU_ITEM_SELECTED
        );
    }

    public static String getHeaderTitleStyle() {
        return String.format(
                "-fx-font-size: 20px; " +
                        "-fx-font-weight: bold; " +
                        "-fx-text-fill: %s;",
                HEADER_TITLE_PRIMARY  // This is #2C3E50 - dark charcoal
        );
    }

    public static String getFieldLabelStyle() {
        return String.format(
                "-fx-font-weight: bold; " +
                        "-fx-text-fill: %s;",
                HEADER_TITLE_PRIMARY  // Use the dark charcoal color, not TEXT_HEADER
        );
    }

    public static String getSectionTitleStyle() {
        return String.format(
                "-fx-font-size: 16px; " +
                        "-fx-font-weight: bold; " +
                        "-fx-text-fill: %s;",
                HEADER_TITLE_PRIMARY  // Use the dark charcoal color, not TEXT_PRIMARY
        );
    }
    // Header Container Style
    public static String getHeaderContainerStyle() {
        return String.format(
                "-fx-background-color: %s; " +
                        "-fx-padding: 15px; " +
                        "-fx-border-color: %s; " +
                        "-fx-border-width: 0 0 2px 0;",  // Bottom border only
                MAIN_BACKGROUND, HEADER_BORDER_DARK
        );
    }

    // Style builders for common components
    public static String getInputFieldStyle() {
        return String.format(
                "-fx-background-color: %s; " +
                        "-fx-border-color: %s; " +
                        "-fx-border-width: 1px; " +
                        "-fx-border-radius: 3px; " +
                        "-fx-background-radius: 3px; " +
                        "-fx-text-fill: %s; " +
                        "-fx-prompt-text-fill: %s;",
                INPUT_BACKGROUND, BORDER_DEFAULT, TEXT_PRIMARY, TEXT_PLACEHOLDER
        );
    }

    public static String getInputFieldFocusedStyle() {
        return getInputFieldStyle().replace(BORDER_DEFAULT, BORDER_FOCUSED).replace("1px", "2px");
    }

    public static String getTextAreaStyle() {
        return String.format(
                "-fx-background-color: %s; " +
                        "-fx-border-color: %s; " +
                        "-fx-border-width: 1px; " +
                        "-fx-border-radius: 4px; " +
                        "-fx-background-radius: 4px; " +
                        "-fx-text-fill: %s;",
                RESULTS_BACKGROUND, BORDER_DEFAULT, TEXT_PRIMARY
        );
    }


    public static String getHeaderLabelStyle() {
        return String.format(
                        "-fx-font-weight: bold; " +
                        "-fx-text-fill: %s;",
                TEXT_HEADER
        );
    }

    public static String getPlaceholderLabelStyle() {
        return String.format(
                "-fx-text-fill: %s; " +
                        "-fx-font-style: normal;",
                TEXT_PLACEHOLDER
        );
    }

    public static String getScrollPaneStyle() {
        return String.format(
                "-fx-background-color: %s; " +
                        "-fx-border-color: %s; " +
                        "-fx-border-width: 2px; " +
                        "-fx-border-radius: 4px;",
                INPUT_BACKGROUND, BORDER_ACCENT
        );
    }
}