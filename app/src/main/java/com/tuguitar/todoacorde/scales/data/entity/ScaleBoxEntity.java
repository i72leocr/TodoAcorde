package com.tuguitar.todoacorde.scales.data.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "ScaleBox",
        foreignKeys = @ForeignKey(
                entity = ScaleEntity.class,
                parentColumns = "id",
                childColumns = "scaleId",
                onDelete = ForeignKey.CASCADE
        ),
        indices = {
                @Index(value = {"scaleId", "boxOrder"}, unique = true),
                @Index(value = {"scaleId"})
        }
)
public class ScaleBoxEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;

    public long scaleId;
    public int boxOrder; // 1..N (orden de las cajas)
}
