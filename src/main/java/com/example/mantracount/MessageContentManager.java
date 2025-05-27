package com.example.mantracount;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Centralized manager for extracting message content from WhatsApp lines.
 * This eliminates duplication between LineAnalyzer and LineParser.
 */
public class MessageContentManager {

    // Pattern for Android WhatsApp format
    private static final Pattern ANDROID_PATTERN = Pattern.compile("^(\\d{1,2}/\\d{1,2}/\\d{2,4})\\s+\\d{1,2}:\\d{1,2}\\s+-\\s+");

    /**
     * Extract just the message content from WhatsApp line, excluding timestamp and name
     * @param line The WhatsApp line to process
     * @return The message content only, or the whole line if no format detected
     */
    public static String extractMessageContent(String line) {
        if (line == null || line.trim().isEmpty()) {
            return line;
        }

        // Handle iPhone WhatsApp format: [date, time] Name: Message
        if (line.startsWith("[")) {
            int closeBracket = line.indexOf(']');
            if (closeBracket > 0) {
                int nameEnd = line.indexOf(':', closeBracket + 1);
                if (nameEnd > 0) {
                    return line.substring(nameEnd + 1).trim();
                }
            }
        }

        // Handle Android WhatsApp format: DD/MM/YYYY HH:MM - Name: Message
        Matcher androidMatcher = ANDROID_PATTERN.matcher(line);
        if (androidMatcher.find()) {
            int androidMatchEnd = androidMatcher.end();
            int nameEnd = line.indexOf(':', androidMatchEnd);
            if (nameEnd > 0) {
                return line.substring(nameEnd + 1).trim();
            }
        }

        // Fallback: try to find first colon that's not part of time notation
        int colonIndex = findFirstNonTimeColon(line);
        if (colonIndex > 0) {
            return line.substring(colonIndex + 1).trim();
        }

        // If no format detected, return the whole line
        return line;
    }

    /**
     * Find first colon that's not part of time notation
     */
    private static int findFirstNonTimeColon(String line) {
        for (int i = 1; i < line.length(); i++) {
            if (line.charAt(i) == ':') {
                // Check if this colon is part of time notation (digit:digit)
                boolean isTimeColon = (i > 0 && Character.isDigit(line.charAt(i - 1))) &&
                        (i < line.length() - 1 && Character.isDigit(line.charAt(i + 1)));

                if (!isTimeColon) {
                    return i;
                }
            }
        }
        return -1;
    }
}