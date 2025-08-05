package com.tuguitar.todoacorde;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "chord_types")
public class ChordType {
    @PrimaryKey(autoGenerate = true)
    public int id;

    @NonNull
    @ColumnInfo(name = "type_name")
    public String typeName; // Nombre del tipo de acorde (e.g., Mayor, Menor, Dominante)

    @ColumnInfo(name = "description")
    public String description; // Descripción opcional del tipo

    // Constructor
    public ChordType(@NonNull String typeName, String description) {
        this.typeName = typeName;
        this.description = description;
    }

    // Métodos getter y setter
    @NonNull
    public String getTypeName() {
        return typeName;
    }

    public void setTypeName(@NonNull String typeName) {
        this.typeName = typeName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
