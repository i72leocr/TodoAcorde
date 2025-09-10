package com.todoacorde.todoacorde.practice.data;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;

import com.todoacorde.todoacorde.AppExecutors;
import com.todoacorde.todoacorde.Chord;
import com.todoacorde.todoacorde.ChordDao;
import com.todoacorde.todoacorde.SongWithDetails;
import com.todoacorde.todoacorde.songs.data.FavoriteSong;
import com.todoacorde.todoacorde.songs.data.FavoriteSongDao;
import com.todoacorde.todoacorde.songs.data.SongChord;
import com.todoacorde.todoacorde.songs.data.SongChordDao;
import com.todoacorde.todoacorde.songs.data.SongChordWithInfo;
import com.todoacorde.todoacorde.songs.data.SongDao;
import com.todoacorde.todoacorde.songs.data.SongLyric;
import com.todoacorde.todoacorde.songs.data.SongLyricDao;
import com.todoacorde.todoacorde.todoAcordeDatabase;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Repositorio de práctica.
 *
 * Orquesta el acceso a datos relacionados con canciones, acordes, sesiones de práctica,
 * detalles por acorde, velocidades de usuario y favoritos. Implementa cachés en memoria
 * para reducir lecturas repetidas a Room. Las operaciones de escritura se realizan fuera
 * del hilo principal.
 */
@Singleton
public class PracticeRepository {
    private static final String TAG = "PracticeRepository";

    private final SongDao songDao;
    private final SongChordDao songChordDao;
    private final SongLyricDao songLyricDao;
    private final ChordDao chordDao;
    private final PracticeSessionDao practiceSessionDao;
    private final PracticeDetailDao practiceDetailDao;
    private final SongUserSpeedDao songUserSpeedDao;
    private final FavoriteSongDao favoriteSongDao;

    /** Caché de detalles completos por canción. */
    private final Map<Integer, LiveData<SongWithDetails>> songDetailsCache = new HashMap<>();
    /** Caché de todos los acordes. */
    private LiveData<List<Chord>> allChords;
    /** Caché de mejor puntuación por clave compuesta songId:userId:speed. */
    private final Map<String, LiveData<Integer>> bestScoreCache = new HashMap<>();
    /** Caché de acordes por canción. */
    private final Map<Integer, LiveData<List<SongChord>>> chordsForSongCache = new HashMap<>();
    /** Caché de letras por canción. */
    private final Map<Integer, LiveData<List<SongLyric>>> lyricsForSongCache = new HashMap<>();
    /** Caché de acordes con información adicional por canción. */
    private final Map<Integer, LiveData<List<SongChordWithInfo>>> chordsWithInfoCache = new HashMap<>();

    /**
     * Crea el repositorio con los DAOs necesarios.
     */
    @Inject
    public PracticeRepository(
            SongDao songDao,
            SongChordDao songChordDao,
            SongLyricDao songLyricDao,
            ChordDao chordDao,
            PracticeSessionDao practiceSessionDao,
            PracticeDetailDao practiceDetailDao,
            SongUserSpeedDao songUserSpeedDao,
            FavoriteSongDao favoriteSongDao
    ) {
        this.songDao = songDao;
        this.songChordDao = songChordDao;
        this.songLyricDao = songLyricDao;
        this.chordDao = chordDao;
        this.practiceSessionDao = practiceSessionDao;
        this.practiceDetailDao = practiceDetailDao;
        this.songUserSpeedDao = songUserSpeedDao;
        this.favoriteSongDao = favoriteSongDao;
    }

    /**
     * Devuelve los detalles completos de una canción.
     */
    public LiveData<SongWithDetails> getSongDetails(int songId) {
        return songDao.getSongWithDetails(songId);
    }

    /**
     * Limpia todas las cachés en memoria.
     */
    public void clearCache() {
        Log.d(TAG, "Clearing all caches");
        songDetailsCache.clear();
        allChords = null;
        bestScoreCache.clear();
        chordsForSongCache.clear();
        lyricsForSongCache.clear();
        chordsWithInfoCache.clear();
    }

    /**
     * Devuelve todos los acordes disponibles. Usa caché si está poblada.
     */
    public LiveData<List<Chord>> getAllChords() {
        if (allChords == null) {
            Log.d(TAG, "DB fetch for allChords");
            allChords = chordDao.getAllChords();
        } else {
            Log.d(TAG, "Cache hit for allChords");
        }
        return allChords;
    }

    /**
     * Observa el estado de velocidad desbloqueada del usuario para una canción.
     */
    public LiveData<SongUserSpeed> getSongUserSpeed(int songId, int userId) {
        return songUserSpeedDao.getSongSpeedForUser(songId, userId);
    }

    /**
     * Intenta desbloquear la siguiente velocidad para una canción y usuario dados.
     * Ejecuta el resultado en el callback recibido. El trabajo se realiza en discoIO.
     *
     * Regla de desbloqueo:
     * - Si la velocidad actual es 0.5x, se desbloquea 0.75x.
     * - Si la velocidad actual es 0.75x, se desbloquea 1x.
     */
    public void tryUnlockNextSpeed(int songId, int userId, float currentSpeed, Consumer<Boolean> callback) {
        AppExecutors.getInstance().diskIO().execute(() -> {
            SongUserSpeed sus = songUserSpeedDao.getSongUserSpeedSync(songId, userId);
            if (sus == null) {
                callback.accept(false);
                return;
            }

            boolean unlocked = false;

            if (currentSpeed == 0.5f && !sus.isUnlocked0_75x) {
                sus.isUnlocked0_75x = true;
                unlocked = true;
            } else if (currentSpeed == 0.75f && !sus.isUnlocked1x) {
                sus.isUnlocked1x = true;
                unlocked = true;
            }

            if (unlocked) {
                songUserSpeedDao.insertOrUpdate(sus);
            }

            callback.accept(unlocked);
        });
    }

    /**
     * Asegura que exista un registro de velocidad para la pareja (songId, userId).
     * Si no existe, crea uno con 0.5x desbloqueado por defecto.
     */
    public void ensureSpeedRecordExists(int songId, int userId) {
        AppExecutors.getInstance().diskIO().execute(() -> {
            SongUserSpeed existing = songUserSpeedDao.getSongUserSpeedSync(songId, userId);
            if (existing == null) {
                songUserSpeedDao.insertOrUpdate(new SongUserSpeed(songId, userId, true, false, false));
            }
        });
    }

    /**
     * Devuelve y cachea los detalles completos de una canción.
     */
    public LiveData<SongWithDetails> getSongWithDetails(int songId) {
        if (!songDetailsCache.containsKey(songId)) {
            Log.d(TAG, "DB fetch for SongWithDetails songId=" + songId);
            songDetailsCache.put(songId, songDao.getSongWithDetails(songId));
        } else {
            Log.d(TAG, "Cache hit for SongWithDetails songId=" + songId);
        }
        return songDetailsCache.get(songId);
    }

    /**
     * Devuelve y cachea la lista de acordes de una canción.
     */
    public LiveData<List<SongChord>> getChordsForSong(int songId) {
        if (!chordsForSongCache.containsKey(songId)) {
            Log.d(TAG, "DB fetch for chordsForSong songId=" + songId);
            chordsForSongCache.put(songId, songChordDao.getChordsForSong(songId));
        } else {
            Log.d(TAG, "Cache hit for chordsForSong songId=" + songId);
        }
        return chordsForSongCache.get(songId);
    }

    /**
     * Devuelve y cachea la lista de líneas de letra de una canción.
     */
    public LiveData<List<SongLyric>> getLyricsForSong(int songId) {
        if (!lyricsForSongCache.containsKey(songId)) {
            Log.d(TAG, "DB fetch for lyricsForSong songId=" + songId);
            lyricsForSongCache.put(songId, songLyricDao.getLyricsForSong(songId));
        } else {
            Log.d(TAG, "Cache hit for lyricsForSong songId=" + songId);
        }
        return lyricsForSongCache.get(songId);
    }

    /**
     * Devuelve y cachea la lista de acordes con información auxiliar unificada.
     */
    public LiveData<List<SongChordWithInfo>> getChordsWithInfoForSong(int songId) {
        if (!chordsWithInfoCache.containsKey(songId)) {
            Log.d(TAG, "DB fetch for chordsWithInfo songId=" + songId);
            chordsWithInfoCache.put(
                    songId,
                    songChordDao.getChordsWithInfoForSong(songId)
            );
        } else {
            Log.d(TAG, "Cache hit for chordsWithInfo songId=" + songId);
        }
        return chordsWithInfoCache.get(songId);
    }

    /**
     * Inserta una sesión y sus detalles en una operación de escritura.
     * Actualiza en memoria el sessionId de cada detalle con el id generado.
     */
    public void saveSessionWithDetails(PracticeSession session, List<PracticeDetail> details) {
        todoAcordeDatabase.databaseWriteExecutor.execute(() -> {
            long id = practiceSessionDao.insertSessionWithFlags(session);
            for (PracticeDetail d : details) {
                d.sessionId = (int) id;
            }
            practiceDetailDao.insertDetails(details);
        });
    }

    /**
     * Inserta una sesión y devuelve el id generado.
     */
    public long insertSessionReturningId(PracticeSession session) {
        Log.d(TAG, "Inserting PracticeSession for songId=" + session.songId);
        return practiceSessionDao.insertReturningId(session);
    }

    /**
     * Devuelve el mejor score para una clave (songId, userId, speed) con caché.
     */
    public LiveData<Integer> getBestScore(int songId, int userId, float speed) {
        String key = songId + ":" + userId + ":" + speed;
        if (!bestScoreCache.containsKey(key)) {
            Log.d(TAG, "DB fetch for bestScore key=" + key);
            bestScoreCache.put(key, practiceSessionDao.getBestScore(songId, userId, speed));
        } else {
            Log.d(TAG, "Cache hit for bestScore key=" + key);
        }
        return bestScoreCache.get(key);
    }

    /**
     * Devuelve el último score de una sesión para una clave (songId, userId, speed).
     * Si no hay registro, devuelve 0.
     */
    public LiveData<Integer> getLastScore(int songId, int userId, float speed) {
        return Transformations.map(
                practiceSessionDao.getLastSession(songId, userId, speed),
                ps -> ps != null ? ps.totalScore : 0
        );
    }

    /**
     * Observa los ids de canciones marcadas como favoritas por un usuario.
     */
    public LiveData<List<Integer>> getFavoriteSongIds(int userId) {
        Log.d(TAG, "Fetching favoriteSongIds for userId=" + userId);
        return favoriteSongDao.getFavoriteSongIds(userId);
    }

    /**
     * Indica si una canción es favorita para un usuario.
     */
    public LiveData<Boolean> isFavorite(int userId, int songId) {
        Log.d(TAG, "Fetching isFavorite for userId=" + userId + ", songId=" + songId);
        return favoriteSongDao.isFavorite(userId, songId);
    }

    /**
     * Observa el estado de velocidades desbloqueadas del usuario para una canción.
     * Variante con parámetros invertidos respecto a getSongUserSpeed.
     */
    public LiveData<SongUserSpeed> getUserSpeed(int userId, int songId) {
        Log.d(TAG, "Fetching userSpeed for userId=" + userId + ", songId=" + songId);
        return songUserSpeedDao.getSongSpeedForUser(userId, songId);
    }

    /**
     * Inserta o actualiza una sesión de práctica.
     */
    public void insertOrUpdatePracticeSession(PracticeSession session) {
        Log.d(TAG, "Inserting/updating PracticeSession id=" + session.id);
        practiceSessionDao.insertOrUpdateSession(session);
    }

    /**
     * Inserta o actualiza el estado de velocidad de usuario por canción.
     */
    public void insertOrUpdateUserSpeed(SongUserSpeed speed) {
        Log.d(TAG, "Inserting/updating UserSpeed for songId=" + speed.songId + ", userId=" + speed.userId);
        songUserSpeedDao.insertOrUpdate(speed);
    }

    /**
     * Añade una canción a favoritos.
     */
    public void addFavorite(FavoriteSong fav) {
        Log.d(TAG, "Adding FavoriteSong songId=" + fav.songId + ", userId=" + fav.userId);
        favoriteSongDao.insert(fav);
    }

    /**
     * Elimina una canción de favoritos.
     */
    public void removeFavorite(FavoriteSong fav) {
        Log.d(TAG, "Removing FavoriteSong songId=" + fav.songId + ", userId=" + fav.userId);
        favoriteSongDao.delete(fav);
    }

    /**
     * Devuelve el top de acordes con mayor porcentaje de error desde una fecha.
     * El porcentaje se calcula como incorrectCount * 100 / totalAttempts.
     */
    public LiveData<List<PracticeDetailDao.ChordPercentage>> getTopErroredChords(int songId, long since) {
        Log.d(TAG, "Fetching topErroredChords for songId=" + songId + ", since=" + since);
        return practiceDetailDao.getTopErroredChordsByPercentage(songId, since);
    }

    /**
     * Devuelve el top de acordes con mayor porcentaje de acierto desde una fecha.
     * El porcentaje se calcula como correctCount * 100 / totalAttempts.
     */
    public LiveData<List<PracticeDetailDao.ChordPercentage>> getTopSuccessfulChords(int songId, long since) {
        Log.d(TAG, "Fetching topSuccessfulChords for songId=" + songId + ", since=" + since);
        return practiceDetailDao.getTopSuccessfulChordsByPercentage(songId, since);
    }

    /**
     * Devuelve los porcentajes de acierto por acorde para una sesión.
     */
    public LiveData<List<PracticeDetailDao.ChordAccuracy>> getChordAccuracies(int sessionId) {
        Log.d(TAG, "Fetching chordAccuracies for sessionId=" + sessionId);
        return practiceDetailDao.getChordAccuraciesForSession(sessionId);
    }

    /**
     * Devuelve el identificador del acorde con más aciertos acumulados para una canción.
     */
    public LiveData<Integer> getMostCorrectChord(int songId) {
        Log.d(TAG, "Fetching mostCorrectChord for songId=" + songId);
        return practiceDetailDao.getMostCorrectChord(songId);
    }

    /**
     * Devuelve el identificador del acorde con más fallos acumulados para una canción.
     */
    public LiveData<Integer> getMostFailedChord(int songId) {
        Log.d(TAG, "Fetching mostFailedChord for songId=" + songId);
        return practiceDetailDao.getMostFailedChord(songId);
    }

    /**
     * Devuelve estadísticas agregadas de práctica para una canción.
     * Incluye totales de intentos, aciertos y fallos.
     */
    public LiveData<PracticeDetailDao.SongStats> getSongStats(int songId) {
        Log.d(TAG, "Fetching songStats for songId=" + songId);
        return practiceDetailDao.getSongStats(songId);
    }
}
