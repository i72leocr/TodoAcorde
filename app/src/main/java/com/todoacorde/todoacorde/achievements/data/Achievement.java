package com.todoacorde.todoacorde.achievements.data;

import androidx.annotation.NonNull;

import java.util.Objects;

/**
 * Modelo inmutable que representa un logro (achievement) en la aplicación.
 *
 * Semántica de diseño:
 * - Inmutabilidad: todos los campos son final y se fijan en el constructor privado.
 *   Las “modificaciones” crean nuevas instancias (ver {@link #withAddedProgress(int)}).
 * - Consistencia: se valida que el título no sea vacío, que {@code threshold > 0},
 *   y que {@code 0 <= progress <= threshold}.
 * - Estado derivado: el {@link State} se infiere a partir de {@code progress} y {@code threshold}
 *   mediante {@link #getState()} (no se guarda para evitar incoherencias).
 *
 * Uso típico: crear un logro bloqueado con {@link #createLocked(String, Level, int, int)}
 * y, a medida que el usuario progresa, producir nuevas instancias con {@link #withAddedProgress(int)}.
 */
public final class Achievement {

    /**
     * Estado derivado del logro según el progreso.
     * LOCKED: {@code progress == 0}.
     * UNLOCKED: {@code 0 < progress < threshold}.
     * COMPLETED: {@code progress >= threshold}.
     */
    public enum State { LOCKED, UNLOCKED, COMPLETED }

    /**
     * Nivel de distinción del logro. Es informativo (no altera la lógica).
     */
    public enum Level { BRONZE, SILVER, GOLD }

    /** Título descriptivo y no vacío del logro. */
    private final @NonNull String title;

    /** Nivel asignado al logro. */
    private final @NonNull Level level;

    /** Identificador de recurso del icono asociado (por ejemplo, {@code R.drawable.*}). */
    private final int iconResId;

    /**
     * Progreso acumulado actual. Invariante: {@code 0 <= progress <= threshold}.
     * Desde la API pública de esta clase, el progreso no decrece.
     */
    private final int progress;

    /** Umbral positivo requerido para completar el logro ({@code threshold > 0}). */
    private final int threshold;

    /**
     * Crea una instancia validando invariantes de dominio.
     *
     * @param title     título no vacío (se valida con {@code trim()}).
     * @param level     nivel del logro.
     * @param iconResId identificador del recurso de icono.
     * @param progress  progreso inicial; debe cumplir {@code 0 <= progress <= threshold}.
     * @param threshold umbral positivo que determina la finalización del logro.
     * @throws IllegalArgumentException si el título es vacío, si {@code threshold <= 0}
     *                                  o si {@code progress} está fuera de rango.
     */
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

        this.title = title;
        this.level = level;
        this.iconResId = iconResId;
        this.progress = progress;
        this.threshold = threshold;
    }

    /**
     * Crea un logro inicialmente bloqueado (progreso = 0).
     *
     * @param title     título del logro (no vacío).
     * @param level     nivel del logro.
     * @param iconResId identificador del icono asociado.
     * @param threshold umbral positivo para completar el logro.
     * @return nueva instancia con progreso 0.
     * @throws IllegalArgumentException si el título es vacío o el umbral no es válido.
     * @see #withAddedProgress(int)
     */
    public static Achievement createLocked(
            @NonNull String title,
            @NonNull Level level,
            int iconResId,
            int threshold
    ) {
        return new Achievement(title, level, iconResId, 0, threshold);
    }

    /** @return título del logro (no nulo). */
    @NonNull
    public String getTitle() {
        return title;
    }

    /** @return nivel del logro (no nulo). */
    @NonNull
    public Level getLevel() {
        return level;
    }

    /** @return identificador de recurso del icono asociado. */
    public int getIconResId() {
        return iconResId;
    }

    /** @return progreso actual acumulado dentro del rango {@code [0, threshold]}. */
    public int getProgress() {
        return progress;
    }

    /** @return umbral positivo requerido para la finalización del logro. */
    public int getThreshold() {
        return threshold;
    }

    /**
     * Devuelve el estado actual del logro a partir del progreso y el umbral.
     *
     * @return {@link State#COMPLETED} si {@code progress >= threshold};
     *         {@link State#UNLOCKED} si {@code progress > 0};
     *         en caso contrario {@link State#LOCKED}.
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
     * Crea una nueva instancia con el progreso incrementado de forma segura.
     * El incremento no puede ser negativo y el valor resultante se satura en {@code threshold}.
     * Si no hay cambio efectivo en el progreso, se devuelve la misma instancia.
     *
     * @param additionalProgress incremento no negativo a aplicar.
     * @return nueva instancia con el progreso actualizado, o {@code this} si no hay cambios.
     * @throws IllegalArgumentException si {@code additionalProgress < 0}.
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

    /**
     * Igualdad estructural basada en todos los campos.
     *
     * @param o objeto a comparar.
     * @return {@code true} si ambas instancias son equivalentes; en caso contrario {@code false}.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Achievement)) return false;
        Achievement that = (Achievement) o;
        return iconResId == that.iconResId
                && progress == that.progress
                && threshold == that.threshold
                && title.equals(that.title)
                && level == that.level;
    }

    /**
     * Hash consistente con {@link #equals(Object)}.
     *
     * @return valor hash calculado a partir de todos los campos.
     */
    @Override
    public int hashCode() {
        return Objects.hash(title, level, iconResId, progress, threshold);
    }

    /**
     * Representación textual útil para depuración y registro.
     *
     * @return cadena con los campos principales y el estado derivado.
     */
    @Override
    public String toString() {
        return "Achievement{"
                + "title='" + title + '\''
                + ", level=" + level
                + ", icon=" + iconResId
                + ", progress=" + progress
                + "/" + threshold
                + ", state=" + getState()
                + '}';
    }
}
