package com.tuguitar.todoacorde.achievements.data;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.TypeConverters;

import com.tuguitar.todoacorde.Converters;
import com.tuguitar.todoacorde.achievements.data.Achievement.Level;

/**
 * Entidad Room para persistir el estado del logro por familia y nivel.
 */
@Entity(
        tableName = "achievements",
        primaryKeys = {"familyId", "level"}
)
@TypeConverters(Converters.class)
public final class AchievementEntity {

    /**
     * PK compuesta: identificador de la familia de logro (por ejemplo "primeros_acorde").
     */
    @NonNull
    @ColumnInfo(name = "familyId")
    private final String familyId;

    /**
     * PK compuesta: Nivel actual del logro: BRONZE, SILVER o GOLD.
     */
    @NonNull
    @ColumnInfo(name = "level")
    private final Level level;

    /**
     * Progreso dentro del nivel (0…threshold).
     */
    @ColumnInfo(name = "progress")
    private final int progress;

    /**
     * Constructor público para Room.
     *
     * @param familyId Identificador de la familia de logro
     * @param level    Nivel actual
     * @param progress Progreso dentro del nivel
     */
    public AchievementEntity(
            @NonNull String familyId,
            @NonNull Level level,
            int progress
    ) {
        this.familyId = familyId;
        this.level    = level;
        this.progress = progress;
    }

    /**
     * Factory para crear la entidad desde dominio.
     */
    public static AchievementEntity fromDomain(
            @NonNull String familyId,
            @NonNull Level level,
            int progress
    ) {
        return new AchievementEntity(familyId, level, progress);
    }

    @NonNull public String getFamilyId() { return familyId; }
    @NonNull public Level getLevel()     { return level; }
    public int getProgress()             { return progress; }
}
