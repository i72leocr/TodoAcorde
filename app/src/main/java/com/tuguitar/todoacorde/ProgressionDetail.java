package com.tuguitar.todoacorde;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "progression_details",
        foreignKeys = {
                @ForeignKey(entity = ProgressionSession.class, parentColumns = "id", childColumns = "session_id", onDelete = ForeignKey.CASCADE),
                @ForeignKey(entity = Chord.class, parentColumns = "id", childColumns = "chord_id", onDelete = ForeignKey.CASCADE)
        }
)
public class ProgressionDetail {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public int session_id; // Referencia a la sesión de progresión
    public int chord_id; // Referencia al acorde
    public long timeTaken; // Tiempo tomado para tocar el acorde
    public boolean isCorrect; // Indica si se tocó correctamente

    // Constructor
    public ProgressionDetail(int session_id, int chord_id, long timeTaken, boolean isCorrect) {
        this.session_id = session_id;
        this.chord_id = chord_id;
        this.timeTaken = timeTaken;
        this.isCorrect = isCorrect;
    }
}
