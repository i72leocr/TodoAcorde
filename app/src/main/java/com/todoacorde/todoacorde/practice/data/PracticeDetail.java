package com.todoacorde.todoacorde.practice.data;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;

import com.todoacorde.todoacorde.Chord;

/**
 * Entidad Room que representa el detalle de práctica por acorde dentro de una sesión.
 *
 * <p>Características:</p>
 * <ul>
 *   <li>Clave primaria compuesta por {@code sessionId} y {@code chordId}.</li>
 *   <li>FK a {@link PracticeSession} (borrado en cascada).</li>
 *   <li>FK a {@link Chord} (borrado en cascada).</li>
 *   <li>Índices en {@code sessionId} y {@code chordId} para acelerar consultas.</li>
 * </ul>
 */
@Entity(
        tableName = "practice_details",
        primaryKeys = {"sessionId", "chordId"},
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
                @Index(value = "chordId")
        }
)
public class PracticeDetail {

    /** Identificador de la sesión a la que pertenece este detalle. */
    @ColumnInfo(name = "sessionId")
    public int sessionId;

    /** Identificador del acorde evaluado en la sesión. */
    @ColumnInfo(name = "chordId")
    public int chordId;

    /** Intentos totales realizados para el acorde en la sesión. */
    @ColumnInfo(name = "totalAttempts")
    public int totalAttempts;

    /** Número de aciertos registrados para el acorde. */
    @ColumnInfo(name = "correctCount")
    public int correctCount;

    /** Número de fallos registrados para el acorde. */
    @ColumnInfo(name = "incorrectCount")
    public int incorrectCount;

    /**
     * Crea un detalle de práctica.
     *
     * @param sessionId      id de la sesión.
     * @param chordId        id del acorde.
     * @param totalAttempts  intentos totales.
     * @param correctCount   aciertos.
     * @param incorrectCount fallos.
     */
    public PracticeDetail(int sessionId, int chordId, int totalAttempts, int correctCount, int incorrectCount) {
        this.sessionId = sessionId;
        this.chordId = chordId;
        this.totalAttempts = totalAttempts;
        this.correctCount = correctCount;
        this.incorrectCount = incorrectCount;
    }
}
