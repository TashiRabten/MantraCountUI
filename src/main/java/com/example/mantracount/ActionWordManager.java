package com.example.mantracount;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Centralized manager for all action word detection and counting.
 * This eliminates duplication across LineAnalyzer, LineParser, and MissingFizAnalyzer.
 */
public class ActionWordManager {

    // SINGLE SOURCE OF TRUTH for action words
    private static final String[] ACTION_WORDS = {
            "fiz", "recitei", "completei", "feitos", "feito",
            "completo", "completos", "pratiquei",
            "realizei", "terminei", "acabei", "entrego", "entreguei", "entregues"
    };
    /**
     * Check if a line contains specifically the "fiz" word (not other action words)
     * @param line The line to check
     * @return true if "fiz" is found
     */
    public static boolean hasFizWord(String line) {
        String lineLower = line.toLowerCase();
        Pattern pattern = Pattern.compile("\\b(fiz)\\b", Pattern.CASE_INSENSITIVE);
        boolean found = pattern.matcher(lineLower).find();
        return found;
    }
    /**
     * Check if a line contains any action words
     * @param line The line to check
     * @return true if any action words are found
     */
    public static boolean hasActionWords(String line) {
        String lineLower = line.toLowerCase();

        for (String action : ACTION_WORDS) {
            Pattern pattern = Pattern.compile("\\b" + Pattern.quote(action) + "\\b", Pattern.CASE_INSENSITIVE);
            if (pattern.matcher(lineLower).find()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Count total number of action words in a line
     * @param line The line to analyze
     * @return Total count of action words found
     */
    public static int countActionWords(String line) {
        String lineLower = line.toLowerCase();
        int totalCount = 0;

        for (String action : ACTION_WORDS) {
            Pattern pattern = Pattern.compile("\\b" + Pattern.quote(action) + "\\b", Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(lineLower);
            while (matcher.find()) {
                totalCount++;
            }
        }

        return totalCount;
    }

    /**
     * Get the action words array (for external use if needed)
     * @return Copy of the action words array
     */
    public static String[] getActionWords() {
        return ACTION_WORDS.clone();
    }

    /**
     * Check if a specific word is an action word
     * @param word The word to check
     * @return true if the word is in the action words list
     */
    public static boolean isActionWord(String word) {
        String wordLower = word.toLowerCase();
        for (String action : ACTION_WORDS) {
            if (action.equals(wordLower)) {
                return true;
            }
        }
        return false;
    }
}