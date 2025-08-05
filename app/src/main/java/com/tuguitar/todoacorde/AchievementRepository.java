package com.tuguitar.todoacorde;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import com.tuguitar.todoacorde.Achievement;
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

    void evaluateAll();
    void updateAchievements(@NonNull List<Achievement> achievements);



    /**
     * Persiste un nuevo estado de un logro concreto (instancia de dominio ya actualizada
     * usando {@link Achievement#withAddedProgress(int)}).
     */
    void updateAchievement(Achievement achievement);

    /**
     * Devuelve el progreso actual (0…threshold) para un nivel concreto de una familia de logros.
     *
     * @param familyTitle Título de la familia de logro
     * @param level Nivel (BRONZE, SILVER, GOLD)
     * @return el progreso actual, o 0 si aún no existe registro
     */
    int getProgress(String familyTitle, Achievement.Level level);
}
