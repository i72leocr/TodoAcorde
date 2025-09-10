package com.todoacorde.todoacorde.achievements.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.TypeConverters;

import com.todoacorde.todoacorde.Converters;

/**
 * Entidad Room que define la configuración estática de un logro.
 *
 * <p>Se persiste en la tabla {@code achievement_definitions} y utiliza una clave primaria
 * compuesta por {@code familyId} y {@code level}. Estas definiciones representan la “plantilla”
 * (título, umbral e icono) a partir de la cual se instancian logros para el usuario.</p>
 *
 * <p>El campo {@link Achievement.Level} se almacena mediante los conversores declarados
 * en {@link Converters}.</p>
 */
@Entity(
        tableName = "achievement_definitions",
        primaryKeys = {"familyId", "level"}
)
@TypeConverters(Converters.class)
public class AchievementDefinitionEntity {

    /** Identificador lógico de la familia de logros. Forma parte de la clave primaria. */
    @NonNull
    private final String familyId;

    /** Nivel del logro (BRONZE, SILVER, GOLD). Forma parte de la clave primaria. */
    @NonNull
    private final Achievement.Level level;

    /** Título descriptivo del logro. */
    @NonNull
    private final String title;

    /** Umbral requerido para considerar el logro como completado. Debe ser positivo. */
    private final int threshold;

    /** Identificador de recurso del icono asociado (por ejemplo, {@code R.drawable.*}). */
    private final int iconResId;

    /**
     * Crea una definición inmutable de logro.
     *
     * @param familyId identificador de la familia de logros; no debe ser nulo.
     * @param level    nivel del logro; no debe ser nulo.
     * @param title    título descriptivo; no debe ser nulo.
     * @param threshold umbral de progreso para completar el logro (se espera &gt; 0).
     * @param iconResId identificador de recurso del icono.
     */
    public AchievementDefinitionEntity(
            @NonNull String familyId,
            @NonNull Achievement.Level level,
            @NonNull String title,
            int threshold,
            int iconResId
    ) {
        this.familyId = familyId;
        this.level = level;
        this.title = title;
        this.threshold = threshold;
        this.iconResId = iconResId;
    }

    /** @return identificador de la familia de logros. */
    @NonNull
    public String getFamilyId() {
        return familyId;
    }

    /** @return nivel del logro. */
    @NonNull
    public Achievement.Level getLevel() {
        return level;
    }

    /** @return título descriptivo del logro. */
    @NonNull
    public String getTitle() {
        return title;
    }

    /** @return umbral necesario para completar el logro. */
    public int getThreshold() {
        return threshold;
    }

    /** @return identificador de recurso del icono asociado. */
    public int getIconResId() {
        return iconResId;
    }
}
