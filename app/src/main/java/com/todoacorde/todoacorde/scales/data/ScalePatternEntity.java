package com.todoacorde.todoacorde.scales.data;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Entidad Room que representa un patrón/posición de una escala en el diapasón.
 *
 * Índices útiles para consultas por tipo de escala, nota raíz y posición.
 * Campos:
 * - {@link #name}: nombre legible del patrón.
 * - {@link #scaleType}: tipo de escala (ej. "Ionian", "Minor Pentatonic").
 * - {@link #rootNote}: nota raíz (ej. "C", "F#", "Bb" normalizada a sostenidos si procede).
 * - {@link #startFret} y {@link #endFret}: ventana de trastes cubierta por el patrón.
 * - {@link #positionIndex}: índice/posición dentro del sistema (ej. CAGED 1..5).
 * - {@link #system}: sistema de digitación (ej. "CAGED", "3NPS"), opcional.
 */
@Entity(
        tableName = "scale_patterns",
        indices = {
                @Index(value = {"scaleType"}),
                @Index(value = {"rootNote"}),
                @Index(value = {"scaleType", "rootNote"}),
                @Index(value = {"scaleType", "rootNote", "positionIndex"})
        }
)
public class ScalePatternEntity {

    @PrimaryKey(autoGenerate = true)
    public long id;

    public String name;
    public String scaleType;
    public String rootNote;
    public int startFret;
    public int endFret;
    public int positionIndex;
    public String system;

    /**
     * Constructor por defecto: ventana 0–12, posición 0, sistema nulo.
     */
    public ScalePatternEntity() {
        this.startFret = 0;
        this.endFret = 12;
        this.positionIndex = 0;
        this.system = null;
    }

    /**
     * Crea un patrón con ventana por defecto 0–12 y posición 0.
     */
    @Ignore
    public ScalePatternEntity(String name, String scaleType, String rootNote) {
        this(name, scaleType, rootNote, 0, 12, 0, null);
    }

    /**
     * Crea un patrón con ventana indicada y posición 0.
     */
    @Ignore
    public ScalePatternEntity(String name, String scaleType, String rootNote,
                              int startFret, int endFret) {
        this(name, scaleType, rootNote, startFret, endFret, 0, null);
    }

    /**
     * Crea un patrón especificando todos los campos relevantes.
     */
    @Ignore
    public ScalePatternEntity(String name, String scaleType, String rootNote,
                              int startFret, int endFret,
                              int positionIndex, String system) {
        this.name = name;
        this.scaleType = scaleType;
        this.rootNote = rootNote;
        this.startFret = startFret;
        this.endFret = endFret;
        this.positionIndex = positionIndex;
        this.system = system;
    }
}
