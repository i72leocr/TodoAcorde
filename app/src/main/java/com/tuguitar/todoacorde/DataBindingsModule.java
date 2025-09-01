package com.tuguitar.todoacorde;

import com.tuguitar.todoacorde.achievements.data.AchievementRepository;
import com.tuguitar.todoacorde.achievements.data.AchievementRepositoryImpl;

import javax.inject.Singleton;
import dagger.Binds;
import dagger.Module;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;
import dagger.Provides;

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
}
