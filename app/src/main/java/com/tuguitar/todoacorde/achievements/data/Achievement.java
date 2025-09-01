package com.tuguitar.todoacorde.achievements.data;

import androidx.annotation.NonNull;
import java.util.Objects;

/**
 * Representa un logro indivisible (nivel concreto) con progreso y lógica de estado.
 * Inmutable: cada actualización de progreso devuelve una nueva instancia.
 */
public final class Achievement {
    public enum State { LOCKED, UNLOCKED, COMPLETED }
    public enum Level { BRONZE, SILVER, GOLD }

    private final @NonNull String title;
    private final @NonNull Level level;
    private final int iconResId;
    private final int progress;
    private final int threshold;

    private Achievement(
            @NonNull String title,
            @NonNull Level level,
            int iconResId,
            int progress,
            int threshold
    ) {
        if (title.trim().isEmpty()) {
            throw new IllegalArgumentException("title no puede estar vacío");
        }
        if (threshold <= 0) {
            throw new IllegalArgumentException("threshold debe ser mayor que cero");
        }
        if (progress < 0 || progress > threshold) {
            throw new IllegalArgumentException(
                    "progress debe estar entre 0 y threshold ("
                            + progress + " fuera de rango 0–" + threshold + ")"
            );
        }

        this.title     = title;
        this.level     = level;
        this.iconResId = iconResId;
        this.progress  = progress;
        this.threshold = threshold;
    }

    /**
     * Crea un logro bloqueado (progress = 0).
     */
    public static Achievement createLocked(
            @NonNull String title,
            @NonNull Level level,
            int iconResId,
            int threshold
    ) {
        return new Achievement(title, level, iconResId, 0, threshold);
    }

    @NonNull public String getTitle()         { return title; }
    @NonNull public Level  getLevel()         { return level; }
    public int    getIconResId()               { return iconResId; }
    public int    getProgress()                { return progress; }
    public int    getThreshold()               { return threshold; }

    /**
     * Determina el estado según el progreso y el umbral.
     */
    @NonNull
    public State getState() {
        if (progress >= threshold) {
            return State.COMPLETED;
        } else if (progress > 0) {
            return State.UNLOCKED;
        } else {
            return State.LOCKED;
        }
    }

    /**
     * Retorna una nueva instancia con progreso incrementado y
     * estado recalculado. Nunca excede el threshold.
     *
     * @param additionalProgress cantidad a sumar (>= 0)
     */
    @NonNull
    public Achievement withAddedProgress(int additionalProgress) {
        if (additionalProgress < 0) {
            throw new IllegalArgumentException("additionalProgress no puede ser negativo");
        }
        int newProg = Math.min(threshold, this.progress + additionalProgress);
        if (newProg == this.progress) {
            return this;
        }
        return new Achievement(title, level, iconResId, newProg, threshold);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Achievement)) return false;
        Achievement that = (Achievement) o;
        return iconResId == that.iconResId
                && progress  == that.progress
                && threshold == that.threshold
                && title.equals(that.title)
                && level == that.level;
    }

    @Override
    public int hashCode() {
        return Objects.hash(title, level, iconResId, progress, threshold);
    }

    @Override
    public String toString() {
        return "Achievement{"
                + "title='"     + title       + '\''
                + ", level="     + level
                + ", icon="      + iconResId
                + ", progress="  + progress
                + "/"           + threshold
                + ", state="     + getState()
                + '}';
    }
}
