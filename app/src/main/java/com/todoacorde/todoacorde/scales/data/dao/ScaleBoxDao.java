package com.todoacorde.todoacorde.scales.data.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.todoacorde.todoacorde.scales.data.entity.ScaleBoxEntity;

import java.util.List;

/**
 * DAO para el acceso a las cajas (boxes) de una escala.
 *
 * Ofrece operaciones de inserción (individual y en lote) y consultas
 * por identificador de escala, así como la obtención del orden máximo
 * de caja registrado para una escala.
 */
@Dao
public interface ScaleBoxDao {

    /**
     * Inserta una entidad de caja.
     *
     * Estrategia de conflicto: IGNORE (no reemplaza si existe conflicto).
     *
     * @param box entidad {@link ScaleBoxEntity} a insertar.
     * @return identificador de fila generado; -1 si se ignoró por conflicto.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insert(ScaleBoxEntity box);

    /**
     * Inserta múltiples entidades de caja.
     *
     * Estrategia de conflicto: IGNORE (cada elemento conflictivo devuelve -1).
     *
     * @param boxes lista de entidades {@link ScaleBoxEntity} a insertar.
     * @return lista de ids generados alineada con la entrada; -1 en posiciones ignoradas.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    List<Long> insertAll(List<ScaleBoxEntity> boxes);

    /**
     * Recupera todas las cajas de una escala ordenadas por su posición.
     *
     * @param scaleId identificador de la escala.
     * @return lista de {@link ScaleBoxEntity} ordenada ascendentemente por boxOrder.
     */
    @Query("SELECT * FROM ScaleBox WHERE scaleId = :scaleId ORDER BY boxOrder ASC")
    List<ScaleBoxEntity> getByScale(long scaleId);

    /**
     * Obtiene el mayor valor de orden de caja existente para una escala.
     *
     * @param scaleId identificador de la escala.
     * @return máximo boxOrder; puede ser null si no hay registros para la escala.
     */
    @Query("SELECT MAX(boxOrder) FROM ScaleBox WHERE scaleId = :scaleId")
    Integer getMaxBoxOrder(long scaleId);
}
