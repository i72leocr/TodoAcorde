package com.tuguitar.todoacorde;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Query;
import androidx.room.Upsert;

import java.util.List;

/**
 * DAO para persistir y consultar el estado de los logros.
 */
@Dao
public interface AchievementDao {

    /**
     * Observa todos los logros en base de datos, ordenados por familia y nivel.
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
     * Inserta o actualiza un logro de forma atómica.
     */
    @Upsert
    void upsert(AchievementEntity achievement);

    /**
     * Inserta o actualiza varios logros a la vez.
     */
    @Upsert
    void upsertAll(List<AchievementEntity> achievements);

    /**
     * Lee de forma síncrona el estado de un logro para la familia y nivel dados.
     * Útil para llamadas en hilo de disco.
     */
    @Query("SELECT * FROM achievements WHERE familyId = :family AND level = :level LIMIT 1")
    AchievementEntity getByFamilyAndLevel(String family, Achievement.Level level);

    /**
     * Lee de forma síncrona el estado de un logro para la familia dada (primer registro encontrado).
     * ¡Obsoleto si usas niveles! Mejor usa getByFamilyAndLevel.
     */
    @Query("SELECT * FROM achievements WHERE familyId = :familyId LIMIT 1")
    AchievementEntity getByFamily(String familyId);
}
