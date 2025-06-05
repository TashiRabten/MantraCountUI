package com.example.mantracount;

import java.util.regex.Pattern;

/**
 * Utility class for classifying different types of content, particularly for identifying
 * instructional/educational content that should be excluded from mantra counting.
 *
 * This class centralizes the logic previously duplicated between AllMantrasUI and MissingFizAnalyzer.
 */
public class ContentClassificationUtils {

    /**
     * Helper method to identify instructional/educational content that should be excluded
     * from mantra counting consideration.
     *
     * @param line The complete line to analyze
     * @return true if the line contains instructional content that should be excluded
     */
    public static boolean isInstructionalContent(String line) {
        String lowerCase = line.toLowerCase();

        // Extract only the message content (editable portion) for analysis
        LineParser.LineSplitResult splitResult = LineParser.splitEditablePortion(line);
        String messageContent = splitResult.getEditableSuffix();

        if (messageContent == null || messageContent.trim().isEmpty()) {
            return false;
        }

        String messageLower = messageContent.toLowerCase();

        // Pattern 1: "Item X -" at the beginning (instructional lists)
        if (messageLower.matches("^\\s*item\\s+\\d+\\s*[-–].*")) {
            return true;
        }

        // Pattern 2: Other instructional/educational patterns
        String[] instructionalPatterns = {
                "^\\s*\\d+\\s*[-–]\\s*o\\s+buddha\\s+praticou",  // "1 - o buddha praticou..."
                "^\\s*\\d+\\s*[-–]\\s*a\\s+",                     // "2 - a meditação..."
                "^\\s*\\d+\\s*[-–]\\s*os\\s+",                    // "3 - os ensinamentos..."
                "^\\s*\\d+\\s*[-–]\\s*quando\\s+",               // "4 - quando o buddha..."
                "^\\s*\\d+\\s*[-–]\\s*depois\\s+",               // "5 - depois que..."
                "^\\s*passo\\s+\\d+",                            // "passo 1", "passo 2"
                "^\\s*etapa\\s+\\d+",                            // "etapa 1", "etapa 2"
                "^\\s*capítulo\\s+\\d+",                         // "capítulo 1"
                "^\\s*lição\\s+\\d+"                             // "lição 1"
        };

        for (String pattern : instructionalPatterns) {
            if (Pattern.compile(pattern, Pattern.CASE_INSENSITIVE)
                    .matcher(messageLower).find()) {
                return true;
            }
        }

        // Pattern 3: Long descriptive text about Buddhist teachings (heuristic)
        // If the message is very long and contains philosophical terms but no action words
        return messageLower.length() > 100 &&
                !ActionWordManager.hasActionWords(line) &&
                containsPhilosophicalTerms(messageLower);
    }

    /**
     * Helper method to detect philosophical/educational content based on presence
     * of multiple Buddhist/philosophical terms.
     *
     * @param text The text to analyze (should be lowercase)
     * @return true if the text contains multiple philosophical terms indicating educational content
     */
    public static boolean containsPhilosophicalTerms(String text) {
        String[] philosophicalTerms = {
                "praticou meditação",
                "completo despertar",
                "amizade que os",
                "meios habilidosos",
                "se tornaram alunos",
                "ensinamentos",
                "sabedoria",
                "compaixão",
                "iluminação",
                "dharma",
                "sangha",
                "bodhisattva",
                "samsara",
                "nirvana"
        };

        int matches = 0;
        for (String term : philosophicalTerms) {
            if (text.contains(term)) {
                matches++;
            }
        }

        // If it contains multiple philosophical terms, it's likely educational content
        return matches >= 2;
    }

    /**
     * Convenience method that checks if a line should be excluded from mantra counting
     * due to being instructional content. This is the main method other classes should use.
     *
     * @param line The line to check
     * @return true if the line should be excluded from counting
     */
    public static boolean shouldExcludeFromCounting(String line) {
        return isInstructionalContent(line);
    }
}