package com.tuguitar.todoacorde;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.RoomDatabase.Callback;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.tuguitar.todoacorde.achievements.data.AchievementDefinitionDao;
import com.tuguitar.todoacorde.practice.data.PracticeDetailDao;
import com.tuguitar.todoacorde.practice.data.PracticeSessionDao;
import com.tuguitar.todoacorde.practice.data.SongUserSpeedDao;
import com.tuguitar.todoacorde.songs.data.SongChordDao;
import com.tuguitar.todoacorde.songs.data.SongDao;
import com.tuguitar.todoacorde.songs.data.SongLyricDao;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;

@Module
@InstallIn(SingletonComponent.class)
public class DatabaseModule {
    private static final String TAG = "DB_SEED";

    @Provides
    @Singleton
    static todoAcordeDatabase provideDatabase(Application app) {
        return Room.databaseBuilder(app, todoAcordeDatabase.class, "todoacorde_database")
                .fallbackToDestructiveMigration()
                .addCallback(new Callback() {
                    private final ExecutorService dbExecutor = Executors.newFixedThreadPool(4);

                    @Override
                    public void onCreate(@NonNull SupportSQLiteDatabase db) {
                        super.onCreate(db);
                        Log.d(TAG, "onCreate: seeding database");
                        seedDatabase(app);
                    }

                    @Override
                    public void onOpen(@NonNull SupportSQLiteDatabase db) {
                        super.onOpen(db);
                        Log.d(TAG, "onOpen: (re)seeding database");
                        seedDatabase(app);
                    }

                    private void seedDatabase(Context ctx) {
                        todoAcordeDatabase database = todoAcordeDatabase.getInstance(ctx);
                        dbExecutor.execute(() -> {
                            database.runInTransaction(() -> {
                                database.clearAllTables();
                                DatabaseSeeder.seed(database, ctx);
                                Log.d(TAG, "DatabaseSeeder: completed");
                            });
                        });
                    }
                })
                .build();
    }

    @Provides @Singleton static SongDao provideSongDao(todoAcordeDatabase db)           { return db.songDao(); }
    @Provides @Singleton static ChordDao provideChordDao(todoAcordeDatabase db)         { return db.chordDao(); }
    @Provides @Singleton static SongChordDao provideSongChordDao(todoAcordeDatabase db) { return db.songChordDao(); }
    @Provides @Singleton static SongLyricDao provideSongLyricDao(todoAcordeDatabase db) { return db.songLyricDao(); }
    @Provides @Singleton static ProgressionDao provideProgressionDao(todoAcordeDatabase db)           { return db.progressionDao(); }
    @Provides @Singleton static ProgressionChordDao provideProgressionChordDao(todoAcordeDatabase db) { return db.progressionChordDao(); }
    @Provides @Singleton static ProgressionDetailDao provideProgressionDetailDao(todoAcordeDatabase db) { return db.progressionDetailDao(); }
    @Provides @Singleton static ProgressionSessionDao provideProgressionSessionDao(todoAcordeDatabase db) { return db.progressionSessionDao(); }
    @Provides @Singleton static PracticeSessionDao providePracticeSessionDao(todoAcordeDatabase db)       { return db.practiceSessionDao(); }
    @Provides @Singleton static PracticeDetailDao providePracticeDetailDao(todoAcordeDatabase db)         { return db.practiceDetailDao(); }
    @Provides @Singleton static SongUserSpeedDao provideSongUserSpeedDao(todoAcordeDatabase db)           { return db.songUserSpeedDao(); }
    @Provides @Singleton static FavoriteSongDao provideFavoriteSongDao(todoAcordeDatabase db)             { return db.favoriteSongDao(); }
    @Provides @Singleton static UserDao provideUserDao(todoAcordeDatabase db)                             { return db.userDao(); }
    @Provides @Singleton static DifficultyDao provideDifficultyDao(todoAcordeDatabase db)                 { return db.difficultyDao(); }
    @Provides @Singleton static ChordTypeDao provideChordTypeDao(todoAcordeDatabase db)                   { return db.chordTypeDao(); }
    @Provides @Singleton static AchievementDefinitionDao provideAchievementDefinitionDao(todoAcordeDatabase db) { return db.achievementDefinitionDao(); }
    @Provides @Singleton static AchievementDao provideAchievementDao(todoAcordeDatabase db)               { return db.achievementDao(); }
}
