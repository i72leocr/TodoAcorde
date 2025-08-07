package com.tuguitar.todoacorde.practice.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import java.util.List;

@Dao
public interface PracticeDetailDao {



    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertDetails(List<PracticeDetail> details);

    @Query("SELECT * FROM practice_details")
    List<PracticeDetail> getAllRaw(); // método síncrono para uso en background

    @Update
    void updatePracticeDetail(PracticeDetail detail);

    @Query("SELECT * FROM practice_details WHERE sessionId = :sessionId AND chordId = :chordId LIMIT 1")
    PracticeDetail getDetailBySessionAndChord(int sessionId, int chordId);

    @Query("SELECT * FROM practice_details")
    LiveData<List<PracticeDetail>> getAll();

    // ✅ Precisión por acorde en una sesión
    @Query("SELECT chordId, " +
            "correctCount * 100.0 / totalAttempts AS percentage " +
            "FROM practice_details " +
            "WHERE sessionId = :sessionId")
    LiveData<List<ChordAccuracy>> getChordAccuraciesForSession(int sessionId);

    // ✅ Detalles de una sesión y acorde
    @Query("SELECT * FROM practice_details " +
            "WHERE sessionId = :sessionId AND chordId = :chordId")
    LiveData<PracticeDetail> getDetailsForSessionChord(int sessionId, int chordId);

    // ✅ Todos los detalles de una canción desde cierta fecha
    @Query("SELECT pd.* " +
            "FROM practice_details pd " +
            "JOIN practice_sessions ps ON ps.id = pd.sessionId " +
            "WHERE ps.songId = :songId " +
            "  AND (:since = 0 OR ps.startTime >= :since)")
    LiveData<List<PracticeDetail>> getDetailsForSongSince(int songId, long since);

    // ✅ Conteos de correctos/incorrectos desde cierta fecha
    @Query("SELECT SUM(pd.correctCount) " +
            "FROM practice_details pd " +
            "JOIN practice_sessions ps ON ps.id = pd.sessionId " +
            "WHERE ps.songId = :songId AND ps.startTime >= :since")
    LiveData<Integer> countCorrectForSongSince(int songId, long since);

    @Query("SELECT SUM(pd.incorrectCount) " +
            "FROM practice_details pd " +
            "JOIN practice_sessions ps ON ps.id = pd.sessionId " +
            "WHERE ps.songId = :songId AND ps.startTime >= :since")
    LiveData<Integer> countIncorrectForSongSince(int songId, long since);

    // ✅ Top 3 acordes con más errores (%)
    @Query("SELECT pd.chordId AS chordId, " +
            "pd.incorrectCount * 100.0 / pd.totalAttempts AS percentage " +
            "FROM practice_details pd " +
            "JOIN practice_sessions ps ON ps.id = pd.sessionId " +
            "WHERE ps.songId = :songId AND ps.startTime >= :since " +
            "GROUP BY pd.chordId " +
            "ORDER BY percentage DESC " +
            "LIMIT 3")
    LiveData<List<ChordPercentage>> getTopErroredChordsByPercentage(int songId, long since);

    // ✅ Top 3 acordes con más aciertos (%)
    @Query("SELECT pd.chordId AS chordId, " +
            "pd.correctCount * 100.0 / pd.totalAttempts AS percentage " +
            "FROM practice_details pd " +
            "JOIN practice_sessions ps ON ps.id = pd.sessionId " +
            "WHERE ps.songId = :songId AND ps.startTime >= :since " +
            "GROUP BY pd.chordId " +
            "ORDER BY percentage DESC " +
            "LIMIT 3")
    LiveData<List<ChordPercentage>> getTopSuccessfulChordsByPercentage(int songId, long since);

    // ✅ Top 3 nombres de acordes correctos/fallados (por cantidad)
    @Query("SELECT c.name " +
            "FROM practice_details pd " +
            "JOIN practice_sessions ps ON ps.id = pd.sessionId " +
            "JOIN chords c ON pd.chordId = c.id " +
            "WHERE ps.songId = :songId " +
            "GROUP BY pd.chordId " +
            "ORDER BY SUM(pd.correctCount) DESC " +
            "LIMIT 3")
    LiveData<List<String>> findTop3CorrectChords(int songId);

    @Query("SELECT c.name " +
            "FROM practice_details pd " +
            "JOIN practice_sessions ps ON ps.id = pd.sessionId " +
            "JOIN chords c ON pd.chordId = c.id " +
            "WHERE ps.songId = :songId " +
            "GROUP BY pd.chordId " +
            "ORDER BY SUM(pd.incorrectCount) DESC " +
            "LIMIT 3")
    LiveData<List<String>> findTop3FailedChords(int songId);

    // ✅ Estadísticas agregadas por canción
    @Query("SELECT SUM(pd.totalAttempts) AS totalChords, " +
            "SUM(pd.correctCount) AS correctChords, " +
            "SUM(pd.incorrectCount) AS incorrectChords " +
            "FROM practice_details pd " +
            "JOIN practice_sessions ps ON ps.id = pd.sessionId " +
            "WHERE ps.songId = :songId")
    LiveData<SongStats> getSongStats(int songId);

    // ✅ Acorde más acertado / fallado (por cantidad)
    @Query("SELECT pd.chordId " +
            "FROM practice_details pd " +
            "JOIN practice_sessions ps ON ps.id = pd.sessionId " +
            "WHERE ps.songId = :songId " +
            "GROUP BY pd.chordId " +
            "ORDER BY SUM(pd.correctCount) DESC " +
            "LIMIT 1")
    LiveData<Integer> getMostCorrectChord(int songId);

    @Query("SELECT pd.chordId " +
            "FROM practice_details pd " +
            "JOIN practice_sessions ps ON ps.id = pd.sessionId " +
            "WHERE ps.songId = :songId " +
            "GROUP BY pd.chordId " +
            "ORDER BY SUM(pd.incorrectCount) DESC " +
            "LIMIT 1")
    LiveData<Integer> getMostFailedChord(int songId);

    // 📊 Clases auxiliares
    class ChordPercentage {
        public int chordId;
        public float percentage;
    }

    class ChordAccuracy {
        public int chordId;
        public float percentage;
    }

    class SongStats {
        public int totalChords;
        public int correctChords;
        public int incorrectChords;
    }
}
