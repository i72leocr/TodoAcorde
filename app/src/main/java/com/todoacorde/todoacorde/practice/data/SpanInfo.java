package com.todoacorde.todoacorde.practice.data;

/**
 * Información de un segmento (span) dentro de una línea compuesta.
 *
 * Cada instancia identifica un rango [start, end) relativo a una línea o buffer,
 * junto con un índice global que permite referenciar el span en colecciones
 * agregadas (por ejemplo, dentro de una canción completa).
 */
public class SpanInfo {

    /** Índice global del span dentro de la colección completa. */
    public final int globalIndex;

    /** Posición de inicio (inclusive) del span en la línea. */
    public final int start;

    /** Posición de fin (exclusiva) del span en la línea. */
    public final int end;

    /**
     * Crea un descriptor de span.
     *
     * @param globalIndex índice global del span en la colección completa
     * @param start       posición de inicio (inclusive) en la línea
     * @param end         posición de fin (exclusiva) en la línea
     */
    public SpanInfo(int globalIndex, int start, int end) {
        this.globalIndex = globalIndex;
        this.start = start;
        this.end = end;
    }
}
