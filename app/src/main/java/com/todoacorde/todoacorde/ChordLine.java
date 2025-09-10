package com.todoacorde.todoacorde;

/**
 * Representa una línea de canción compuesta por una parte de la letra y
 * el acorde asociado que debe tocarse en ese punto.
 *
 * Se utiliza como modelo simple para vincular texto (letra) con un acorde
 * específico dentro de la estructura de una canción.
 */
public class ChordLine {
    /** Fragmento de letra asociado a la línea. */
    private final String lyric;

    /** Acorde que acompaña al fragmento de letra. */
    private final String chord;

    /**
     * Construye una línea de acorde con su letra correspondiente.
     *
     * @param lyric texto de la letra en esta línea.
     * @param chord acorde que debe interpretarse junto con la letra.
     */
    public ChordLine(String lyric, String chord) {
        this.lyric = lyric;
        this.chord = chord;
    }

    /**
     * Obtiene la letra asociada a la línea.
     *
     * @return texto de la letra.
     */
    public String getLyric() {
        return lyric;
    }

    /**
     * Obtiene el acorde asignado a la línea.
     *
     * @return nombre del acorde.
     */
    public String getChord() {
        return chord;
    }
}
