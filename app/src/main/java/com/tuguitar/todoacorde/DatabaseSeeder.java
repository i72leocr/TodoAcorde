package com.tuguitar.todoacorde;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Seeder para poblar la base de datos la primera vez.
 * Se ejecuta dentro de runInTransaction desde el callback de Room.
 */
public class DatabaseSeeder {
    private static final String TAG = "DatabaseSeeder";

    /**
     * Siembra TODOS los datos: base + definiciones de logros.
     */
    public static void seed(@NonNull todoAcordeDatabase db, @NonNull Context ctx) {
        Log.d(TAG, "=== START SEEDING ===");

        // 1) Leer fuentes JSON
        Log.d(TAG, "--> Leyendo JSON de raw...");
        List<Difficulty> difficulties = JsonReader.readDifficultiesFromRaw(ctx);
        List<ChordType> chordTypes    = JsonReader.readChordTypesFromRaw(ctx);
        List<Chord>     chords        = JsonReader.readChordsFromRaw(ctx);
        List<SongRepositoryModel> songs = JsonReader.readSongsFromRaw(ctx);
        Log.d(TAG, "   Difficulties: " + difficulties.size());
        Log.d(TAG, "   ChordTypes : " + chordTypes.size());
        Log.d(TAG, "   Chords     : " + chords.size());
        Log.d(TAG, "   Songs      : " + songs.size());

        // 2) Limpiar tablas
        Log.d(TAG, "--> Limpiando tablas...");
        db.clearAllTables();
        db.getOpenHelper().getWritableDatabase()
                .execSQL("DELETE FROM sqlite_sequence");
        Log.d(TAG, "   Tablas limpias.");

        // 3) Insertar datos base
        Log.d(TAG, "--> Insertando Difficulties...");
        db.difficultyDao().insertAll(difficulties);
        Log.d(TAG, "   Difficulties insertadas.");

        Log.d(TAG, "--> Insertando ChordTypes...");
        db.chordTypeDao().insertAll(chordTypes);
        Log.d(TAG, "   ChordTypes insertados.");

        Log.d(TAG, "--> Insertando Chords...");
        db.chordDao().insertAll(chords);
        Log.d(TAG, "   Chords insertados.");

        // 4) Usuario por defecto
        Log.d(TAG, "--> Verificando usuario por defecto...");
        User user = db.userDao().getUserById(1);
        if (user == null) {
            Log.d(TAG, "   Usuario 1 no existe, creando...");
            user = new User();
            user.id = 1;
            user.username = "Default User";
            user.createdAt = System.currentTimeMillis();
            user.lastActive = user.createdAt;
            db.userDao().insertUser(user);
            Log.d(TAG, "   Usuario 1 creado.");
        } else {
            Log.d(TAG, "   Usuario 1 ya existe (username=" + user.username + ").");
        }

        // 5) Songs + Lyrics + SongChords
        Log.d(TAG, "--> Insertando Songs + Lyrics + SongChords...");
        int songCount = 0, lyricCount = 0, chordCount = 0;
        for (SongRepositoryModel m : songs) {
            songCount++;
            long songId = db.songDao().insert(
                    new Song(m.title, m.author, m.difficulty,
                            m.bpm, m.measure, m.isFavorite)
            );
            Log.d(TAG, "   [" + songCount + "/" + songs.size() + "] Song insertada: id=" + songId + " título=\"" + m.title + "\"");

            // Lyrics
            List<Integer> lyricIds = new ArrayList<>();
            for (int i = 0; i < m.lyrics.size(); i++) {
                long lid = db.songLyricDao().insert(
                        new SongLyric((int)songId, i, m.lyrics.get(i))
                );
                lyricCount++;
                lyricIds.add((int)lid);
                Log.d(TAG, "     Lyric insertada: id=" + lid + " texto=\"" + m.lyrics.get(i).substring(0, Math.min(20, m.lyrics.get(i).length())) + "...\"");
            }

            // SongChords
            Pattern token = Pattern.compile("\\S+");
            int durationIndex = 0;
            for (int line=0; line<m.chords.size(); line++) {
                Matcher mc = token.matcher(m.chords.get(line));
                while (mc.find()) {
                    String sym = mc.group();
                    Chord c = db.chordDao().findByNameSync(sym);
                    if (c!=null && durationIndex < m.chordDurations.size()) {
                        db.songChordDao().insertSongChord(
                                new SongChord((int)songId,
                                        c.getId(),
                                        lyricIds.get(Math.min(line, lyricIds.size()-1)),
                                        m.chordDurations.get(durationIndex++),
                                        mc.start())
                        );
                        chordCount++;
                        Log.d(TAG, "     SongChord insertado: songId=" + songId + " chord=\"" + sym + "\"");
                    } else {
                        Log.w(TAG, "     [WARN] No se insertó SongChord: sym=\"" + sym + "\"");
                    }
                }
            }
        }
        Log.d(TAG, "   Total Songs: " + songCount + ", Lyrics: " + lyricCount + ", SongChords: " + chordCount);
        Log.d(TAG, "✅ Seeding de datos base completado");

        // 6) Definiciones de logros
        Log.d(TAG, "--> Insertando definiciones de logros...");
        AchievementDefinitionDao defDao = db.achievementDefinitionDao();
        List<AchievementDefinitionEntity> defs = provideInitialAchievementDefinitions();
        defDao.insertAllDefinitions(defs);
        Log.d(TAG, "   Definiciones de logros insertadas: " + defs.size());

        Log.d(TAG, "=== SEEDING FINALIZADO ===");
    }

    /**
     * Genera la lista de definiciones iniciales (sin progreso).
     */
    public static List<AchievementDefinitionEntity> provideInitialAchievementDefinitions() {
        List<AchievementDefinitionEntity> list = new ArrayList<>();
        addDef(list, "primeros_acorde",       "Primeros Acorde",      new int[]{1,3,6});
        addDef(list, "moderato_maestro",      "Moderato Maestro",     new int[]{1,3,6});
        addDef(list, "norma_legendaria",      "Norma Legendaria",     new int[]{1,3,6});
        addDef(list, "cien_por_ciento_rock",  "Cien Por Ciento Rock", new int[]{1,2,3});
        addDef(list, "acorde_unico",          "Acorde Único",         new int[]{5,10,15});
        return list;
    }

    private static void addDef(
            List<AchievementDefinitionEntity> list,
            String familyId, String title, int[] thr
    ) {
        for (Achievement.Level lvl : Achievement.Level.values()) {
            int t = lvl == Achievement.Level.BRONZE ? thr[0]
                    : lvl == Achievement.Level.SILVER ? thr[1]
                    : thr[2];
            int icon = lvl == Achievement.Level.BRONZE
                    ? R.drawable.ic_medal_bronze
                    : lvl == Achievement.Level.SILVER
                    ? R.drawable.ic_medal_silver
                    : R.drawable.ic_medal_gold;
            list.add(new AchievementDefinitionEntity(
                    familyId, lvl, title, t, icon
            ));
        }
    }
}
