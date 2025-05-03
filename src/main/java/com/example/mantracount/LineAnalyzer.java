package com.example.mantracount;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Classe auxiliar para análise avançada de linhas e detecção de problemas
 */
public class LineAnalyzer {

    /**
     * Classe para armazenar informações detalhadas sobre problemas em linhas
     */
    public static class LineIssue {
        private final String line;
        private final String issue;
        private final String suggestion;

        public LineIssue(String line, String issue, String suggestion) {
            this.line = line;
            this.issue = issue;
            this.suggestion = suggestion;
        }

        public String getLine() {
            return line;
        }

        public String getIssue() {
            return issue;
        }

        public String getSuggestion() {
            return suggestion;
        }

        @Override
        public String toString() {
            return "ISSUE: " + issue + "\n" +
                    "LINE: " + line + "\n" +
                    "SUGGESTION: " + suggestion;
        }
    }

    /**
     * Analisa possíveis problemas em linhas relacionadas a uma data específica
     * @param lines Lista de linhas do arquivo
     * @param date Data para buscar problemas relacionados
     * @param mantraKeyword Palavra-chave do mantra
     * @return Lista de problemas encontrados
     */
    public static List<LineIssue> analyzeLines(List<String> lines, LocalDate date, String mantraKeyword) {
        List<LineIssue> issues = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("M/d/yy");
        String dateStr = formatter.format(date);

        // Busca por linhas contendo a data em qualquer formato
        List<String> candidateLines = findLinesWithDate(lines, date);

        // Verificar problemas comuns
        for (String line : candidateLines) {
            // 1. Verifica se a linha contém a data, mas não contém o mantra (possível erro de digitação)
            if (!MantraCount.hasApproximateMatch(line, mantraKeyword)) {
                issues.add(new LineIssue(
                        line,
                        "Date found but no mantra match",
                        "Check if '" + mantraKeyword + "' is misspelled or missing"
                ));
            }

            // 2. Verifica se a linha tem formato incorreto (ex: falta de colchetes, dois-pontos)
            if (!line.contains("[") || !line.contains("]") || !line.contains(":")) {
                issues.add(new LineIssue(
                        line,
                        "Incorrect format",
                        "Line should follow format: [MM/DD/YY, ...]: ..."
                ));
            }

            // 3. Verifica se há números faltantes após o terceiro dois-pontos
            int colonCount = countChars(line, ':');
            if (colonCount >= 3) {
                int lastColonIndex = line.lastIndexOf(':');
                if (lastColonIndex != -1 && lastColonIndex + 1 < line.length()) {
                    String afterLastColon = line.substring(lastColonIndex + 1).trim();
                    if (!afterLastColon.matches(".*\\d+.*")) {
                        issues.add(new LineIssue(
                                line,
                                "Missing number after third colon",
                                "Add number after the last colon"
                        ));
                    }
                }
            }

            // 4. Verifica se há inconsistência entre contagem de "fiz" e "mantra(s)"
            LineParser.LineData lineData = LineParser.parseLine(line, mantraKeyword);
            if (lineData.getFizCount() != lineData.getMantraWordsCount()) {
                issues.add(new LineIssue(
                        line,
                        "Mismatch between 'fiz' (" + lineData.getFizCount() + ") and 'mantra(s)' (" + lineData.getMantraWordsCount() + ")",
                        "Make sure counts match"
                ));
            }
        }

        // 5. Verificar se há possível continuação de linha (problema mencionado pelo usuário)
        checkForSplitLines(lines, candidateLines, issues, date);

        return issues;
    }

    /**
     * Verifica se há linhas divididas/continuadas (onde o mantra pode estar em duas linhas)
     */
    private static void checkForSplitLines(List<String> allLines, List<String> dateLines,
                                           List<LineIssue> issues, LocalDate date) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("M/d/yy");
        String dateStr = formatter.format(date);

        // Procurar por linhas que possam ser continuação de uma linha com data
        for (int i = 0; i < allLines.size() - 1; i++) {
            String currentLine = allLines.get(i);
            String nextLine = allLines.get(i + 1);

            // Se a linha atual contém a data no formato esperado
            if (currentLine.contains(dateStr)) {
                // Verificar se a linha seguinte parece ser uma continuação
                // Sinais de continuação: não tem data própria, não começa com colchete, etc.
                if (!nextLine.contains("[") && !containsDate(nextLine) && !nextLine.trim().isEmpty()) {
                    boolean isAlreadyInDateLines = false;
                    for (String dateLine : dateLines) {
                        if (dateLine.equals(currentLine + " " + nextLine)) {
                            isAlreadyInDateLines = true;
                            break;
                        }
                    }

                    if (!isAlreadyInDateLines) {
                        issues.add(new LineIssue(
                                currentLine + "\n" + nextLine,
                                "Possible split line detected",
                                "Consider merging these lines: " + currentLine + " " + nextLine
                        ));
                    }
                }
            }
        }
    }

    /**
     * Verifica se a linha contém alguma data no formato comum (M/D/YY ou MM/DD/YY)
     */
    private static boolean containsDate(String line) {
        // Expressão regular para detectar datas no formato M/D/YY ou MM/DD/YY
        Pattern datePattern = Pattern.compile("\\b\\d{1,2}/\\d{1,2}/\\d{2}\\b");
        Matcher matcher = datePattern.matcher(line);
        return matcher.find();
    }

    /**
     * Encontra linhas que contêm a data especificada em qualquer formato
     */
    private static List<String> findLinesWithDate(List<String> lines, LocalDate date) {
        List<String> matchingLines = new ArrayList<>();

        // Diferentes formatos possíveis para a data
        DateTimeFormatter formatter1 = DateTimeFormatter.ofPattern("M/d/yy");
        DateTimeFormatter formatter2 = DateTimeFormatter.ofPattern("MM/dd/yy");
        DateTimeFormatter formatter3 = DateTimeFormatter.ofPattern("M/d/yyyy");
        DateTimeFormatter formatter4 = DateTimeFormatter.ofPattern("MM/dd/yyyy");

        String dateStr1 = formatter1.format(date);
        String dateStr2 = formatter2.format(date);
        String dateStr3 = formatter3.format(date);
        String dateStr4 = formatter4.format(date);

        for (String line : lines) {
            if (line.contains(dateStr1) || line.contains(dateStr2) ||
                    line.contains(dateStr3) || line.contains(dateStr4)) {
                matchingLines.add(line);
            }
        }

        return matchingLines;
    }

    /**
     * Conta ocorrências de um caractere em uma string
     */
    private static int countChars(String str, char c) {
        int count = 0;
        for (int i = 0; i < str.length(); i++) {
            if (str.charAt(i) == c) {
                count++;
            }
        }
        return count;
    }
}