package com.todoacorde.todoacorde.scales.data;

import androidx.room.Embedded;
import androidx.room.Relation;

import java.util.List;

/**
 * Proyección Room que agrupa un patrón de escala con sus notas asociadas.
 *
 * Estructura:
 * - {@link #pattern}: entidad del patrón (tabla ScalePattern).
 * - {@link #notes}: lista de notas relacionadas (tabla ScaleNote) enlazadas por patternId.
 *
 * Uso típico:
 * - Se utiliza en consultas @Transaction del DAO para recuperar el patrón junto con sus notas
 *   en una sola operación.
 */
public class PatternWithNotes {

    /** Entidad principal del patrón. */
    @Embedded
    public ScalePatternEntity pattern;

    /**
     * Notas asociadas al patrón.
     * parentColumn: id del patrón.
     * entityColumn: patternId en la tabla de notas.
     */
    @Relation(
            parentColumn = "id",
            entityColumn = "patternId",
            entity = ScaleNoteEntity.class
    )
    public List<ScaleNoteEntity> notes;
}
