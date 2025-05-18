package com.example.mantracount;

import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


/**
 * Shared utility class for line editing functionality.
 * Can be used by both AllMantrasUI and mismatched lines feature.
 */
public class LineEditingUtils {

    /**
     * Updates file content with edited lines.
     *
     * @param mantraData The data model
     * @param originalToUpdated Map of original lines to updated lines
     * @return Number of lines updated
     */
    public static int updateFileContent(MantraData mantraData, Map<String, String> originalToUpdated) {
        int updateCount = 0;
        List<String> originalLines = mantraData.getLines();
        List<String> updatedLines = new ArrayList<>(originalLines);

        for (Map.Entry<String, String> entry : originalToUpdated.entrySet()) {
            String originalLine = entry.getKey();
            String updatedLine = entry.getValue();
            for (int i = 0; i < originalLines.size(); i++) {
                if (originalLines.get(i).equals(originalLine)) {
                    updatedLines.set(i, updatedLine);
                    updateCount++;
                }
            }
        }

        mantraData.setLines(updatedLines);
        return updateCount;
    }


    public static boolean saveChangesToFile(MantraData mantraData, Map<String, String> originalToUpdated) {
        try {
            int updateCount = updateFileContent(mantraData, originalToUpdated);

            // Save to file
            FileEditSaver.saveToFile(mantraData.getLines(), mantraData.getFilePath());

            // Handle zip files
            if (mantraData.isFromZip()) {
                FileEditSaver.updateZipFile(
                        mantraData.getOriginalZipPath(),
                        mantraData.getFilePath(),
                        mantraData.getLines(),
                        mantraData.getOriginalZipEntryName()
                );
            }

            UIUtils.showInfo("✔ " + updateCount + " line(s) updated. / " + updateCount + " linha(s) atualizada(s).");

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            UIUtils.showError("❌ Error saving changes / Erro ao salvar alterações", e.getMessage());
            return false;
        }
    }

    /**
     * Splits a line into fixed prefix and editable suffix.
     *
     * @param line The line to split
     * @return Array containing [fixed prefix, editable suffix]
     */
    public static String[] splitLineForEditing(String line) {
        int closeBracket = line.indexOf(']');
        int colon = line.indexOf(':', closeBracket);

        if (closeBracket != -1 && colon != -1) {
            String prefix = line.substring(0, colon + 1);
            String suffix = (colon + 1 < line.length()) ? line.substring(colon + 1) : "";
            return new String[] { prefix, suffix };
        } else {
            // If can't split properly, make entire line editable
            return new String[] { "", line };
        }
    }

    /**
     * Determines if a line has been modified.
     *
     * @param original Original line
     * @param current Current line
     * @return true if modified, false otherwise
     */
    public static boolean isLineModified(String original, String current) {
        return !original.equals(current);
    }

    /**
     * Creates a filter predicate for searching text in a collection of lines.
     *
     * @param searchText The text to search for
     * @return A functional predicate to use in stream filters
     */
    public static java.util.function.Predicate<String> createSearchPredicate(String searchText) {
        String lowerCaseSearch = searchText.toLowerCase();
        return line -> line.toLowerCase().contains(lowerCaseSearch);
    }

    public static SearchState searchInContainer(String searchText, VBox container,
                                                ScrollPane scrollPane,
                                                Button prevButton, Button nextButton) {
        SearchState state = new SearchState();

        // Skip if empty search
        if (searchText == null || searchText.trim().isEmpty()) {
            return state;
        }

        searchText = searchText.toLowerCase().trim();

        // Clear previous highlights and search matches
        if (state.currentSearchIndex >= 0 && state.currentSearchIndex < state.searchMatches.size()) {
            unhighlightNode(state.searchMatches.get(state.currentSearchIndex));
        }
        state.searchMatches.clear();
        state.currentSearchIndex = -1;

        // Find all matches
        for (Node node : container.getChildren()) {
            if (node instanceof HBox lineContainer) {
                if (containsSearchText(lineContainer, searchText)) {
                    state.searchMatches.add(lineContainer);
                }
            }
        }

        // Update navigation buttons
        boolean hasMatches = !state.searchMatches.isEmpty();
        prevButton.setDisable(!hasMatches);
        nextButton.setDisable(!hasMatches);

        // Navigate to first match if any
        if (hasMatches) {
            navigateSearch(state, 1, scrollPane, container);
        }

        return state;
    }

    /**
     * Navigate between search results.
     */
    public static void navigateSearch(SearchState state, int direction,
                                      ScrollPane scrollPane, VBox container) {
        if (state.searchMatches.isEmpty()) {
            return;
        }

        // Unhighlight current match
        if (state.currentSearchIndex >= 0 && state.currentSearchIndex < state.searchMatches.size()) {
            unhighlightNode(state.searchMatches.get(state.currentSearchIndex));
        }

        // Calculate new index
        state.currentSearchIndex += direction;
        if (state.currentSearchIndex < 0) {
            state.currentSearchIndex = state.searchMatches.size() - 1;
        } else if (state.currentSearchIndex >= state.searchMatches.size()) {
            state.currentSearchIndex = 0;
        }

        // Highlight new match
        highlightNode(state.searchMatches.get(state.currentSearchIndex));

        // Scroll to match
        Node currentMatch = state.searchMatches.get(state.currentSearchIndex);
        double scrollY = currentMatch.getBoundsInParent().getMinY() / container.getHeight();
        scrollPane.setVvalue(scrollY);
    }

    /**
     * Check if a node contains search text.
     */
    private static boolean containsSearchText(HBox lineContainer, String searchText) {
        for (Node child : lineContainer.getChildren()) {
            if (child instanceof Label label) {
                if (label.getText().toLowerCase().contains(searchText)) {
                    return true;
                }
            } else if (child instanceof TextField field) {
                if (field.getText().toLowerCase().contains(searchText)) {
                    return true;
                }
            } else if (child instanceof HBox editablePart) {
                for (Node editNode : editablePart.getChildren()) {
                    if (editNode instanceof TextField editField) {
                        if (editField.getText().toLowerCase().contains(searchText)) {
                            return true;
                        }
                    } else if (editNode instanceof Label label) {
                        if (label.getText().toLowerCase().contains(searchText)) {
                            return true;
                        }
                    }
                }
            } else if (child instanceof VBox vbox) {
                for (Node vChild : vbox.getChildren()) {
                    if (vChild instanceof Label label) {
                        if (label.getText().toLowerCase().contains(searchText)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    /**
     * Highlight a node.
     */
    private static void highlightNode(Node node) {
        node.setStyle("-fx-background-color: #FFFF99;");
    }

    /**
     * Unhighlight a node.
     */
    private static void unhighlightNode(Node node) {
        node.setStyle("");
    }

    /**
     * Class to hold search state.
     */
    public static class SearchState {
        private final List<Node> searchMatches = new ArrayList<>();
        private int currentSearchIndex = -1;

        public List<Node> getSearchMatches() {
            return searchMatches;
        }

        public int getCurrentSearchIndex() {
            return currentSearchIndex;
        }

        public void setCurrentSearchIndex(int index) {
            this.currentSearchIndex = index;
        }

        public boolean hasMatches() {
            return !searchMatches.isEmpty();
        }
    }
}