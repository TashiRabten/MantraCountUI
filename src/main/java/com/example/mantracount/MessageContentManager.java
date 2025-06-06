package com.example.mantracount;

/**
 * Centralized manager for extracting message content from WhatsApp lines.
 * This eliminates duplication between LineAnalyzer and LineParser.
 */
public class MessageContentManager {

    /**
     * Extract just the message content from WhatsApp line, excluding timestamp and name
     * @param line The WhatsApp line to process
     * @return The message content only, or the whole line if no format detected
     */
    public static String extractMessageContent(String line) {
        return ParsingUtils.extractMessageContent(line);
    }
}