package com.todoacorde.todoacorde.songs.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

import com.todoacorde.todoacorde.Chord;
import com.todoacorde.todoacorde.ChordDao;
import com.todoacorde.todoacorde.SongWithDetails;

import java.util.List;

/**
 * Acceso a datos para la tabla {@code songs}.
 */
@Dao
public interface SongDao {

    /**
     * Inserta una canción.
     *
     * @param song entidad a persistir.
     * @return identificador autogenerado.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(Song song);

    /**
     * Actualiza una canción existente.
     *
     * @param song entidad a actualizar.
     */
    @Update
    void updateSong(Song song);

    /**
     * Obtiene todas las canciones.
     *
     * @return lista reactiva de canciones.
     */
    @Query("SELECT * FROM songs")
    LiveData<List<Song>> getAllSongs();

    /**
     * Obtiene una canción por título.
     *
     * @param title título a buscar.
     * @return canción coincidente o null.
     */
    @Query("SELECT * FROM songs WHERE title = :title LIMIT 1")
    LiveData<Song> getSongByTitle(String title);

    /**
     * Obtiene una canción por id.
     *
     * @param songId identificador de la canción.
     * @return canción coincidente o null.
     */
    @Query("SELECT * FROM songs WHERE id = :songId LIMIT 1")
    LiveData<Song> getSongById(int songId);

    /**
     * Obtiene canciones marcadas como favoritas (flag {@code isFavorite}).
     *
     * @return lista reactiva de canciones favoritas.
     */
    @Query("SELECT * FROM songs WHERE isFavorite = 1")
    LiveData<List<Song>> getFavoriteSongs();

    /**
     * Cuenta total de canciones.
     *
     * @return número total de filas.
     */
    @Query("SELECT COUNT(*) FROM songs")
    LiveData<Integer> getTotalSongCount();

    /**
     * Inserta una canción y sus relaciones en {@code song_chords} en una única transacción.
     *
     * @param song         entidad canción.
     * @param chords       acordes relacionados (no usado aquí).
     * @param songChords   relaciones {@code SongChord}.
     * @param chordDao     DAO de acordes (no usado aquí).
     * @param songChordDao DAO de relaciones canción–acorde.
     */
    @Transaction
    default void insertSongWithChords(Song song,
                                      List<Chord> chords,
                                      List<SongChord> songChords,
                                      ChordDao chordDao,
                                      SongChordDao songChordDao) {
        long rowId = insert(song);
        int songId = (int) rowId;
        song.setId(songId);

        if (songChords != null) {
            for (SongChord sc : songChords) {
                if (sc == null) continue;
                sc.songId = songId;
                songChordDao.insertSongChord(sc);
            }
        }
    }

    /**
     * Obtiene una canción con detalles relacionados.
     *
     * @param songId identificador de la canción.
     * @return datos agregados de la canción.
     */
    @Transaction
    @Query("SELECT * FROM songs WHERE id = :songId")
    LiveData<SongWithDetails> getSongWithDetails(int songId);
}
