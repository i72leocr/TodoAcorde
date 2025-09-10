package com.todoacorde.todoacorde.practice.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

/**
 * DAO Room para acceder y consultar detalles de práctica por acorde.
 *
 * Proporciona operaciones de inserción/actualización y consultas agregadas
 * con combinaciones a {@code practice_sessions} para derivar métricas por canción
 * y periodo temporal.
 */
@Dao
public interface PracticeDetailDao {

    /**
     * Inserta una lista de detalles de práctica reemplazando conflictos.
     *
     * @param details lista de {@link PracticeDetail} a persistir.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertDetails(List<PracticeDetail> details);

    /**
     * Devuelve todos los detalles en forma sincrónica (uso interno/worker).
     *
     * @return lista de {@link PracticeDetail}.
     */
    @Query("SELECT * FROM practice_details")
    List<PracticeDetail> getAllRaw();

    /**
     * Actualiza un detalle existente.
     *
     * @param detail entidad a actualizar.
     */
    @Update
    void updatePracticeDetail(PracticeDetail detail);

    /**
     * Obtiene un detalle por clave compuesta (sesión + acorde).
     *
     * @param sessionId id de la sesión.
     * @param chordId   id del acorde.
     * @return {@link PracticeDetail} o null si no existe.
     */
    @Query("SELECT * FROM practice_details WHERE sessionId = :sessionId AND chordId = :chordId LIMIT 1")
    PracticeDetail getDetailBySessionAndChord(int sessionId, int chordId);

    /**
     * Observa todos los detalles como {@link LiveData}.
     *
     * @return live data con la lista completa.
     */
    @Query("SELECT * FROM practice_details")
    LiveData<List<PracticeDetail>> getAll();

    /**
     * Calcula el porcentaje de acierto por acorde dentro de una sesión.
     *
     * Fórmula: {@code correctCount * 100.0 / totalAttempts}.
     *
     * @param sessionId id de la sesión.
     * @return live data con pares acorde-porcentaje.
     */
    @Query("SELECT chordId, " +
            "correctCount * 100.0 / totalAttempts AS percentage " +
            "FROM practice_details " +
            "WHERE sessionId = :sessionId")
    LiveData<List<ChordAccuracy>> getChordAccuraciesForSession(int sessionId);

    /**
     * Observa el detalle para una combinación sesión+acorde.
     *
     * @param sessionId id de la sesión.
     * @param chordId   id del acorde.
     * @return live data con el detalle.
     */
    @Query("SELECT * FROM practice_details " +
            "WHERE sessionId = :sessionId AND chordId = :chordId")
    LiveData<PracticeDetail> getDetailsForSessionChord(int sessionId, int chordId);

    /**
     * Observa los detalles de práctica de una canción desde un instante dado.
     * Si {@code since} es 0, no se filtra por fecha.
     *
     * @param songId id de la canción.
     * @param since  timestamp (ms) inicio del intervalo o 0 para ignorar filtro.
     * @return live data con los detalles.
     */
    @Query("SELECT pd.* " +
            "FROM practice_details pd " +
            "JOIN practice_sessions ps ON ps.id = pd.sessionId " +
            "WHERE ps.songId = :songId " +
            "  AND (:since = 0 OR ps.startTime >= :since)")
    LiveData<List<PracticeDetail>> getDetailsForSongSince(int songId, long since);

    /**
     * Cuenta el total de aciertos acumulados para una canción desde un instante dado.
     *
     * @param songId id de la canción.
     * @param since  timestamp (ms) inicio del intervalo.
     * @return live data con la suma de aciertos.
     */
    @Query("SELECT SUM(pd.correctCount) " +
            "FROM practice_details pd " +
            "JOIN practice_sessions ps ON ps.id = pd.sessionId " +
            "WHERE ps.songId = :songId AND ps.startTime >= :since")
    LiveData<Integer> countCorrectForSongSince(int songId, long since);

    /**
     * Cuenta el total de fallos acumulados para una canción desde un instante dado.
     *
     * @param songId id de la canción.
     * @param since  timestamp (ms) inicio del intervalo.
     * @return live data con la suma de fallos.
     */
    @Query("SELECT SUM(pd.incorrectCount) " +
            "FROM practice_details pd " +
            "JOIN practice_sessions ps ON ps.id = pd.sessionId " +
            "WHERE ps.songId = :songId AND ps.startTime >= :since")
    LiveData<Integer> countIncorrectForSongSince(int songId, long since);

    /**
     * Top 3 de acordes con mayor porcentaje de error para una canción en un intervalo.
     *
     * Fórmula: {@code incorrectCount * 100.0 / totalAttempts} por acorde.
     *
     * @param songId id de la canción.
     * @param since  timestamp (ms) inicio del intervalo.
     * @return live data con lista ordenada descendentemente por porcentaje de error.
     */
    @Query("SELECT pd.chordId AS chordId, " +
            "pd.incorrectCount * 100.0 / pd.totalAttempts AS percentage " +
            "FROM practice_details pd " +
            "JOIN practice_sessions ps ON ps.id = pd.sessionId " +
            "WHERE ps.songId = :songId AND ps.startTime >= :since " +
            "GROUP BY pd.chordId " +
            "ORDER BY percentage DESC " +
            "LIMIT 3")
    LiveData<List<ChordPercentage>> getTopErroredChordsByPercentage(int songId, long since);

    /**
     * Top 3 de acordes con mayor porcentaje de acierto para una canción en un intervalo.
     *
     * Fórmula: {@code correctCount * 100.0 / totalAttempts} por acorde.
     *
     * @param songId id de la canción.
     * @param since  timestamp (ms) inicio del intervalo.
     * @return live data con lista ordenada descendentemente por porcentaje de acierto.
     */
    @Query("SELECT pd.chordId AS chordId, " +
            "pd.correctCount * 100.0 / pd.totalAttempts AS percentage " +
            "FROM practice_details pd " +
            "JOIN practice_sessions ps ON ps.id = pd.sessionId " +
            "WHERE ps.songId = :songId AND ps.startTime >= :since " +
            "GROUP BY pd.chordId " +
            "ORDER BY percentage DESC " +
            "LIMIT 3")
    LiveData<List<ChordPercentage>> getTopSuccessfulChordsByPercentage(int songId, long since);

    /**
     * Obtiene los nombres de los 3 acordes con más aciertos acumulados para una canción.
     *
     * @param songId id de la canción.
     * @return live data con nombres de acordes.
     */
    @Query("SELECT c.name " +
            "FROM practice_details pd " +
            "JOIN practice_sessions ps ON ps.id = pd.sessionId " +
            "JOIN chords c ON pd.chordId = c.id " +
            "WHERE ps.songId = :songId " +
            "GROUP BY pd.chordId " +
            "ORDER BY SUM(pd.correctCount) DESC " +
            "LIMIT 3")
    LiveData<List<String>> findTop3CorrectChords(int songId);

    /**
     * Obtiene los nombres de los 3 acordes con más fallos acumulados para una canción.
     *
     * @param songId id de la canción.
     * @return live data con nombres de acordes.
     */
    @Query("SELECT c.name " +
            "FROM practice_details pd " +
            "JOIN practice_sessions ps ON ps.id = pd.sessionId " +
            "JOIN chords c ON pd.chordId = c.id " +
            "WHERE ps.songId = :songId " +
            "GROUP BY pd.chordId " +
            "ORDER BY SUM(pd.incorrectCount) DESC " +
            "LIMIT 3")
    LiveData<List<String>> findTop3FailedChords(int songId);

    /**
     * Obtiene estadísticas agregadas (totales, aciertos, fallos) para una canción.
     *
     * @param songId id de la canción.
     * @return live data con {@link SongStats}.
     */
    @Query("SELECT SUM(pd.totalAttempts) AS totalChords, " +
            "SUM(pd.correctCount) AS correctChords, " +
            "SUM(pd.incorrectCount) AS incorrectChords " +
            "FROM practice_details pd " +
            "JOIN practice_sessions ps ON ps.id = pd.sessionId " +
            "WHERE ps.songId = :songId")
    LiveData<SongStats> getSongStats(int songId);

    /**
     * Devuelve el id del acorde con mayor cantidad de aciertos acumulados para una canción.
     *
     * @param songId id de la canción.
     * @return live data con el id del acorde más acertado.
     */
    @Query("SELECT pd.chordId " +
            "FROM practice_details pd " +
            "JOIN practice_sessions ps ON ps.id = pd.sessionId " +
            "WHERE ps.songId = :songId " +
            "GROUP BY pd.chordId " +
            "ORDER BY SUM(pd.correctCount) DESC " +
            "LIMIT 1")
    LiveData<Integer> getMostCorrectChord(int songId);

    /**
     * Devuelve el id del acorde con mayor cantidad de fallos acumulados para una canción.
     *
     * @param songId id de la canción.
     * @return live data con el id del acorde con más fallos.
     */
    @Query("SELECT pd.chordId " +
            "FROM practice_details pd " +
            "JOIN practice_sessions ps ON ps.id = pd.sessionId " +
            "WHERE ps.songId = :songId " +
            "GROUP BY pd.chordId " +
            "ORDER BY SUM(pd.incorrectCount) DESC " +
            "LIMIT 1")
    LiveData<Integer> getMostFailedChord(int songId);

    /**
     * Proyección de porcentaje por acorde (para tops de éxito/error).
     */
    class ChordPercentage {
        /** id del acorde. */
        public int chordId;
        /** porcentaje asociado al acorde. */
        public float percentage;
    }

    /**
     * Proyección de precisión por acorde para una sesión concreta.
     */
    class ChordAccuracy {
        /** id del acorde. */
        public int chordId;
        /** porcentaje de acierto. */
        public float percentage;
    }

    /**
     * Estadísticas agregadas por canción.
     */
    class SongStats {
        /** Total de intentos (suma de {@code totalAttempts}). */
        public int totalChords;
        /** Total de aciertos. */
        public int correctChords;
        /** Total de fallos. */
        public int incorrectChords;
    }
}
