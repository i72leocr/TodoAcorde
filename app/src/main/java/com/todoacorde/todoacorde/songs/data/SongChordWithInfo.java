package com.todoacorde.todoacorde.songs.data;

import androidx.room.Embedded;
import androidx.room.Relation;

import com.todoacorde.todoacorde.Chord;

/**
 * POJO de resultado para obtener un SongChord y su Chord asociado usando @Relation.
 *
 * Requisitos:
 * - El DAO debe seleccionar la entidad padre (SongChord) sin JOIN manual.
 * - Room ejecutará internamente la consulta relacionada para traer el Chord.
 */
public class SongChordWithInfo {

    /**
     * Entidad padre: fila de la tabla song_chords.
     */
    @Embedded
    public SongChord songChord;

    /**
     * Relación 1–1: SongChord.chordId → Chord.id
     * Room resolverá este vínculo cuando el DAO devuelva SongChord como padre.
     */
    @Relation(
            parentColumn = "chordId",
            entityColumn = "id"
    )
    public Chord chord;
}
