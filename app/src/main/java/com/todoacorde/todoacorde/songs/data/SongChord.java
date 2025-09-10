package com.todoacorde.todoacorde.songs.data;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import com.todoacorde.todoacorde.Chord;

/**
 * Entidad Room que representa un acorde colocado sobre una línea de la letra (verso) de una canción.
 *
 * Tabla: song_chords
 * - id (PK autogenerada)
 * - songId (FK → Song.id)
 * - chordId (FK → Chord.id)
 * - lyricId (FK → SongLyric.id)
 * - duration (duración en unidades de tu métrica/compás)
 * - positionInVerse (posición ordinal del acorde dentro del verso)
 *
 * Notas:
 * - Se añaden índices por columnas FK para acelerar joins/consultas.
 * - Los métodos get/set usan exactamente el mismo nombre del campo (positionInVerse)
 *   para evitar errores (antes había “getPositionInverse”/“setPositionInverse”).
 */
@Entity(
        tableName = "song_chords",
        indices = {
                @Index("songId"),
                @Index("chordId"),
                @Index("lyricId"),
                @Index(value = {"songId", "lyricId"}),
                @Index(value = {"songId", "positionInVerse"})
        },
        foreignKeys = {
                @ForeignKey(
                        entity = Song.class,
                        parentColumns = "id",
                        childColumns = "songId",
                        onDelete = ForeignKey.CASCADE
                ),
                @ForeignKey(
                        entity = Chord.class,
                        parentColumns = "id",
                        childColumns = "chordId",
                        onDelete = ForeignKey.CASCADE
                ),
                @ForeignKey(
                        entity = SongLyric.class,
                        parentColumns = "id",
                        childColumns = "lyricId",
                        onDelete = ForeignKey.CASCADE
                )
        }
)
public class SongChord {

    /** Identificador único autogenerado. */
    @PrimaryKey(autoGenerate = true)
    public int id;

    /** Canción a la que pertenece el acorde. FK → Song.id */
    public int songId;

    /** Acorde referenciado. FK → Chord.id */
    public int chordId;

    /** Línea/verso de la letra donde se coloca el acorde. FK → SongLyric.id */
    public int lyricId;

    /** Duración del acorde en unidades de tu métrica. */
    public int duration;

    /** Posición ordinal del acorde dentro del verso. */
    public int positionInVerse;

    // --------- Getters ---------

    public int getId() {
        return id;
    }

    public int getSongId() {
        return songId;
    }

    public int getChordId() {
        return chordId;
    }

    public int getLyricId() {
        return lyricId;
    }

    public int getDuration() {
        return duration;
    }

    public int getPositionInVerse() {
        return positionInVerse;
    }

    // --------- Setters ---------

    public void setId(int id) {
        this.id = id;
    }

    public void setSongId(int songId) {
        this.songId = songId;
    }

    public void setChordId(int chordId) {
        this.chordId = chordId;
    }

    public void setLyricId(int lyricId) {
        this.lyricId = lyricId;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public void setPositionInVerse(int positionInVerse) {
        this.positionInVerse = positionInVerse;
    }

    /**
     * Constructor completo para Room.
     *
     * @param songId          id de la canción.
     * @param chordId         id del acorde.
     * @param lyricId         id del verso/línea.
     * @param duration        duración del acorde.
     * @param positionInVerse posición dentro del verso.
     */
    public SongChord(int songId, int chordId, int lyricId, int duration, int positionInVerse) {
        this.songId = songId;
        this.chordId = chordId;
        this.lyricId = lyricId;
        this.duration = duration;
        this.positionInVerse = positionInVerse;
    }
}
