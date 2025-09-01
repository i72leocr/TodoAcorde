package com.tuguitar.todoacorde;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface DifficultyDao {
    @Insert
    void insert(Difficulty difficulty);
    @Query("SELECT * FROM difficulties")
    List<Difficulty> getAllDifficulties();
    @Query("SELECT * FROM difficulties WHERE id = :difficultyId")
    Difficulty getDifficultyById(int difficultyId);
    @Query("SELECT * FROM difficulties WHERE difficulty_level = :difficultyName")
    Difficulty getDifficultyByName(String difficultyName);
    @Query("DELETE FROM difficulties WHERE id = :difficultyId")
    void deleteDifficultyById(int difficultyId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<Difficulty> difficulties);
}

