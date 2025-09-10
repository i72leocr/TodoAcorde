package com.todoacorde.todoacorde.achievements.data;

import androidx.annotation.NonNull;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Agrupación inmutable de logros pertenecientes a una misma familia temática.
 *
 * Una familia contiene un título, una descripción y una lista inmutable de
 * {@link Achievement} que representan los distintos niveles o etapas.
 * Se validan las precondiciones mínimas: título no vacío y lista de niveles no vacía.
 */
public final class AchievementFamily {

    /** Título de la familia de logros. Debe ser no vacío. */
    private final @NonNull String familyTitle;

    /** Descripción corta de la familia de logros. */
    private final @NonNull String description;

    /** Lista inmutable de logros que componen la familia. Debe contener al menos un elemento. */
    private final @NonNull List<Achievement> levels;

    /**
     * Crea una familia de logros validando precondiciones de dominio.
     *
     * @param familyTitle título de la familia; no puede ser vacío (tras {@code trim()}).
     * @param description descripción de la familia; no nula.
     * @param levels      lista de logros de la familia; no nula ni vacía. Se almacena como lista inmutable.
     * @throws IllegalArgumentException si {@code familyTitle} es vacío o {@code levels} está vacía.
     * @throws NullPointerException     si {@code levels} es null.
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
        this.levels = Collections.unmodifiableList(levels);
    }

    /** @return título de la familia de logros. */
    @NonNull
    public String getFamilyTitle() {
        return familyTitle;
    }

    /** @return descripción de la familia de logros. */
    @NonNull
    public String getDescription() {
        return description;
    }

    /**
     * Devuelve la lista inmutable de logros que componen la familia.
     * Las modificaciones externas de la lista original no afectan al estado interno.
     *
     * @return lista inmutable de logros.
     */
    @NonNull
    public List<Achievement> getLevels() {
        return levels;
    }

    /**
     * Igualdad estructural basada en título, descripción y lista de niveles.
     *
     * @param o objeto a comparar.
     * @return {@code true} si ambos objetos son equivalentes; en caso contrario {@code false}.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AchievementFamily)) return false;
        AchievementFamily that = (AchievementFamily) o;
        return familyTitle.equals(that.familyTitle)
                && description.equals(that.description)
                && levels.equals(that.levels);
    }

    /**
     * Hash consistente con {@link #equals(Object)}.
     *
     * @return valor hash calculado a partir de los campos significativos.
     */
    @Override
    public int hashCode() {
        return Objects.hash(familyTitle, description, levels);
    }

    /**
     * Representación textual con los campos principales para depuración.
     *
     * @return cadena con título, descripción y niveles.
     */
    @Override
    public String toString() {
        return "AchievementFamily{"
                + "familyTitle='" + familyTitle + '\''
                + ", description='" + description + '\''
                + ", levels=" + levels
                + '}';
    }

    /**
     * Conjunto de títulos predefinidos para familias de dominio "maestría de escalas".
     * Clase utilitaria no instanciable.
     */
    public static final class ScaleMasteryTitles {
        private ScaleMasteryTitles() { }

        public static final String EASY_PER_TONALITY = "Escalas fáciles (por tonalidad)";
        public static final String MEDIUM_PER_TONALITY = "Escalas medias (por tonalidad)";
        public static final String HARD_PER_TONALITY = "Escalas difíciles (por tonalidad)";
        public static final String EASY_ALL_TONALITIES = "Escalas fáciles (todas las tonalidades)";
        public static final String MEDIUM_ALL_TONALITIES = "Escalas medias (todas las tonalidades)";
        public static final String HARD_ALL_TONALITIES = "Escalas difíciles (todas las tonalidades)";
    }

    /**
     * Conjunto de descripciones predefinidas para familias de dominio "maestría de escalas".
     * Clase utilitaria no instanciable.
     */
    public static final class ScaleMasteryDescriptions {
        private ScaleMasteryDescriptions() { }

        public static final String EASY_PER_TONALITY =
                "Completa una escala de dificultad fácil (todas sus cajas) en una tonalidad.";
        public static final String MEDIUM_PER_TONALITY =
                "Completa una escala de dificultad media (todas sus cajas) en una tonalidad.";
        public static final String HARD_PER_TONALITY =
                "Completa una escala de dificultad difícil (todas sus cajas) en una tonalidad.";
        public static final String EASY_ALL_TONALITIES =
                "Completa una escala de dificultad fácil (todas sus cajas) en todas las tonalidades.";
        public static final String MEDIUM_ALL_TONALITIES =
                "Completa una escala de dificultad media (todas sus cajas) en todas las tonalidades.";
        public static final String HARD_ALL_TONALITIES =
                "Completa una escala de dificultad difícil (todas sus cajas) en todas las tonalidades.";
    }

    /**
     * Fábrica de conveniencia para crear la familia "Escalas fáciles (por tonalidad)".
     *
     * @param levels lista de logros que conforman los niveles de la familia.
     * @return instancia de {@link AchievementFamily} con título y descripción predefinidos.
     */
    public static AchievementFamily scalesEasyPerTonality(@NonNull List<Achievement> levels) {
        return new AchievementFamily(
                ScaleMasteryTitles.EASY_PER_TONALITY,
                ScaleMasteryDescriptions.EASY_PER_TONALITY,
                levels
        );
    }

    /**
     * Fábrica de conveniencia para crear la familia "Escalas medias (por tonalidad)".
     *
     * @param levels lista de logros que conforman los niveles de la familia.
     * @return instancia de {@link AchievementFamily} con título y descripción predefinidos.
     */
    public static AchievementFamily scalesMediumPerTonality(@NonNull List<Achievement> levels) {
        return new AchievementFamily(
                ScaleMasteryTitles.MEDIUM_PER_TONALITY,
                ScaleMasteryDescriptions.MEDIUM_PER_TONALITY,
                levels
        );
    }

    /**
     * Fábrica de conveniencia para crear la familia "Escalas difíciles (por tonalidad)".
     *
     * @param levels lista de logros que conforman los niveles de la familia.
     * @return instancia de {@link AchievementFamily} con título y descripción predefinidos.
     */
    public static AchievementFamily scalesHardPerTonality(@NonNull List<Achievement> levels) {
        return new AchievementFamily(
                ScaleMasteryTitles.HARD_PER_TONALITY,
                ScaleMasteryDescriptions.HARD_PER_TONALITY,
                levels
        );
    }

    /**
     * Fábrica de conveniencia para crear la familia "Escalas fáciles (todas las tonalidades)".
     *
     * @param levels lista de logros que conforman los niveles de la familia.
     * @return instancia de {@link AchievementFamily} con título y descripción predefinidos.
     */
    public static AchievementFamily scalesEasyAllTonalities(@NonNull List<Achievement> levels) {
        return new AchievementFamily(
                ScaleMasteryTitles.EASY_ALL_TONALITIES,
                ScaleMasteryDescriptions.EASY_ALL_TONALITIES,
                levels
        );
    }

    /**
     * Fábrica de conveniencia para crear la familia "Escalas medias (todas las tonalidades)".
     *
     * @param levels lista de logros que conforman los niveles de la familia.
     * @return instancia de {@link AchievementFamily} con título y descripción predefinidos.
     */
    public static AchievementFamily scalesMediumAllTonalities(@NonNull List<Achievement> levels) {
        return new AchievementFamily(
                ScaleMasteryTitles.MEDIUM_ALL_TONALITIES,
                ScaleMasteryDescriptions.MEDIUM_ALL_TONALITIES,
                levels
        );
    }

    /**
     * Fábrica de conveniencia para crear la familia "Escalas difíciles (todas las tonalidades)".
     *
     * @param levels lista de logros que conforman los niveles de la familia.
     * @return instancia de {@link AchievementFamily} con título y descripción predefinidos.
     */
    public static AchievementFamily scalesHardAllTonalities(@NonNull List<Achievement> levels) {
        return new AchievementFamily(
                ScaleMasteryTitles.HARD_ALL_TONALITIES,
                ScaleMasteryDescriptions.HARD_ALL_TONALITIES,
                levels
        );
    }
}
