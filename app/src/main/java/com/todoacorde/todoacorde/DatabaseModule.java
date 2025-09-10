package com.todoacorde.todoacorde;

import android.app.Application;

import com.todoacorde.todoacorde.achievements.data.AchievementDao;
import com.todoacorde.todoacorde.achievements.data.AchievementDefinitionDao;
import com.todoacorde.todoacorde.practice.data.PracticeDetailDao;
import com.todoacorde.todoacorde.practice.data.PracticeSessionDao;
import com.todoacorde.todoacorde.practice.data.SongUserSpeedDao;
import com.todoacorde.todoacorde.scales.data.ScalePatternDao;
import com.todoacorde.todoacorde.scales.data.dao.ProgressionDao;
import com.todoacorde.todoacorde.scales.data.dao.ScaleBoxDao;
import com.todoacorde.todoacorde.scales.data.dao.ScaleDao;
import com.todoacorde.todoacorde.scales.data.dao.TonalityDao;
import com.todoacorde.todoacorde.scales.data.dao.UserScaleCompletionDao;
import com.todoacorde.todoacorde.scales.domain.repository.ProgressionRepository;
import com.todoacorde.todoacorde.songs.data.FavoriteSongDao;
import com.todoacorde.todoacorde.songs.data.SongChordDao;
import com.todoacorde.todoacorde.songs.data.SongDao;
import com.todoacorde.todoacorde.songs.data.SongLyricDao;

import java.util.concurrent.Executor;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;

/**
 * Módulo de Dagger/Hilt responsable de proveer la instancia de la base de datos Room
 * y de exponer los DAOs y repositorios asociados como dependencias @Singleton.
 *
 * <p>Se instala en {@link SingletonComponent}, por lo que las instancias provistas
 * tienen ciclo de vida de aplicación.</p>
 */
@Module
@InstallIn(SingletonComponent.class)
public class DatabaseModule {

    /**
     * Proporciona la instancia singleton de la base de datos Room.
     *
     * @param app {@link Application} para obtener el contexto de aplicación.
     * @return instancia única de {@link todoAcordeDatabase}.
     */
    @Provides
    @Singleton
    static todoAcordeDatabase provideDatabase(Application app) {
        return todoAcordeDatabase.getInstance(app);
    }

    /**
     * Proporciona un {@link Executor} de IO compartido por la base de datos.
     *
     * @param db base de datos principal.
     * @return ejecutor de escritura/IO asociado a la BD.
     */
    @Provides
    @Singleton
    static Executor provideIoExecutor(todoAcordeDatabase db) {
        return todoAcordeDatabase.databaseWriteExecutor;
    }

    /**
     * DAO de canciones.
     */
    @Provides
    @Singleton
    static SongDao provideSongDao(todoAcordeDatabase db) {
        return db.songDao();
    }

    /**
     * DAO de acordes.
     */
    @Provides
    @Singleton
    static ChordDao provideChordDao(todoAcordeDatabase db) {
        return db.chordDao();
    }

    /**
     * DAO de relaciones canción–acorde.
     */
    @Provides
    @Singleton
    static SongChordDao provideSongChordDao(todoAcordeDatabase db) {
        return db.songChordDao();
    }

    /**
     * DAO de letras/versos de canciones.
     */
    @Provides
    @Singleton
    static SongLyricDao provideSongLyricDao(todoAcordeDatabase db) {
        return db.songLyricDao();
    }

    /**
     * DAO de sesiones de práctica.
     */
    @Provides
    @Singleton
    static PracticeSessionDao providePracticeSessionDao(todoAcordeDatabase db) {
        return db.practiceSessionDao();
    }

    /**
     * DAO de detalles de práctica.
     */
    @Provides
    @Singleton
    static PracticeDetailDao providePracticeDetailDao(todoAcordeDatabase db) {
        return db.practiceDetailDao();
    }

    /**
     * DAO de velocidades por usuario/canción.
     */
    @Provides
    @Singleton
    static SongUserSpeedDao provideSongUserSpeedDao(todoAcordeDatabase db) {
        return db.songUserSpeedDao();
    }

    /**
     * DAO de favoritos de canciones.
     */
    @Provides
    @Singleton
    static FavoriteSongDao provideFavoriteSongDao(todoAcordeDatabase db) {
        return db.favoriteSongDao();
    }

    /**
     * DAO de usuarios.
     */
    @Provides
    @Singleton
    static UserDao provideUserDao(todoAcordeDatabase db) {
        return db.userDao();
    }

    /**
     * DAO de niveles de dificultad.
     */
    @Provides
    @Singleton
    static DifficultyDao provideDifficultyDao(todoAcordeDatabase db) {
        return db.difficultyDao();
    }

    /**
     * DAO de tipos de acorde.
     */
    @Provides
    @Singleton
    static ChordTypeDao provideChordTypeDao(todoAcordeDatabase db) {
        return db.chordTypeDao();
    }

    /**
     * DAO de definiciones de logros.
     */
    @Provides
    @Singleton
    static AchievementDefinitionDao provideAchievementDefinitionDao(todoAcordeDatabase db) {
        return db.achievementDefinitionDao();
    }

    /**
     * DAO de logros alcanzados.
     */
    @Provides
    @Singleton
    static AchievementDao provideAchievementDao(todoAcordeDatabase db) {
        return db.achievementDao();
    }

    /**
     * DAO de patrones de escalas (diagramas/variantes).
     */
    @Provides
    @Singleton
    static ScalePatternDao provideScalePatternDao(todoAcordeDatabase db) {
        return db.scalePatternDao();
    }

    /**
     * DAO de escalas.
     */
    @Provides
    @Singleton
    static ScaleDao provideScaleDao(todoAcordeDatabase db) {
        return db.scaleDao();
    }

    /**
     * DAO de cajas/posiciones de escalas.
     */
    @Provides
    @Singleton
    static ScaleBoxDao provideScaleBoxDao(todoAcordeDatabase db) {
        return db.scaleBoxDao();
    }

    /**
     * DAO de tonalidades.
     */
    @Provides
    @Singleton
    static TonalityDao provideTonalityDao(todoAcordeDatabase db) {
        return db.tonalityDao();
    }

    /**
     * DAO de completado de escalas por usuario.
     */
    @Provides
    @Singleton
    static UserScaleCompletionDao provideUserScaleCompletionDao(todoAcordeDatabase db) {
        return db.userScaleCompletionDao();
    }

    /**
     * DAO de consultas de progresión (agregados/consultas complejas).
     */
    @Provides
    @Singleton
    static ProgressionDao provideProgressionDao(todoAcordeDatabase db) {
        return db.progressionDao();
    }

    /**
     * Repositorio de progresión de escalas que orquesta varios DAOs.
     *
     * @param scaleDao        acceso a escalas.
     * @param scaleBoxDao     acceso a cajas de escala.
     * @param tonalityDao     acceso a tonalidades.
     * @param completionDao   acceso a completados por usuario.
     * @param progressionDao  consultas agregadas de progresión.
     * @return instancia singleton de {@link ProgressionRepository}.
     */
    @Provides
    @Singleton
    static ProgressionRepository provideProgressionRepository(
            ScaleDao scaleDao,
            ScaleBoxDao scaleBoxDao,
            TonalityDao tonalityDao,
            UserScaleCompletionDao completionDao,
            ProgressionDao progressionDao
    ) {
        return new ProgressionRepository(
                scaleDao,
                scaleBoxDao,
                tonalityDao,
                completionDao,
                progressionDao
        );
    }
}
