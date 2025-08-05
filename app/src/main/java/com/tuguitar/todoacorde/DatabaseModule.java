package com.tuguitar.todoacorde;

import android.app.Application;

import androidx.room.Room;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;


@Module
@InstallIn(SingletonComponent.class)
public class DatabaseModule {

    @Provides @Singleton
    static todoAcordeDatabase provideDatabase(Application app) {
        return Room.databaseBuilder(app, todoAcordeDatabase.class, "todoacorde_database")
                .fallbackToDestructiveMigration()
                .build();
    }

    @Provides static SongDao provideSongDao(todoAcordeDatabase db) {
        return db.songDao();
    }

    @Provides static ChordDao provideChordDao(todoAcordeDatabase db) {
        return db.chordDao();
    }

    @Provides static SongChordDao provideSongChordDao(todoAcordeDatabase db) {
        return db.songChordDao();
    }

    @Provides static SongLyricDao provideSongLyricDao(todoAcordeDatabase db) {
        return db.songLyricDao();
    }

    @Provides static ProgressionDao provideProgressionDao(todoAcordeDatabase db) {
        return db.progressionDao();
    }

    @Provides static ProgressionChordDao provideProgressionChordDao(todoAcordeDatabase db) {
        return db.progressionChordDao();
    }

    @Provides static ProgressionDetailDao provideProgressionDetailDao(todoAcordeDatabase db) {
        return db.progressionDetailDao();
    }

    @Provides static ProgressionSessionDao provideProgressionSessionDao(todoAcordeDatabase db) {
        return db.progressionSessionDao();
    }

    @Provides static PracticeSessionDao providePracticeSessionDao(todoAcordeDatabase db) {
        return db.practiceSessionDao();
    }

    @Provides static PracticeDetailDao providePracticeDetailDao(todoAcordeDatabase db) {
        return db.practiceDetailDao();
    }

    @Provides static SongUserSpeedDao provideSongUserSpeedDao(todoAcordeDatabase db) {
        return db.songUserSpeedDao();
    }

    @Provides static FavoriteSongDao provideFavoriteSongDao(todoAcordeDatabase db) {
        return db.favoriteSongDao();
    }

    @Provides static UserDao provideUserDao(todoAcordeDatabase db) {
        return db.userDao();
    }

    @Provides static DifficultyDao provideDifficultyDao(todoAcordeDatabase db) {
        return db.difficultyDao();
    }

    @Provides static ChordTypeDao provideChordTypeDao(todoAcordeDatabase db) {
        return db.chordTypeDao();
    }

    // DAO de definiciones de logro (no cambia)
    @Provides static AchievementDefinitionDao provideAchievementDefinitionDao(todoAcordeDatabase db) {
        return db.achievementDefinitionDao();
    }

    // Nuevo DAO de logros: insert/update con @Upsert
    @Provides static AchievementDao provideAchievementDao(todoAcordeDatabase db) {
        return db.achievementDao();
    }
}
