package com.tuguitar.todoacorde;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.TypeConverters;

import com.tuguitar.todoacorde.achievements.data.Achievement;
import com.tuguitar.todoacorde.achievements.data.AchievementDefinitionEntity;

/**
 * Entidad que persiste el progreso de cada usuario en cada nivel de logro.
 * Relaciona usuario ⇄ definición de logro.
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
@TypeConverters(Converters.class) // convierte Achievement.Level
public class UserAchievementEntity {

    /** ID del usuario (clave foránea a User.id). */
    @NonNull
    private final Long userId;

    /** Clave de la familia de logro (p.ej. "primeros_acorde"). */
    @NonNull
    private final String familyId;

    /** Nivel de logro (BRONZE, SILVER, GOLD). */
    @NonNull
    private final Achievement.Level level;

    /** Progreso actual (0…threshold). */
    private final int progress;

    public UserAchievementEntity(
            @NonNull Long userId,
            @NonNull String familyId,
            @NonNull Achievement.Level level,
            int progress
    ) {
        this.userId   = userId;
        this.familyId = familyId;
        this.level    = level;
        this.progress = progress;
    }

    @NonNull
    public Long getUserId() {
        return userId;
    }

    @NonNull
    public String getFamilyId() {
        return familyId;
    }

    @NonNull
    public Achievement.Level getLevel() {
        return level;
    }

    public int getProgress() {
        return progress;
    }
}
