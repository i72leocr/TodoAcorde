package com.tuguitar.todoacorde;

import android.app.Application;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;

import com.tuguitar.todoacorde.achievements.data.AchievementDao;
import com.tuguitar.todoacorde.achievements.data.AchievementDefinitionDao;
import com.tuguitar.todoacorde.practice.data.PracticeDetailDao;
import com.tuguitar.todoacorde.practice.data.PracticeSessionDao;
import com.tuguitar.todoacorde.practice.data.SongUserSpeedDao;
import com.tuguitar.todoacorde.songs.data.FavoriteSongDao;
import com.tuguitar.todoacorde.songs.data.SongChordDao;
import com.tuguitar.todoacorde.songs.data.SongDao;
import com.tuguitar.todoacorde.songs.data.SongLyricDao;

// ✅ IMPORTA el DAO nuevo de escalas
import com.tuguitar.todoacorde.scales.data.ScalePatternDao;

// ✅ IMPORTA Executor
import java.util.concurrent.Executor;

@Module
@InstallIn(SingletonComponent.class)
public class DatabaseModule {

    @Provides
    @Singleton
    static todoAcordeDatabase provideDatabase(Application app) {
        // ✅ Usa TU singleton ya configurado (con sus callbacks/seed internos)
        return todoAcordeDatabase.getInstance(app);
    }

    // ✅ Nuevo: Executor para IO (sin @Named) → lo usará el ViewModel
    @Provides
    @Singleton
    static Executor provideIoExecutor(todoAcordeDatabase db) {
        // Reutiliza el pool que ya tienes en la BD
        return todoAcordeDatabase.databaseWriteExecutor;
    }

    // --- DAOs existentes ---
    @Provides @Singleton static SongDao provideSongDao(todoAcordeDatabase db)           { return db.songDao(); }
    @Provides @Singleton static ChordDao provideChordDao(todoAcordeDatabase db)         { return db.chordDao(); }
    @Provides @Singleton static SongChordDao provideSongChordDao(todoAcordeDatabase db) { return db.songChordDao(); }
    @Provides @Singleton static SongLyricDao provideSongLyricDao(todoAcordeDatabase db) { return db.songLyricDao(); }
    @Provides @Singleton static PracticeSessionDao providePracticeSessionDao(todoAcordeDatabase db)       { return db.practiceSessionDao(); }
    @Provides @Singleton static PracticeDetailDao providePracticeDetailDao(todoAcordeDatabase db)         { return db.practiceDetailDao(); }
    @Provides @Singleton static SongUserSpeedDao provideSongUserSpeedDao(todoAcordeDatabase db)           { return db.songUserSpeedDao(); }
    @Provides @Singleton static FavoriteSongDao provideFavoriteSongDao(todoAcordeDatabase db)             { return db.favoriteSongDao(); }
    @Provides @Singleton static UserDao provideUserDao(todoAcordeDatabase db)                             { return db.userDao(); }
    @Provides @Singleton static DifficultyDao provideDifficultyDao(todoAcordeDatabase db)                 { return db.difficultyDao(); }
    @Provides @Singleton static ChordTypeDao provideChordTypeDao(todoAcordeDatabase db)                   { return db.chordTypeDao(); }
    @Provides @Singleton static AchievementDefinitionDao provideAchievementDefinitionDao(todoAcordeDatabase db) { return db.achievementDefinitionDao(); }
    @Provides @Singleton static AchievementDao provideAchievementDao(todoAcordeDatabase db)               { return db.achievementDao(); }

    // ✅ Nuevo: DAO de patrones de escala
    @Provides @Singleton static ScalePatternDao provideScalePatternDao(todoAcordeDatabase db)             { return db.scalePatternDao(); }
}
