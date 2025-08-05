package com.tuguitar.todoacorde;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface ProgressionDetailDao {

    @Insert
    void insertProgressionDetail(ProgressionDetail progressionDetail);


    @Insert
    void insertDetails(List<ProgressionDetail> progressionDetails);

    @Query("SELECT * FROM progression_details WHERE session_id = :sessionId")
    List<ProgressionDetail> getDetailsBySessionId(int sessionId);

    @Query("DELETE FROM progression_details WHERE session_id = :sessionId")
    int deleteDetailsBySessionId(int sessionId);
}
