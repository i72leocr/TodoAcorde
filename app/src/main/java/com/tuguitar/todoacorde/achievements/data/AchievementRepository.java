package com.tuguitar.todoacorde.achievements.data;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;

import java.util.List;

/**
 * Repositorio para exponer y actualizar el estado de los logros
 * usando el modelo de dominio {@link Achievement}.
 */
public interface AchievementRepository {
    /**
     * Observa el listado completo de {@link Achievement} (todos los niveles de todas las familias),
     * en orden por familyId y nivel.
     */
    LiveData<List<Achievement>> observeAll();

    /** Fuerza evaluación de todos los logros registrados en el sistema. */
    void evaluateAll();

    /** Reemplaza el estado de un conjunto de logros. */
    void updateAchievements(@NonNull List<Achievement> achievements);

    /**
     * Persiste un nuevo estado de un logro concreto (instancia de dominio ya actualizada
     * usando {@link Achievement#withAddedProgress(int)}).
     */
    void updateAchievement(@NonNull Achievement achievement);

    /**
     * Devuelve el progreso actual (0…threshold) para un nivel concreto de una familia de logros.
     *
     * @param familyTitle Título legible de la familia de logros (p.ej., "Escalas fáciles")
     * @param level       Nivel (BRONZE, SILVER, GOLD)
     * @return el progreso actual, o 0 si aún no existe registro
     */
    int getProgress(@NonNull String familyTitle, @NonNull Achievement.Level level);

    /**
     * Incrementa el progreso del logro identificado por código.
     * Si el logro no existe aún en almacenamiento, la implementación debe crearlo con progreso 0 y aplicar el incremento.
     *
     * Este método es usado por EvaluateScaleMasteryAchievementUseCase para:
     *  - SCALES_EASY_PER_TONALITY / _ALL_TONALITIES
     *  - SCALES_MEDIUM_PER_TONALITY / _ALL_TONALITIES
     *  - SCALES_HARD_PER_TONALITY / _ALL_TONALITIES
     *
     * @param code  identificador único del logro (sembrado en BD por el seeder)
     * @param delta incremento (positivo) a aplicar
     */
    void incrementProgress(@NonNull String code, int delta);
}
