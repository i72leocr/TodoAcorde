package com.tuguitar.todoacorde;

import javax.inject.Singleton;
import dagger.Binds;
import dagger.Module;
import dagger.hilt.InstallIn;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import dagger.hilt.components.SingletonComponent;
import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;

@Module
@InstallIn(SingletonComponent.class)
public abstract class DataBindingsModule {

    @Binds
    @Singleton
    public abstract IChordDetector bindChordDetector(ChordDetectorImpl impl);

    @Binds
    @Singleton
    public abstract AchievementRepository bindAchievementRepository(
            AchievementRepositoryImpl impl
    );
    @Provides
    @Singleton
    public static AppExecutors provideAppExecutors() {
        return AppExecutors.getInstance();
    }



    // Aquí podrías añadir más binds para otros repositorios o casos de uso
}
