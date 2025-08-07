package com.tuguitar.todoacorde;

import com.tuguitar.todoacorde.scales.data.ScaleFretNote;

import java.util.*;

public class PatternRepository {

    public static class ScalePattern {
        public final String name;
        public final String scaleType;
        public final String rootNote;
        public final List<ScaleFretNote> notes;

        public ScalePattern(String name, String scaleType, String rootNote, List<ScaleFretNote> notes) {
            this.name = name;
            this.scaleType = scaleType;
            this.rootNote = rootNote;
            this.notes = notes;
        }
    }

    private static final List<ScalePattern> patterns = new ArrayList<>();

    static {
        // Ejemplo: Pentatónica menor de LA (caja 1)
        patterns.add(new ScalePattern(
                "Pentatónica menor Caja 1 A",
                "Minor Pentatonic",
                "A",
                Arrays.asList(
                        new ScaleFretNote(0, 5,  "R",  true,  null, "A"),   // 6ª cuerda, traste 5
                        new ScaleFretNote(0, 8,  "b3", false, null, "C"),
                        new ScaleFretNote(1, 5,  "p4", false, null, "D"),
                        new ScaleFretNote(1, 7,  "p5", false, null, "E"),
                        new ScaleFretNote(2, 5,  "b7", false, null, "G"),
                        new ScaleFretNote(2, 7,  "R",  true,  null, "A"),
                        new ScaleFretNote(3, 5,  "b3", false, null, "C"),
                        new ScaleFretNote(3, 7,  "p4", false, null, "D"),
                        new ScaleFretNote(4, 5,  "p5", false, null, "E"),
                        new ScaleFretNote(4, 8,  "b7", false, null, "G"),
                        new ScaleFretNote(5, 5,  "R",  true,  null, "A"),
                        new ScaleFretNote(5, 8,  "b3", false, null, "C")
                )
        ));

        // Añade más patrones aquí según necesites
    }

    public static List<ScalePattern> getAllPatterns() {
        return patterns;
    }

    public static List<ScalePattern> findPatterns(String scaleType, String rootNote) {
        List<ScalePattern> found = new ArrayList<>();
        for (ScalePattern p : patterns) {
            if (p.scaleType.equalsIgnoreCase(scaleType) && p.rootNote.equalsIgnoreCase(rootNote)) {
                found.add(p);
            }
        }
        return found;
    }
}
