package com.tuguitar.todoacorde;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface DifficultyDao {

    // Insertar una nueva dificultad
    @Insert
    void insert(Difficulty difficulty);


    // Obtener todas las dificultades
    @Query("SELECT * FROM difficulties")
    List<Difficulty> getAllDifficulties();

    // Obtener una dificultad por su ID
    @Query("SELECT * FROM difficulties WHERE id = :difficultyId")
    Difficulty getDifficultyById(int difficultyId);

    // Obtener una dificultad por su nombre
    @Query("SELECT * FROM difficulties WHERE difficulty_level = :difficultyName")
    Difficulty getDifficultyByName(String difficultyName);

    // Eliminar una dificultad por su ID
    @Query("DELETE FROM difficulties WHERE id = :difficultyId")
    void deleteDifficultyById(int difficultyId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<Difficulty> difficulties);
}

