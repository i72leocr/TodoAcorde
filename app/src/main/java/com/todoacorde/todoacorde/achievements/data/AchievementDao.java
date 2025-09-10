package com.todoacorde.todoacorde.achievements.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Query;
import androidx.room.Upsert;

import java.util.List;

/**
 * Interfaz DAO (Data Access Object) para la gestión de logros en la base de datos.
 *
 * Proporciona operaciones de consulta, inserción y actualización sobre la tabla
 * {@code achievements}, permitiendo tanto observación reactiva como acceso directo.
 */
@Dao
public interface AchievementDao {

    /**
     * Obtiene todos los logros ordenados por familia y por nivel.
     * El orden de los niveles se define manualmente como BRONZE &lt; SILVER &lt; GOLD.
     *
     * @return un objeto {@link LiveData} que emite la lista de {@link AchievementEntity}
     *         cada vez que la tabla de logros cambia.
     */
    @Query(
            "SELECT * FROM achievements " +
                    "ORDER BY familyId, " +
                    "CASE level " +
                    "  WHEN 'BRONZE' THEN 0 " +
                    "  WHEN 'SILVER' THEN 1 " +
                    "  WHEN 'GOLD'   THEN 2 " +
                    "  ELSE 3 END"
    )
    LiveData<List<AchievementEntity>> observeAll();

    /**
     * Inserta un logro en la base de datos. Si ya existe uno con la misma clave primaria,
     * se actualiza.
     *
     * @param achievement entidad {@link AchievementEntity} a insertar o actualizar.
     */
    @Upsert
    void upsert(AchievementEntity achievement);

    /**
     * Inserta o actualiza una lista de logros en la base de datos.
     *
     * @param achievements lista de entidades {@link AchievementEntity} a persistir.
     */
    @Upsert
    void upsertAll(List<AchievementEntity> achievements);

    /**
     * Recupera un logro específico a partir de su familia y nivel.
     *
     * @param family identificador de la familia de logros (columna {@code familyId}).
     * @param level  nivel del logro, consistente con {@link Achievement.Level}.
     * @return la entidad encontrada o {@code null} si no existe.
     */
    @Query("SELECT * FROM achievements WHERE familyId = :family AND level = :level LIMIT 1")
    AchievementEntity getByFamilyAndLevel(String family, Achievement.Level level);

    /**
     * Recupera el primer logro asociado a una familia.
     *
     * @param familyId identificador de la familia (columna {@code familyId}).
     * @return la entidad encontrada o {@code null} si no existe.
     */
    @Query("SELECT * FROM achievements WHERE familyId = :familyId LIMIT 1")
    AchievementEntity getByFamily(String familyId);
}
