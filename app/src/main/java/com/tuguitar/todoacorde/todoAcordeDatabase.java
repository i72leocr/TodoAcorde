
/*
package com.tuguitar.todoacorde;

import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import androidx.sqlite.db.SupportSQLiteDatabase;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.Executors;

@Database(entities = {Chord.class}, version = 3, exportSchema = false)
@TypeConverters({PCPConverter.class})
public abstract class todoAcordeDatabase extends RoomDatabase {
    private static final String TAG = "todoAcordeDatabase";
    private static volatile todoAcordeDatabase INSTANCE;

    public abstract ChordDao chordDao();

    public static todoAcordeDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (todoAcordeDatabase.class) {
                if (INSTANCE == null) {
                    Log.d(TAG, "Creating new instance of the database");
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    todoAcordeDatabase.class, "todoacorde_database")
                            .fallbackToDestructiveMigration()
                            .addCallback(new Callback() {
                                @Override
                                public void onCreate(@NonNull SupportSQLiteDatabase db) {
                                    super.onCreate(db);
                                    Log.d(TAG, "Database onCreate callback called");
                                    Executors.newSingleThreadScheduledExecutor().execute(() -> {
                                        todoAcordeDatabase database = getInstance(context);
                                        ChordDao chordDao = database.chordDao();
                                        loadChordsFromCSV(chordDao, context);
                                    });
                                }

                                @Override
                                public void onOpen(@NonNull SupportSQLiteDatabase db) {
                                    super.onOpen(db);
                                    Log.d(TAG, "Database onOpen callback called");
                                }
                            })
                            .build();
                    Log.d(TAG, "Database instance created");
                } else {
                    Log.d(TAG, "Using existing instance of the database");
                }
            }
        } else {
            Log.d(TAG, "Using existing instance of the database");
        }
        return INSTANCE;
    }

    private static void loadChordsFromCSV(ChordDao chordDao, Context context) {
        Log.d(TAG, "Loading chords from CSV");
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(context.getResources().openRawResource(R.raw.chord_pcp_dataset)))) {
            String line;
            while ((line = reader.readLine()) != null) {
                Log.d(TAG, "Reading line: " + line);
                String[] parts = line.split(",");
                if (parts.length < 2) {
                    Log.d(TAG, "Skipping invalid line: " + line);
                    continue;
                }
                try {
                    String name = parts[0];
                    String pcpString = parts[1];
                    double[] pcp = new double[12];
                    for (int i = 0; i < 12; i++) {
                        pcp[i] = Double.parseDouble(String.valueOf(pcpString.charAt(i)));
                    }
                    Chord chord = new Chord(name, pcp);
                    chordDao.insertChord(chord);
                    Log.d(TAG, "Inserted chord: " + name);
                } catch (NumberFormatException | IndexOutOfBoundsException e) {
                    Log.e(TAG, "Error parsing line: " + line, e);
                }
            }
            Log.d(TAG, "All chords loaded from CSV");
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "Error loading chords from CSV", e);
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "Unexpected error", e);
        }
    }

    public static void initializeDataIfNeeded(Context context) {
        Executors.newSingleThreadScheduledExecutor().execute(() -> {
            todoAcordeDatabase database = getInstance(context);
            ChordDao chordDao = database.chordDao();
            if (chordDao.getAllChords().isEmpty()) {
                Log.d(TAG, "Chord database is empty, loading data");
                loadChordsFromCSV(chordDao, context);
            } else {
                Log.d(TAG, "Chord database already initialized");
            }
        });
    }
}
*/