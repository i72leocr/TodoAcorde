package com.todoacorde.todoacorde.achievements.data;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.TypeConverters;

import com.todoacorde.todoacorde.Converters;
import com.todoacorde.todoacorde.achievements.data.Achievement.Level;

/**
 * Entidad Room que representa el estado de progreso de un logro para una familia y nivel concretos.
 *
 * Se persiste en la tabla {@code achievements} y utiliza una clave primaria compuesta por
 * {@code familyId} y {@code level}. El campo {@link Level} se serializa mediante
 * los conversores declarados en {@link Converters}.
 */
@Entity(
        tableName = "achievements",
        primaryKeys = {"familyId", "level"}
)
@TypeConverters(Converters.class)
public final class AchievementEntity {

    /** Identificador lógico de la familia de logros. Forma parte de la clave primaria. */
    @NonNull
    @ColumnInfo(name = "familyId")
    private final String familyId;

    /** Nivel del logro (BRONZE, SILVER, GOLD). Forma parte de la clave primaria. */
    @NonNull
    @ColumnInfo(name = "level")
    private final Level level;

    /** Progreso acumulado actual asociado al par (familia, nivel). */
    @ColumnInfo(name = "progress")
    private final int progress;

    /**
     * Crea una entidad inmutable con el estado de progreso de un logro.
     *
     * @param familyId identificador de la familia de logros; no debe ser nulo.
     * @param level    nivel del logro; no debe ser nulo.
     * @param progress progreso acumulado actual.
     */
    public AchievementEntity(
            @NonNull String familyId,
            @NonNull Level level,
            int progress
    ) {
        this.familyId = familyId;
        this.level = level;
        this.progress = progress;
    }

    /**
     * Fábrica de conveniencia para crear una instancia desde el dominio.
     *
     * @param familyId identificador de la familia de logros.
     * @param level    nivel del logro.
     * @param progress progreso acumulado actual.
     * @return nueva instancia de {@link AchievementEntity} con los valores proporcionados.
     */
    public static AchievementEntity fromDomain(
            @NonNull String familyId,
            @NonNull Level level,
            int progress
    ) {
        return new AchievementEntity(familyId, level, progress);
    }

    /** @return identificador de la familia de logros. */
    @NonNull
    public String getFamilyId() {
        return familyId;
    }

    /** @return nivel del logro. */
    @NonNull
    public Level getLevel() {
        return level;
    }

    /** @return progreso acumulado actual. */
    public int getProgress() {
        return progress;
    }
}
