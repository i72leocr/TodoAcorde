package com.todoacorde.todoacorde.achievements.domain.usecase;

import com.todoacorde.todoacorde.AppExecutors;
import com.todoacorde.todoacorde.SessionManager;
import com.todoacorde.todoacorde.achievements.data.AchievementDao;
import com.todoacorde.todoacorde.achievements.data.AchievementDefinitionDao;
import com.todoacorde.todoacorde.practice.data.PracticeDetailDao;
import com.todoacorde.todoacorde.practice.data.PracticeSessionDao;
import com.todoacorde.todoacorde.practice.data.SongUserSpeedDao;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;

/**
 * Módulo de inyección de dependencias (Hilt) para registrar y proveer
 * los casos de uso de evaluación de logros.
 *
 * <p>Los {@code @Provides} crean instancias de los {@code UseCase} con sus
 * dependencias y las registran en {@link AchievementUseCaseRegistry} de modo
 * que {@code AchievementRepositoryImpl} pueda invocarlos cuando corresponda.</p>
 */
@Module
@InstallIn(SingletonComponent.class)
public class AchievementUseCaseModule {

    /**
     * Provee el caso de uso que evalúa logros basados en acordes únicos practicados.
     * Registra la instancia en el {@link AchievementUseCaseRegistry}.
     *
     * @param achievementDao   DAO de estados/progresos de logros.
     * @param appExecutors     ejecutores para trabajo en hilos auxiliares.
     * @param practiceDetailDao DAO para detalles de práctica (fuente de métricas).
     * @param definitionDao    DAO de definiciones de logros.
     * @return instancia singleton de {@link EvaluateUniqueChordsAchievementUseCase}.
     */
    @Provides
    @Singleton
    public EvaluateUniqueChordsAchievementUseCase provideUniqueChordsUseCase(
            AchievementDao achievementDao,
            AppExecutors appExecutors,
            PracticeDetailDao practiceDetailDao,
            AchievementDefinitionDao definitionDao
    ) {
        EvaluateUniqueChordsAchievementUseCase useCase =
                new EvaluateUniqueChordsAchievementUseCase(
                        achievementDao,
                        appExecutors,
                        practiceDetailDao,
                        definitionDao
                );
        AchievementUseCaseRegistry.register(useCase);
        return useCase;
    }

    /**
     * Provee el caso de uso que evalúa logros de desbloqueo por velocidad alcanzada.
     * Registra la instancia en el {@link AchievementUseCaseRegistry}.
     *
     * @param achievementDao    DAO de estados/progresos de logros.
     * @param appExecutors      ejecutores para trabajo en hilos auxiliares.
     * @param songUserSpeedDao  DAO con velocidades alcanzadas por el usuario.
     * @param sessionManager    gestor de sesión para contexto de usuario.
     * @param definitionDao     DAO de definiciones de logros.
     * @return instancia singleton de {@link EvaluateSpeedUnlockAchievementUseCase}.
     */
    @Provides
    @Singleton
    public EvaluateSpeedUnlockAchievementUseCase provideSpeedUnlockUseCase(
            AchievementDao achievementDao,
            AppExecutors appExecutors,
            SongUserSpeedDao songUserSpeedDao,
            SessionManager sessionManager,
            AchievementDefinitionDao definitionDao
    ) {
        EvaluateSpeedUnlockAchievementUseCase useCase =
                new EvaluateSpeedUnlockAchievementUseCase(
                        achievementDao,
                        appExecutors,
                        songUserSpeedDao,
                        sessionManager,
                        definitionDao
                );
        AchievementUseCaseRegistry.register(useCase);
        return useCase;
    }

    /**
     * Provee el caso de uso que evalúa logros de puntuación perfecta en sesiones de práctica.
     * Registra la instancia en el {@link AchievementUseCaseRegistry}.
     *
     * @param achievementDao DAO de estados/progresos de logros.
     * @param appExecutors   ejecutores para trabajo en hilos auxiliares.
     * @param sessionDao     DAO de sesiones de práctica (fuente de puntuaciones).
     * @param sessionManager gestor de sesión para contexto de usuario.
     * @param definitionDao  DAO de definiciones de logros.
     * @return instancia singleton de {@link EvaluatePerfectScoreAchievementUseCase}.
     */
    @Provides
    @Singleton
    public EvaluatePerfectScoreAchievementUseCase providePerfectScoreUseCase(
            AchievementDao achievementDao,
            AppExecutors appExecutors,
            PracticeSessionDao sessionDao,
            SessionManager sessionManager,
            AchievementDefinitionDao definitionDao
    ) {
        EvaluatePerfectScoreAchievementUseCase useCase =
                new EvaluatePerfectScoreAchievementUseCase(
                        achievementDao,
                        appExecutors,
                        sessionDao,
                        sessionManager,
                        definitionDao
                );
        AchievementUseCaseRegistry.register(useCase);
        return useCase;
    }
}
