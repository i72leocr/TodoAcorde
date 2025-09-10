package com.todoacorde.todoacorde.practice.data;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

/**
 * Entidad Room que representa una sesión de práctica de un usuario sobre una canción.
 *
 * Contiene metadatos de la sesión (tiempos, duración, puntuación, velocidad)
 * y flags de conveniencia para marcar si es la última o la mejor sesión registrada
 * para una combinación dada de canción/usuario a una velocidad concreta.
 */
@Entity(tableName = "practice_sessions")
public class PracticeSession {

    /** Identificador autogenerado de la sesión. */
    @PrimaryKey(autoGenerate = true)
    public int id;

    /** Identificador del usuario que realizó la sesión. */
    public int userId;

    /** Identificador de la canción practicada. */
    public int songId;

    /** Indica si la sesión se completó (por ejemplo, llegó al final). */
    public boolean isCompleted;

    /** Marca de tiempo de inicio (epoch millis). */
    public long startTime;

    /** Marca de tiempo de fin (epoch millis). */
    public long endTime;

    /** Duración total de la sesión en milisegundos. */
    public long duration;

    /** Puntuación total obtenida en la sesión. */
    public int totalScore;

    /** Velocidad de reproducción utilizada (por ejemplo, 0.5, 0.75, 1.0). */
    public float speed;

    /** Marca si esta sesión es la última registrada para la pareja canción/usuario/velocidad. */
    public boolean isLastSession;

    /** Marca si esta sesión es la mejor registrada para la pareja canción/usuario/velocidad. */
    public boolean isBestSession;

    /** Constructor por defecto requerido por Room. */
    public PracticeSession() {
    }

    /**
     * Constructor de conveniencia para crear una sesión con todos los campos,
     * ignorado por Room para la instanciación.
     *
     * @param songId        id de la canción
     * @param userId        id del usuario
     * @param isCompleted   si la sesión se completó
     * @param startTime     inicio en epoch millis
     * @param endTime       fin en epoch millis
     * @param duration      duración en millis
     * @param totalScore    puntuación total
     * @param speed         velocidad de reproducción
     * @param isLastSession indicador de última sesión
     * @param isBestSession indicador de mejor sesión
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
