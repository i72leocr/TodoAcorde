package com.todoacorde.todoacorde;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Entidad Room que representa un nivel de dificultad.
 *
 * Se almacena en la tabla {@code difficulties}. Cada fila define un nivel
 * con su descripción opcional. La clave primaria es autogenerada.
 */
@Entity(tableName = "difficulties")
public class Difficulty {
    /**
     * Identificador interno autogenerado.
     */
    @PrimaryKey(autoGenerate = true)
    public int id;

    /**
     * Nombre del nivel de dificultad (por ejemplo, "Fácil", "Media", "Difícil").
     * No puede ser nulo.
     */
    @NonNull
    @ColumnInfo(name = "difficulty_level")
    public String difficultyLevel;

    /**
     * Descripción opcional del nivel de dificultad.
     */
    @ColumnInfo(name = "description")
    public String description;

    /**
     * Crea una nueva entrada de dificultad.
     *
     * @param difficultyLevel nombre del nivel (no nulo).
     * @param description     descripción opcional.
     */
    public Difficulty(@NonNull String difficultyLevel, String description) {
        this.difficultyLevel = difficultyLevel;
        this.description = description;
    }

    /**
     * Obtiene el nombre del nivel de dificultad.
     */
    @NonNull
    public String getDifficultyLevel() {
        return difficultyLevel;
    }

    /**
     * Obtiene el identificador autogenerado.
     */
    public int getId() {
        return id;
    }

    /**
     * Establece el nombre del nivel de dificultad.
     *
     * @param difficultyLevel nombre del nivel (no nulo).
     */
    public void setDifficultyLevel(@NonNull String difficultyLevel) {
        this.difficultyLevel = difficultyLevel;
    }

    /**
     * Obtiene la descripción del nivel.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Establece la descripción del nivel.
     *
     * @param description texto descriptivo opcional.
     */
    public void setDescription(String description) {
        this.description = description;
    }
}
