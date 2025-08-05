package com.tuguitar.todoacorde;

import static androidx.room.ForeignKey.CASCADE;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "UserProgression",
        foreignKeys = {
                @ForeignKey(entity = User.class, parentColumns = "id", childColumns = "user_id", onDelete = CASCADE),
                @ForeignKey(entity = Progression.class, parentColumns = "id", childColumns = "progression_id", onDelete = CASCADE)
        },
        indices = {@Index("user_id"), @Index("progression_id")}
)
public class UserProgression {
    @PrimaryKey(autoGenerate = true)
    public int id;
    @ColumnInfo(name = "user_id")
    public int userId; // Usuario relacionado
    @ColumnInfo(name = "progression_id")
    public int progressionId; // Progresión desbloqueada
    @ColumnInfo(name = "is_unlocked")
    public boolean isUnlocked; // Estado de desbloqueo
}
