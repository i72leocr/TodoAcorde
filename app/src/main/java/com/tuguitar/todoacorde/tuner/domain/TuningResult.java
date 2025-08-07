package com.tuguitar.todoacorde.tuner.domain;

/** Domain data class representing the tuning UI state. */
public class TuningResult {
    public final int progress;
    public final boolean showPlus;
    public final boolean showMinus;
    public final String actionText;
    public final float offset; // <- NUEVO campo

    public TuningResult(int progress, boolean showPlus, boolean showMinus, String actionText, float offset) {
        this.progress = progress;
        this.showPlus = showPlus;
        this.showMinus = showMinus;
        this.actionText = actionText;
        this.offset = offset;
    }

    public float getOffset() {
        return offset;
    }
}
