package com.tuguitar.todoacorde;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.tuguitar.todoacorde.achievements.data.AchievementDao;
import com.tuguitar.todoacorde.achievements.data.AchievementDefinitionDao;
import com.tuguitar.todoacorde.achievements.data.AchievementDefinitionEntity;
import com.tuguitar.todoacorde.achievements.data.AchievementEntity;
import com.tuguitar.todoacorde.practice.data.PracticeDetail;
import com.tuguitar.todoacorde.practice.data.PracticeDetailDao;
import com.tuguitar.todoacorde.practice.data.PracticeSession;
import com.tuguitar.todoacorde.practice.data.PracticeSessionDao;
import com.tuguitar.todoacorde.practice.data.SongUserSpeed;
import com.tuguitar.todoacorde.practice.data.SongUserSpeedDao;
import com.tuguitar.todoacorde.songs.data.FavoriteSong;
import com.tuguitar.todoacorde.songs.data.FavoriteSongDao;
import com.tuguitar.todoacorde.songs.data.Song;
import com.tuguitar.todoacorde.songs.data.SongChord;
import com.tuguitar.todoacorde.songs.data.SongChordDao;
import com.tuguitar.todoacorde.songs.data.SongDao;
import com.tuguitar.todoacorde.songs.data.SongLyric;
import com.tuguitar.todoacorde.songs.data.SongLyricDao;

// ✅ IMPORTS: entidades/DAO de escalas (nuevos)
import com.tuguitar.todoacorde.scales.data.ScalePatternEntity;
import com.tuguitar.todoacorde.scales.data.ScaleNoteEntity;
import com.tuguitar.todoacorde.scales.data.ScalePatternDao;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Base de datos Room principal de la app.
 *
 * Se ha integrado el módulo de escalas (patrones + notas de diapasón):
 *  - Entities: ScalePatternEntity, ScaleNoteEntity
 *  - DAO:      ScalePatternDao
 *
 * IMPORTANTE:
 *  - Versión subida a 70 para forzar onCreate y ver el seed.
 *  - fallbackToDestructiveMigration() solo recomendable en desarrollo.
 */
@Database(
        entities = {
                // --- Canciones / progresiones / usuarios / logros (existentes) ---
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

                // --- ✅ NUEVO: escalas ---
                ScalePatternEntity.class,
                ScaleNoteEntity.class
        },
        version = 73,            // ⬅️ subido para forzar recreación
        exportSchema = false
)
@TypeConverters({PCPConverter.class, Converters.class})
public abstract class todoAcordeDatabase extends RoomDatabase {

    // --- DAOs existentes ---
    public abstract SongDao songDao();
    public abstract ChordDao chordDao();
    public abstract SongChordDao songChordDao();
    public abstract SongLyricDao songLyricDao();
    public abstract PracticeSessionDao practiceSessionDao();
    public abstract PracticeDetailDao practiceDetailDao();
    public abstract SongUserSpeedDao songUserSpeedDao();
    public abstract UserDao userDao();
    public abstract ChordTypeDao chordTypeDao();
    public abstract DifficultyDao difficultyDao();
    public abstract FavoriteSongDao favoriteSongDao();
    public abstract AchievementDefinitionDao achievementDefinitionDao();
    public abstract AchievementDao achievementDao();

    // --- ✅ NUEVO: DAO de escalas/patrones ---
    public abstract ScalePatternDao scalePatternDao();

    private static volatile todoAcordeDatabase INSTANCE;
    public static final ExecutorService databaseWriteExecutor =
            Executors.newFixedThreadPool(4);

    /**
     * Crea/recupera la instancia singleton de la BD.
     *
     * Callback de creación/apertura:
     *  - Limpia tablas y ejecuta DatabaseSeeder.seed(INSTANCE, ctx).
     *  - Logs en INFO para verlos siempre.
     *
     * Además: se fuerza la apertura inmediata de la BD para disparar onCreate/onOpen.
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
                                    // ⚠️ Útil en desarrollo. En producción, normalmente NO se vacía en cada apertura.
                                    Log.i("DB_SEED", "onOpen: limpiando y (re)sembrando base");
                                    databaseWriteExecutor.execute(() -> {
                                        INSTANCE.runInTransaction(() -> {
                                            try {
                                                Log.i("DB_SEED", "onOpen: clearAllTables()");
                                                INSTANCE.clearAllTables();
                                                Log.i("DB_SEED", "onOpen: llamando a DatabaseSeeder.seed()");
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

                    // ✅ Fuerza apertura inmediata para disparar onCreate/onOpen y ver logs del seed
                    databaseWriteExecutor.execute(() -> {
                        try {
                            Log.i("DB_SEED", "Forzando apertura de BD para disparar callbacks...");
                            INSTANCE.getOpenHelper().getWritableDatabase(); // abre y dispara callbacks
                            Log.i("DB_SEED", "Apertura forzada OK");
                        } catch (Exception e) {
                            Log.e("DB_SEED", "Error forzando apertura de BD", e);
                        }
                    });
                }
            }
        }
        return INSTANCE;
    }
}
