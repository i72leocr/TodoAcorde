package com.todoacorde.todoacorde.songs.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

/**
 * DAO Room para el acceso a las letras de canciones.
 *
 * Operaciones principales:
 * - Inserción individual y en lote.
 * - Consulta reactiva (LiveData) por canción y por orden de verso.
 * - Borrado de todas las líneas de una canción.
 */
@Dao
public interface SongLyricDao {

    /**
     * Inserta una lista de líneas de letra. Reemplaza en caso de conflicto.
     *
     * @param lyrics lista de entidades {@link SongLyric} a insertar.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertLyrics(List<SongLyric> lyrics);

    /**
     * Obtiene todas las líneas de una canción ordenadas por {@code verseOrder}.
     *
     * @param songId id de la canción.
     * @return LiveData con la lista de líneas.
     */
    @Query("SELECT * FROM song_lyrics WHERE songId = :songId ORDER BY verseOrder")
    LiveData<List<SongLyric>> getLyricsForSong(int songId);

    /**
     * Inserta una línea de letra. Reemplaza en caso de conflicto.
     *
     * @param lyric entidad a insertar.
     * @return id autogenerado de la línea insertada.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(SongLyric lyric);

    /**
     * Elimina todas las líneas asociadas a una canción.
     *
     * @param songId id de la canción.
     */
    @Query("DELETE FROM song_lyrics WHERE songId = :songId")
    void deleteLyricsForSong(int songId);

    /**
     * Obtiene una línea concreta por canción y orden de verso.
     *
     * @param songId     id de la canción.
     * @param verseOrder orden del verso/estrofa.
     * @return LiveData con la línea encontrada (o vacío si no existe).
     */
    @Query("SELECT * FROM song_lyrics WHERE songId = :songId AND verseOrder = :verseOrder LIMIT 1")
    LiveData<SongLyric> getLyricByVerseOrder(long songId, int verseOrder);

    /**
     * Alias de {@link #getLyricsForSong(int)}. Devuelve las líneas de una canción
     * ordenadas por {@code verseOrder}.
     *
     * @param songId id de la canción.
     * @return LiveData con la lista de líneas.
     */
    @Query("SELECT * FROM song_lyrics WHERE songId = :songId ORDER BY verseOrder")
    LiveData<List<SongLyric>> getLyricsBySongId(int songId);
}
