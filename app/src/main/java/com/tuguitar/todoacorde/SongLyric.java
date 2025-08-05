package com.tuguitar.todoacorde;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;
import androidx.room.Query;

@Entity(
        tableName = "song_lyrics",
        foreignKeys = @ForeignKey(entity = Song.class, parentColumns = "id", childColumns = "songId")
)
public class SongLyric {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public int songId;      // Relación con la canción
    public int verseOrder;  // Orden del verso
    public String line;     // Texto del verso

    public SongLyric(int songId, int verseOrder, String line) {
        this.songId = songId;
        this.verseOrder = verseOrder;
        this.line = line;
    }
}
