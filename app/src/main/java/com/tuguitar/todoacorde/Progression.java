package com.tuguitar.todoacorde;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;
import androidx.room.Index;

@Entity(
        tableName = "progressions",
        foreignKeys = {
                @ForeignKey(
                        entity = ChordType.class,
                        parentColumns = "id",
                        childColumns = "type_filter",
                        onDelete = ForeignKey.NO_ACTION
                ),
                @ForeignKey(
                        entity = Difficulty.class,
                        parentColumns = "id",
                        childColumns = "difficulty_filter",
                        onDelete = ForeignKey.NO_ACTION
                )
        },
        indices = {@Index("type_filter"), @Index("difficulty_filter")}
)
public class Progression {
    @PrimaryKey(autoGenerate = true)
    public int id;

    @ColumnInfo(name = "name")
    @NonNull
    public String name; // Nombre de la progresión

    @ColumnInfo(name = "description")
    public String description; // Descripción opcional

    @ColumnInfo(name = "is_dynamic")
    public boolean isDynamic; // Indica si es una progresión dinámica

    @ColumnInfo(name = "type_filter")
    public Integer typeFilter; // Filtro opcional por tipo de acorde (referencia a ChordType)

    @ColumnInfo(name = "difficulty_filter")
    public Integer difficultyFilter; // Filtro opcional por dificultad (referencia a Difficulty)

    // Getters
    public int getId() {
        return id;
    }

    @NonNull
    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public boolean isDynamic() {
        return isDynamic;
    }

    public int getTypeFilter() {
        return typeFilter;
    }

    public int getDifficultyFilter() {
        return difficultyFilter;
    }
}


