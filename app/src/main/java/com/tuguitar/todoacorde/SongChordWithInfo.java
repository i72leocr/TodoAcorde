// SongChordWithInfo.java
package com.tuguitar.todoacorde;

import androidx.room.Embedded;
import androidx.room.Relation;

/**
 * Una fila de SongChord, junto con la entidad Chord a la que apunta.
 */
public class SongChordWithInfo {
    /** Todos los campos de SongChord vendrán con su nombre original. */
    @Embedded
    public SongChord songChord;

    /**
     * Room rellenará esta lista buscando
     * SELECT * FROM Chord WHERE id = songChord.chordId
     */
    @Relation(
            parentColumn = "chordId",
            entityColumn = "id"
    )
    public Chord chord;
}
