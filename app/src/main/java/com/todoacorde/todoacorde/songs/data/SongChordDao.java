package com.todoacorde.todoacorde.songs.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;

import java.util.List;

/**
 * DAO de Room para acceder a los acordes de una canción.
 *
 * Responsabilidades:
 * - Insertar registros en la tabla {@code song_chords}.
 * - Consultar los acordes de una canción ordenados por verso y posición.
 * - Consultar los acordes con su información de acorde (JOIN con tabla {@code chords}).
 *
 * Notas de mapeo:
 * - El método {@link #getChordsWithInfoForSong(int)} realiza un JOIN y devuelve
 *   {@code SongChordWithInfo}. Asegúrate de que esa clase esté preparada para
 *   el mapeo (por ejemplo, con campos anotados con {@code @Embedded} para
 *   {@code SongChord} y para la entidad/POJO del acorde).
 */
@Dao
public interface SongChordDao {

    /**
     * Inserta o reemplaza un acorde de canción.
     * Estrategia REPLACE: si existe el mismo {@code id}, se sobrescribe.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertSongChord(SongChord songChord);

    /**
     * Devuelve los acordes de una canción ordenados por verso y posición.
     *
     * @param songId id de la canción.
     * @return LiveData con la lista de {@code SongChord}.
     */
    @Query("SELECT * FROM song_chords " +
            "WHERE songId = :songId " +
            "ORDER BY lyricId, positionInVerse")
    LiveData<List<SongChord>> getChordsForSong(int songId);

    /**
     * Devuelve los acordes de una canción junto con la info del acorde (JOIN con tabla chords),
     * ordenados por verso y posición.
     *
     * IMPORTANTE: {@code SongChordWithInfo} debe estar definido para este resultado combinado.
     * Un patrón habitual es:
     * - Campo {@code @Embedded} para {@code SongChord}.
     * - Campo {@code @Embedded(prefix = "chord_")} para el POJO del acorde,
     *   y ajustar los alias de columnas en el SELECT para que coincidan con el prefijo.
     *
     * @param songId id de la canción.
     * @return LiveData con la lista de {@code SongChordWithInfo}.
     */
    @Transaction
    @Query("SELECT sc.*, c.* " +
            "FROM song_chords sc " +
            "JOIN chords c ON sc.chordId = c.id " +
            "WHERE sc.songId = :songId " +
            "ORDER BY sc.lyricId, sc.positionInVerse")
    LiveData<List<SongChordWithInfo>> getChordsWithInfoForSong(int songId);
}
