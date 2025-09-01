package com.tuguitar.todoacorde.scales.data;

import androidx.room.Embedded;
import androidx.room.Relation;

import java.util.List;

/** POJO para cargar patrón con sus notas en una sola consulta. */
public class PatternWithNotes {
    @Embedded
    public ScalePatternEntity pattern;

    @Relation(
            parentColumn = "id",
            entityColumn = "patternId",
            entity = ScaleNoteEntity.class
    )
    public List<ScaleNoteEntity> notes;
}
