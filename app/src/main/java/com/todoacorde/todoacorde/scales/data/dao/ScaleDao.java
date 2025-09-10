package com.todoacorde.todoacorde.scales.data.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.todoacorde.todoacorde.scales.data.entity.ScaleEntity;

import java.util.List;

/**
 * DAO para el acceso y gestión de entidades {@link ScaleEntity}.
 *
 * Proporciona operaciones de inserción (individual y en lote) y consultas
 * por nivel de dificultad (tier) o globales, incluyendo conteos.
 */
@Dao
public interface ScaleDao {

    /**
     * Inserta una escala.
     *
     * Estrategia de conflicto: {@link OnConflictStrategy#IGNORE}. Si existe
     * conflicto (por claves únicas/primarias), la inserción se ignora.
     *
     * @param scale entidad {@link ScaleEntity} a insertar.
     * @return id de fila generado; -1 si la inserción fue ignorada por conflicto.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insert(ScaleEntity scale);

    /**
     * Inserta múltiples escalas.
     *
     * Estrategia de conflicto: {@link OnConflictStrategy#IGNORE}. Cada elemento
     * conflictivo devuelve -1 en su posición correspondiente del resultado.
     *
     * @param scales lista de entidades {@link ScaleEntity} a insertar.
     * @return lista de ids generados alineada con la entrada; -1 en posiciones ignoradas.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    List<Long> insertAll(List<ScaleEntity> scales);

    /**
     * Recupera todas las escalas ordenadas por nivel (tier) y nombre.
     *
     * @return lista completa de {@link ScaleEntity}.
     */
    @Query("SELECT * FROM Scale ORDER BY tier ASC, name ASC")
    List<ScaleEntity> getAll();

    /**
     * Recupera todas las escalas de un tier concreto ordenadas por nombre.
     *
     * @param tier nivel de dificultad/agrupación de la escala.
     * @return lista de {@link ScaleEntity} pertenecientes al tier indicado.
     */
    @Query("SELECT * FROM Scale WHERE tier = :tier ORDER BY name ASC")
    List<ScaleEntity> getByTier(int tier);

    /**
     * Obtiene el número de escalas existentes para un tier concreto.
     *
     * @param tier nivel de dificultad/agrupación.
     * @return cantidad de registros en {@code Scale} con el tier indicado.
     */
    @Query("SELECT COUNT(*) FROM Scale WHERE tier = :tier")
    int countByTier(int tier);
}
