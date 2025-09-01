package com.tuguitar.todoacorde.scales.data.entity;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "Tonality",
        indices = @Index(value = {"name"}, unique = true)
)
public class TonalityEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;

    public String name; // "C", "C#", "D", ... según notación de notas

    @Override
    public String toString() {
        return name;
    }
}
