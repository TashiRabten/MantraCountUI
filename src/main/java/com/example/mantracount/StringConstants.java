package com.example.mantracount;

/**
 * Centralized constants for all UI strings, error messages, and bilingual text.
 * Ensures consistency across the application and makes localization easier.
 */
public class StringConstants {

    // Button texts (without emojis - icons handled by ButtonImageUtils)
    public static final String SAVE_CHANGES_PT = "Salvar Alterações";
    public static final String SAVE_CHANGES_EN = "Save Changes - Save any changes made";

    public static final String CLOSE_PT = "Fechar";
    public static final String CLOSE_EN = "Close - Close this window";

    public static final String CANCEL_PT = "Cancelar";
    public static final String CANCEL_EN = "Cancel - Cancel current operation";

    public static final String ANALYZE_PT = "Analisar";
    public static final String ANALYZE_EN = "Analyze - Start the analysis process";

    public static final String APPLY_CHANGES_PT = "Aplicar Alterações";
    public static final String APPLY_CHANGES_EN = "Apply Edits - Save all changes made to the entries";

    public static final String PROCESS_MANTRAS_PT = "Contar Mantras";
    public static final String PROCESS_MANTRAS_EN = "Count Mantras";

    public static final String CLEAR_RESULTS_PT = "Limpar";
    public static final String CLEAR_RESULTS_EN = "Clear Results";

    public static final String MISSING_DAYS_PT = "Dias Faltantes";
    public static final String MISSING_DAYS_EN = "Check Missing Days";

    public static final String ALL_MANTRAS_PT = "Todos os Mantras";
    public static final String ALL_MANTRAS_EN = "View All Mantras";

    public static final String SEM_FIZ_PT = "Sem Fiz";
    public static final String SEM_FIZ_EN = "Missing Fiz Analysis";

    public static final String OPEN_FILE_PT = "Abrir Arquivo";
    public static final String OPEN_FILE_EN = "Open File - Browse and select your journal/diary file";

    public static final String SEARCH_PT = "Buscar";
    public static final String SEARCH_EN = "Search - Execute the search";

    // Navigation buttons
    public static final String PREVIOUS_PT = "Anterior";
    public static final String PREVIOUS_EN = "Previous - Go to previous search result";

    public static final String NEXT_PT = "Próximo";
    public static final String NEXT_EN = "Next - Go to next search result";

    public static final String SHOW_ALL_PT = "Mostrar Todos";
    public static final String SHOW_ALL_EN = "Show All - Display all entries";

    // Common placeholder texts
    public static final String SEARCH_PLACEHOLDER_PT = "Buscar...";
    public static final String SEARCH_TOOLTIP_EN = "Search - Enter text to search within content";

    public static final String EDIT_LINE_PLACEHOLDER_PT = "Editar linha";
    public static final String EDIT_LINE_TOOLTIP_EN = "Editable content - You can modify this text";

    public static final String MANTRA_NAME_PLACEHOLDER_PT = "Nome do Mantra ou Rito";
    public static final String MANTRA_NAME_TOOLTIP_EN = "Mantra or Rite Name - Enter the name of the mantra or ritual you want to count";

    public static final String FILE_PATH_PLACEHOLDER_PT = "Abrir arquivo...";
    public static final String FILE_PATH_TOOLTIP_EN = "Open a file - Click to browse and select your journal/diary file";

    // Error message patterns
    public static final String ERROR_PREFIX = "❌ ";
    public static final String SUCCESS_PREFIX = "✔ ";
    public static final String WARNING_PREFIX = "⚠ ";
    public static final String INFO_PREFIX = "ℹ ";

    // Common validation messages
    public static final String MISSING_DATE_EN = "Date is required";
    public static final String MISSING_DATE_PT = "Data é obrigatória";

    public static final String MISSING_MANTRA_EN = "Mantra name is required";
    public static final String MISSING_MANTRA_PT = "Nome do mantra é obrigatório";

    public static final String MISSING_FILE_EN = "File is required";
    public static final String MISSING_FILE_PT = "Arquivo é obrigatório";

    public static final String INVALID_DATE_EN = "Invalid date format";
    public static final String INVALID_DATE_PT = "Formato de data inválido";

    public static final String INVALID_DATE_RANGE_EN = "End date cannot be before start date";
    public static final String INVALID_DATE_RANGE_PT = "Data final não pode ser anterior à data inicial";

    // File operation messages
    public static final String FILE_SAVED_EN = "Changes saved successfully";
    public static final String FILE_SAVED_PT = "Alterações salvas com sucesso";

    public static final String FILE_LOADED_EN = "File loaded";
    public static final String FILE_LOADED_PT = "Arquivo carregado";

    public static final String FILE_LOAD_ERROR_EN = "Failed to load file";
    public static final String FILE_LOAD_ERROR_PT = "Falha ao carregar arquivo";

    public static final String FILE_SAVE_ERROR_EN = "Failed to save file";
    public static final String FILE_SAVE_ERROR_PT = "Falha ao salvar arquivo";

    public static final String FILE_NOT_FOUND_EN = "File not found";
    public static final String FILE_NOT_FOUND_PT = "Arquivo não encontrado";

    public static final String EXTRACT_ZIP_ERROR_EN = "Failed to extract .zip file";
    public static final String EXTRACT_ZIP_ERROR_PT = "Falha ao extrair arquivo .zip";

    // Analysis results
    public static final String NO_MISMATCHES_EN = "No mismatches found";
    public static final String NO_MISMATCHES_PT = "Nenhuma discrepância encontrada";

    public static final String NO_MISSING_DAYS_EN = "No missing days found";
    public static final String NO_MISSING_DAYS_PT = "Nenhum salto de dia encontrado";

    public static final String NO_MISSING_FIZ_EN = "No missing fiz lines found";
    public static final String NO_MISSING_FIZ_PT = "Nenhuma linha sem 'fiz' encontrada";

    public static final String NO_CHANGES_EN = "No changes to save";
    public static final String NO_CHANGES_PT = "Nenhuma alteração para salvar";

    public static final String CHANGES_REVERTED_EN = "Changes reverted";
    public static final String CHANGES_REVERTED_PT = "Alterações revertidas";

    public static final String NO_SEARCH_RESULTS_EN = "No matches found";
    public static final String NO_SEARCH_RESULTS_PT = "Nenhuma correspondência encontrada";

    public static final String PROCESSING_EN = "Processing...";
    public static final String PROCESSING_PT = "Processando...";

    // Tooltip texts for common UI elements
    public static final String PROTECTED_CONTENT_TOOLTIP = "Protected content - Date, time and sender (cannot be edited)";
    public static final String EDITABLE_CONTENT_TOOLTIP = "Editable content - You can modify this text";
    public static final String PROGRESS_TOOLTIP = "Processing - Please wait while the operation completes";
    public static final String REMOVE_TOOLTIP = "Remove - Remove this line (can be undone)";
    public static final String UNDO_TOOLTIP = "Undo Last Removal - Restore the last entry that was removed";
    public static final String EXACT_WORD_TOOLTIP = "Exact word - Check to search for exact word matches only";

    // Window titles
    public static final String MISSING_DAYS_TITLE = "Análise de Saltos de Dias";
    public static final String MISSING_FIZ_TITLE = "Análise 'Sem Fiz' - Linhas sem Palavra de Ação";
    public static final String ALL_MANTRAS_TITLE = "Todos os Mantras do Período";
    public static final String UPDATE_AVAILABLE_TITLE = "Update Available / Atualização Disponível";

    // Status messages
    public static final String READY_EN = "Ready";
    public static final String READY_PT = "Pronto";

    public static final String LOADING_EN = "Loading mantras to see summary";
    public static final String LOADING_PT = "Carregue os mantras para ver o resumo";

    public static final String NO_ANALYSIS_EN = "No analysis performed yet";
    public static final String NO_ANALYSIS_PT = "Nenhuma análise realizada ainda";

    public static final String SELECT_END_DATE_EN = "Select end date and click Load";
    public static final String SELECT_END_DATE_PT = "Selecione data final e clique em Carregar";

    // Labels and headers (without emojis)
    public static final String START_DATE_PT = "Data Inicial";
    public static final String START_DATE_EN = "Start Date - Select the date from which to start counting mantras";

    public static final String END_DATE_PT = "Data Final";
    public static final String END_DATE_EN = "End Date - Select the final date for the period";

    public static final String MANTRA_COUNT_RESULT_PT = "Contagem de Mantras";
    public static final String MISMATCH_LINES_PT = "Discrepância de linhas";
    public static final String MISMATCH_LINES_EN = "Mismatch Lines - Click to expand/collapse. Shows lines requiring attention or confirmation.";

    public static final String LINES_REQUIRING_ATTENTION_PT = "Linhas Requerendo Atenção";
    public static final String NO_MISMATCH_LINES_PT = "Não há discrepância de linhas";

    public static final String FOUND_LINES_PT = "Linhas encontradas";
    public static final String MISSING_DAY_PT = "Faltante:";

    // Update messages
    public static final String UPDATE_LABEL_PT = "Atualizar";
    public static final String UPDATE_TOOLTIP_EN = "Update - Check for application updates";

    public static final String UPDATE_SUCCESS_EN = "App is up-to-date";
    public static final String UPDATE_SUCCESS_PT = "Aplicativo está atualizado";

    public static final String UPDATE_FAILED_EN = "Connection to Update Failed";
    public static final String UPDATE_FAILED_PT = "Conexão de Atualização Falhou";

    /**
     * Creates bilingual error message with standard format
     */
    public static String createBilingualError(String englishMessage, String portugueseMessage) {
        return ERROR_PREFIX + englishMessage + "\n" + ERROR_PREFIX + portugueseMessage;
    }

    /**
     * Creates bilingual success message with standard format
     */
    public static String createBilingualSuccess(String englishMessage, String portugueseMessage) {
        return SUCCESS_PREFIX + englishMessage + "\n" + SUCCESS_PREFIX + portugueseMessage;
    }

    /**
     * Creates bilingual warning message with standard format
     */
    public static String createBilingualWarning(String englishMessage, String portugueseMessage) {
        return WARNING_PREFIX + englishMessage + "\n" + WARNING_PREFIX + portugueseMessage;
    }

    /**
     * Creates bilingual info message with standard format
     */
    public static String createBilingualInfo(String englishMessage, String portugueseMessage) {
        return INFO_PREFIX + englishMessage + "\n" + INFO_PREFIX + portugueseMessage;
    }

    /**
     * Common validation error messages
     */
    public static class ValidationMessages {
        public static final String MISSING_DATE = createBilingualError(MISSING_DATE_EN, MISSING_DATE_PT);
        public static final String MISSING_MANTRA = createBilingualError(MISSING_MANTRA_EN, MISSING_MANTRA_PT);
        public static final String MISSING_FILE = createBilingualError(MISSING_FILE_EN, MISSING_FILE_PT);
        public static final String INVALID_DATE = createBilingualError(INVALID_DATE_EN, INVALID_DATE_PT);
        public static final String INVALID_DATE_RANGE = createBilingualError(INVALID_DATE_RANGE_EN, INVALID_DATE_RANGE_PT);
    }

    /**
     * Common success messages
     */
    public static class SuccessMessages {
        public static final String FILE_SAVED = createBilingualSuccess(FILE_SAVED_EN, FILE_SAVED_PT);
        public static final String FILE_LOADED = createBilingualSuccess(FILE_LOADED_EN, FILE_LOADED_PT);
        public static final String NO_MISMATCHES = createBilingualSuccess(NO_MISMATCHES_EN, NO_MISMATCHES_PT);
        public static final String NO_MISSING_DAYS = createBilingualSuccess(NO_MISSING_DAYS_EN, NO_MISSING_DAYS_PT);
        public static final String NO_MISSING_FIZ = createBilingualSuccess(NO_MISSING_FIZ_EN, NO_MISSING_FIZ_PT);
        public static final String CHANGES_REVERTED = createBilingualSuccess(CHANGES_REVERTED_EN, CHANGES_REVERTED_PT);
        public static final String UPDATE_SUCCESS = createBilingualSuccess(UPDATE_SUCCESS_EN, UPDATE_SUCCESS_PT);
    }

    /**
     * Common error messages
     */
    public static class ErrorMessages {
        public static final String FILE_LOAD_ERROR = createBilingualError(FILE_LOAD_ERROR_EN, FILE_LOAD_ERROR_PT);
        public static final String FILE_SAVE_ERROR = createBilingualError(FILE_SAVE_ERROR_EN, FILE_SAVE_ERROR_PT);
        public static final String FILE_NOT_FOUND = createBilingualError(FILE_NOT_FOUND_EN, FILE_NOT_FOUND_PT);
        public static final String EXTRACT_ZIP_ERROR = createBilingualError(EXTRACT_ZIP_ERROR_EN, EXTRACT_ZIP_ERROR_PT);
        public static final String NO_CHANGES = createBilingualError(NO_CHANGES_EN, NO_CHANGES_PT);
        public static final String NO_SEARCH_RESULTS = createBilingualError(NO_SEARCH_RESULTS_EN, NO_SEARCH_RESULTS_PT);
        public static final String UPDATE_FAILED = createBilingualError(UPDATE_FAILED_EN, UPDATE_FAILED_PT);
    }
}