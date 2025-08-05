package com.tuguitar.todoacorde;

/**
 * Representa una nota dibujable en el mástil para una escala.
 */
public class ScaleFretNote {
    public final int stringIndex; // 0 = 6ª cuerda (Mi grave)
    public final int fret;        // 0 = cuerda al aire, 1+ trastes
    public final String degree;   // "R", "2", "b3", etc.
    public final boolean isRoot;
    public final String finger;   // sugerencia de dedo, opcional
    public final String noteName; // nombre de la nota ("C","D#", etc.)

    public ScaleFretNote(int stringIndex, int fret, String degree, boolean isRoot, String finger, String noteName) {
        this.stringIndex = stringIndex;
        this.fret = fret;
        this.degree = degree;
        this.isRoot = isRoot;
        this.finger = finger;
        this.noteName = noteName;
    }
}
