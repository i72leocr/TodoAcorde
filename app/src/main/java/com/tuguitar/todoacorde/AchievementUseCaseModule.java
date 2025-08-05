package com.tuguitar.todoacorde;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;

@Module
@InstallIn(SingletonComponent.class)
public class AchievementUseCaseModule {

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
