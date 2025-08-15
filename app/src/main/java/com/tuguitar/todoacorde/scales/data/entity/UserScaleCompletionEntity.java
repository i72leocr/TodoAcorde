package com.tuguitar.todoacorde.scales.data.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "UserScaleCompletion",
        foreignKeys = {
                @ForeignKey(entity = ScaleEntity.class, parentColumns = "id", childColumns = "scaleId", onDelete = ForeignKey.CASCADE),
                @ForeignKey(entity = TonalityEntity.class, parentColumns = "id", childColumns = "tonalityId", onDelete = ForeignKey.CASCADE)
                // Nota: userId referencia a tu tabla de usuarios si la tienes en Room. Si no, mantenlo como long simple.
        },
        indices = {
                @Index(value = {"userId", "scaleId", "tonalityId", "boxOrder"}, unique = true),
                @Index(value = {"scaleId"}),
                @Index(value = {"tonalityId"}),
                @Index(value = {"userId"})
        }
)
public class UserScaleCompletionEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;

    public long userId;
    public long scaleId;
    public long tonalityId;
    public int boxOrder;         // caja completada
    public long completedAtUtc;  // epoch millis
}
