package com.tuguitar.todoacorde.scales.data;

import java.util.*;

/**
 * Genera patrones de diapasón a partir de:
 * - Tónica (p.ej. "E" o "A")
 * - Fórmula de grados (p.ej. ["1","b2","b3","4","5","b6","b7"])
 * - Ventana de trastes [start..end] (p.ej. 0..3, 5..8)
 *
 * Salida: List<ScaleFretNote> listo para tu PatternRepository/DAO.
 * Convenciones:
 *  - stringIndex: 0 = 6ª, 5 = 1ª (como en tu código).
 *  - Etiquetas de nota SIEMPRE en SOSTENIDOS (C#, D#, F#, G#, A#).
 *  - Grados: 1→"R", 4→"p4", 5→"p5"; el resto tal cual (b2, b3, 3, b6, b7…).
 */
public final class ScalePatternGenerator {

    // Nombres en sostenidos
    private static final String[] NOTE_SHARP = {
            "C","C#","D","D#","E","F","F#","G","G#","A","A#","B"
    };

    // Afinación estándar EADGBE, 0=6ª … 5=1ª
    private static final String[] OPEN_STRINGS = {"E","A","D","G","B","E"};

    // Desplazamiento en semitonos por grado relativo a la tónica
    private static final Map<String,Integer> DEGREE_TO_SEMITONES = new HashMap<>();
    static {
        DEGREE_TO_SEMITONES.put("1", 0);
        DEGREE_TO_SEMITONES.put("b2", 1);
        DEGREE_TO_SEMITONES.put("2", 2);
        DEGREE_TO_SEMITONES.put("#2", 3);  // = b3
        DEGREE_TO_SEMITONES.put("b3", 3);
        DEGREE_TO_SEMITONES.put("3", 4);
        DEGREE_TO_SEMITONES.put("4", 5);
        DEGREE_TO_SEMITONES.put("#4", 6);  // = b5
        DEGREE_TO_SEMITONES.put("b5", 6);
        DEGREE_TO_SEMITONES.put("5", 7);
        DEGREE_TO_SEMITONES.put("#5", 8);  // = b6
        DEGREE_TO_SEMITONES.put("b6", 8);
        DEGREE_TO_SEMITONES.put("6", 9);
        DEGREE_TO_SEMITONES.put("bb7", 9); // alias raro, por si acaso
        DEGREE_TO_SEMITONES.put("b7", 10);
        DEGREE_TO_SEMITONES.put("7", 11);
    }

    private ScalePatternGenerator() { }

    /** Construye el patrón dentro de una ventana de trastes. */
    public static List<ScaleFretNote> buildPattern(String rootNote,
                                                   List<String> degreeFormula,
                                                   int windowStartFret,
                                                   int windowEndFret) {

        // Normalizamos la raíz a sostenidos para todo el pipeline
        final String rootSharp = toSharp(rootNote);
        Map<String,String> pitchToDegree = buildPitchDegreeMap(rootSharp, degreeFormula);

        List<ScaleFretNote> out = new ArrayList<>();

        for (int stringIndex = 0; stringIndex < 6; stringIndex++) {
            for (int fret = windowStartFret; fret <= windowEndFret; fret++) {
                // nombre de la nota SIEMPRE en sostenidos
                String pitchSharp = noteAtSharp(stringIndex, fret);
                String degree = pitchToDegree.get(pitchSharp);
                if (degree != null) {
                    String label = normalizeDegreeLabel(degree); // "R", "p4", "p5", "b2", etc.
                    boolean isRoot = "R".equals(label) || pitchSharp.equalsIgnoreCase(rootSharp);
                    out.add(new ScaleFretNote(
                            stringIndex,
                            fret,
                            label,
                            isRoot,
                            /*finger*/ null,
                            pitchSharp   // ⇦ guardamos la etiqueta en sostenidos
                    ));
                }
            }
        }
        return out;
    }

    // -------- Helpers ---------------------------------------------------------------------------

    private static int sharpIndex(String note) {
        String n = toSharp(note);
        for (int i = 0; i < NOTE_SHARP.length; i++) {
            if (NOTE_SHARP[i].equalsIgnoreCase(n)) return i;
        }
        throw new IllegalArgumentException("Nota desconocida: " + note);
    }

    /** Convierte posibles bemoles a su enarmónico en sostenidos. */
    private static String toSharp(String s) {
        if (s == null) return "";
        s = s.trim().toUpperCase(Locale.ROOT);
        switch (s) {
            case "DB": return "C#";
            case "EB": return "D#";
            case "GB": return "F#";
            case "AB": return "G#";
            case "BB": return "A#";
            // casos enarmónicos menos comunes:
            case "E#": return "F";
            case "B#": return "C";
            default:   return s;
        }
    }

    /** Nombre de la nota en la cuerda/traste en SOSTENIDOS. */
    private static String noteAtSharp(int stringIndex, int fret) {
        int openIdx = sharpIndex(OPEN_STRINGS[stringIndex]);
        int idx = (openIdx + fret) % 12;
        return NOTE_SHARP[idx];
    }

    /** Mapa pitch (sharp) → degree según la tónica y la fórmula. */
    private static Map<String,String> buildPitchDegreeMap(String rootSharp, List<String> degrees) {
        int rootIdx = sharpIndex(rootSharp);
        Map<String,String> map = new HashMap<>();
        if (degrees == null) return map;

        for (String deg : degrees) {
            Integer semis = DEGREE_TO_SEMITONES.get(deg);
            if (semis == null) continue;
            String sharp = NOTE_SHARP[(rootIdx + semis) % 12];
            map.put(sharp, deg); // clave en sostenidos
        }
        return map;
    }

    /** Ajusta etiquetas a tu convención (R, p4, p5…). */
    private static String normalizeDegreeLabel(String deg) {
        switch (deg) {
            case "1": return "R";
            case "4": return "p4";
            case "5": return "p5";
            default:  return deg; // b2, 2, b3, 3, #4/b5, b6, 6, b7, 7…
        }
    }
}
