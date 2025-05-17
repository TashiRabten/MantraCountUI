package com.example.mantracount;

import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles search functionality for the Mantra application.
 * Manages search input, navigation, and highlighting of search results.
 */
public class SearchController {
    
    private final TextField searchField;
    private final CheckBox exactWordCheckBox;
    private final Button searchButton;
    private final Button prevButton;
    private final Button nextButton;
    private final HBox searchContainer;
    private final ScrollPane scrollPane;
    private final VBox contentContainer;
    
    private String lastSearchQuery = "";
    private int currentSearchIndex = -1;
    private final List<Node> searchMatches = new ArrayList<>();
    
    /**
     * Creates a new SearchController.
     * 
     * @param contentContainer The container with content to search in
     * @param scrollPane The scroll pane containing the content container
     */
    public SearchController(VBox contentContainer, ScrollPane scrollPane) {
        this.contentContainer = contentContainer;
        this.scrollPane = scrollPane;
        
        // Initialize search field
        searchField = new TextField();
        UIUtils.setPlaceholder(searchField, "Search... / Buscar...");
        
        // Initialize checkbox for exact word match
        exactWordCheckBox = new CheckBox("Exact word / Palavra exata");
        exactWordCheckBox.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (oldValue != newValue) {  // Only reset if the value actually changed
                resetSearchState();

                // If there's text in the search field, automatically perform a new search
                if (searchField.getText() != null && !searchField.getText().isEmpty()) {
                    searchInContent();
                }
            }
        });
        
        // Initialize search and navigation buttons
        searchButton = new Button("Search / Buscar");
        prevButton = new Button("◀ Prev / Anterior");
        nextButton = new Button("Next / Próximo ▶");
        
        // Set initial state for navigation buttons
        prevButton.setDisable(true);
        nextButton.setDisable(true);
        
        // Setup search container
        searchContainer = new HBox(10, searchField, exactWordCheckBox, searchButton, prevButton, nextButton);
        searchContainer.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        
        // Add listeners
        setupListeners();
    }
    
    /**
     * Sets up event listeners for search components.
     */
    private void setupListeners() {
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.equals(lastSearchQuery)) {
                resetSearchState();
            }
        });
        
        searchButton.setOnAction(e -> searchInContent());
        prevButton.setOnAction(e -> navigateSearch(-1));
        nextButton.setOnAction(e -> navigateSearch(1));
    }
    
    /**
     * Gets the search UI container.
     * @return The HBox containing search components
     */
    public HBox getSearchContainer() {
        return searchContainer;
    }
    
    /**
     * Searches through content for the search query and highlights matches.
     */
    public void searchInContent() {
        String query = searchField.getText();
        if (query == null || query.isEmpty() || contentContainer.getChildren().isEmpty()) {
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
        for (Node node : contentContainer.getChildren()) {
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
    
    /**
     * Searches through content for the search query without showing notifications.
     * Used when search criteria change but no notification is needed.
     */
    public void searchInContentQuietly() {
        String query = searchField.getText();
        if (query == null || query.isEmpty() || contentContainer.getChildren().isEmpty()) {
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
        for (Node node : contentContainer.getChildren()) {
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
        }
    }
    
    /**
     * Navigates to the next/previous search match.
     * @param direction 1 for next, -1 for previous
     */
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
        scrollPane.setVvalue(
                (double) contentContainer.getChildren().indexOf(currentMatch) / contentContainer.getChildren().size()
        );

        // Update navigation buttons state
        updateNavigationButtonState();
    }
    
    /**
     * Highlights the current search match.
     */
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
    
    /**
     * Removes highlighting from the current search match.
     */
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
    
    /**
     * Resets the search state, clearing all highlights and matches.
     */
    public void resetSearchState() {
        if (searchField != null) {
            lastSearchQuery = "";
            currentSearchIndex = -1;
            searchMatches.clear();
            prevButton.setDisable(true);
            nextButton.setDisable(true);

            // Remove any highlights, but only from TextFields
            for (Node node : contentContainer.getChildren()) {
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
    
    /**
     * Updates the state of navigation buttons based on search results.
     */
    private void updateNavigationButtonState() {
        boolean hasMatches = !searchMatches.isEmpty();
        prevButton.setDisable(!hasMatches);
        nextButton.setDisable(!hasMatches);
    }
    
    /**
     * Checks if text contains the search query.
     * 
     * @param text The text to search in
     * @param query The search query
     * @param exactWord Whether to match whole words only
     * @return true if a match is found, false otherwise
     */
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
}
