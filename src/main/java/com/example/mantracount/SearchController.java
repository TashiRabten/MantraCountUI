package com.example.mantracount;

import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

/**
 * Refactored SearchController using centralized components and consistent styling.
 * Handles search functionality for the Mantra application with reduced duplication.
 */
public class SearchController {

    private final TextField searchField;
    private final CheckBox exactWordCheckBox;
    private final Button searchButton;
    private final Button prevButton;
    private final Button nextButton;
    private HBox searchContainer;

    // Fields for different container types
    private final VBox contentContainer;
    private final ScrollPane actualScrollPane;
    private final TitledPane mismatchesTitledPane;

    private String lastSearchQuery = "";
    private int currentSearchIndex = -1;
    private final List<Node> searchMatches = new ArrayList<>();
    private boolean isAllMantrasUI = false;

    /**
     * Constructor for mismatched lines panel (with TitledPane)
     */
    public SearchController(VBox mismatchesContainer, TitledPane mismatchesTitledPane) {
        this.contentContainer = mismatchesContainer;
        this.mismatchesTitledPane = mismatchesTitledPane;
        this.actualScrollPane = (ScrollPane) mismatchesTitledPane.getContent();

        // Create UI components using factory
        this.searchField = UIComponentFactory.TextFields.createSearchField();
        this.exactWordCheckBox = UIComponentFactory.createExactWordCheckBox();
        this.searchButton = UIComponentFactory.ActionButtons.createSearchButton();
        this.prevButton = UIComponentFactory.ActionButtons.createPreviousButton();
        this.nextButton = UIComponentFactory.ActionButtons.createNextButton();

        initializeSearchComponents();
    }

    /**
     * Constructor for general content containers (with ScrollPane directly)
     */
    public SearchController(VBox contentContainer, ScrollPane scrollPane) {
        this.contentContainer = contentContainer;
        this.actualScrollPane = scrollPane;
        this.mismatchesTitledPane = null;

        // Create UI components using factory
        this.searchField = UIComponentFactory.TextFields.createSearchField();
        this.exactWordCheckBox = UIComponentFactory.createExactWordCheckBox();
        this.searchButton = UIComponentFactory.ActionButtons.createSearchButton();
        this.prevButton = UIComponentFactory.ActionButtons.createPreviousButton();
        this.nextButton = UIComponentFactory.ActionButtons.createNextButton();

        initializeSearchComponents();
    }

    /**
     * Initialize search components and setup listeners
     */
    private void initializeSearchComponents() {
        // Set initial state for navigation buttons
        prevButton.setDisable(true);
        nextButton.setDisable(true);

        // Setup search container using factory
        searchContainer = UIComponentFactory.Layouts.createSearchContainer(
                searchField, exactWordCheckBox, searchButton, prevButton, nextButton
        );

        setupListeners();
    }

    /**
     * Adapts the search controller to different UI structures
     */
    public void adaptToContainerStructure(boolean isAllMantrasUI) {
        this.isAllMantrasUI = isAllMantrasUI;
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

        exactWordCheckBox.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (oldValue != newValue) {
                resetSearchState();
                if (searchField.getText() != null && !searchField.getText().isEmpty()) {
                    searchInContent();
                }
            }
        });

        searchButton.setOnAction(e -> searchInContent());
        prevButton.setOnAction(e -> navigateSearch(-1));
        nextButton.setOnAction(e -> navigateSearch(1));
    }

    /**
     * Gets the search UI container.
     */
    public HBox getSearchContainer() {
        return searchContainer;
    }

    /**
     * Searches through content for the search query.
     */
    public void searchInContent() {
        String query = searchField.getText();
        if (query == null || query.isEmpty() || contentContainer.getChildren().isEmpty()) {
            return;
        }

        // Expand the titled pane if we're searching in mismatched lines
        if (mismatchesTitledPane != null) {
            mismatchesTitledPane.setExpanded(true);
        }

        performSearch(query);

        if (!searchMatches.isEmpty()) {
            navigateSearch(1);
        } else {
            UIUtils.showNoSearchResultsInfo();
        }
    }

    /**
     * Performs the actual search operation
     */
    private void performSearch(String query) {
        unhighlightCurrentMatch();
        searchMatches.clear();

        lastSearchQuery = query;
        currentSearchIndex = -1;

        boolean exactWord = exactWordCheckBox.isSelected();

        for (Node node : contentContainer.getChildren()) {
            if (containsSearch(node, query, exactWord)) {
                searchMatches.add(node);
            }
        }

        updateNavigationButtonState();
    }

    /**
     * Searches through content quietly (without notifications)
     */
    public void searchInContentQuietly() {
        String query = searchField.getText();
        if (query == null || query.isEmpty() || contentContainer.getChildren().isEmpty()) {
            return;
        }

        performSearch(query);

        if (!searchMatches.isEmpty()) {
            navigateSearch(1);
        }
    }

    /**
     * Navigates to the next/previous search match.
     */
    private void navigateSearch(int direction) {
        if (searchMatches.isEmpty()) {
            return;
        }

        unhighlightCurrentMatch();

        currentSearchIndex += direction;
        if (currentSearchIndex < 0) {
            currentSearchIndex = searchMatches.size() - 1;
        } else if (currentSearchIndex >= searchMatches.size()) {
            currentSearchIndex = 0;
        }

        highlightCurrentMatch();
        scrollToCurrentMatch();
        updateNavigationButtonState();
    }

    /**
     * Scrolls to the current search match
     */
    private void scrollToCurrentMatch() {
        if (currentSearchIndex >= 0 && currentSearchIndex < searchMatches.size()) {
            Node currentMatch = searchMatches.get(currentSearchIndex);
            if (actualScrollPane != null) {
                double scrollPosition = (double) contentContainer.getChildren().indexOf(currentMatch) /
                        contentContainer.getChildren().size();
                actualScrollPane.setVvalue(scrollPosition);
            }
        }
    }

    /**
     * Checks if text contains the search query using different strategies.
     */
    private boolean containsSearch(Node node, String query, boolean exactWord) {
        if (isAllMantrasUI) {
            return customContainsSearchForAllMantras(node, query, exactWord);
        } else {
            return standardContainsSearch(node, query, exactWord);
        }
    }

    /**
     * Standard search for mismatched lines and other containers
     */
    private boolean standardContainsSearch(Node node, String query, boolean exactWord) {
        String textToSearch = "";

        if (node instanceof HBox lineContainer) {
            for (Node child : lineContainer.getChildren()) {
                if (child instanceof TextField) {
                    textToSearch = ((TextField) child).getText();
                    break;
                }
            }
        } else if (node instanceof TextField) {
            textToSearch = ((TextField) node).getText();
        }

        if (textToSearch != null && !textToSearch.isEmpty()) {
            return exactWord ? containsExactWord(textToSearch, query) :
                    textToSearch.toLowerCase().contains(query.toLowerCase());
        }

        return false;
    }

    /**
     * Custom search for AllMantrasUI structure
     */
    private boolean customContainsSearchForAllMantras(Node node, String query, boolean exactWord) {
        HBox lineEditor = extractLineEditor(node);
        if (lineEditor == null || lineEditor.getChildren().size() < 2) {
            return false;
        }

        return searchInFirstElement(lineEditor.getChildren().get(0), query, exactWord) ||
               searchInEditableField(lineEditor.getChildren().get(1), query, exactWord);
    }

    private HBox extractLineEditor(Node node) {
        if (node instanceof VBox wrapper) {
            return extractLineEditorFromVBox(wrapper);
        } else if (node instanceof HBox) {
            return (HBox) node;
        }
        return null;
    }

    private HBox extractLineEditorFromVBox(VBox wrapper) {
        if (wrapper.getChildren().isEmpty()) {
            return null;
        }

        Node firstChild = wrapper.getChildren().get(0);
        return firstChild instanceof HBox ? (HBox) firstChild : null;
    }

    private boolean searchInFirstElement(Node firstElement, String query, boolean exactWord) {
        if (firstElement instanceof HBox firstHBox && !firstHBox.getChildren().isEmpty()) {
            return searchInHBoxLabels(firstHBox, query, exactWord);
        } else if (firstElement instanceof Label label) {
            return searchInLabel(label, query, exactWord);
        }
        return false;
    }

    private boolean searchInHBoxLabels(HBox hbox, String query, boolean exactWord) {
        for (Node child : hbox.getChildren()) {
            if (child instanceof Label label && searchInLabel(label, query, exactWord)) {
                return true;
            }
        }
        return false;
    }

    private boolean searchInLabel(Label label, String query, boolean exactWord) {
        String labelText = label.getText();
        return exactWord ? containsExactWord(labelText, query) :
                labelText.toLowerCase().contains(query.toLowerCase());
    }

    private boolean searchInEditableField(Node secondElement, String query, boolean exactWord) {
        if (secondElement instanceof TextField editableField) {
            String fieldText = editableField.getText();
            return exactWord ? containsExactWord(fieldText, query) :
                    fieldText.toLowerCase().contains(query.toLowerCase());
        }
        return false;
    }

    /**
     * Checks if the text contains the exact word, reliably handling accented characters
     */
    public boolean containsExactWord(String text, String word) {
        if (text == null || word == null) return false;

        String normalizedText = normalizeText(text.toLowerCase());
        String normalizedWord = normalizeText(word.toLowerCase());

        String[] words = normalizedText.split("[\\s\\p{Punct}]+");

        for (String w : words) {
            if (w.equals(normalizedWord)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Normalizes text by removing diacritical marks (accents)
     */
    private String normalizeText(String text) {
        if (text == null) return "";
        return java.text.Normalizer.normalize(text, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}", "");
    }

    /**
     * Highlights the current search match
     */
    private void highlightCurrentMatch() {
        if (currentSearchIndex >= 0 && currentSearchIndex < searchMatches.size()) {
            Node node = searchMatches.get(currentSearchIndex);

            if (isAllMantrasUI) {
                highlightAllMantrasNode(node);
            } else {
                highlightStandardNode(node);
            }
        }
    }

    /**
     * Highlights node in AllMantrasUI structure
     */
    private void highlightAllMantrasNode(Node node) {
        setAllMantrasNodeStyle(node, UIColorScheme.getSearchHighlightStyle());
    }

    /**
     * Highlights node in standard structure
     */
    private void highlightStandardNode(Node node) {
        if (node instanceof HBox lineContainer) {
            for (Node child : lineContainer.getChildren()) {
                if (child instanceof TextField textField) {
                    // Use the same highlighting style as AllMantrasUI
                    textField.setStyle(UIColorScheme.getSearchHighlightStyle());
                    break;
                }
            }
        } else if (node instanceof TextField textField) {
            // Use the same highlighting style as AllMantrasUI
            textField.setStyle(UIColorScheme.getSearchHighlightStyle());
        }
    }

    /**
     * Removes highlighting from the current search match.
     */
    private void unhighlightCurrentMatch() {
        if (currentSearchIndex >= 0 && currentSearchIndex < searchMatches.size()) {
            Node node = searchMatches.get(currentSearchIndex);
            unhighlightNode(node);
        }
    }

    /**
     * Removes highlighting from a specific node
     */
    private void unhighlightNode(Node node) {
        if (isAllMantrasUI) {
            unhighlightAllMantrasNode(node);
        } else {
            unhighlightStandardNode(node);
        }
    }

    /**
     * Removes highlighting from AllMantrasUI node
     */
    private void unhighlightAllMantrasNode(Node node) {
        setAllMantrasNodeStyle(node, UIColorScheme.getSearchUnhighlightStyle());
    }

    /**
     * Removes highlighting from standard node structure
     */
    private void unhighlightStandardNode(Node node) {
        if (node instanceof HBox lineContainer) {
            for (Node child : lineContainer.getChildren()) {
                if (child instanceof TextField textField) {
                    // Restore original style - need to determine what the original style should be
                    // For main UI, it's likely just the default style
                    textField.setStyle("");
                    break;
                }
            }
        } else if (node instanceof TextField textField) {
            textField.setStyle("");
        }
    }

    /**
     * Helper method to apply style to TextField in AllMantrasUI node structure.
     * Handles both VBox wrapper and direct HBox patterns.
     */
    private void setAllMantrasNodeStyle(Node node, String style) {
        // Handle VBox wrapper first (from AllMantrasUI.createSearchCompatibleLineEditor)
        HBox lineEditor = null;

        if (node instanceof VBox wrapper) {
            // AllMantrasUI structure: VBox -> HBox
            if (!wrapper.getChildren().isEmpty() && wrapper.getChildren().get(0) instanceof HBox) {
                lineEditor = (HBox) wrapper.getChildren().get(0);
            }
        } else if (node instanceof HBox) {
            // Direct HBox
            lineEditor = (HBox) node;
        }

        if (lineEditor != null && lineEditor.getChildren().size() >= 2) {
            // Apply style to the editable TextField (second element)
            Node secondElement = lineEditor.getChildren().get(1);
            if (secondElement instanceof TextField textField) {
                textField.setStyle(style);
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

            // Remove highlights from all TextFields
            for (Node node : contentContainer.getChildren()) {
                unhighlightNode(node);
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
}