package com.tuguitar.todoacorde;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "difficulties")
public class Difficulty {
    @PrimaryKey(autoGenerate = true)
    public int id;

    @NonNull
    @ColumnInfo(name = "difficulty_level")
    public String difficultyLevel; // Nivel de dificultad (e.g., Baja, Media, Alta)

    @ColumnInfo(name = "description")
    public String description; // Descripción opcional de la dificultad

    // Constructor
    public Difficulty(@NonNull String difficultyLevel, String description) {
        this.difficultyLevel = difficultyLevel;
        this.description = description;
    }

    // Métodos getter y setter
    @NonNull
    public String getDifficultyLevel() {
        return difficultyLevel;
    }

    public int getId() {
        return id;
    }

    public void setDifficultyLevel(@NonNull String difficultyLevel) {
        this.difficultyLevel = difficultyLevel;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
