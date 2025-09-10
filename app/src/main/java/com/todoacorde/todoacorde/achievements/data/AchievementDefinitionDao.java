package com.todoacorde.todoacorde.achievements.data;

import static androidx.room.OnConflictStrategy.IGNORE;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

/**
 * Interfaz DAO para la gestión de definiciones de logros en la base de datos.
 *
 * Una definición de logro representa la configuración estática (familia, nivel,
 * umbral, etc.) a partir de la cual se instancian los logros de usuario.
 * Este DAO permite consultar y almacenar dichas definiciones.
 */
@Dao
public interface AchievementDefinitionDao {

    /**
     * Observa todas las definiciones de logros, ordenadas por familia y nivel.
     *
     * @return un {@link LiveData} que emite la lista completa de
     *         {@link AchievementDefinitionEntity} cada vez que la tabla cambia.
     */
    @Query("SELECT * FROM achievement_definitions ORDER BY familyId, level")
    LiveData<List<AchievementDefinitionEntity>> observeAllDefinitions();

    /**
     * Inserta una lista de definiciones de logros en la base de datos.
     * Si alguna definición ya existe (conflicto en clave primaria), se ignora.
     *
     * @param defs lista de entidades {@link AchievementDefinitionEntity} a insertar.
     */
    @Insert(onConflict = IGNORE)
    void insertAllDefinitions(List<AchievementDefinitionEntity> defs);

    /**
     * Recupera todas las definiciones asociadas a una familia, ordenadas por nivel.
     * El orden se establece manualmente como BRONZE &lt; SILVER &lt; GOLD.
     *
     * @param familyId identificador de la familia (columna {@code familyId}).
     * @return lista ordenada de entidades {@link AchievementDefinitionEntity}.
     */
    @Query(
            "SELECT * FROM achievement_definitions " +
                    "WHERE familyId = :familyId " +
                    "ORDER BY CASE level " +
                    "  WHEN 'BRONZE' THEN 0 " +
                    "  WHEN 'SILVER' THEN 1 " +
                    "  WHEN 'GOLD' THEN 2 " +
                    "  ELSE 3 END"
    )
    List<AchievementDefinitionEntity> getDefinitionsForFamily(String familyId);
}
