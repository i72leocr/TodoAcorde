package com.tuguitar.todoacorde;

public enum ScaleType {
    MAJOR(new int[]{0, 2, 4, 5, 7, 9, 11, 12}),
    MINOR_NATURAL(new int[]{0, 2, 3, 5, 7, 8, 10, 12}),
    HARMONIC_MINOR(new int[]{0, 2, 3, 5, 7, 8, 11, 12}),
    PENTATONIC_MAJOR(new int[]{0, 2, 4, 7, 9, 12});
    // Añade más si quieres

    private final int[] intervals;

    ScaleType(int[] intervals) {
        this.intervals = intervals;
    }

    public int[] getIntervals() { return intervals; }
}

