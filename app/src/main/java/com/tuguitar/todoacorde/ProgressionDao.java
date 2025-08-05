package com.tuguitar.todoacorde;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Transaction;

import java.util.List;

@Dao
public interface ProgressionDao {
    @Insert
    void insertProgression(Progression progression);

    @Query("SELECT * FROM progressions")
    List<Progression> getAllProgressions();

    @Query("SELECT * FROM progressions WHERE name = :name LIMIT 1")
    Progression getProgressionByName(String name);

    @Query("SELECT * FROM progressions WHERE id = :progressionId")
    Progression getProgressionById(int progressionId);

    @Query("SELECT * FROM Progressions WHERE is_dynamic = 0")
    List<Progression> getStaticProgressions();
}
