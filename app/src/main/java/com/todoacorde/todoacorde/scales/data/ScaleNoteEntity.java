package com.todoacorde.todoacorde.scales.data;

import static androidx.room.ForeignKey.CASCADE;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Entidad Room que representa una nota de una posición/patrón de escala en el diapasón.
 *
 * Reglas:
 * - Clave primaria autogenerada en {@link #id}.
 * - Clave foránea {@link #patternId} → {@code ScalePatternEntity.id} con borrado en cascada.
 * - Índices para consultas rápidas por {@code patternId} y combinación
 *   {@code (patternId, stringIndex)}.
 */
@Entity(
        tableName = "scale_notes",
        foreignKeys = @ForeignKey(
                entity = ScalePatternEntity.class,
                parentColumns = "id",
                childColumns = "patternId",
                onDelete = CASCADE
        ),
        indices = {
                @Index("patternId"),
                @Index({"patternId", "stringIndex"})
        }
)
public class ScaleNoteEntity {

    /** Identificador único autogenerado. */
    @PrimaryKey(autoGenerate = true)
    public long id;

    /** Referencia al patrón/posición al que pertenece esta nota. */
    public long patternId;

    /** Índice de la cuerda (0 = 6ª, 5 = 1ª). */
    public int stringIndex;

    /** Traste donde se toca la nota (0 = al aire). */
    public int fret;

    /** Grado relativo dentro de la escala (por ejemplo: "1", "b3", "5"). */
    public String degree;

    /** Indica si la nota es la tónica del patrón/escala. */
    public boolean isRoot;

    /** Nombre de la nota (por ejemplo: "C", "F#", "Bb"). */
    public String noteName;

    /** Etiqueta opcional (por ejemplo, dedo sugerido u otra marca). */
    public String tag;

    /**
     * Constructor completo.
     *
     * @param patternId   id del patrón de escala al que pertenece
     * @param stringIndex índice de la cuerda (0 a 5)
     * @param fret        número de traste (>= 0)
     * @param degree      grado relativo de la escala
     * @param isRoot      true si es la tónica
     * @param tag         etiqueta opcional (puede ser "")
     * @param noteName    nombre de la nota musical
     */
    public ScaleNoteEntity(long patternId,
                           int stringIndex,
                           int fret,
                           String degree,
                           boolean isRoot,
                           String tag,
                           String noteName) {
        this.patternId = patternId;
        this.stringIndex = stringIndex;
        this.fret = fret;
        this.degree = degree;
        this.isRoot = isRoot;
        this.tag = tag;
        this.noteName = noteName;
    }
}
