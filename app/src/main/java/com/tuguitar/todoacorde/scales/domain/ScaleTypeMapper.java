package com.tuguitar.todoacorde.scales.domain;

import java.util.HashMap;
import java.util.Locale;

/** Utilidades puras de mapeo EN↔ES y normalización de nombres de escala. */
public final class ScaleTypeMapper {

    private ScaleTypeMapper() {}

    /** EN → ES (nombre DB), siempre la forma que usa el seeder. */
    public static String mapEnglishTypeToDbName(String englishRaw) {
        if (englishRaw == null) return "";
        String english = normalizeEnglishTypeAlias(englishRaw);
        String e = english.trim().toLowerCase(Locale.ROOT);
        HashMap<String,String> m = new HashMap<>();

        // Pentatónicas / Blues
        m.put("major pentatonic", "Pentatónica mayor");
        m.put("minor pentatonic", "Pentatónica menor");
        m.put("blues",            "Blues");

        // Modos mayores/menores
        m.put("ionian",    "Mayor");
        m.put("aeolian",   "Menor natural");
        m.put("dorian",    "Dórica");
        m.put("mixolydian","Mixolidia");
        m.put("lydian",    "Lidia");
        m.put("phrygian",  "Frigia");
        m.put("locrian",   "Locria");

        // Flamenco / exóticos
        m.put("phrygian dominant", "Frigia dominante");
        m.put("double harmonic major", "Doble armónica mayor");
        m.put("spanish 8-tone", "Española 8 tonos");

        // Menores compuestas
        m.put("harmonic minor", "Menor armónica");
        m.put("melodic minor (asc)", "Menor melódica");

        String db = m.get(e);
        return db != null ? db : english;
    }

    /** ES → EN canónico (para PatternRepository). */
    public static String mapDbNameToEnglishType(String esRaw) {
        if (esRaw == null) return "";
        String s = esRaw.trim().toLowerCase(Locale.ROOT);
        HashMap<String,String> inv = new HashMap<>();

        // Pentatónicas / Blues
        inv.put("pentatónica mayor", "Major Pentatonic");
        inv.put("pentatónica menor", "Minor Pentatonic");
        inv.put("blues",             "Blues");

        // Modos
        inv.put("mayor",          "Ionian");
        inv.put("menor natural",  "Aeolian");
        inv.put("dórica",         "Dorian");
        inv.put("mixolidia",      "Mixolydian");
        inv.put("lidia",          "Lydian");
        inv.put("frigia",         "Phrygian");
        inv.put("locria",         "Locrian");

        // Flamenco / exóticos
        inv.put("frigia dominante",             "Phrygian Dominant");
        inv.put("modo flamenco (mayor-frigio)", "Phrygian Dominant"); // alias
        inv.put("doble armónica mayor",         "Double Harmonic Major");
        inv.put("española 8 tonos",             "Spanish 8-Tone");

        // Menores compuestas
        inv.put("menor armónica",  "Harmonic Minor");
        inv.put("menor melódica",  "Melodic Minor (Asc)");

        String en = inv.get(s);
        return normalizeEnglishTypeAlias(en != null ? en : esRaw);
    }

    /** Normaliza alias/typos ingleses a canónicos. */
    public static String normalizeEnglishTypeAlias(String st) {
        if (st == null) return "";
        String k = st.trim().toLowerCase(Locale.ROOT);

        // Typos comunes
        if (k.equals("ionion")) return "Ionian";

        // Flamenco / Frigio mayor
        if (k.equals("flamenco mode (major-phrygian)")
                || k.equals("flamenco mode")
                || k.equals("major-phrygian")
                || k.equals("major phrygian")
                || k.equals("spanish phrygian")
                || k.equals("phrygian dominant (flamenco)")) {
            return "Phrygian Dominant";
        }

        // Pentatónicas
        if (k.equals("pentatonic major")) return "Major Pentatonic";
        if (k.equals("pentatonic minor")) return "Minor Pentatonic";

        // Spanish 8-Tone
        if (k.equals("spanish 8 tone")) return "Spanish 8-Tone";

        // Melodic minor
        if (k.equals("melodic minor (asc)") || k.equals("melodic minor asc") || k.equals("melodic minor")) {
            return "Melodic Minor (Asc)";
        }

        // Capitaliza primera letra por defecto (fallback)
        return st.substring(0,1).toUpperCase(Locale.ROOT) + st.substring(1);
    }
}
