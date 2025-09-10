package com.todoacorde.todoacorde.scales.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Generador de patrones de escala sobre diapasón de guitarra en afinación estándar.
 *
 * Convenciones:
 * - Notación de notas en sostenidos (C, C#, D, ... B).
 * - Se parte de una fórmula de grados (ej.: 1, 2, b3, 4, 5, 6, b7) relativa a la tónica.
 * - La ventana de trastes se define con start/end inclusive.
 *
 * Salida:
 * - Lista de {@link ScaleFretNote} con índice de cuerda (0 = 6ª/E grave),
 *   traste, etiqueta de grado normalizada y nota en notación de sostenidos.
 */
public final class ScalePatternGenerator {

    /** Notas cromáticas en notación de sostenidos. */
    private static final String[] NOTE_SHARP = {
            "C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"
    };

    /** Afinación estándar de guitarra (6ª a 1ª): E A D G B E. */
    private static final String[] OPEN_STRINGS = {"E", "A", "D", "G", "B", "E"};

    /**
     * Mapa de grados a desplazamiento en semitonos desde la tónica.
     * Se incluyen alias equivalentes (#4 ~ b5, #2 ~ b3, #5 ~ b6, bb7 ~ 6).
     */
    private static final Map<String, Integer> DEGREE_TO_SEMITONES = new HashMap<>();

    static {
        DEGREE_TO_SEMITONES.put("1", 0);
        DEGREE_TO_SEMITONES.put("b2", 1);
        DEGREE_TO_SEMITONES.put("2", 2);
        DEGREE_TO_SEMITONES.put("#2", 3);
        DEGREE_TO_SEMITONES.put("b3", 3);
        DEGREE_TO_SEMITONES.put("3", 4);
        DEGREE_TO_SEMITONES.put("4", 5);
        DEGREE_TO_SEMITONES.put("#4", 6);
        DEGREE_TO_SEMITONES.put("b5", 6);
        DEGREE_TO_SEMITONES.put("5", 7);
        DEGREE_TO_SEMITONES.put("#5", 8);
        DEGREE_TO_SEMITONES.put("b6", 8);
        DEGREE_TO_SEMITONES.put("6", 9);
        DEGREE_TO_SEMITONES.put("bb7", 9);
        DEGREE_TO_SEMITONES.put("b7", 10);
        DEGREE_TO_SEMITONES.put("7", 11);
    }

    private ScalePatternGenerator() {
        /* No instanciable */
    }

    /**
     * Construye un patrón de escala dentro de una ventana de trastes.
     *
     * @param rootNote        nota raíz (se normaliza a sostenidos; admite bemoles)
     * @param degreeFormula   fórmula de grados relativa a la tónica (ej. [ "1","2","b3","4","5","6","b7" ])
     * @param windowStartFret traste inicial (inclusive)
     * @param windowEndFret   traste final (inclusive)
     * @return lista de notas del patrón en el diapasón
     */
    public static List<ScaleFretNote> buildPattern(String rootNote,
                                                   List<String> degreeFormula,
                                                   int windowStartFret,
                                                   int windowEndFret) {
        final String rootSharp = toSharp(rootNote);
        Map<String, String> pitchToDegree = buildPitchDegreeMap(rootSharp, degreeFormula);

        List<ScaleFretNote> out = new ArrayList<>();

        for (int stringIndex = 0; stringIndex < 6; stringIndex++) {
            for (int fret = windowStartFret; fret <= windowEndFret; fret++) {
                String pitchSharp = noteAtSharp(stringIndex, fret);
                String degree = pitchToDegree.get(pitchSharp);
                if (degree != null) {
                    String label = normalizeDegreeLabel(degree);
                    boolean isRoot = "R".equals(label) || pitchSharp.equalsIgnoreCase(rootSharp);
                    out.add(new ScaleFretNote(
                            stringIndex,
                            fret,
                            label,
                            isRoot,
                            null,
                            pitchSharp
                    ));
                }
            }
        }
        return out;
    }

    /**
     * Devuelve el índice (0..11) de la nota en notación de sostenidos.
     */
    private static int sharpIndex(String note) {
        String n = toSharp(note);
        for (int i = 0; i < NOTE_SHARP.length; i++) {
            if (NOTE_SHARP[i].equalsIgnoreCase(n)) return i;
        }
        throw new IllegalArgumentException("Nota desconocida: " + note);
    }

    /**
     * Normaliza una nota a notación con sostenidos.
     * Acepta alias comunes de bemoles y casos especiales E#/B#.
     */
    private static String toSharp(String s) {
        if (s == null) return "";
        s = s.trim().toUpperCase(Locale.ROOT);
        switch (s) {
            case "DB": return "C#";
            case "EB": return "D#";
            case "GB": return "F#";
            case "AB": return "G#";
            case "BB": return "A#";
            case "E#": return "F";
            case "B#": return "C";
            default:   return s;
        }
    }

    /**
     * Calcula la nota (en sostenidos) en cuerda/traste dados.
     */
    private static String noteAtSharp(int stringIndex, int fret) {
        int openIdx = sharpIndex(OPEN_STRINGS[stringIndex]);
        int idx = (openIdx + fret) % 12;
        return NOTE_SHARP[idx];
    }

    /**
     * Construye el mapa nota→grado para la fórmula, relativa a la tónica.
     */
    private static Map<String, String> buildPitchDegreeMap(String rootSharp, List<String> degrees) {
        int rootIdx = sharpIndex(rootSharp);
        Map<String, String> map = new HashMap<>();
        if (degrees == null) return map;

        for (String deg : degrees) {
            Integer semis = DEGREE_TO_SEMITONES.get(deg);
            if (semis == null) continue;
            String sharp = NOTE_SHARP[(rootIdx + semis) % 12];
            map.put(sharp, deg);
        }
        return map;
    }

    /**
     * Normaliza la etiqueta del grado para visualización:
     * - 1 → R
     * - 4 → p4
     * - 5 → p5
     * El resto se devuelve tal cual (b3, #4, b7, etc.).
     */
    private static String normalizeDegreeLabel(String deg) {
        switch (deg) {
            case "1": return "R";
            case "4": return "p4";
            case "5": return "p5";
            default:  return deg;
        }
    }
}
