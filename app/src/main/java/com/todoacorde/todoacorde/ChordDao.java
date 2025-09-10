package com.todoacorde.todoacorde;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.todoacorde.todoacorde.songs.data.SongChord;

import java.util.List;

/**
 * DAO de Room para operaciones sobre la tabla {@code chords} y la relación con {@link SongChord}.
 * Expone consultas reactivas mediante {@link LiveData} y operaciones de inserción.
 *
 * Notas:
 * - Los métodos que retornan {@link LiveData} se observan en la capa UI para recibir actualizaciones.
 * - Los métodos sin {@link LiveData} (sincrónicos) deben llamarse fuera del hilo principal.
 * - La política {@link OnConflictStrategy#REPLACE} sobrescribe filas en caso de conflicto de clave.
 */
@Dao
public interface ChordDao {

    /**
     * Obtiene un acorde por su nombre (reactivo). Limita a un único resultado.
     *
     * @param name nombre exacto del acorde.
     * @return acorde como {@link LiveData}, o vacío si no existe.
     */
    @Query("SELECT * FROM chords WHERE name = :name LIMIT 1")
    LiveData<Chord> getChordByName(String name);

    /**
     * Devuelve todos los acordes disponibles (reactivo).
     *
     * @return lista de acordes envuelta en {@link LiveData}.
     */
    @Query("SELECT * FROM chords")
    LiveData<List<Chord>> getAllChords();

    /**
     * Obtiene un acorde por su identificador (reactivo).
     *
     * @param chordId id del acorde.
     * @return acorde como {@link LiveData}.
     */
    @Query("SELECT * FROM chords WHERE id = :chordId")
    LiveData<Chord> getChordById(int chordId);

    /**
     * Busca un acorde por nombre de forma sincrónica. Útil en lógica de dominio/repositorio.
     * No debe invocarse en el hilo principal.
     *
     * @param name nombre exacto del acorde.
     * @return entidad {@link Chord} o {@code null} si no existe.
     */
    @Query("SELECT * FROM chords WHERE name = :name LIMIT 1")
    Chord findByNameSync(String name);

    /**
     * Recupera acordes filtrando por tipo y dificultad (reactivo).
     *
     * @param typeId id de tipo de acorde (foreign key).
     * @param difficultyId id de dificultad (foreign key).
     * @return lista de acordes como {@link LiveData}.
     */
    @Query("SELECT * FROM chords WHERE type_id = :typeId AND difficulty_id = :difficultyId")
    LiveData<List<Chord>> getChordsByTypeAndDifficulty(int typeId, int difficultyId);

    /**
     * Recupera acordes por nivel de dificultad (reactivo).
     *
     * @param difficulty id de dificultad (foreign key a {@code Difficulty}).
     * @return lista de acordes como {@link LiveData}.
     */
    @Query("SELECT * FROM chords WHERE difficulty_id = :difficulty")
    LiveData<List<Chord>> getChordsByDifficulty(int difficulty);

    /**
     * Inserta una lista de acordes. Reemplaza en caso de conflicto.
     *
     * @param chords lista de entidades {@link Chord}.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<Chord> chords);

    /**
     * Inserta un acorde individual. Reemplaza en caso de conflicto.
     *
     * @param chord entidad {@link Chord}.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Chord chord);

    /**
     * Inserta un vínculo acorde–canción en la tabla {@code song_chords}.
     * Reemplaza en caso de conflicto.
     *
     * @param songChord entidad {@link SongChord}.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertSongChord(SongChord songChord);
}
