package com.example.mantracount;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Classe responsável por detectar dias faltantes na sequência de registros de mantras.
 */
public class MissingDaysDetector {

    /**
     * Classe interna para armazenar informações sobre um dia faltante
     */
    public static class MissingDayInfo {
        private final LocalDate date;
        private final LocalDate previousDate;
        private final LocalDate nextDate;

        public MissingDayInfo(LocalDate date, LocalDate previousDate, LocalDate nextDate) {
            this.date = date;
            this.previousDate = previousDate;
            this.nextDate = nextDate;
        }

        public LocalDate getDate() {
            return date;
        }

        public LocalDate getPreviousDate() {
            return previousDate;
        }

        public LocalDate getNextDate() {
            return nextDate;
        }

        @Override
        public String toString() {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd/yy");
            return formatter.format(date) + " (between " +
                    formatter.format(previousDate) + " and " +
                    formatter.format(nextDate) + ")";
        }
    }

    /**
     * Detecta dias faltantes na sequência de registros entre a data inicial e final
     * @param lines As linhas do arquivo a serem analisadas
     * @param startDate A data inicial para verificação
     * @param mantraKeyword A palavra-chave do mantra para filtrar linhas relevantes
     * @return Uma lista de informações sobre dias faltantes
     */
    public static List<MissingDayInfo> detectMissingDays(List<String> lines, LocalDate startDate, String mantraKeyword) {
        // Extrair todas as datas de linhas válidas do arquivo
        Set<LocalDate> existingDates = new HashSet<>();


        for (String line : lines) {
            if (line.trim().isEmpty()) continue;

            LineParser.LineData lineData = LineParser.parseLine(line, mantraKeyword);

            System.out.println("LINE: " + line);
            System.out.println(" → Parsed Date: " + lineData.getDate());
            System.out.println(" → Mantra Count: " + lineData.getMantraKeywordCount());

            if (lineData.getDate() != null && lineData.getMantraKeywordCount() > 0) {
                existingDates.add(lineData.getDate());
            }
        }

        // Processar cada linha para extrair datas
//        for (String line : lines) {
//            if (line.trim().isEmpty()) continue;
//
//            // Usar o LineParser para extrair os dados da linha
//            LineParser.LineData lineData = LineParser.parseLine(line, mantraKeyword);
//
//            // Só considera linhas que tenham data e que contenham o mantra especificado
//            if (lineData.getDate() != null && lineData.getMantraKeywordCount() > 0) {
//                existingDates.add(lineData.getDate());
//            }
//        }

        // Encontrar a data mais recente para definir o período de busca
        LocalDate endDate = existingDates.stream()
                .max(LocalDate::compareTo)
                .orElse(LocalDate.now());

        // Se não houver datas válidas, retornar lista vazia
        if (existingDates.isEmpty()) {
            return new ArrayList<>();
        }

        // Ordenar as datas existentes para facilitar a detecção de lacunas
        List<LocalDate> sortedDates = new ArrayList<>(existingDates);
        Collections.sort(sortedDates);

        // Se a data inicial for anterior à primeira data encontrada, ajustar para a primeira
        if (startDate.isBefore(sortedDates.get(0))) {
            startDate = sortedDates.get(0);
        }

        // Criar um mapa de data -> data anterior e próxima data
        Map<LocalDate, LocalDate> previousDateMap = new HashMap<>();
        Map<LocalDate, LocalDate> nextDateMap = new HashMap<>();

        for (int i = 0; i < sortedDates.size(); i++) {
            if (i > 0) {
                previousDateMap.put(sortedDates.get(i), sortedDates.get(i-1));
            }
            if (i < sortedDates.size() - 1) {
                nextDateMap.put(sortedDates.get(i), sortedDates.get(i+1));
            }
        }

        // Encontrar dias faltantes
        List<MissingDayInfo> missingDays = new ArrayList<>();
        LocalDate currentDate = startDate;

        while (!currentDate.isAfter(endDate)) {
            // Se a data atual não estiver na lista de datas existentes
            if (!existingDates.contains(currentDate)) {
                // Encontrar a data anterior mais próxima
                LocalDate previousDate = findClosestPreviousDate(sortedDates, currentDate);
                // Encontrar a próxima data mais próxima
                LocalDate nextDate = findClosestNextDate(sortedDates, currentDate);

                if (previousDate != null && nextDate != null) {
                    missingDays.add(new MissingDayInfo(currentDate, previousDate, nextDate));
                }
            }

            // Avançar para o próximo dia
            currentDate = currentDate.plusDays(1);
        }

        return missingDays;
    }

    /**
     * Encontra a data anterior mais próxima da data alvo
     */
    private static LocalDate findClosestPreviousDate(List<LocalDate> sortedDates, LocalDate targetDate) {
        LocalDate result = null;
        for (LocalDate date : sortedDates) {
            if (date.isBefore(targetDate)) {
                result = date;
            } else {
                break;
            }
        }
        return result;
    }

    /**
     * Encontra a próxima data mais próxima da data alvo
     */
    private static LocalDate findClosestNextDate(List<LocalDate> sortedDates, LocalDate targetDate) {
        for (LocalDate date : sortedDates) {
            if (date.isAfter(targetDate)) {
                return date;
            }
        }
        return null;
    }

    /**
     * Procura por possíveis causas do dia faltante, verificando se há linhas com a data
     * mas que podem ter problemas de formatação ou registro incorreto.
     * @param lines As linhas do arquivo
     * @param missingDate A data faltante
     * @return Uma lista de linhas suspeitas que podem estar relacionadas ao dia faltante
     */
    public static List<String> findPotentialIssues(List<String> lines, LocalDate missingDate) {
        List<String> potentialIssues = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("M/d/yy");
        String dateStr = formatter.format(missingDate);

        // Remover o zero inicial do mês e dia, já que podem aparecer em diferentes formatos
        String simplifiedDateStr = dateStr.replaceAll("^0", "").replaceAll("/0", "/");

        for (String line : lines) {
            // Verifica se a linha contém a data em algum formato
            if (line.contains(dateStr) || line.contains(simplifiedDateStr)) {
                potentialIssues.add(line);
                continue;
            }

            // Verificar possíveis variações da data (M/d/yyyy)
            String yearExtendedDateStr = dateStr.substring(0, dateStr.lastIndexOf("/") + 1) + "20" + dateStr.substring(dateStr.lastIndexOf("/") + 1);
            if (line.contains(yearExtendedDateStr)) {
                potentialIssues.add(line);
                continue;
            }

            // Verificar se há datas próximas (±1 dia) no caso de erros de digitação
            LocalDate oneDayBefore = missingDate.minusDays(1);
            LocalDate oneDayAfter = missingDate.plusDays(1);

            String dayBeforeStr = formatter.format(oneDayBefore);
            String dayAfterStr = formatter.format(oneDayAfter);

            if (line.contains(dayBeforeStr) || line.contains(dayAfterStr)) {
                // Verificar se esta linha já tem uma data válida mas pode ter outro problema
                if (!potentialIssues.contains(line)) {
                    potentialIssues.add(line);
                }
            }
        }

        return potentialIssues;
    }

}