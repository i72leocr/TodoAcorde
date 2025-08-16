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
import com.tuguitar.todoacorde.scales.data.ScalePatternEntity;
import com.tuguitar.todoacorde.scales.data.ScaleNoteEntity;
import com.tuguitar.todoacorde.scales.data.ScalePatternDao;
// 👉 Importamos entidades y DAOs nuevos de escalas progresivas
import com.tuguitar.todoacorde.scales.data.dao.ScaleDao;
import com.tuguitar.todoacorde.scales.data.dao.ScaleBoxDao;
import com.tuguitar.todoacorde.scales.data.dao.TonalityDao;
import com.tuguitar.todoacorde.scales.data.dao.UserScaleCompletionDao;
import com.tuguitar.todoacorde.scales.data.dao.ProgressionDao;
import com.tuguitar.todoacorde.scales.data.entity.ScaleEntity;
import com.tuguitar.todoacorde.scales.data.entity.ScaleBoxEntity;
import com.tuguitar.todoacorde.scales.data.entity.TonalityEntity;
import com.tuguitar.todoacorde.scales.data.entity.UserScaleCompletionEntity;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Base de datos Room principal de la app.
 * (Comentario existente)
 */
@Database(
        entities = {
                // Entidades existentes...
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
                // ✅ Nuevas entidades de escalas (progresivas)
                ScalePatternEntity.class,
                ScaleNoteEntity.class,
                ScaleEntity.class,
                ScaleBoxEntity.class,
                TonalityEntity.class,
                UserScaleCompletionEntity.class
        },
        version = 75,  // ⬆️ Actualizado para incluir nuevas tablas
        exportSchema = false
)
@TypeConverters({PCPConverter.class, Converters.class})
public abstract class todoAcordeDatabase extends RoomDatabase {
    // DAOs existentes...
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
    // ✅ DAOs nuevos para escalas progresivas
    public abstract ScalePatternDao scalePatternDao();
    public abstract ScaleDao scaleDao();
    public abstract ScaleBoxDao scaleBoxDao();
    public abstract TonalityDao tonalityDao();
    public abstract UserScaleCompletionDao userScaleCompletionDao();
    public abstract ProgressionDao progressionDao();

    private static volatile todoAcordeDatabase INSTANCE;
    public static final ExecutorService databaseWriteExecutor =
            Executors.newFixedThreadPool(4);

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
                    // (código de apertura forzada omitido por brevedad)
                }
            }
        }
        return INSTANCE;
    }
}
