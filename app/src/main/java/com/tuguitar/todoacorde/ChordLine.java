// File: app/src/main/java/com/tuguitar/todoacorde/practice/ChordLine.java
package com.tuguitar.todoacorde;

/**
 * Representa una sola línea de letra con su acorde asociado.
 */
public class ChordLine {
    private final String lyric;
    private final String chord;

    public ChordLine(String lyric, String chord) {
        this.lyric = lyric;
        this.chord = chord;
    }

    public String getLyric() {
        return lyric;
    }

    public String getChord() {
        return chord;
    }
}
