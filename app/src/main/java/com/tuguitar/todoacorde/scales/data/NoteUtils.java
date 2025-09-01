package com.tuguitar.todoacorde.scales.data;

import java.util.Locale;

public class NoteUtils {

    private static final String[] NOTE_NAMES_SHARP = {
            "C", "C#", "D", "D#", "E", "F",
            "F#", "G", "G#", "A", "A#", "B"
    };

    /** Convierte frecuencia en número MIDI (puede ser decimal). */
    public static double frequencyToMidi(double frequency) {
        if (frequency <= 0) return -1;
        return 12 * (Math.log(frequency / 440.0) / Math.log(2)) + 69;
    }

    /** Devuelve el MIDI entero más cercano a una frecuencia. */
    public static int frequencyToNearestMidi(double frequency) {
        double midi = frequencyToMidi(frequency);
        if (midi < 0) return -1;
        return (int) Math.round(midi);
    }

    /** Redondea al MIDI entero más cercano y devuelve el nombre de nota (sin octava). */
    public static String midiToNoteName(int midi) {
        if (midi < 0) return "";
        int index = ((midi % 12) + 12) % 12; // módulo seguro
        return NOTE_NAMES_SHARP[index];
    }

    /** Devuelve cuántos cents de desviación hay respecto al midi más cercano. */
    public static double centsOff(double frequency) {
        if (frequency <= 0) return 0;
        double midi = frequencyToMidi(frequency);
        int nearest = (int) Math.round(midi);
        return (midi - nearest) * 100.0;
    }

    /**
     * Elimina dígitos de octava del final, p. ej. "E4" -> "E", "C#3" -> "C#".
     */
    public static String stripOctave(String noteName) {
        if (noteName == null) return "";
        String s = noteName.trim();
        int i = s.length() - 1;
        while (i >= 0 && Character.isDigit(s.charAt(i))) i--;
        return s.substring(0, i + 1);
    }

    /**
     * Normaliza el nombre de nota a notación con # cuando es posible.
     * - Convierte bemoles comunes: Db→C#, Eb→D#, Gb→F#, Ab→G#, Bb→A#.
     * - Ajustes típicos: E#→F, B#→C, Cb→B, Fb→E.
     * - Soporta unicode ♭/♯ y elimina dígitos de octava si vienen.
     * - Devuelve "" si la entrada es nula o vacía.
     */
    public static String normalizeToSharp(String s) {
        if (s == null) return "";
        s = stripOctave(s)
                .replace('♭', 'B')
                .replace('♯', '#')
                .trim()
                .toUpperCase(Locale.ROOT);

        if (s.isEmpty()) return s;
        switch (s) {
            case "E#": return "F";
            case "B#": return "C";
            case "CB": return "B";
            case "FB": return "E";
        }
        switch (s) {
            case "DB": return "C#";
            case "EB": return "D#";
            case "GB": return "F#";
            case "AB": return "G#";
            case "BB": return "A#";
        }
        switch (s) {
            case "C": case "C#":
            case "D": case "D#":
            case "E":
            case "F": case "F#":
            case "G": case "G#":
            case "A": case "A#":
            case "B":
                return s;
        }
        return s;
    }

    /**
     * Compara dos nombres de nota ignorando octava y usando equivalencias enarmónicas.
     * Ejemplos: equalsEnharmonic("Db", "C#") -> true, equalsEnharmonic("E#", "F") -> true.
     */
    public static boolean equalsEnharmonic(String a, String b) {
        if (a == null || b == null) return false;
        String aa = normalizeToSharp(a);
        String bb = normalizeToSharp(b);
        return aa.equals(bb);
    }
}
