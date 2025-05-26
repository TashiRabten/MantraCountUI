package com.example.mantracount;

import java.util.*;

public class SynonymManager {
    private static final Map<String, Set<String>> synonymGroups = new HashMap<>();
    private static final Map<String, String> variantToCanonical = new HashMap<>();

    static {
        // Initialize synonym groups - first parameter is the canonical (correct) form
        addSynonymGroup("tare", "tara");
        addSynonymGroup("vajrasattva", "vajrasatva");
        addSynonymGroup("manjushri", "manjusri, Mañjuśrī");
        
    }

    private static void addSynonymGroup(String canonical, String... variants) {
        Set<String> group = new HashSet<>();
        group.add(canonical.toLowerCase());

        // Map canonical to itself
        variantToCanonical.put(canonical.toLowerCase(), canonical.toLowerCase());

        // Add all variants
        for (String variant : variants) {
            String lowerVariant = variant.toLowerCase();
            group.add(lowerVariant);
            variantToCanonical.put(lowerVariant, canonical.toLowerCase());
        }

        synonymGroups.put(canonical.toLowerCase(), group);
    }

    /**
     * Gets the canonical form of a term (e.g., "tara" -> "tare")
     */
    public static String getCanonicalForm(String term) {
        if (term == null) return null;
        return variantToCanonical.getOrDefault(term.toLowerCase(), term.toLowerCase());
    }

    /**
     * Checks if two terms are synonyms of each other
     */
    public static boolean areSynonyms(String term1, String term2) {
        if (term1 == null || term2 == null) return false;
        String canonical1 = getCanonicalForm(term1);
        String canonical2 = getCanonicalForm(term2);
        return canonical1.equals(canonical2);
    }

    /**
     * Gets all variants (including canonical) for a term
     */
    public static Set<String> getAllVariants(String term) {
        if (term == null) return Collections.emptySet();
        String canonical = getCanonicalForm(term);
        return synonymGroups.getOrDefault(canonical, Collections.singleton(term.toLowerCase()));
    }

    /**
     * Checks if a term is a variant (not canonical) of another term
     */
    public static boolean isVariant(String term, String canonical) {
        if (term == null || canonical == null) return false;
        String termCanonical = getCanonicalForm(term);
        return termCanonical.equals(canonical.toLowerCase()) && !term.toLowerCase().equals(canonical.toLowerCase());
    }
}