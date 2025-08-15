package com.tuguitar.todoacorde.scales.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class ScaleUtils {

    public enum ScaleType {
        MAJOR("Mayor", new int[]{0, 2, 4, 5, 7, 9, 11, 12}),
        NATURAL_MINOR("Menor natural", new int[]{0, 2, 3, 5, 7, 8, 10, 12}),
        HARMONIC_MINOR("Menor armónica", new int[]{0, 2, 3, 5, 7, 8, 11, 12}),
        MELODIC_MINOR_ASC("Menor melódica", new int[]{0, 2, 3, 5, 7, 9, 11, 12}),
        PENTATONIC_MAJOR("Pentatónica mayor", new int[]{0, 2, 4, 7, 9, 12}),
        PENTATONIC_MINOR("Pentatónica menor", new int[]{0, 3, 5, 7, 10, 12}),
        BLUES("Blues", new int[]{0, 3, 5, 6, 7, 10, 12}),
        DORIAN("Dórica", new int[]{0, 2, 3, 5, 7, 9, 10, 12}),
        MIXOLYDIAN("Mixolidia", new int[]{0, 2, 4, 5, 7, 9, 10, 12}),
        LYDIAN("Lidia", new int[]{0, 2, 4, 6, 7, 9, 11, 12}),
        PHRYGIAN("Frigia", new int[]{0, 1, 3, 5, 7, 8, 10, 12}),
        LOCRIAN("Locria", new int[]{0, 1, 3, 5, 6, 8, 10, 12});

        private final String displayName;
        private final int[] intervals;

        ScaleType(String displayName, int[] intervals) {
            this.displayName = displayName;
            this.intervals = intervals;
        }

        public String getDisplayName() {
            return displayName;
        }

        public int[] getIntervals() {
            return intervals;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    public static List<String> buildScale(int rootMidiNote, ScaleType type) {
        if (type == null) return Collections.emptyList();
        List<String> result = new ArrayList<>();
        for (int interval : type.getIntervals()) {
            int midi = rootMidiNote + interval;
            String name = NoteUtils.midiToNoteName(midi);
            result.add(name);
        }
        return result;
    }

    public static ScaleType fromDisplayName(String name) {
        if (name == null) return ScaleType.MAJOR;
        for (ScaleType t : ScaleType.values()) {
            if (t.getDisplayName().equalsIgnoreCase(name.trim())) {
                return t;
            }
        }
        return ScaleType.MAJOR;
    }

    public static List<String> getDisplayNames() {
        List<String> names = new ArrayList<>();
        for (ScaleType t : ScaleType.values()) {
            names.add(t.getDisplayName());
        }
        return names;
    }

    public static String getDegreeLabel(ScaleType type, int degreeIndex) {
        if (degreeIndex == 0) return "R";
        switch (type) {
            case MAJOR:
                return new String[]{"2", "3", "4", "p5", "6", "7", "8"}[degreeIndex - 1];
            case NATURAL_MINOR:
                return new String[]{"2", "b3", "4", "5", "b6", "b7", "8"}[degreeIndex - 1];
            case HARMONIC_MINOR:
                return new String[]{"2", "b3", "4", "5", "b6", "7", "8"}[degreeIndex - 1];
            case MELODIC_MINOR_ASC:
                return new String[]{"2", "b3", "4", "5", "6", "7", "8"}[degreeIndex - 1];
            case PENTATONIC_MAJOR:
                return new String[]{"2", "3", "p5", "6", "8"}[degreeIndex - 1];
            case PENTATONIC_MINOR:
                return new String[]{"b3", "4", "5", "b7", "8"}[degreeIndex - 1];
            case BLUES:
                return new String[]{"b3", "4", "b5", "p5", "b7", "8"}[degreeIndex - 1];
            case DORIAN:
                return new String[]{"2", "b3", "4", "5", "6", "b7", "8"}[degreeIndex - 1];
            case MIXOLYDIAN:
                return new String[]{"2", "3", "4", "5", "6", "b7", "8"}[degreeIndex - 1];
            case LYDIAN:
                return new String[]{"2", "3", "#4", "5", "6", "7", "8"}[degreeIndex - 1];
            case PHRYGIAN:
                return new String[]{"b2", "b3", "4", "5", "b6", "b7", "8"}[degreeIndex - 1];
            case LOCRIAN:
                return new String[]{"b2", "b3", "4", "b5", "b6", "b7", "8"}[degreeIndex - 1];
            default:
                return "";
        }
    }

    public static ScaleType fromDbTypeName(String raw) {
        if (raw == null) return ScaleType.MAJOR;
        String t = raw.trim().toLowerCase(Locale.ROOT);
        switch (t) {
            case "ionian":
            case "major":
                return ScaleType.MAJOR;
            case "aeolian":
            case "natural minor":
                return ScaleType.NATURAL_MINOR;
            case "harmonic minor":
                return ScaleType.HARMONIC_MINOR;
            case "melodic minor (asc)":
            case "melodic minor":
                return ScaleType.MELODIC_MINOR_ASC;
            case "dorian":
                return ScaleType.DORIAN;
            case "phrygian":
                return ScaleType.PHRYGIAN;
            case "lydian":
                return ScaleType.LYDIAN;
            case "mixolydian":
                return ScaleType.MIXOLYDIAN;
            case "locrian":
                return ScaleType.LOCRIAN;
            // por si algún día metes estas por BD:
            case "pentatonic major":
                return ScaleType.PENTATONIC_MAJOR;
            case "pentatonic minor":
                return ScaleType.PENTATONIC_MINOR;
            case "blues":
                return ScaleType.BLUES;
            default:
                return ScaleType.MAJOR;
        }
    }

}
