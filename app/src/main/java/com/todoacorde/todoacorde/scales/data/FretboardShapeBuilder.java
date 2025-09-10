package com.todoacorde.todoacorde.scales.data;

import java.util.ArrayList;
import java.util.List;

/**
 * Utilidad para construir la “forma” de una escala sobre el diapasón.
 * Recorre las 6 cuerdas en afinación estándar y, hasta un traste máximo,
 * agrega todas las posiciones que pertenecen a la escala indicada.
 */
public class FretboardShapeBuilder {

    /**
     * Notas MIDI de las cuerdas al aire en afinación estándar EADGBE.
     * Ordenadas de 6ª a 1ª cuerda: E2(40), A2(45), D3(50), G3(55), B3(59), E4(64).
     */
    private static final int[] OPEN_STRING_MIDI = {40, 45, 50, 55, 59, 64};

    /**
     * Construye la disposición de una escala en el diapasón.
     *
     * Reglas:
     * - Para cada cuerda, se evalúan los trastes desde 0 hasta maxFret (ambos inclusive).
     * - Si la nota del traste pertenece a la escala, se añade un {@link ScaleFretNote}.
     * - Se marca si la nota encontrada es la tónica de la escala.
     *
     * @param rootMidi  nota raíz de la escala en valor MIDI
     * @param scaleType tipo de escala (por ejemplo, mayor, menor, modos)
     * @param maxFret   traste máximo a evaluar por cuerda (incluido)
     * @return lista de posiciones pertenecientes a la escala con cuerda, traste, grado y banderas
     */
    public static List<ScaleFretNote> buildScaleShape(int rootMidi,
                                                      ScaleUtils.ScaleType scaleType,
                                                      int maxFret) {
        List<String> scaleNotes = ScaleUtils.buildScale(rootMidi, scaleType);
        List<ScaleFretNote> result = new ArrayList<>();

        for (int stringIdx = 0; stringIdx < OPEN_STRING_MIDI.length; stringIdx++) {
            int openMidi = OPEN_STRING_MIDI[stringIdx];
            for (int fret = 0; fret <= maxFret; fret++) {
                int midi = openMidi + fret;
                String noteName = NoteUtils.midiToNoteName(midi);
                int degreeIndex = scaleNotes.indexOf(noteName);
                if (degreeIndex >= 0) {
                    boolean isRoot = degreeIndex == 0;
                    String degreeLabel = ScaleUtils.getDegreeLabel(scaleType, degreeIndex);
                    result.add(new ScaleFretNote(
                            stringIdx,      // índice de cuerda (0 = 6ª cuerda)
                            fret,           // número de traste
                            degreeLabel,    // etiqueta del grado (1, b3, 5, etc.)
                            isRoot,         // si es la tónica
                            "",             // campo libre para anotaciones
                            noteName        // nombre de la nota (C, D#, etc.)
                    ));
                }
            }
        }
        return result;
    }
}
