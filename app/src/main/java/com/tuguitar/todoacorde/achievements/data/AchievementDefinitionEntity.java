package com.tuguitar.todoacorde.achievements.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.TypeConverters;

import com.tuguitar.todoacorde.Achievement;
import com.tuguitar.todoacorde.Converters;

/**
 * Representa la definición de un logro (umbral, icono, título),
 * sin estado de usuario. Una fila por familyId + level.
 */
@Entity(
        tableName = "achievement_definitions",
        primaryKeys = {"familyId","level"}
)
@TypeConverters(Converters.class)  // convierte Achievement.Level
public class AchievementDefinitionEntity {

    /** Clave de familia (p.ej. "primeros_acorde"). */
    @NonNull
    private final String familyId;

    /** Nivel (BRONZE, SILVER, GOLD). */
    @NonNull
    private final Achievement.Level level;

    /** Título legible (p.ej. "Primeros Acorde"). */
    @NonNull
    private final String title;

    /** Umbral para este nivel (ej: 15, 30…). */
    private final int threshold;

    /** Drawable resource ID para el icono. */
    private final int iconResId;

    public AchievementDefinitionEntity(
            @NonNull String familyId,
            @NonNull Achievement.Level level,
            @NonNull String title,
            int threshold,
            int iconResId
    ) {
        this.familyId  = familyId;
        this.level     = level;
        this.title     = title;
        this.threshold = threshold;
        this.iconResId = iconResId;
    }

    @NonNull public String getFamilyId()           { return familyId; }
    @NonNull public Achievement.Level getLevel()   { return level; }
    @NonNull public String getTitle()              { return title; }
    public int getThreshold()                      { return threshold; }
    public int getIconResId()                      { return iconResId; }
}
