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

    public static final class ScaleMasteryTitles {
        private ScaleMasteryTitles() {}
        public static final String EASY_PER_TONALITY    = "Escalas fáciles (por tonalidad)";
        public static final String MEDIUM_PER_TONALITY  = "Escalas medias (por tonalidad)";
        public static final String HARD_PER_TONALITY    = "Escalas difíciles (por tonalidad)";
        public static final String EASY_ALL_TONALITIES   = "Escalas fáciles (todas las tonalidades)";
        public static final String MEDIUM_ALL_TONALITIES = "Escalas medias (todas las tonalidades)";
        public static final String HARD_ALL_TONALITIES   = "Escalas difíciles (todas las tonalidades)";
    }

    public static final class ScaleMasteryDescriptions {
        private ScaleMasteryDescriptions() {}
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

    /** Escalas fáciles por tonalidad. */
    public static AchievementFamily scalesEasyPerTonality(@NonNull List<Achievement> levels) {
        return new AchievementFamily(
                ScaleMasteryTitles.EASY_PER_TONALITY,
                ScaleMasteryDescriptions.EASY_PER_TONALITY,
                levels
        );
    }

    /** Escalas medias por tonalidad. */
    public static AchievementFamily scalesMediumPerTonality(@NonNull List<Achievement> levels) {
        return new AchievementFamily(
                ScaleMasteryTitles.MEDIUM_PER_TONALITY,
                ScaleMasteryDescriptions.MEDIUM_PER_TONALITY,
                levels
        );
    }

    /** Escalas difíciles por tonalidad. */
    public static AchievementFamily scalesHardPerTonality(@NonNull List<Achievement> levels) {
        return new AchievementFamily(
                ScaleMasteryTitles.HARD_PER_TONALITY,
                ScaleMasteryDescriptions.HARD_PER_TONALITY,
                levels
        );
    }

    /** Escalas fáciles en todas las tonalidades. */
    public static AchievementFamily scalesEasyAllTonalities(@NonNull List<Achievement> levels) {
        return new AchievementFamily(
                ScaleMasteryTitles.EASY_ALL_TONALITIES,
                ScaleMasteryDescriptions.EASY_ALL_TONALITIES,
                levels
        );
    }

    /** Escalas medias en todas las tonalidades. */
    public static AchievementFamily scalesMediumAllTonalities(@NonNull List<Achievement> levels) {
        return new AchievementFamily(
                ScaleMasteryTitles.MEDIUM_ALL_TONALITIES,
                ScaleMasteryDescriptions.MEDIUM_ALL_TONALITIES,
                levels
        );
    }

    /** Escalas difíciles en todas las tonalidades. */
    public static AchievementFamily scalesHardAllTonalities(@NonNull List<Achievement> levels) {
        return new AchievementFamily(
                ScaleMasteryTitles.HARD_ALL_TONALITIES,
                ScaleMasteryDescriptions.HARD_ALL_TONALITIES,
                levels
        );
    }
}
