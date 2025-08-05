package com.tuguitar.todoacorde;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "SongUserSpeed",
        foreignKeys = {
                @ForeignKey(
                        entity = Song.class,
                        parentColumns = "id",
                        childColumns = "songId",
                        onDelete = ForeignKey.CASCADE
                ),
                @ForeignKey(
                        entity = User.class,
                        parentColumns = "id",
                        childColumns = "userId",
                        onDelete = ForeignKey.CASCADE
                )
        },
        indices = {
                @Index(value = {"songId"}), // Índice para optimizar consultas por songId
                @Index(value = {"userId"}), // Índice para optimizar consultas por userId
                @Index(value = {"songId", "userId"}, unique = true) // Garantiza que no haya duplicados para una combinación de usuario y canción
        }
)
public class SongUserSpeed {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public int songId; // ID de la canción, clave foránea
    public int userId; // ID del usuario, clave foránea

    public boolean isUnlocked0_5x; // Velocidad 0.5x desbloqueada
    public boolean isUnlocked0_75x; // Velocidad 0.75x desbloqueada
    public boolean isUnlocked1x; // Velocidad 1x desbloqueada

    // Constructor
    public SongUserSpeed(int songId, int userId, boolean isUnlocked0_5x, boolean isUnlocked0_75x, boolean isUnlocked1x) {
        this.songId = songId;
        this.userId = userId;
        this.isUnlocked0_5x = isUnlocked0_5x;
        this.isUnlocked0_75x = isUnlocked0_75x;
        this.isUnlocked1x = isUnlocked1x;
    }

    public float getMaxUnlockedSpeed() {
        if (isUnlocked1x) return 1.0f;
        if (isUnlocked0_75x) return 0.75f;
        return 0.5f;
    }
}
