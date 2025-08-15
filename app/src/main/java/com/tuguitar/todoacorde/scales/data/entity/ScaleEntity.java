package com.tuguitar.todoacorde.scales.data.entity;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "Scale",
        indices = {
                @Index(value = {"name"}, unique = true),
                @Index(value = {"tier"})
        }
)
public class ScaleEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;

    public String name;   // p.ej. "Mayor", "Frigia", etc.
    public int tier;      // 0 = fácil, 1 = intermedio, 2 = difícil...
}
