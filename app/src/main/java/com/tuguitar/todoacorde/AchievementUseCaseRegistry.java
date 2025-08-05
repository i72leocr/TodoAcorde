package com.tuguitar.todoacorde;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Registro central de UseCases que se evalúan al llamar evaluateAll().
 * Los UseCase se registran desde Hilt (en AchievementUseCaseModule).
 */
public class AchievementUseCaseRegistry {

    private static final List<EvaluateAchievementUseCase> registered = new ArrayList<>();

    public static void register(EvaluateAchievementUseCase useCase) {
        registered.add(useCase);
    }

    public static List<EvaluateAchievementUseCase> getAll() {
        return Collections.unmodifiableList(registered);
    }
}