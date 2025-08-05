package com.tuguitar.todoacorde;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "progression_sessions",
        foreignKeys = {
                @ForeignKey(entity = Progression.class, parentColumns = "id", childColumns = "progression_id", onDelete = ForeignKey.CASCADE)
        }
)
public class ProgressionSession {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public int progression_id; // Referencia a la progresión
    public long startTime; // Hora de inicio de la sesión
    public long endTime; // Hora de fin de la sesión
    public boolean isCompleted; // Indica si se completó
    public int totalScore; // Puntuación total de la sesión

    // Constructor
    public ProgressionSession(int progression_id, long startTime, long endTime, boolean isCompleted, int totalScore) {
        this.progression_id = progression_id;
        this.startTime = startTime;
        this.endTime = endTime;
        this.isCompleted = isCompleted;
        this.totalScore = totalScore;
    }
}
