package com.tuguitar.todoacorde;

import java.util.List;

/**
 * Modelo que agrupa una línea de acordes, la línea de letra
 * y la información de los spans de cada acorde en esa línea.
 */
public class LineItem {
    public final String chordLine;
    public final String lyricLine;
    public final List<SpanInfo> spans;

    public LineItem(String chordLine, String lyricLine, List<SpanInfo> spans) {
        this.chordLine = chordLine;
        this.lyricLine = lyricLine;
        this.spans     = spans;
    }
}
