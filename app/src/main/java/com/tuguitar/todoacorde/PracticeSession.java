package com.tuguitar.todoacorde;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.Ignore;

/**
 * Entidad que representa una sesión de práctica.
 */
@Entity(tableName = "practice_sessions")
public class PracticeSession {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public int userId;
    public int songId;
    public boolean isCompleted;
    // Marcas de tiempo de inicio y fin
    public long startTime;
    public long endTime;

    public long duration;

    public int totalScore;
    public float speed;

    public boolean isLastSession;
    public boolean isBestSession;

    /**
     * Constructor vacío requerido por Room.
     */
    public PracticeSession() { }

    /**
     * Constructor para uso en app (no usado por Room).
     */
    @Ignore
    public PracticeSession(int songId,
                           int userId,
                           boolean isCompleted,
                           long startTime,
                           long endTime,
                           long duration,
                           int totalScore,
                           float speed,
                           boolean isLastSession,
                           boolean isBestSession) {
        this.songId = songId;
        this.userId = userId;
        this.isCompleted = isCompleted;
        this.startTime = startTime;
        this.endTime = endTime;
        this.duration = duration;
        this.totalScore = totalScore;
        this.speed = speed;
        this.isLastSession = isLastSession;
        this.isBestSession = isBestSession;
    }
}
