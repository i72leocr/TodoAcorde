package com.tuguitar.todoacorde;

import static androidx.room.ForeignKey.CASCADE;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "progression_chords",
        foreignKeys = {
                @ForeignKey(entity = Progression.class, parentColumns = "id", childColumns = "progression_id", onDelete = CASCADE),
                @ForeignKey(entity = Chord.class, parentColumns = "id", childColumns = "chord_id", onDelete = CASCADE)
        },
        indices = {@Index("progression_id"), @Index("chord_id")}
)
public class ProgressionChord {
    @PrimaryKey(autoGenerate = true)
    public int id;

    @ColumnInfo(name = "progression_id")
    public int progressionId; // ID de la progresión estática

    @ColumnInfo(name = "chord_id")
    public int chordId; // ID del acorde

    @ColumnInfo(name = "order_in_progression")
    public int orderInProgression; // Orden del acorde en la progresión

    @ColumnInfo(name = "duration_in_seconds")
    @NonNull
    public int durationInSeconds; // Duración recomendada del acorde en segundos

    @ColumnInfo(name = "notes")
    public String notes; // Notas opcionales para el acorde
}
