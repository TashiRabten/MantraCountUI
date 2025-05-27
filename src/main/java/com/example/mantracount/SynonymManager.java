package com.example.mantracount;

import java.util.*;

public class SynonymManager {
    private static final Map<String, Set<String>> synonymGroups = new HashMap<>();
    private static final Map<String, String> variantToCanonical = new HashMap<>();

    static {
        // Initialize synonym groups - first parameter is the canonical (correct) form
        addSynonymGroup("tare", "tara");
        addSynonymGroup("vajrasattva", "vajrasatva");
        addSynonymGroup("refúgio", "refugio");
        addSynonymGroup("manjushri", "manjusri", "mañjuśrī");
    }

    private static void addSynonymGroup(String canonical, String... variants) {
        Set<String> group = new HashSet<>();
        String canonicalLower = canonical.toLowerCase();
        group.add(canonicalLower);

        // Map canonical to itself
        variantToCanonical.put(canonicalLower, canonicalLower);

        // Add all variants
        for (String variant : variants) {
            String lowerVariant = variant.toLowerCase();
            group.add(lowerVariant);
            variantToCanonical.put(lowerVariant, canonicalLower);
        }

        synonymGroups.put(canonicalLower, group);
    }

    public static String[] getAllCanonicalKeywords() {
        return synonymGroups.keySet().toArray(new String[0]);
    }
    /**
     * Gets the canonical form of a term (e.g., "tara" -> "tare")
     */
    public static String getCanonicalForm(String term) {
        if (term == null) return null;
        return variantToCanonical.getOrDefault(term.toLowerCase(), term.toLowerCase());
    }

    /**
     * Gets all variants (including canonical) for a term
     */
    public static Set<String> getAllVariants(String term) {
        if (term == null) return Collections.emptySet();
        String canonical = getCanonicalForm(term);
        return synonymGroups.getOrDefault(canonical, Collections.singleton(term.toLowerCase()));
    }
}