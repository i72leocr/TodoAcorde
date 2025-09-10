package com.todoacorde.todoacorde.scales.data;

import java.util.Locale;

/**
 * Utilidades para trabajar con notas musicales y conversiones entre
 * frecuencia (Hz), valores MIDI y nombres de nota.
 *
 * Convenciones:
 * - Se normalizan alteraciones a sostenidos (#) cuando es posible.
 * - Los nombres de nota devueltos no incluyen octava.
 */
public class NoteUtils {

    /**
     * Nombres de las 12 notas dentro de una octava usando sostenidos.
     * Índices: 0=C, 1=C#, 2=D, ..., 11=B.
     */
    private static final String[] NOTE_NAMES_SHARP = {
            "C", "C#", "D", "D#", "E", "F",
            "F#", "G", "G#", "A", "A#", "B"
    };

    /**
     * Convierte una frecuencia en Hz a un valor MIDI en coma flotante.
     * A4 = 440 Hz corresponde a MIDI 69.
     *
     * Si la frecuencia no es positiva, devuelve -1.
     *
     * @param frequency frecuencia en Hz
     * @return valor MIDI (double), o -1 si la frecuencia no es válida
     */
    public static double frequencyToMidi(double frequency) {
        if (frequency <= 0) return -1;
        return 12 * (Math.log(frequency / 440.0) / Math.log(2)) + 69;
    }

    /**
     * Convierte una frecuencia en Hz al valor MIDI entero más cercano.
     * Si la frecuencia no es válida, devuelve -1.
     *
     * @param frequency frecuencia en Hz
     * @return valor MIDI redondeado, o -1 si no es válido
     */
    public static int frequencyToNearestMidi(double frequency) {
        double midi = frequencyToMidi(frequency);
        if (midi < 0) return -1;
        return (int) Math.round(midi);
    }

    /**
     * Convierte un valor MIDI a nombre de nota sin octava,
     * usando la tabla con sostenidos (#).
     *
     * Si el MIDI es negativo, devuelve cadena vacía.
     *
     * @param midi valor MIDI (0..127 recomendado)
     * @return nombre de la nota (C, C#, D, ...), o "" si no válido
     */
    public static String midiToNoteName(int midi) {
        if (midi < 0) return "";
        int index = ((midi % 12) + 12) % 12;  // manejo seguro de negativos
        return NOTE_NAMES_SHARP[index];
    }

    /**
     * Calcula el desvío en cents respecto del tono temperado más cercano
     * para una frecuencia dada.
     *
     * Si la frecuencia no es válida, devuelve 0.
     *
     * @param frequency frecuencia en Hz
     * @return desviación en cents (positivo o negativo)
     */
    public static double centsOff(double frequency) {
        if (frequency <= 0) return 0;
        double midi = frequencyToMidi(frequency);
        int nearest = (int) Math.round(midi);
        return (midi - nearest) * 100.0;
    }

    /**
     * Elimina sufijos de octava en un nombre de nota. Ejemplos:
     * "C#4" → "C#", "A3" → "A".
     *
     * Si la entrada es null, devuelve "".
     *
     * @param noteName nombre de nota posiblemente con octava
     * @return nombre sin dígitos finales
     */
    public static String stripOctave(String noteName) {
        if (noteName == null) return "";
        String s = noteName.trim();
        int i = s.length() - 1;
        while (i >= 0 && Character.isDigit(s.charAt(i))) i--;
        return s.substring(0, i + 1);
    }

    /**
     * Normaliza un nombre de nota a formato con sostenidos (#) cuando aplica.
     * Soporta bemoles (♭ o 'B') y sostenidos (♯ o '#'), e ignora la octava.
     *
     * Reglas especiales:
     * - E# → F, B# → C, Cb → B, Fb → E.
     * - Db → C#, Eb → D#, Gb → F#, Ab → G#, Bb → A#.
     *
     * Si la entrada es null, devuelve "".
     *
     * @param s nombre de nota
     * @return nombre normalizado con sostenidos o la misma cadena si ya es válida
     */
    public static String normalizeToSharp(String s) {
        if (s == null) return "";
        s = stripOctave(s)
                .replace('♭', 'B')
                .replace('♯', '#')
                .trim()
                .toUpperCase(Locale.ROOT);

        if (s.isEmpty()) return s;

        // Equivalencias naturales ↔ alteradas especiales
        switch (s) {
            case "E#": return "F";
            case "B#": return "C";
            case "CB": return "B";
            case "FB": return "E";
        }

        // Bemoles comunes a sostenidos equivalentes
        switch (s) {
            case "DB": return "C#";
            case "EB": return "D#";
            case "GB": return "F#";
            case "AB": return "G#";
            case "BB": return "A#";
        }

        // Validación de nombres ya en formato sostenido o natural
        switch (s) {
            case "C":
            case "C#":
            case "D":
            case "D#":
            case "E":
            case "F":
            case "F#":
            case "G":
            case "G#":
            case "A":
            case "A#":
            case "B":
                return s;
        }

        // Si no coincide con nada conocido, se devuelve tal cual
        return s;
    }

    /**
     * Compara dos nombres de nota ignorando la octava y considerando
     * equivalencias enarmónicas (por ejemplo, Db = C#).
     *
     * @param a primera nota
     * @param b segunda nota
     * @return true si representan el mismo sonido, false en otro caso
     */
    public static boolean equalsEnharmonic(String a, String b) {
        if (a == null || b == null) return false;
        String aa = normalizeToSharp(a);
        String bb = normalizeToSharp(b);
        return aa.equals(bb);
    }
}
