package com.tuguitar.todoacorde;

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
}
