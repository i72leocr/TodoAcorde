package com.todoacorde.todoacorde;

import androidx.room.Embedded;
import androidx.room.Relation;

import com.todoacorde.todoacorde.songs.data.Song;
import com.todoacorde.todoacorde.songs.data.SongChord;
import com.todoacorde.todoacorde.songs.data.SongLyric;

import java.util.List;

/**
 * Proyección Room que agrega a una entidad Song sus líneas de letra y acordes asociados.
 * Relaciones:
 * - lyricLines: Song (id) -> SongLyric (songId)
 * - chordLines: Song (id) -> SongChord (songId)
 */
public class SongWithDetails {

    /** Entidad principal de la que cuelgan las relaciones. */
    @Embedded
    public Song song;

    /**
     * Relación uno-a-muchos con las líneas de letra de la canción.
     * parentColumn: Song.id
     * entityColumn: SongLyric.songId
     */
    @Relation(
            parentColumn = "id",
            entityColumn = "songId",
            entity = SongLyric.class
    )
    public List<SongLyric> lyricLines;

    /**
     * Relación uno-a-muchos con las líneas de acordes de la canción.
     * parentColumn: Song.id
     * entityColumn: SongChord.songId
     */
    @Relation(
            parentColumn = "id",
            entityColumn = "songId",
            entity = SongChord.class
    )
    public List<SongChord> chordLines;
}
