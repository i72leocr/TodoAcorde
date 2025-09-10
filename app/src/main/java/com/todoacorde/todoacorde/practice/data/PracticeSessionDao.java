package com.todoacorde.todoacorde.practice.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

import java.util.List;

/**
 * DAO de sesiones de práctica.
 *
 * Provee operaciones de inserción/actualización y consultas agregadas y por clave,
 * incluyendo utilidades transaccionales para mantener los flags de última/mejor sesión.
 */
@Dao
public interface PracticeSessionDao {

    /**
     * Suma total de aciertos para una canción en todas sus sesiones.
     */
    @Query("SELECT SUM(pd.correctCount) " +
            "FROM practice_details pd " +
            "JOIN practice_sessions ps ON ps.id = pd.sessionId " +
            "WHERE ps.songId = :songId")
    LiveData<Integer> getTotalCorrectChords(int songId);

    /**
     * Suma total de fallos para una canción en todas sus sesiones.
     */
    @Query("SELECT SUM(pd.incorrectCount) " +
            "FROM practice_details pd " +
            "JOIN practice_sessions ps ON ps.id = pd.sessionId " +
            "WHERE ps.songId = :songId")
    LiveData<Integer> getTotalIncorrectChords(int songId);

    /**
     * IDs de canciones con puntuación perfecta (100) a velocidad 1.0 para un usuario.
     */
    @Query("SELECT DISTINCT songId FROM practice_sessions WHERE userId = :userId AND speed = 1.0 AND totalScore = 100")
    List<Integer> getPerfectScoreSongIds(long userId);

    /**
     * Número total de sesiones registradas para una canción.
     */
    @Query("SELECT COUNT(*) FROM practice_sessions WHERE songId = :songId")
    LiveData<Integer> getTotalSessionsForSong(int songId);

    /**
     * Limpia el flag de última sesión para todas las sesiones previas de una canción/usuario/velocidad.
     */
    @Query("UPDATE practice_sessions SET isLastSession = 0 WHERE songId = :songId AND userId = :userId AND speed = :speed")
    void clearLastSessionFlag(int songId, int userId, float speed);

    /**
     * Limpia el flag de mejor sesión para todas las sesiones previas de una canción/usuario/velocidad.
     */
    @Query("UPDATE practice_sessions SET isBestSession = 0 WHERE songId = :songId AND userId = :userId AND speed = :speed")
    void clearBestSessionFlag(int songId, int userId, float speed);

    /**
     * Devuelve la mejor sesión (marcada con flag) para una clave canción/usuario/velocidad.
     */
    @Query("SELECT * FROM practice_sessions " +
            "WHERE songId = :songId AND userId = :userId " +
            "AND speed = :speed AND isBestSession = 1 " +
            "LIMIT 1")
    LiveData<PracticeSession> getBestSession(int songId, int userId, float speed);

    /**
     * Inserta una sesión. En conflicto, reemplaza el registro.
     *
     * @return id autogenerado.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertSession(PracticeSession session);

    /**
     * Actualiza una sesión existente.
     *
     * @return número de filas afectadas.
     */
    @Update
    int updateSession(PracticeSession session);

    /**
     * Inserta o actualiza según el id de sesión.
     *
     * @return id final de la sesión.
     */
    @Transaction
    default long insertOrUpdateSession(PracticeSession session) {
        if (session.id == 0) {
            long newId = insertSession(session);
            session.id = (int) newId;
            return newId;
        } else {
            updateSession(session);
            return session.id;
        }
    }

    /**
     * Inserta una sesión y devuelve el id generado (sin estrategia de conflicto explícita).
     */
    @Insert
    long insertReturningId(PracticeSession session);

    /**
     * Sesiones de una canción desde una marca de tiempo (inclusive).
     */
    @Query("SELECT * FROM practice_sessions WHERE songId = :songId AND startTime >= :since")
    LiveData<List<PracticeSession>> getSessionsForSongSince(int songId, long since);

    /**
     * Sesiones de una canción (todas).
     */
    @Query("SELECT * FROM practice_sessions WHERE songId = :songId")
    LiveData<List<PracticeSession>> getSessionsForSong(int songId);

    /**
     * Mejor puntuación para canción/usuario/velocidad.
     */
    @Query("SELECT MAX(totalScore) FROM practice_sessions " +
            "WHERE songId = :songId AND userId = :userId AND speed = :speed")
    LiveData<Integer> getBestScore(int songId, int userId, float speed);

    /**
     * Todas las sesiones de práctica (útil para diagnósticos).
     */
    @Query("SELECT * FROM practice_sessions")
    LiveData<List<PracticeSession>> getAllSessions();

    /**
     * Número de canciones con puntuación perfecta (100) en cualquier sesión.
     */
    @Query("SELECT COUNT(DISTINCT songId) FROM practice_sessions WHERE totalScore = 100")
    LiveData<Integer> getSongCountWithFullScore();

    /**
     * Mejor sesión de una canción desde una marca de tiempo, ordenada por puntuación.
     */
    @Query("SELECT * FROM practice_sessions " +
            "WHERE songId = :songId AND startTime >= :since " +
            "ORDER BY totalScore DESC LIMIT 1")
    LiveData<PracticeSession> getBestSessionForSongSince(int songId, long since);

    /**
     * Marca una sesión como la última sesión para su clave.
     */
    @Query("UPDATE practice_sessions SET isLastSession = 1 WHERE id = :sessionId")
    void markLastSession(int sessionId);

    /**
     * Marca una sesión como la mejor sesión para su clave.
     */
    @Query("UPDATE practice_sessions SET isBestSession = 1 WHERE id = :sessionId")
    void markBestSession(int sessionId);

    /**
     * Inserta una sesión estableciendo coherentemente los flags de última y mejor sesión.
     *
     * Reglas:
     * 1) Borra el flag de última sesión previo para la clave (songId, userId, speed).
     * 2) Calcula si la sesión es mejor que la mejor puntuación actual; si lo es,
     *    borra el flag de mejor sesión previo y marca esta como mejor.
     * 3) Marca siempre la sesión como última.
     *
     * @return id autogenerado por la inserción.
     */
    @Transaction
    default long insertSessionWithFlags(PracticeSession session) {
        clearLastSessionFlag(session.songId, session.userId, session.speed);

        Integer currentBest = getBestScoreSync(session.songId, session.userId, session.speed);
        boolean isBest = currentBest == null || session.totalScore > currentBest;
        if (isBest) {
            clearBestSessionFlag(session.songId, session.userId, session.speed);
            session.isBestSession = true;
        } else {
            session.isBestSession = false;
        }

        session.isLastSession = true;

        return insertSession(session);
    }

    /**
     * Mejor puntuación sync para clave canción/usuario/velocidad.
     */
    @Query("SELECT MAX(totalScore) FROM practice_sessions " +
            "WHERE songId = :songId AND userId = :userId AND speed = :speed")
    Integer getBestScoreSync(int songId, int userId, float speed);

    /**
     * Última sesión (por fecha de inicio descendente) para una clave canción/usuario/velocidad.
     */
    @Query("SELECT * FROM practice_sessions " +
            "WHERE songId = :songId AND userId = :userId AND speed = :speed " +
            "ORDER BY startTime DESC LIMIT 1")
    LiveData<PracticeSession> getLastSession(int songId, int userId, float speed);
}
