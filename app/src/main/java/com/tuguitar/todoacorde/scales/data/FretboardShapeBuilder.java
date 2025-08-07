package com.tuguitar.todoacorde.scales.data;

import com.tuguitar.todoacorde.NoteUtils;

import java.util.ArrayList;
import java.util.List;

public class FretboardShapeBuilder {

    private static final int[] OPEN_STRING_MIDI = {40, 45, 50, 55, 59, 64};

    public static List<ScaleFretNote> buildScaleShape(int rootMidi, ScaleUtils.ScaleType scaleType, int maxFret) {
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
                    result.add(new ScaleFretNote(stringIdx, fret, degreeLabel, isRoot, "", noteName));
                }
            }
        }
        return result;
    }
}
