package com.todoacorde.todoacorde;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.TypeConverters;

import com.todoacorde.todoacorde.achievements.data.Achievement;
import com.todoacorde.todoacorde.achievements.data.AchievementDefinitionEntity;

/**
 * Entidad Room que representa el progreso de un usuario para un logro concreto.
 *
 * Clave primaria compuesta: {@code (userId, familyId, level)}.
 *
 * Claves foráneas:
 * - {@code userId} → {@link User#id} (CASCADE)
 * - {@code (familyId, level)} → {@link AchievementDefinitionEntity} (CASCADE)
 *
 * Conversores:
 * - Usa {@link Converters} para persistir {@link Achievement.Level}.
 */
@Entity(
        tableName = "user_achievements",
        primaryKeys = {"userId", "familyId", "level"},
        foreignKeys = {
                @ForeignKey(
                        entity = User.class,
                        parentColumns = "id",
                        childColumns = "userId",
                        onDelete = ForeignKey.CASCADE
                ),
                @ForeignKey(
                        entity = AchievementDefinitionEntity.class,
                        parentColumns = {"familyId", "level"},
                        childColumns = {"familyId", "level"},
                        onDelete = ForeignKey.CASCADE
                )
        }
)
@TypeConverters(Converters.class)
public class UserAchievementEntity {

    /** Id del usuario propietario del progreso. */
    @NonNull
    private final Long userId;

    /** Identificador normalizado de la familia del logro (p.ej. “practice_sessions”). */
    @NonNull
    private final String familyId;

    /** Nivel del logro dentro de la familia. */
    @NonNull
    private final Achievement.Level level;

    /** Progreso acumulado (métrica específica del logro). */
    private final int progress;

    /**
     * Crea una entidad de progreso de logro para usuario.
     *
     * @param userId   id del usuario.
     * @param familyId id de familia de logro (normalizado).
     * @param level    nivel del logro.
     * @param progress valor de progreso.
     */
    public UserAchievementEntity(
            @NonNull Long userId,
            @NonNull String familyId,
            @NonNull Achievement.Level level,
            int progress
    ) {
        this.userId = userId;
        this.familyId = familyId;
        this.level = level;
        this.progress = progress;
    }

    /** @return id del usuario. */
    @NonNull
    public Long getUserId() {
        return userId;
    }

    /** @return id de familia del logro. */
    @NonNull
    public String getFamilyId() {
        return familyId;
    }

    /** @return nivel del logro. */
    @NonNull
    public Achievement.Level getLevel() {
        return level;
    }

    /** @return progreso acumulado. */
    public int getProgress() {
        return progress;
    }
}
