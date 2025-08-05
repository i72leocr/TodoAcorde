package com.tuguitar.todoacorde;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import androidx.room.Transaction;

import java.util.List;
@Dao
public interface PracticeSessionDao {

    @Query("SELECT SUM(pd.correctCount) " +
            "FROM practice_details pd " +
            "JOIN practice_sessions ps ON ps.id = pd.sessionId " +
            "WHERE ps.songId = :songId")
    LiveData<Integer> getTotalCorrectChords(int songId);

    @Query("SELECT SUM(pd.incorrectCount) " +
            "FROM practice_details pd " +
            "JOIN practice_sessions ps ON ps.id = pd.sessionId " +
            "WHERE ps.songId = :songId")
    LiveData<Integer> getTotalIncorrectChords(int songId);

    @Query("SELECT DISTINCT songId FROM practice_sessions WHERE userId = :userId AND speed = 1.0 AND totalScore = 100")
    List<Integer> getPerfectScoreSongIds(long userId);


    @Query("SELECT COUNT(*) FROM practice_sessions WHERE songId = :songId")
    LiveData<Integer> getTotalSessionsForSong(int songId);

    @Query("UPDATE practice_sessions SET isLastSession = 0 WHERE songId = :songId AND userId = :userId AND speed = :speed")
    void clearLastSessionFlag(int songId, int userId, float speed);

    @Query("UPDATE practice_sessions SET isBestSession = 0 WHERE songId = :songId AND userId = :userId AND speed = :speed")
    void clearBestSessionFlag(int songId, int userId, float speed);

    @Query("SELECT * FROM practice_sessions " +
            "WHERE songId = :songId AND userId = :userId " +
            "AND speed = :speed AND isBestSession = 1 " +
            "LIMIT 1")
    LiveData<PracticeSession> getBestSession(int songId, int userId, float speed);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertSession(PracticeSession session);

    @Update
    int updateSession(PracticeSession session);

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

    @Insert
    long insertReturningId(PracticeSession session);

    @Query("SELECT * FROM practice_sessions WHERE songId = :songId AND startTime >= :since")
    LiveData<List<PracticeSession>> getSessionsForSongSince(int songId, long since);

    @Query("SELECT * FROM practice_sessions WHERE songId = :songId")
    LiveData<List<PracticeSession>> getSessionsForSong(int songId);

    @Query("SELECT MAX(totalScore) FROM practice_sessions " +
            "WHERE songId = :songId AND userId = :userId AND speed = :speed")
    LiveData<Integer> getBestScore(int songId, int userId, float speed);

    @Query("SELECT * FROM practice_sessions")
    LiveData<List<PracticeSession>> getAllSessions();

    @Query("SELECT COUNT(DISTINCT songId) FROM practice_sessions WHERE totalScore = 100")
    LiveData<Integer> getSongCountWithFullScore();

    @Query("SELECT * FROM practice_sessions " +
            "WHERE songId = :songId AND startTime >= :since " +
            "ORDER BY totalScore DESC LIMIT 1")
    LiveData<PracticeSession> getBestSessionForSongSince(int songId, long since);

    @Query("UPDATE practice_sessions SET isLastSession = 1 WHERE id = :sessionId")
    void markLastSession(int sessionId);

    @Query("UPDATE practice_sessions SET isBestSession = 1 WHERE id = :sessionId")
    void markBestSession(int sessionId);

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

    @Query("SELECT MAX(totalScore) FROM practice_sessions " +
            "WHERE songId = :songId AND userId = :userId AND speed = :speed")
    Integer getBestScoreSync(int songId, int userId, float speed);

    @Query("SELECT * FROM practice_sessions " +
            "WHERE songId = :songId AND userId = :userId AND speed = :speed " +
            "ORDER BY startTime DESC LIMIT 1")
    LiveData<PracticeSession> getLastSession(int songId, int userId, float speed);
}

