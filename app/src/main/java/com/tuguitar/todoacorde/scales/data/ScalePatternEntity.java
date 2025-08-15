package com.tuguitar.todoacorde.scales.data;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import androidx.room.Ignore;

/**
 * Entity de patrón de escala.
 * Cada fila representa UNA caja/variante para un (scaleType, rootNote).
 */
@Entity(
        tableName = "scale_patterns",
        indices = {
                @Index(value = {"scaleType"}),
                @Index(value = {"rootNote"}),
                @Index(value = {"scaleType","rootNote"}),
                // Útil para ordenar/filtrar cajas por tipo+raíz+posición
                @Index(value = {"scaleType","rootNote","positionIndex"})
        }
)
public class ScalePatternEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;

    public String name;       // p.ej. "E Phrygian – Caja 5–8"
    public String scaleType;  // p.ej. "Phrygian", "Ionian", etc.
    public String rootNote;   // p.ej. "E", "A"

    // Ventana de trastes de la caja (opcional pero recomendable)
    public int startFret;     // p.ej. 5
    public int endFret;       // p.ej. 9

    // Orden natural de la caja (0..N-1). Útil para UI.
    public int positionIndex; // por defecto 0 si no se setea

    // Sistema/colección (“CAGED”, “3NPS”, etc.) opcional
    public String system;

    /** Constructor vacío para Room. */
    public ScalePatternEntity() {
        this.startFret = 0;
        this.endFret = 12;
        this.positionIndex = 0;
        this.system = null;
    }

    /** Conveniencia: sin ventana. */
    @Ignore
    public ScalePatternEntity(String name, String scaleType, String rootNote) {
        this(name, scaleType, rootNote, 0, 12, 0, null);
    }

    /** ✔ NUEVO: Conveniencia con ventana (lo que usa el seeder). */
    @Ignore
    public ScalePatternEntity(String name, String scaleType, String rootNote,
                              int startFret, int endFret) {
        this(name, scaleType, rootNote, startFret, endFret, 0, null);
    }

    /** Conveniencia completa. */
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
