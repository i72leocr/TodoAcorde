package com.tuguitar.todoacorde.achievements.data;

import androidx.annotation.NonNull;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Modelo UI inmutable que agrupa un título, descripción y la lista de niveles de logro.
 */
public final class AchievementFamily {

    private final @NonNull String familyTitle;
    private final @NonNull String description;
    private final @NonNull List<Achievement> levels;

    /**
     * @param familyTitle Título legible de la familia (p.ej. "Primeros Acorde").
     * @param description Descripción breve.
     * @param levels      Lista de instancias de Achievement (BRONZE→SILVER→GOLD).
     */
    public AchievementFamily(
            @NonNull String familyTitle,
            @NonNull String description,
            @NonNull List<Achievement> levels
    ) {
        if (familyTitle.trim().isEmpty()) {
            throw new IllegalArgumentException("familyTitle no puede estar vacío");
        }
        Objects.requireNonNull(levels, "levels no puede ser null");
        if (levels.isEmpty()) {
            throw new IllegalArgumentException("levels no puede estar vacío");
        }
        this.familyTitle = familyTitle;
        this.description = description;
        // Copia defensiva para inmutabilidad
        this.levels = Collections.unmodifiableList(levels);
    }

    /** Título de la familia (p.ej. "Primeros Acorde"). */
    @NonNull
    public String getFamilyTitle() {
        return familyTitle;
    }

    /** Descripción breve del logro. */
    @NonNull
    public String getDescription() {
        return description;
    }

    /**
     * Niveles ordenados (BRONZE → SILVER → GOLD) con estado y progreso actual.
     */
    @NonNull
    public List<Achievement> getLevels() {
        return levels;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AchievementFamily)) return false;
        AchievementFamily that = (AchievementFamily) o;
        return familyTitle.equals(that.familyTitle)
                && description.equals(that.description)
                && levels.equals(that.levels);
    }

    @Override
    public int hashCode() {
        return Objects.hash(familyTitle, description, levels);
    }

    @Override
    public String toString() {
        return "AchievementFamily{" +
                "familyTitle='"  + familyTitle + '\'' +
                ", description='" + description + '\'' +
                ", levels="       + levels +
                '}';
    }
}
