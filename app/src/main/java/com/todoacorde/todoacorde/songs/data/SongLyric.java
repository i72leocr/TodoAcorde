package com.todoacorde.todoacorde.songs.data;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;

/**
 * Entidad Room para una línea de letra de una canción.
 *
 * Estructura:
 * - Tabla: song_lyrics
 * - Clave primaria autogenerada: {@link #id}
 * - Clave foránea: {@link #songId} → {@link Song#id}
 *
 * Semántica:
 * - Cada fila representa una línea de la letra.
 * - {@link #verseOrder} indica el orden del verso/estrofa dentro de la canción.
 * - {@link #line} contiene el texto de la línea.
 */
@Entity(
        tableName = "song_lyrics",
        foreignKeys = @ForeignKey(
                entity = Song.class,
                parentColumns = "id",
                childColumns = "songId"
        )
)
public class SongLyric {

    /**
     * Identificador único autogenerado de la línea.
     */
    @PrimaryKey(autoGenerate = true)
    public int id;

    /**
     * Identificador de la canción a la que pertenece la línea.
     * Clave foránea hacia {@link Song#id}.
     */
    public int songId;

    /**
     * Posición del verso/estrofa dentro de la canción (base arbitraria definida por la inserción).
     */
    public int verseOrder;

    /**
     * Texto de la línea.
     */
    public String line;

    /**
     * Constructor de entidad.
     *
     * @param songId     id de la canción.
     * @param verseOrder orden del verso/estrofa.
     * @param line       contenido de la línea.
     */
    public SongLyric(int songId, int verseOrder, String line) {
        this.songId = songId;
        this.verseOrder = verseOrder;
        this.line = line;
    }
}
