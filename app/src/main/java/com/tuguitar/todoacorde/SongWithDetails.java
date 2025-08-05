package com.tuguitar.todoacorde;

import androidx.room.Embedded;
import androidx.room.Relation;
import java.util.List;

public class SongWithDetails {
    @Embedded
    public Song song;

    @Relation(
            parentColumn  = "id",
            entityColumn  = "songId",
            entity        = SongLyric.class
    )
    public List<SongLyric> lyricLines;

    @Relation(
            parentColumn  = "id",
            entityColumn  = "songId",
            entity        = SongChord.class
    )
    public List<SongChord> chordLines;
}
