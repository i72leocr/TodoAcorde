package com.tuguitar.todoacorde;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.tuguitar.todoacorde.achievements.data.AchievementDefinitionDao;
import com.tuguitar.todoacorde.achievements.data.AchievementDefinitionEntity;
import com.tuguitar.todoacorde.practice.data.PracticeDetail;
import com.tuguitar.todoacorde.practice.data.PracticeDetailDao;
import com.tuguitar.todoacorde.practice.data.PracticeSession;
import com.tuguitar.todoacorde.practice.data.PracticeSessionDao;
import com.tuguitar.todoacorde.practice.data.SongUserSpeed;
import com.tuguitar.todoacorde.practice.data.SongUserSpeedDao;
import com.tuguitar.todoacorde.songs.data.Song;
import com.tuguitar.todoacorde.songs.data.SongChord;
import com.tuguitar.todoacorde.songs.data.SongChordDao;
import com.tuguitar.todoacorde.songs.data.SongDao;
import com.tuguitar.todoacorde.songs.data.SongLyric;
import com.tuguitar.todoacorde.songs.data.SongLyricDao;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Database(
        entities = {
                Song.class,
                Chord.class,
                SongChord.class,
                SongLyric.class,
                Progression.class,
                ProgressionChord.class,
                PracticeSession.class,
                PracticeDetail.class,
                SongUserSpeed.class,
                User.class,
                ChordType.class,
                Difficulty.class,
                ProgressionSession.class,
                ProgressionDetail.class,
                FavoriteSong.class,
                AchievementDefinitionEntity.class,
                AchievementEntity.class
        },
        version = 65,
        exportSchema = false
)
@TypeConverters({ PCPConverter.class, Converters.class })
public abstract class todoAcordeDatabase extends RoomDatabase {

    // — DAOs existentes —
    public abstract SongDao songDao();
    public abstract ChordDao chordDao();
    public abstract SongChordDao songChordDao();
    public abstract SongLyricDao songLyricDao();
    public abstract ProgressionDao progressionDao();
    public abstract ProgressionChordDao progressionChordDao();
    public abstract PracticeSessionDao practiceSessionDao();
    public abstract PracticeDetailDao practiceDetailDao();
    public abstract SongUserSpeedDao songUserSpeedDao();
    public abstract UserDao userDao();
    public abstract ChordTypeDao chordTypeDao();
    public abstract DifficultyDao difficultyDao();
    public abstract ProgressionSessionDao progressionSessionDao();
    public abstract ProgressionDetailDao progressionDetailDao();
    public abstract FavoriteSongDao favoriteSongDao();

    // — DAOs nuevos de logros —
    public abstract AchievementDefinitionDao achievementDefinitionDao();
    public abstract AchievementDao achievementDao();

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
                                    databaseWriteExecutor.execute(() -> {
                                        todoAcordeDatabase d = getInstance(ctx);
                                        d.runInTransaction(() ->
                                                DatabaseSeeder.seed(d, ctx)
                                        );
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
