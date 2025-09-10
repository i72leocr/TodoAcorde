package com.todoacorde.todoacorde.practice.data;

import java.util.List;

/**
 * Representa una línea de una canción compuesta por:
 * <ul>
 *     <li>Una línea de acordes ({@code chordLine}).</li>
 *     <li>Una línea de letra asociada ({@code lyricLine}).</li>
 *     <li>Una colección de {@link SpanInfo} que describe los rangos/segmentos
 *         relevantes para renderizado o interacción.</li>
 * </ul>
 *
 * <p>Es un contenedor inmutable: todos sus campos son {@code final} y se
 * inicializan en el constructor.</p>
 */
public class LineItem {

    /** Cadena con los acordes alineados respecto a la letra. */
    public final String chordLine;

    /** Cadena con la letra correspondiente a la línea. */
    public final String lyricLine;

    /** Lista de segmentos de la línea (por ejemplo, para resaltar o medir). */
    public final List<SpanInfo> spans;

    /**
     * Crea una línea con acordes, letra y los segmentos asociados.
     *
     * @param chordLine texto de acordes (puede ser vacío si no aplica).
     * @param lyricLine texto de la letra alineada con los acordes.
     * @param spans     lista de {@link SpanInfo} asociados a la línea.
     */
    public LineItem(String chordLine, String lyricLine, List<SpanInfo> spans) {
        this.chordLine = chordLine;
        this.lyricLine = lyricLine;
        this.spans = spans;
    }
}
