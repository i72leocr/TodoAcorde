package com.tuguitar.todoacorde.practice.data;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;

import com.tuguitar.todoacorde.AppExecutors;
import com.tuguitar.todoacorde.Chord;
import com.tuguitar.todoacorde.ChordDao;
import com.tuguitar.todoacorde.FavoriteSong;
import com.tuguitar.todoacorde.FavoriteSongDao;
import com.tuguitar.todoacorde.ProgressionDetail;
import com.tuguitar.todoacorde.ProgressionDetailDao;
import com.tuguitar.todoacorde.ProgressionSession;
import com.tuguitar.todoacorde.ProgressionSessionDao;
import com.tuguitar.todoacorde.songs.data.SongChordWithInfo;
import com.tuguitar.todoacorde.SongWithDetails;
import com.tuguitar.todoacorde.songs.data.SongChord;
import com.tuguitar.todoacorde.songs.data.SongChordDao;
import com.tuguitar.todoacorde.songs.data.SongDao;
import com.tuguitar.todoacorde.songs.data.SongLyric;
import com.tuguitar.todoacorde.songs.data.SongLyricDao;
import com.tuguitar.todoacorde.todoAcordeDatabase;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import javax.inject.Inject;
import javax.inject.Singleton;

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
    private final ProgressionSessionDao progressionSessionDao;
    private final ProgressionDetailDao progressionDetailDao;

    private final Map<Integer, LiveData<SongWithDetails>> songDetailsCache = new HashMap<>();
    private LiveData<List<Chord>> allChords;
    private final Map<String, LiveData<Integer>> bestScoreCache = new HashMap<>();
    private final Map<Integer, LiveData<List<SongChord>>> chordsForSongCache = new HashMap<>();
    private final Map<Integer, LiveData<List<SongLyric>>> lyricsForSongCache = new HashMap<>();
    private final Map<Integer, LiveData<List<SongChordWithInfo>>> chordsWithInfoCache = new HashMap<>();

    @Inject
    public PracticeRepository(
            SongDao songDao,
            SongChordDao songChordDao,
            SongLyricDao songLyricDao,
            ChordDao chordDao,
            PracticeSessionDao practiceSessionDao,
            PracticeDetailDao practiceDetailDao,
            SongUserSpeedDao songUserSpeedDao,
            FavoriteSongDao favoriteSongDao,
            ProgressionSessionDao progressionSessionDao,
            ProgressionDetailDao progressionDetailDao
    ) {
        this.songDao               = songDao;
        this.songChordDao          = songChordDao;
        this.songLyricDao          = songLyricDao;
        this.chordDao              = chordDao;
        this.practiceSessionDao    = practiceSessionDao;
        this.practiceDetailDao     = practiceDetailDao;
        this.songUserSpeedDao      = songUserSpeedDao;
        this.favoriteSongDao       = favoriteSongDao;
        this.progressionSessionDao = progressionSessionDao;
        this.progressionDetailDao  = progressionDetailDao;
    }

    public LiveData<SongWithDetails> getSongDetails(int songId) {
        return songDao.getSongWithDetails(songId);
    }

    public void clearCache() {
        Log.d(TAG, "Clearing all caches");
        songDetailsCache.clear();
        allChords = null;
        bestScoreCache.clear();
        chordsForSongCache.clear();
        lyricsForSongCache.clear();
        chordsWithInfoCache.clear();
    }

    public LiveData<List<Chord>> getAllChords() {
        if (allChords == null) {
            Log.d(TAG, "DB fetch for allChords");
            allChords = chordDao.getAllChords();
        } else {
            Log.d(TAG, "Cache hit for allChords");
        }
        return allChords;
    }

    public LiveData<SongUserSpeed> getSongUserSpeed(int songId, int userId) {
        return songUserSpeedDao.getSongSpeedForUser(songId, userId);
    }


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

    public void ensureSpeedRecordExists(int songId, int userId) {
        AppExecutors.getInstance().diskIO().execute(() -> {
            SongUserSpeed existing = songUserSpeedDao.getSongUserSpeedSync(songId, userId);
            if (existing == null) {
                songUserSpeedDao.insertOrUpdate(new SongUserSpeed(songId, userId,true,false,false));
            }
        });
    }


    public LiveData<SongWithDetails> getSongWithDetails(int songId) {
        if (!songDetailsCache.containsKey(songId)) {
            Log.d(TAG, "DB fetch for SongWithDetails songId=" + songId);
            songDetailsCache.put(songId, songDao.getSongWithDetails(songId));
        } else {
            Log.d(TAG, "Cache hit for SongWithDetails songId=" + songId);
        }
        return songDetailsCache.get(songId);
    }

    public LiveData<List<SongChord>> getChordsForSong(int songId) {
        if (!chordsForSongCache.containsKey(songId)) {
            Log.d(TAG, "DB fetch for chordsForSong songId=" + songId);
            chordsForSongCache.put(songId, songChordDao.getChordsForSong(songId));
        } else {
            Log.d(TAG, "Cache hit for chordsForSong songId=" + songId);
        }
        return chordsForSongCache.get(songId);
    }

    public LiveData<List<SongLyric>> getLyricsForSong(int songId) {
        if (!lyricsForSongCache.containsKey(songId)) {
            Log.d(TAG, "DB fetch for lyricsForSong songId=" + songId);
            lyricsForSongCache.put(songId, songLyricDao.getLyricsForSong(songId));
        } else {
            Log.d(TAG, "Cache hit for lyricsForSong songId=" + songId);
        }
        return lyricsForSongCache.get(songId);
    }

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

    /** Inserta la sesión y sus detalles de práctica (resumen por acorde) */
    public void saveSessionWithDetails(PracticeSession session, List<PracticeDetail> details) {
        todoAcordeDatabase.databaseWriteExecutor.execute(() -> {
            long id = practiceSessionDao.insertSessionWithFlags(session);
            for (PracticeDetail d : details) {
                d.sessionId = (int) id;
            }
            practiceDetailDao.insertDetails(details);
        });
    }

    public long insertSessionReturningId(PracticeSession session) {
        Log.d(TAG, "Inserting PracticeSession for songId=" + session.songId);
        return practiceSessionDao.insertReturningId(session);
    }

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

    public LiveData<Integer> getLastScore(int songId, int userId, float speed) {
        return Transformations.map(
                practiceSessionDao.getLastSession(songId, userId, speed),
                ps -> ps != null ? ps.totalScore : 0
        );
    }

    public LiveData<List<Integer>> getFavoriteSongIds(int userId) {
        Log.d(TAG, "Fetching favoriteSongIds for userId=" + userId);
        return favoriteSongDao.getFavoriteSongIds(userId);
    }

    public LiveData<Boolean> isFavorite(int userId, int songId) {
        Log.d(TAG, "Fetching isFavorite for userId=" + userId + ", songId=" + songId);
        return favoriteSongDao.isFavorite(userId, songId);
    }

    public LiveData<SongUserSpeed> getUserSpeed(int userId, int songId) {
        Log.d(TAG, "Fetching userSpeed for userId=" + userId + ", songId=" + songId);
        return songUserSpeedDao.getSongSpeedForUser(userId, songId);
    }

    public void insertOrUpdatePracticeSession(PracticeSession session) {
        Log.d(TAG, "Inserting/updating PracticeSession id=" + session.id);
        practiceSessionDao.insertOrUpdateSession(session);
    }

    public void insertOrUpdateUserSpeed(SongUserSpeed speed) {
        Log.d(TAG, "Inserting/updating UserSpeed for songId=" + speed.songId + ", userId=" + speed.userId);
        songUserSpeedDao.insertOrUpdate(speed);
    }

    public void addFavorite(FavoriteSong fav) {
        Log.d(TAG, "Adding FavoriteSong songId=" + fav.songId + ", userId=" + fav.userId);
        favoriteSongDao.insert(fav);
    }

    public void removeFavorite(FavoriteSong fav) {
        Log.d(TAG, "Removing FavoriteSong songId=" + fav.songId + ", userId=" + fav.userId);
        favoriteSongDao.delete(fav);
    }

    public void insertOrUpdateProgressionSession(ProgressionSession session) {
        Log.d(TAG, "Inserting/updating ProgressionSession id=" + session.id);
        progressionSessionDao.insertOrUpdateSession(session);
    }

    public void insertProgressionDetails(List<ProgressionDetail> details) {
        Log.d(TAG, "Inserting ProgressionDetails size=" + details.size());
        progressionDetailDao.insertDetails(details);
    }

    public LiveData<List<PracticeDetailDao.ChordPercentage>> getTopErroredChords(int songId, long since) {
        Log.d(TAG, "Fetching topErroredChords for songId=" + songId + ", since=" + since);
        return practiceDetailDao.getTopErroredChordsByPercentage(songId, since);
    }

    public LiveData<List<PracticeDetailDao.ChordPercentage>> getTopSuccessfulChords(int songId, long since) {
        Log.d(TAG, "Fetching topSuccessfulChords for songId=" + songId + ", since=" + since);
        return practiceDetailDao.getTopSuccessfulChordsByPercentage(songId, since);
    }

    public LiveData<List<PracticeDetailDao.ChordAccuracy>> getChordAccuracies(int sessionId) {
        Log.d(TAG, "Fetching chordAccuracies for sessionId=" + sessionId);
        return practiceDetailDao.getChordAccuraciesForSession(sessionId);
    }

    public LiveData<Integer> getMostCorrectChord(int songId) {
        Log.d(TAG, "Fetching mostCorrectChord for songId=" + songId);
        return practiceDetailDao.getMostCorrectChord(songId);
    }

    public LiveData<Integer> getMostFailedChord(int songId) {
        Log.d(TAG, "Fetching mostFailedChord for songId=" + songId);
        return practiceDetailDao.getMostFailedChord(songId);
    }

    public LiveData<PracticeDetailDao.SongStats> getSongStats(int songId) {
        Log.d(TAG, "Fetching songStats for songId=" + songId);
        return practiceDetailDao.getSongStats(songId);
    }
}
