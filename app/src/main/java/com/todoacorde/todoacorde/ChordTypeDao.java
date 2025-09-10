package com.todoacorde.todoacorde;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

/**
 * DAO de Room para acceder a la tabla {@code chord_types}.
 * Proporciona operaciones de consulta e inserción para entidades {@link ChordType}.
 */
@Dao
public interface ChordTypeDao {

    /**
     * Obtiene todos los registros de tipos de acorde.
     *
     * @return lista completa de {@link ChordType}.
     */
    @Query("SELECT * FROM chord_types")
    List<ChordType> getAllChordTypes();

    /**
     * Inserta un único tipo de acorde.
     * Si ya existe un conflicto de clave, se producirá un error según la política por defecto.
     *
     * @param chordType entidad a insertar.
     */
    @Insert
    void insert(ChordType chordType);

    /**
     * Inserta o reemplaza en bloque una lista de tipos de acorde.
     * En caso de conflicto, la fila existente será reemplazada.
     *
     * @param chordTypes lista de entidades a insertar o reemplazar.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<ChordType> chordTypes);
}
