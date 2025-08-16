package com.tuguitar.todoacorde.achievements.domain.usecase;

import com.tuguitar.todoacorde.EvaluateAchievementUseCase;

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
        if (useCase != null) {
            registered.add(useCase);
        }
    }

    public static List<EvaluateAchievementUseCase> getAll() {
        return Collections.unmodifiableList(registered);
    }
}
