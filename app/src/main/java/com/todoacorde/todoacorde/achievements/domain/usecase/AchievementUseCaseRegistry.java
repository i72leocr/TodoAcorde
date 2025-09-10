package com.todoacorde.todoacorde.achievements.domain.usecase;

import com.todoacorde.todoacorde.EvaluateAchievementUseCase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Registro estático de casos de uso de evaluación de logros.
 *
 * Mantiene una colección en memoria de instancias {@link EvaluateAchievementUseCase}
 * que pueden ser recuperadas por otros componentes (por ejemplo, el repositorio).
 * Nota: la implementación no es thread-safe; si se registran casos de uso desde
 * varios hilos, se debería sincronizar el acceso.
 */
public class AchievementUseCaseRegistry {

    /** Lista de casos de uso registrados en memoria. */
    private static final List<EvaluateAchievementUseCase> registered = new ArrayList<>();

    /**
     * Registra un caso de uso para su posterior evaluación.
     *
     * @param useCase instancia a registrar; si es {@code null}, no se añade.
     */
    public static void register(EvaluateAchievementUseCase useCase) {
        if (useCase != null) {
            registered.add(useCase);
        }
    }

    /**
     * Devuelve una vista inmutable de todos los casos de uso registrados.
     *
     * @return lista inmodificable de {@link EvaluateAchievementUseCase}.
     */
    public static List<EvaluateAchievementUseCase> getAll() {
        return Collections.unmodifiableList(registered);
    }
}
