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

// ✅ DAO existente de patrones (renderizado de cajas)
import com.tuguitar.todoacorde.scales.data.ScalePatternDao;

// ✅ NUEVO: DAOs de progresión de escalas
import com.tuguitar.todoacorde.scales.data.dao.ScaleDao;
import com.tuguitar.todoacorde.scales.data.dao.ScaleBoxDao;
import com.tuguitar.todoacorde.scales.data.dao.TonalityDao;
import com.tuguitar.todoacorde.scales.data.dao.UserScaleCompletionDao;
import com.tuguitar.todoacorde.scales.data.dao.ProgressionDao;

// ✅ NUEVO: Repositorio de progresión (opcional para inyección directa)
import com.tuguitar.todoacorde.scales.domain.repository.ProgressionRepository;

// ✅ IMPORTA Executor para IO
import java.util.concurrent.Executor;

@Module
@InstallIn(SingletonComponent.class)
public class DatabaseModule {

    @Provides
    @Singleton
    static todoAcordeDatabase provideDatabase(Application app) {
        // Usa TU singleton ya configurado (con callbacks/seed internos)
        return todoAcordeDatabase.getInstance(app);
    }

    // ✅ Executor para IO → reutiliza el pool de la BD (usable en ViewModels)
    @Provides
    @Singleton
    static Executor provideIoExecutor(todoAcordeDatabase db) {
        return todoAcordeDatabase.databaseWriteExecutor;
    }

    // --- DAOs existentes ---
    @Provides @Singleton static SongDao provideSongDao(todoAcordeDatabase db)                 { return db.songDao(); }
    @Provides @Singleton static ChordDao provideChordDao(todoAcordeDatabase db)               { return db.chordDao(); }
    @Provides @Singleton static SongChordDao provideSongChordDao(todoAcordeDatabase db)       { return db.songChordDao(); }
    @Provides @Singleton static SongLyricDao provideSongLyricDao(todoAcordeDatabase db)       { return db.songLyricDao(); }
    @Provides @Singleton static PracticeSessionDao providePracticeSessionDao(todoAcordeDatabase db) { return db.practiceSessionDao(); }
    @Provides @Singleton static PracticeDetailDao providePracticeDetailDao(todoAcordeDatabase db)   { return db.practiceDetailDao(); }
    @Provides @Singleton static SongUserSpeedDao provideSongUserSpeedDao(todoAcordeDatabase db)     { return db.songUserSpeedDao(); }
    @Provides @Singleton static FavoriteSongDao provideFavoriteSongDao(todoAcordeDatabase db)       { return db.favoriteSongDao(); }
    @Provides @Singleton static UserDao provideUserDao(todoAcordeDatabase db)                       { return db.userDao(); }
    @Provides @Singleton static DifficultyDao provideDifficultyDao(todoAcordeDatabase db)           { return db.difficultyDao(); }
    @Provides @Singleton static ChordTypeDao provideChordTypeDao(todoAcordeDatabase db)             { return db.chordTypeDao(); }
    @Provides @Singleton static AchievementDefinitionDao provideAchievementDefinitionDao(todoAcordeDatabase db) { return db.achievementDefinitionDao(); }
    @Provides @Singleton static AchievementDao provideAchievementDao(todoAcordeDatabase db)         { return db.achievementDao(); }

    // ✅ DAO de patrones de escala (render)
    @Provides @Singleton static ScalePatternDao provideScalePatternDao(todoAcordeDatabase db)       { return db.scalePatternDao(); }

    // ✅ DAOs de progresión (desbloqueo)
    @Provides @Singleton static ScaleDao provideScaleDao(todoAcordeDatabase db)                     { return db.scaleDao(); }
    @Provides @Singleton static ScaleBoxDao provideScaleBoxDao(todoAcordeDatabase db)               { return db.scaleBoxDao(); }
    @Provides @Singleton static TonalityDao provideTonalityDao(todoAcordeDatabase db)               { return db.tonalityDao(); }
    @Provides @Singleton static UserScaleCompletionDao provideUserScaleCompletionDao(todoAcordeDatabase db) { return db.userScaleCompletionDao(); }
    @Provides @Singleton static ProgressionDao provideProgressionDao(todoAcordeDatabase db)         { return db.progressionDao(); }

    // ✅ Repositorio de progresión (por si quieres inyectarlo en Fragment/ViewModel)
    @Provides @Singleton
    static ProgressionRepository provideProgressionRepository(
            ScaleDao scaleDao,
            ScaleBoxDao scaleBoxDao,
            TonalityDao tonalityDao,
            UserScaleCompletionDao completionDao,
            ProgressionDao progressionDao
    ) {
        return new ProgressionRepository(scaleDao, scaleBoxDao, tonalityDao, completionDao, progressionDao);
    }
}
