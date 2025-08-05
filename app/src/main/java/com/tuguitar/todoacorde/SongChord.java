package com.tuguitar.todoacorde;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;


@Entity(
        tableName = "song_chords",
        foreignKeys = {
                @ForeignKey(entity = Song.class, parentColumns = "id", childColumns = "songId"),
                @ForeignKey(entity = Chord.class, parentColumns = "id", childColumns = "chordId"),
                @ForeignKey(entity = SongLyric.class, parentColumns = "id", childColumns = "lyricId")

        }
)
public class SongChord {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public int songId;
    public int chordId;
    public int lyricId;       // Relación con una línea específica de la letra
    public int duration; // Duración del acorde en ms
    public int positionInVerse;  // Posición exacta en el verso
    // Getters
    public int getId() {
        return id;
    }

    public int getSongId() {
        return songId;
    }

    public int getChordId() {
        return chordId;
    }

    public int getDuration() {
        return duration;
    }

    public int getPositionInverse() {
        return positionInVerse;
    }

    // Setters
    public void setId(int id) {
        this.id = id;
    }

    public void setSongId(int songId) {
        this.songId = songId;
    }

    public void setChordId(int chordId) {
        this.chordId = chordId;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public void setPositionInverse(int positionInverse) {
        this.positionInVerse = positionInverse;
    }


    public SongChord(int songId, int chordId, int lyricId, int duration, int positionInVerse) {
        this.songId = songId;
        this.chordId = chordId;
        this.lyricId = lyricId;
        this.duration = duration;
        this.positionInVerse = positionInVerse;
    }
}