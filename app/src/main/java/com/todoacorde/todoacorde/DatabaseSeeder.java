package com.todoacorde.todoacorde;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;
import com.todoacorde.todoacorde.achievements.data.Achievement;
import com.todoacorde.todoacorde.achievements.data.AchievementDefinitionDao;
import com.todoacorde.todoacorde.achievements.data.AchievementDefinitionEntity;
import com.todoacorde.todoacorde.scales.data.ScaleFretNote;
import com.todoacorde.todoacorde.scales.data.ScaleNoteEntity;
import com.todoacorde.todoacorde.scales.data.ScalePatternDao;
import com.todoacorde.todoacorde.scales.data.ScalePatternEntity;
import com.todoacorde.todoacorde.scales.data.ScalePatternGenerator;
import com.todoacorde.todoacorde.scales.data.entity.ScaleBoxEntity;
import com.todoacorde.todoacorde.scales.data.entity.ScaleEntity;
import com.todoacorde.todoacorde.scales.data.entity.TonalityEntity;
import com.todoacorde.todoacorde.songs.data.Song;
import com.todoacorde.todoacorde.songs.data.SongChord;
import com.todoacorde.todoacorde.songs.data.SongLyric;
import com.todoacorde.todoacorde.songs.data.SongRepositoryModel;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DatabaseSeeder {
    private static final String TAG = "DatabaseSeeder";

    private DatabaseSeeder() {
    }

        private static final boolean ADD_COMMON_FALLBACK_SCALES = true;

        private static final int PROGRESSION_BOXES_PER_SCALE = 3;

    public static void seed(@NonNull todoAcordeDatabase db, @NonNull Context ctx) {
        Log.d(TAG, "=== START SEEDING (UNIFICADO) ===");
        Log.d(TAG, "--> Leyendo JSON base (difficulties/chords/songs)...");
        List<Difficulty> difficulties = JsonReader.readDifficultiesFromRaw(ctx);
        List<ChordType> chordTypes = JsonReader.readChordTypesFromRaw(ctx);
        List<Chord> chords = JsonReader.readChordsFromRaw(ctx);
        List<SongRepositoryModel> songs = JsonReader.readSongsFromRaw(ctx);
        Log.d(TAG, String.format(Locale.ROOT,
                "   Difficulties=%d, ChordTypes=%d, Chords=%d, Songs=%d",
                sizeOrZero(difficulties), sizeOrZero(chordTypes), sizeOrZero(chords), sizeOrZero(songs)));
        Log.d(TAG, "--> Limpiando todas las tablas y secuencias...");
        db.clearAllTables();
        db.getOpenHelper().getWritableDatabase().execSQL("DELETE FROM sqlite_sequence");
        Log.d(TAG, "   Tablas limpias.");
        Log.d(TAG, "--> Insertando Difficulties...");
        db.difficultyDao().insertAll(difficulties);
        Log.d(TAG, "--> Insertando ChordTypes...");
        db.chordTypeDao().insertAll(chordTypes);
        Log.d(TAG, "--> Insertando Chords...");
        db.chordDao().insertAll(chords);
        Log.d(TAG, "--> Verificando usuario por defecto...");
        User user = db.userDao().getUserById(1);
        if (user == null) {
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
        Log.d(TAG, "--> Insertando Songs + Lyrics + SongChords...");
        int songCount = 0, lyricCount = 0, songChordCount = 0;
        for (SongRepositoryModel m : songs) {
            songCount++;
            long songId = db.songDao().insert(
                    new Song(m.title, m.author, m.difficulty, m.bpm, m.measure, m.isFavorite)
            );
            List<Integer> lyricIds = new ArrayList<>();
            for (int i = 0; i < m.lyrics.size(); i++) {
                long lid = db.songLyricDao().insert(new SongLyric((int) songId, i, m.lyrics.get(i)));
                lyricCount++;
                lyricIds.add((int) lid);
            }
            Pattern token = Pattern.compile("\\S+");
            int durationIndex = 0;
            for (int line = 0; line < m.chords.size(); line++) {
                Matcher mc = token.matcher(m.chords.get(line));
                while (mc.find()) {
                    String sym = mc.group();
                    Chord c = db.chordDao().findByNameSync(sym);
                    if (c != null && durationIndex < m.chordDurations.size()) {
                        db.songChordDao().insertSongChord(
                                new SongChord((int) songId,
                                        c.getId(),
                                        lyricIds.get(Math.min(line, lyricIds.size() - 1)),
                                        m.chordDurations.get(durationIndex++),
                                        mc.start())
                        );
                        songChordCount++;
                    } else {
                        Log.w(TAG, "     [WARN] No se insertó SongChord: sym=\"" + sym + "\"");
                    }
                }
            }
        }
        Log.d(TAG, "   Songs=" + songCount + ", Lyrics=" + lyricCount + ", SongChords=" + songChordCount);
        Log.d(TAG, "--> Preparando catálogo (memoria) para PROGRESIÓN de escalas (desde pattern_repo.json)...");
        RepoFile repoForScales = readRepoJson(ctx);
        Map<String, Integer> typeToDifficulty = new LinkedHashMap<>();

        if (repoForScales != null) {
            List<PatternItem> items = repoForScales.normalizedItems();
            if (items != null) {
                for (PatternItem it : items) {
                    try {
                        it.normalizeAliases();
                        String st = (it.scaleType != null && !it.scaleType.trim().isEmpty())
                                ? it.scaleType.trim()
                                : detectScaleType(it.scaleDegrees);
                        if (st == null || st.isEmpty()) continue;
                        st = normalizeScaleTypeAlias(st);

                        int diff = (it.difficulty != null && it.difficulty >= 1)
                                ? it.difficulty
                                : defaultDifficultyForScaleType(st);
                        Integer prev = typeToDifficulty.get(st);
                        if (prev == null || diff > prev) typeToDifficulty.put(st, diff);
                    } catch (Throwable t) {
                        Log.w(TAG, "   [WARN] Saltando item malformado en catálogo escalas.", t);
                    }
                }
            }
        }
        if (ADD_COMMON_FALLBACK_SCALES) {
            Map<String, Integer> fb = new LinkedHashMap<>();
            fb.put("Phrygian", 1);
            fb.put("Phrygian Dominant", 1);
            fb.put("Major Pentatonic", 1);
            fb.put("Minor Pentatonic", 1);

            fb.put("Ionian", 2);
            fb.put("Aeolian", 2);
            fb.put("Dorian", 2);
            fb.put("Mixolydian", 2);
            fb.put("Harmonic Minor", 2);
            fb.put("Blues", 2);

            fb.put("Lydian", 4);
            fb.put("Double Harmonic Major", 4);
            fb.put("Spanish 8-Tone", 4);

            fb.put("Melodic Minor (Asc)", 5);
            fb.put("Locrian", 5);

            for (Map.Entry<String, Integer> e : fb.entrySet()) {
                String canon = normalizeScaleTypeAlias(e.getKey());
                typeToDifficulty.putIfAbsent(canon, e.getValue());
            }
        }
        List<ScaleEntity> baseScales = new ArrayList<>();
        for (Map.Entry<String, Integer> e : typeToDifficulty.entrySet()) {
            String scaleType = e.getKey();
            int difficulty = Math.max(1, Math.min(5, e.getValue()));
            int tier = diffToTier(difficulty);
            String displayName = localizeScaleType(scaleType);
            baseScales.add(scaleEntity(displayName, tier));
        }

        int easyScales = (int) baseScales.stream().filter(s -> s.tier == 0).count();
        int mediumScales = (int) baseScales.stream().filter(s -> s.tier == 1).count();
        int hardScales = (int) baseScales.stream().filter(s -> s.tier == 2).count();
        final int tonalitiesCount = 12; 
        Log.d(TAG, String.format(Locale.ROOT,
                "   Escalas por tier -> easy=%d, medium=%d, hard=%d | tonalidades=%d (total tipos=%d)",
                easyScales, mediumScales, hardScales, tonalitiesCount, baseScales.size()));
        Log.d(TAG, "--> Insertando definiciones de logros (dinámicos + milestones de escalas)...");
        AchievementDefinitionDao defDao = db.achievementDefinitionDao();
        List<AchievementDefinitionEntity> defs =
                provideInitialAchievementDefinitions(
                        sizeOrZero(chords),
                        sizeOrZero(songs),
                        easyScales, mediumScales, hardScales, tonalitiesCount
                );
        defDao.insertAllDefinitions(defs);
        Log.d(TAG, "   Logros insertados=" + defs.size());
        Log.d(TAG, "--> Insertando patrones de ESCALAS (cajas) desde pattern_repo.json...");
        seedScalePatternsIntoRoom(ctx, db.scalePatternDao(),  false);
        int countPatterns = 0;
        try {
            countPatterns = db.scalePatternDao().countPatterns();
        } catch (Throwable ignore) {
        }
        Log.d(TAG, "   Patrones escalas insertados=" + countPatterns);
        Log.d(TAG, "--> Insertando catálogo para PROGRESIÓN de escalas...");
        db.scaleDao().insertAll(baseScales);
        Log.d(TAG, "   Escalas (progresión) insertadas=" + baseScales.size());
        String[] noteNames = {"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"};
        List<TonalityEntity> tonalities = new ArrayList<>();
        for (String note : noteNames) {
            TonalityEntity t = new TonalityEntity();
            t.name = note;
            tonalities.add(t);
        }
        db.tonalityDao().insertAll(tonalities);
        Log.d(TAG, "   Tonalidades insertadas=" + tonalities.size());
        List<ScaleEntity> allScales = db.scaleDao().getAll();
        List<ScaleBoxEntity> boxEntities = new ArrayList<>();
        for (ScaleEntity s : allScales) {
            for (int boxOrder = 1; boxOrder <= PROGRESSION_BOXES_PER_SCALE; boxOrder++) {
                ScaleBoxEntity box = new ScaleBoxEntity();
                box.scaleId = s.id;
                box.boxOrder = boxOrder;
                boxEntities.add(box);
            }
        }
        db.scaleBoxDao().insertAll(boxEntities);
        Log.d(TAG, "   Cajas insertadas=" + boxEntities.size());

        Log.d(TAG, " SEEDING COMPLETADO (UNIFICADO)");
    }

    private static ScaleEntity scaleEntity(String name, int tier) {
        ScaleEntity e = new ScaleEntity();
        e.name = name;
        e.tier = tier;
        return e;
    }

    public static List<AchievementDefinitionEntity> provideInitialAchievementDefinitions(
            int totalChords,
            int totalSongs,
            int easyScales,
            int mediumScales,
            int hardScales,
            int totalTonalities
    ) {
        List<AchievementDefinitionEntity> list = new ArrayList<>();
        addDef(list, "acorde_unico", "Acorde Único",
                new int[]{pctToTarget(totalChords, 0.10), pctToTarget(totalChords, 0.50), Math.max(1, totalChords)});

        addDef(list, "cien_por_ciento_rock", "Cien Por Ciento Rock",
                new int[]{pctToTarget(totalSongs, 0.10), pctToTarget(totalSongs, 0.50), Math.max(1, totalSongs)});

        addDef(list, "moderato_maestro", "Moderato Maestro",
                new int[]{pctToTarget(totalSongs, 0.10), pctToTarget(totalSongs, 0.50), Math.max(1, totalSongs)});

        addDef(list, "norma_legendaria", "Norma Legendaria",
                new int[]{pctToTarget(totalSongs, 0.10), pctToTarget(totalSongs, 0.50), Math.max(1, totalSongs)});
        list.add(new AchievementDefinitionEntity(
                "primeros_acorde", Achievement.Level.BRONZE, "Primeros Acorde", 1, R.drawable.ic_medal_bronze));
        addDef(list,
                "scales_all_tonalities_milestone",
                "Completar escalas (todas las tonalidades)",
                new int[]{1, 1, 1});
        addDef(list,
                "scales_one_tonality_milestone",
                "Completar escalas (una tonalidad)",
                new int[]{1, 1, 1});

        Log.d(TAG, String.format(Locale.ROOT,
                "   [LOGROS ESCALAS - milestones] easy=%d, med=%d, hard=%d | tonalidades=%d",
                easyScales, mediumScales, hardScales, totalTonalities));

        return list;
    }

    private static int pctToTarget(int total, double pct) {
        if (total <= 0) return 1;
        return Math.max(1, (int) Math.ceil(total * pct));
    }

    private static void addDef(
            List<AchievementDefinitionEntity> list,
            String familyId, String title, int[] thr
    ) {
        int bronze = thr.length > 0 ? thr[0] : 1;
        int silver = thr.length > 1 ? thr[1] : Math.max(1, bronze * 2);
        int gold = thr.length > 2 ? thr[2] : Math.max(silver, bronze);

        list.add(new AchievementDefinitionEntity(
                familyId, Achievement.Level.BRONZE, title, bronze, R.drawable.ic_medal_bronze));
        list.add(new AchievementDefinitionEntity(
                familyId, Achievement.Level.SILVER, title, silver, R.drawable.ic_medal_silver));
        list.add(new AchievementDefinitionEntity(
                familyId, Achievement.Level.GOLD, title, gold, R.drawable.ic_medal_gold));
    }

    private static final int DEFAULT_WINDOW_START = 5;
    private static final int DEFAULT_WINDOW_END = 9;

    public static void seedScalePatternsIntoRoom(Context context,
                                                 ScalePatternDao dao,
                                                 boolean clearBefore) {
        try {
            RepoFile repo = readRepoJson(context);
            if (repo == null) {
                Log.w(TAG, "⚠️ pattern_repo.json no se pudo leer.");
                return;
            }

            List<PatternItem> items = repo.normalizedItems();
            Log.i(TAG, "📦 Colección: " + repo.collection
                    + " | patterns=" + sizeOrZero(repo.patterns)
                    + " | scales=" + sizeOrZero(repo.scales)
                    + " | items=" + sizeOrZero(repo.items));

            if (items == null || items.isEmpty()) {
                Log.w(TAG, "⚠️ pattern_repo vacío o mal formado. No se insertan patrones.");
                return;
            }

            if (clearBefore) {
                dao.deleteAllNotes();
                dao.deleteAllPatterns();
            } else {
                Log.d(TAG, "(escala) clearBefore=false (ya limpiamos todo previamente).");
            }

            int insertedPatterns = 0;
            int insertedNotes = 0;

            final Gson gson = new Gson();

            for (PatternItem item : items) {
                try {
                    item.normalizeAliases();
                    String scaleType = (item.scaleType != null && !item.scaleType.trim().isEmpty())
                            ? item.scaleType.trim()
                            : detectScaleType(item.scaleDegrees);
                    scaleType = normalizeScaleTypeAlias(scaleType);
                    String root = toSharp(item.root);
                    List<int[]> winList = new ArrayList<>();
                    LinkedHashSet<String> seen = new LinkedHashSet<>();
                    if (item.windows != null && item.windows.length > 0) {
                        for (int[] w : item.windows) {
                            if (w == null || w.length != 2) continue;
                            int a = Math.min(w[0], w[1]);
                            int b = Math.max(w[0], w[1]);
                            String key = a + ":" + b;
                            if (seen.add(key)) winList.add(new int[]{a, b});
                        }
                    } else if (item.window != null && item.window.length == 2) {
                        int a = Math.min(item.window[0], item.window[1]);
                        int b = Math.max(item.window[0], item.window[1]);
                        String key = a + ":" + b;
                        if (seen.add(key)) winList.add(new int[]{a, b});
                    } else {
                        for (int[] w : defaultWindowsFor(scaleType, root)) {
                            String key = w[0] + ":" + w[1];
                            if (seen.add(key)) winList.add(w);
                        }
                    }
                    List<NoteItem> explicitNotes = extractExplicitNotesIfAny(item, gson);
                    final boolean useExplicitForSingleBox =
                            (explicitNotes != null && !explicitNotes.isEmpty() && winList.size() <= 1);

                    int posIndex = 0;
                    for (int[] win : winList) {
                        int start = (win != null && win.length == 2) ? win[0] : DEFAULT_WINDOW_START;
                        int end = (win != null && win.length == 2) ? win[1] : DEFAULT_WINDOW_END;
                        List<ScaleFretNote> notes;
                        if (useExplicitForSingleBox && posIndex == 0) {
                            notes = mapExplicitNotes(explicitNotes);                             if (root == null || root.isEmpty()) root = inferRootFromNotes(notes);
                        } else if (root != null && item.scaleDegrees != null && !item.scaleDegrees.isEmpty()) {
                            String rootSharp = toSharp(root);
                            notes = ScalePatternGenerator.buildPattern(rootSharp, item.scaleDegrees, start, end);
                            notes = normalizeGeneratedNotesToSharp(notes);
                        } else {
                            Log.w(TAG, "⚠️ Ítem sin datos suficientes (id=" + item.id + ", name=" + item.name + ")");
                            continue;
                        }
                        String base = (item.name != null && !item.name.trim().isEmpty())
                                ? stripWindowFromName(item.name.trim())
                                : buildBaseName(scaleType, root);
                        String name = base + " – Caja " + (posIndex + 1);
                        ScalePatternEntity pe = new ScalePatternEntity();
                        pe.name = name;
                        pe.scaleType = scaleType;
                        pe.rootNote = (root != null && !root.isEmpty()) ? root : "E";
                        pe.startFret = start;
                        pe.endFret = end;
                        pe.positionIndex = posIndex;                           pe.system = "FLAMENCO";

                        long patternId = dao.insertPattern(pe);
                        insertedPatterns++;

                        List<ScaleNoteEntity> entities = mapFretNotesToEntities(notes, patternId);
                        insertedNotes += entities.size();
                        dao.insertNotes(entities);

                        posIndex++;
                    }

                } catch (Exception ex) {
                    Log.e(TAG, "❌ Error procesando ítem: " + (item != null ? item.id : "null"), ex);
                }
            }

            Log.i(TAG, " Seed ESCALAS: patrones=" + insertedPatterns + ", notas=" + insertedNotes);

        } catch (Exception e) {
            Log.e(TAG, "❌ Error en seedScalePatternsIntoRoom", e);
        }
    }

    private static RepoFile readRepoJson(Context context) {
        try {
            String json = JsonReader.readRawAsString(context, R.raw.pattern_repo);
            if (json == null || json.isEmpty()) {
                Log.w(TAG, "⚠️ pattern_repo.json vacío o nulo.");
                return null;
            }
            Log.d(TAG, "📄 pattern_repo.json leído (" + json.length() + " chars)");

            Gson gson = new Gson();
            try {
                RepoFile repo = gson.fromJson(json, RepoFile.class);
                if (repo != null) {
                    Log.d(TAG, "📄 JSON parseado como RepoFile | collection=" + repo.collection);
                    return repo;
                }
            } catch (Exception ignore) {
            }
            try {
                JsonElement root = JsonParser.parseString(json);
                if (root != null && root.isJsonArray()) {
                    Type t = new TypeToken<List<PatternItem>>() {
                    }.getType();
                    List<PatternItem> arr = new Gson().fromJson(root, t);
                    RepoFile rf = new RepoFile();
                    rf.collection = "root_array";
                    rf.items = arr;
                    return rf;
                }
            } catch (Exception ignore) {
            }

            return null;

        } catch (Exception e) {
            Log.e(TAG, "❌ Error leyendo pattern_repo.json", e);
            return null;
        }
    }


    private static List<NoteItem> extractExplicitNotesIfAny(PatternItem item, Gson gson) {
        if (item == null || item.notesRaw == null) return null;
        try {
            if (item.notesRaw.isJsonArray()) {
                Type t = new TypeToken<List<NoteItem>>() {
                }.getType();
                return gson.fromJson(item.notesRaw, t);
            }
        } catch (Exception e) {
            Log.w(TAG, "⚠️ 'notes' no es un array válido para posiciones en id=" + item.id);
        }
        return null;
    }

    private static List<ScaleFretNote> mapExplicitNotes(List<NoteItem> items) {
        List<ScaleFretNote> notes = new ArrayList<>();
        for (NoteItem n : items) {
            if (n == null) continue;
            String degree = normalizeDegreeLabel(n.degree);
            boolean isRoot = n.isRoot != null ? n.isRoot : "R".equals(degree);
            String noteName = (n.note != null && !n.note.isEmpty())
                    ? toSharp(n.note)
                    : computeNoteName(n.stringIndex, n.fret);
            notes.add(new ScaleFretNote(
                    safeInt(n.stringIndex, 0),
                    safeInt(n.fret, 0),
                    degree,
                    isRoot,
                     null,
                    noteName
            ));
        }
        return notes;
    }


    private static List<ScaleFretNote> normalizeGeneratedNotesToSharp(List<ScaleFretNote> in) {
        if (in == null) return Collections.emptyList();
        List<ScaleFretNote> out = new ArrayList<>(in.size());
        for (ScaleFretNote x : in) {
            if (x == null) continue;
            String sharp = toSharp(x.noteName);
            out.add(new ScaleFretNote(x.stringIndex, x.fret, x.degree, x.isRoot, x.finger, sharp));
        }
        return out;
    }

    private static List<ScaleNoteEntity> mapFretNotesToEntities(List<ScaleFretNote> gen, long patternId) {
        List<ScaleNoteEntity> out = new ArrayList<>();
        for (ScaleFretNote x : gen) {
            out.add(new ScaleNoteEntity(
                    patternId,
                    x.stringIndex,
                    x.fret,
                    x.degree,
                    x.isRoot,
                     x.finger,
                    x.noteName
            ));
        }
        return out;
    }

    private static String inferRootFromNotes(List<ScaleFretNote> notes) {
        if (notes == null) return "E";
        for (ScaleFretNote n : notes) if (n.isRoot) return toSharp(n.noteName);
        return "E";
    }


    private static String buildBaseName(String scaleType, String root) {
        String st = (scaleType != null && !scaleType.isEmpty()) ? scaleType : "Scale";
        return (root != null && !root.isEmpty()) ? (root + " " + st) : st;
    }


    private static String stripWindowFromName(String name) {
        return name.replaceAll("\\s*\\[[0-9]+\\s*[\u2013\\-]\\s*[0-9]+\\]$", "").trim();
    }

    private static String normalizeDegreeLabel(String deg) {
        if (deg == null) return "R";
        deg = deg.trim();
        switch (deg) {
            case "1":
                return "R";
            case "4":
                return "p4";
            case "5":
                return "p5";
            default:
                return deg;
        }
    }


    private static String computeNoteName(Integer stringIndex, Integer fret) {
        final String[] NOTE_SHARP = {"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"};
        final String[] OPEN_STRINGS = {"E", "A", "D", "G", "B", "E"};         int s = safeInt(stringIndex, 0);
        int f = safeInt(fret, 0);
        String open = OPEN_STRINGS[Math.max(0, Math.min(5, s))];
        int openIdx = 0;
        for (int i = 0; i < NOTE_SHARP.length; i++) {
            if (NOTE_SHARP[i].equalsIgnoreCase(open)) {
                openIdx = i;
                break;
            }
        }
        return NOTE_SHARP[(openIdx + f) % 12];
    }


    private static String toSharp(String s) {
        if (s == null) return "";
        String up = s.trim().toUpperCase(Locale.ROOT);
        switch (up) {
            case "DB":
                return "C#";
            case "EB":
                return "D#";
            case "GB":
                return "F#";
            case "AB":
                return "G#";
            case "BB":
                return "A#";
            case "E#":
                return "F";
            case "B#":
                return "C";
            default:
                return up;
        }
    }

    private static int safeInt(Integer v, int def) {
        return v != null ? v : def;
    }

    private static int sizeOrZero(List<?> l) {
        return l == null ? 0 : l.size();
    }

    private static String detectScaleType(List<String> degrees) {
        if (degrees == null || degrees.isEmpty()) return "Scale";
        Set<String> s = new LinkedHashSet<>(degrees);
        if (s.equals(set("1", "2", "3", "4", "5", "6", "7"))) return "Ionian";
        if (s.equals(set("1", "2", "b3", "4", "5", "b6", "b7"))) return "Aeolian";
        if (s.equals(set("1", "2", "b3", "4", "5", "6", "b7"))) return "Dorian";
        if (s.equals(set("1", "b2", "b3", "4", "5", "b6", "b7"))) return "Phrygian";
        if (s.equals(set("1", "b2", "3", "4", "5", "b6", "b7"))) return "Phrygian Dominant";
        if (s.equals(set("1", "2", "b3", "4", "5", "b6", "7"))) return "Harmonic Minor";
        if (s.equals(set("1", "2", "b3", "4", "5", "6", "7"))) return "Melodic Minor (Asc)";
        if (s.equals(set("1", "b2", "3", "4", "5", "b6", "7"))) return "Double Harmonic Major";
        return "Scale";
    }

    private static Set<String> set(String... xs) {
        return new LinkedHashSet<>(Arrays.asList(xs));
    }


    private static List<int[]> defaultWindowsFor(String scaleType, String root) {
        List<int[]> w = new ArrayList<>();
        String r = (root == null) ? "" : root.toUpperCase(Locale.ROOT);
        if ("E".equals(r) || "F".equals(r) || "D#".equals(r)) {
            w.add(new int[]{0, 3});
            w.add(new int[]{2, 5});
            w.add(new int[]{5, 8});
        } else if ("A".equals(r) || "G#".equals(r) || "A#".equals(r)) {
            w.add(new int[]{5, 8});
            w.add(new int[]{7, 10});
            w.add(new int[]{9, 12});
        } else {
            w.add(new int[]{0, 4});
            w.add(new int[]{5, 9});
            w.add(new int[]{7, 11});
        }
        return w;
    }

    private static String normalizeScaleTypeAlias(String st) {
        if (st == null) return "Scale";
        String k = st.trim().toLowerCase(Locale.ROOT);
        if (k.equals("ionion")) return "Ionian";
        if (k.equals("flamenco mode (major-phrygian)")
                || k.equals("flamenco mode")
                || k.equals("major-phrygian")
                || k.equals("major phrygian")
                || k.equals("spanish phrygian")
                || k.equals("phrygian dominant (flamenco)")) {
            return "Phrygian Dominant";
        }
        if (k.equals("pentatonic major") || k.equals("major pentatonic")) return "Major Pentatonic";
        if (k.equals("pentatonic minor") || k.equals("minor pentatonic")) return "Minor Pentatonic";
        if (k.equals("spanish 8 tone") || k.equals("spanish 8-tone")) return "Spanish 8-Tone";
        if (k.equals("melodic minor (asc)") || k.equals("melodic minor asc") || k.equals("melodic minor")) {
            return "Melodic Minor (Asc)";
        }
        return st.substring(0, 1).toUpperCase(Locale.ROOT) + st.substring(1);
    }

    private static final class RepoFile {
        @SerializedName("version")
        String version;
        @SerializedName("collection")
        String collection;

        @SerializedName("patterns")
        List<PatternItem> patterns;
        @SerializedName("scales")
        List<PatternItem> scales;
        @SerializedName("items")
        List<PatternItem> items;

        List<PatternItem> normalizedItems() {
            if (patterns != null && !patterns.isEmpty()) return patterns;
            if (scales != null && !scales.isEmpty()) return scales;
            if (items != null && !items.isEmpty()) return items;
            return Collections.emptyList();
        }
    }

    private static final class PatternItem {
        @SerializedName("id")
        String id;
        @SerializedName("name")
        String name;
        @SerializedName("scaleType")
        String scaleType;
        @SerializedName("root")
        String root;                       @SerializedName("tonic")
        String tonicAlias;                 @SerializedName("example_notes")
        Map<String, List<String>> exampleNotes;
        @SerializedName("scaleDegrees")
        List<String> scaleDegrees;             @SerializedName("degrees")
        List<String> degreesAlias;             @SerializedName("formula")
        List<String> formulaAlias;             @SerializedName("intervals")
        List<String> intervalsAlias;           @SerializedName("degree_formula")
        List<String> degreeFormulaSnake;
        @SerializedName("window")
        int[] window;
        @SerializedName("windows")
        int[][] windows;
        @SerializedName("notes")
        JsonElement notesRaw;

        @SerializedName("difficulty")
        Integer difficulty;
        @SerializedName("aliases")
        List<String> aliases;

        void normalizeAliases() {
            if (root == null || root.isEmpty()) {
                if (tonicAlias != null && !tonicAlias.isEmpty()) {
                    root = tonicAlias;
                } else if (exampleNotes != null && !exampleNotes.isEmpty()) {
                    for (String k : exampleNotes.keySet()) {
                        if (k != null && !k.trim().isEmpty()) {
                            root = k.trim();
                            break;
                        }
                    }
                }
            }
            if (scaleDegrees == null || scaleDegrees.isEmpty()) {
                if (degreesAlias != null && !degreesAlias.isEmpty()) {
                    scaleDegrees = degreesAlias;
                } else if (formulaAlias != null && !formulaAlias.isEmpty()) {
                    scaleDegrees = formulaAlias;
                } else if (intervalsAlias != null && !intervalsAlias.isEmpty()) {
                    scaleDegrees = intervalsAlias;
                } else if (degreeFormulaSnake != null && !degreeFormulaSnake.isEmpty()) {
                    scaleDegrees = degreeFormulaSnake;
                }
            }
        }
    }


    private static final class NoteItem {
        @SerializedName("stringIndex")
        Integer stringIndex;         @SerializedName("fret")
        Integer fret;
        @SerializedName("degree")
        String degree;               @SerializedName("isRoot")
        Boolean isRoot;              @SerializedName("note")
        String note;             }

    private static String localizeScaleType(String st) {
        if (st == null) return "Escala";
        String key = st.trim().toLowerCase(Locale.ROOT);
        switch (key) {
            case "ionian":
                return "Mayor";
            case "aeolian":
                return "Menor natural";
            case "phrygian":
                return "Frigia";
            case "phrygian dominant":
                return "Frigia dominante";
            case "harmonic minor":
                return "Menor armónica";
            case "melodic minor (asc)":
                return "Menor melódica";
            case "dorian":
                return "Dórica";
            case "mixolydian":
                return "Mixolidia";
            case "lydian":
                return "Lidia";
            case "locrian":
                return "Locria";
            case "double harmonic major":
                return "Doble armónica mayor";
            case "spanish 8-tone":
                return "Española 8 tonos";
            case "flamenco mode (major-phrygian)":
                return "Modo flamenco (Mayor-Frigio)";
            case "pentatonic major":
            case "major pentatonic":
                return "Pentatónica mayor";
            case "pentatonic minor":
            case "minor pentatonic":
                return "Pentatónica menor";
            case "blues":
                return "Blues";
            default:
                return st.substring(0, 1).toUpperCase(Locale.ROOT) + st.substring(1);
        }
    }


    private static int defaultDifficultyForScaleType(String st) {
        if (st == null) return 3;
        String key = st.trim().toLowerCase(Locale.ROOT);
        switch (key) {
            case "phrygian":
                return 1;
            case "phrygian dominant":
                return 1;
            case "major pentatonic":
            case "pentatonic major":
                return 1;
            case "minor pentatonic":
            case "pentatonic minor":
                return 1;
            case "ionian":
                return 2;
            case "aeolian":
                return 2;
            case "dorian":
                return 2;
            case "mixolydian":
                return 2;
            case "harmonic minor":
                return 2;
            case "blues":
                return 2;
            case "flamenco mode (major-phrygian)":
                return 1;
            case "lydian":
                return 4;
            case "double harmonic major":
                return 4;
            case "spanish 8 tone":
            case "spanish 8-tone":
                return 4;
            case "locrian":
                return 5;
            case "melodic minor (asc)":
            case "melodic minor":
                return 5;

            default:
                return 3;
        }
    }

        private static int diffToTier(int diff) {
        if (diff <= 1) return 0;              if (diff <= 3) return 1;              return 2;                         }
}
