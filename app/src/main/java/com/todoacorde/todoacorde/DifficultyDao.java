package com.todoacorde.todoacorde;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

/**
 * DAO de Room para acceder a la tabla {@code difficulties}.
 *
 * <p>Proporciona operaciones de inserción, consulta y borrado
 * sobre los registros de {@link Difficulty}.</p>
 */
@Dao
public interface DifficultyDao {

    /**
     * Inserta una dificultad.
     *
     * @param difficulty entidad a insertar.
     */
    @Insert
    void insert(Difficulty difficulty);

    /**
     * Obtiene todas las dificultades existentes.
     *
     * @return lista con todas las filas de {@code difficulties}.
     */
    @Query("SELECT * FROM difficulties")
    List<Difficulty> getAllDifficulties();

    /**
     * Busca una dificultad por su identificador.
     *
     * @param difficultyId id de la dificultad.
     * @return la dificultad encontrada o {@code null} si no existe.
     */
    @Query("SELECT * FROM difficulties WHERE id = :difficultyId")
    Difficulty getDifficultyById(int difficultyId);

    /**
     * Busca una dificultad por su nombre.
     *
     * @param difficultyName valor de la columna {@code difficulty_level}.
     * @return la dificultad encontrada o {@code null} si no existe.
     */
    @Query("SELECT * FROM difficulties WHERE difficulty_level = :difficultyName")
    Difficulty getDifficultyByName(String difficultyName);

    /**
     * Elimina una dificultad por id.
     *
     * @param difficultyId id de la dificultad a borrar.
     */
    @Query("DELETE FROM difficulties WHERE id = :difficultyId")
    void deleteDifficultyById(int difficultyId);

    /**
     * Inserta una lista de dificultades, reemplazando en caso de conflicto.
     *
     * @param difficulties lista de entidades a insertar.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<Difficulty> difficulties);
}
