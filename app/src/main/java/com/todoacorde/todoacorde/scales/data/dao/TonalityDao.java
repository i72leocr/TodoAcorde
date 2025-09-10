package com.todoacorde.todoacorde.scales.data.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.todoacorde.todoacorde.scales.data.entity.TonalityEntity;

import java.util.List;

/**
 * DAO para gestionar entidades {@link TonalityEntity}.
 *
 * Proporciona operaciones de inserción (individual y múltiple) y consultas
 * básicas sobre la tabla Tonality.
 */
@Dao
public interface TonalityDao {

    /**
     * Inserta una tonalidad.
     *
     * Estrategia de conflicto: {@link OnConflictStrategy#IGNORE}.
     * Si existe un conflicto por clave primaria/única, la inserción se ignora.
     *
     * @param t entidad {@link TonalityEntity} a insertar.
     * @return id de fila generado; -1 si la inserción fue ignorada por conflicto.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insert(TonalityEntity t);

    /**
     * Inserta una lista de tonalidades.
     *
     * Estrategia de conflicto: {@link OnConflictStrategy#IGNORE}.
     * Para cada elemento conflictivo se devuelve -1 en la posición correspondiente.
     *
     * @param tonalities lista de entidades {@link TonalityEntity}.
     * @return lista de ids generados alineada con la entrada; -1 en posiciones ignoradas.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    List<Long> insertAll(List<TonalityEntity> tonalities);

    /**
     * Obtiene todas las tonalidades ordenadas alfabéticamente por nombre.
     *
     * @return lista de {@link TonalityEntity}.
     */
    @Query("SELECT * FROM Tonality ORDER BY name ASC")
    List<TonalityEntity> getAll();

    /**
     * Cuenta el total de tonalidades registradas.
     *
     * @return número total de filas en la tabla Tonality.
     */
    @Query("SELECT COUNT(*) FROM Tonality")
    int countAll();
}
