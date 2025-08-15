package com.tuguitar.todoacorde.practice.data;

/**
 * Representa un tramo de texto (start…end-1) en el chordLine
 * y el índice global de ese acorde.
 */
public class SpanInfo {
    public final int globalIndex;
    public final int start;  // inclusive
    public final int end;    // exclusive

    public SpanInfo(int globalIndex, int start, int end) {
        this.globalIndex = globalIndex;
        this.start       = start;
        this.end         = end;
    }
}
