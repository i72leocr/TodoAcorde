package com.tuguitar.todoacorde;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface ProgressionSessionDao {

    @Insert
    long insertProgressionSession(ProgressionSession progressionSession);

    @Update
    int updateProgressionSession(ProgressionSession progressionSession);

    @Query("SELECT * FROM progression_sessions WHERE id = :sessionId")
    ProgressionSession getProgressionSessionById(int sessionId);

    @Query("SELECT * FROM progression_sessions WHERE progression_id = :progressionId ORDER BY totalScore DESC LIMIT 1")
    ProgressionSession getSessionByProgressionId(int progressionId);

    @Query("DELETE FROM progression_sessions WHERE id = :sessionId")
    int deleteSessionById(int sessionId);

    @Query("SELECT * FROM progression_sessions WHERE progression_id = :progressionId ORDER BY totalScore DESC LIMIT :limit")
    List<ProgressionSession> getTopSessionsByProgressionId(int progressionId, int limit);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertOrUpdateSession(ProgressionSession progressionSession);
}
