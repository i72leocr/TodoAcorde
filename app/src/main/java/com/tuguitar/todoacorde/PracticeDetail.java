package com.tuguitar.todoacorde;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;

@Entity(
        tableName = "practice_details",
        primaryKeys = {"sessionId", "chordId"}, // ⬅️ Clave primaria compuesta
        foreignKeys = {
                @ForeignKey(entity = PracticeSession.class,
                        parentColumns = "id",
                        childColumns = "sessionId",
                        onDelete = ForeignKey.CASCADE),
                @ForeignKey(entity = Chord.class,
                        parentColumns = "id",
                        childColumns = "chordId",
                        onDelete = ForeignKey.CASCADE)
        },
        indices = {
                @Index(value = "sessionId"),
                @Index(value = "chordId") // Índices para mejorar consultas
        }
)
public class PracticeDetail {

    @ColumnInfo(name = "sessionId")
    public int sessionId; // Relación con PracticeSession

    @ColumnInfo(name = "chordId")
    public int chordId; // Relación con Chord

    @ColumnInfo(name = "totalAttempts")
    public int totalAttempts;

    @ColumnInfo(name = "correctCount")
    public int correctCount;

    @ColumnInfo(name = "incorrectCount")
    public int incorrectCount;

    public PracticeDetail(int sessionId, int chordId, int totalAttempts, int correctCount, int incorrectCount) {
        this.sessionId = sessionId;
        this.chordId = chordId;
        this.totalAttempts = totalAttempts;
        this.correctCount = correctCount;
        this.incorrectCount = incorrectCount;
    }
}
