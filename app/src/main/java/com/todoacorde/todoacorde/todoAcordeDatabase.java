package com.todoacorde.todoacorde;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.todoacorde.todoacorde.achievements.data.AchievementDao;
import com.todoacorde.todoacorde.achievements.data.AchievementDefinitionDao;
import com.todoacorde.todoacorde.achievements.data.AchievementDefinitionEntity;
import com.todoacorde.todoacorde.achievements.data.AchievementEntity;
import com.todoacorde.todoacorde.practice.data.PracticeDetail;
import com.todoacorde.todoacorde.practice.data.PracticeDetailDao;
import com.todoacorde.todoacorde.practice.data.PracticeSession;
import com.todoacorde.todoacorde.practice.data.PracticeSessionDao;
import com.todoacorde.todoacorde.practice.data.SongUserSpeed;
import com.todoacorde.todoacorde.practice.data.SongUserSpeedDao;
import com.todoacorde.todoacorde.scales.data.ScaleNoteEntity;
import com.todoacorde.todoacorde.scales.data.ScalePatternDao;
import com.todoacorde.todoacorde.scales.data.ScalePatternEntity;
import com.todoacorde.todoacorde.scales.data.dao.ProgressionDao;
import com.todoacorde.todoacorde.scales.data.dao.ScaleBoxDao;
import com.todoacorde.todoacorde.scales.data.dao.ScaleDao;
import com.todoacorde.todoacorde.scales.data.dao.TonalityDao;
import com.todoacorde.todoacorde.scales.data.dao.UserScaleCompletionDao;
import com.todoacorde.todoacorde.scales.data.entity.ScaleBoxEntity;
import com.todoacorde.todoacorde.scales.data.entity.ScaleEntity;
import com.todoacorde.todoacorde.scales.data.entity.TonalityEntity;
import com.todoacorde.todoacorde.scales.data.entity.UserScaleCompletionEntity;
import com.todoacorde.todoacorde.songs.data.FavoriteSong;
import com.todoacorde.todoacorde.songs.data.FavoriteSongDao;
import com.todoacorde.todoacorde.songs.data.Song;
import com.todoacorde.todoacorde.songs.data.SongChord;
import com.todoacorde.todoacorde.songs.data.SongChordDao;
import com.todoacorde.todoacorde.songs.data.SongDao;
import com.todoacorde.todoacorde.songs.data.SongLyric;
import com.todoacorde.todoacorde.songs.data.SongLyricDao;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Implementación de Room para la base de datos local de la app.
 *
 * Definición:
 * - {@link Database}: lista de entidades y versión del esquema.
 * - {@link TypeConverters}: convertidores para tipos personalizados.
 *
 * Provee acceso a los distintos DAO mediante métodos abstractos.
 * Implementa un patrón singleton con {@link #getInstance(Context)} y
 * configura un {@link RoomDatabase.Callback} para sembrado de datos.
 */
@Database(
        entities = {
                Song.class,
                Chord.class,
                SongChord.class,
                SongLyric.class,
                PracticeSession.class,
                PracticeDetail.class,
                SongUserSpeed.class,
                User.class,
                ChordType.class,
                Difficulty.class,
                FavoriteSong.class,
                AchievementDefinitionEntity.class,
                AchievementEntity.class,
                ScalePatternEntity.class,
                ScaleNoteEntity.class,
                ScaleEntity.class,
                ScaleBoxEntity.class,
                TonalityEntity.class,
                UserScaleCompletionEntity.class
        },
        version = 75,
        exportSchema = false
)
@TypeConverters({PCPConverter.class, Converters.class})
public abstract class todoAcordeDatabase extends RoomDatabase {

    // DAOs principales de canciones y acordes
    public abstract SongDao songDao();
    public abstract ChordDao chordDao();
    public abstract SongChordDao songChordDao();
    public abstract SongLyricDao songLyricDao();

    // DAOs de práctica
    public abstract PracticeSessionDao practiceSessionDao();
    public abstract PracticeDetailDao practiceDetailDao();
    public abstract SongUserSpeedDao songUserSpeedDao();

    // DAOs auxiliares
    public abstract UserDao userDao();
    public abstract ChordTypeDao chordTypeDao();
    public abstract DifficultyDao difficultyDao();
    public abstract FavoriteSongDao favoriteSongDao();

    // DAOs de logros
    public abstract AchievementDefinitionDao achievementDefinitionDao();
    public abstract AchievementDao achievementDao();

    // DAOs de escalas
    public abstract ScalePatternDao scalePatternDao();
    public abstract ScaleDao scaleDao();
    public abstract ScaleBoxDao scaleBoxDao();
    public abstract TonalityDao tonalityDao();
    public abstract UserScaleCompletionDao userScaleCompletionDao();
    public abstract ProgressionDao progressionDao();

    /** Instancia singleton de la base. */
    private static volatile todoAcordeDatabase INSTANCE;

    /**
     * Pool de hilos para operaciones fuera del hilo principal
     * relacionadas con la base de datos (sembrado, cargas pesadas, etc.).
     */
    public static final ExecutorService databaseWriteExecutor =
            Executors.newFixedThreadPool(4);

    /**
     * Obtiene/crea la instancia singleton de la base de datos.
     *
     * Configuración:
     * - {@code fallbackToDestructiveMigration()} para descartar datos
     *   ante cambios de esquema sin migración.
     * - {@code addCallback(...)} para ejecutar tareas de semilla en
     *   {@code onCreate} y {@code onOpen}.
     *
     * @param ctx contexto de aplicación.
     * @return instancia de {@link todoAcordeDatabase}.
     */
    public static todoAcordeDatabase getInstance(Context ctx) {
        if (INSTANCE == null) {
            synchronized (todoAcordeDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    ctx.getApplicationContext(),
                                    todoAcordeDatabase.class,
                                    "todoacorde_database"
                            )
                            .fallbackToDestructiveMigration()
                            .addCallback(new Callback() {
                                @Override
                                public void onCreate(@NonNull SupportSQLiteDatabase db) {
                                    super.onCreate(db);
                                    Log.i("DB_SEED", "onCreate: limpiando y sembrando base");
                                    databaseWriteExecutor.execute(() -> {
                                        INSTANCE.runInTransaction(() -> {
                                            try {
                                                Log.i("DB_SEED", "onCreate: clearAllTables()");
                                                INSTANCE.clearAllTables();
                                                Log.i("DB_SEED", "onCreate: llamando a DatabaseSeeder.seed()");
                                                DatabaseSeeder.seed(INSTANCE, ctx);
                                                Log.i("DB_SEED", "onCreate: seed COMPLETADO");
                                            } catch (Exception e) {
                                                Log.e("DB_SEED", "onCreate: seed FALLÓ", e);
                                            }
                                        });
                                    });
                                }

                                @Override
                                public void onOpen(@NonNull SupportSQLiteDatabase db) {
                                    super.onOpen(db);
                                    Log.i("DB_SEED", "onOpen: limpiando y resembrando base");
                                    databaseWriteExecutor.execute(() -> {
                                        INSTANCE.runInTransaction(() -> {
                                            try {
                                                INSTANCE.clearAllTables();
                                                DatabaseSeeder.seed(INSTANCE, ctx);
                                                Log.i("DB_SEED", "onOpen: seed COMPLETADO");
                                            } catch (Exception e) {
                                                Log.e("DB_SEED", "onOpen: seed FALLÓ", e);
                                            }
                                        });
                                    });
                                }
                            })
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
