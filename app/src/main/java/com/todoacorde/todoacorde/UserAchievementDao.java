package com.todoacorde.todoacorde;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.todoacorde.todoacorde.achievements.data.Achievement;

import java.util.List;

/**
 * DAO para el acceso y actualización de logros de usuario.
 *
 * Proporciona operaciones reactivas (vía {@link LiveData}) para observar los
 * logros y métodos sincrónicos para consultas puntuales y upsert.
 */
@Dao
public interface UserAchievementDao {

    /**
     * Observa los logros de un usuario ordenados por familia y nivel.
     *
     * @param userId id del usuario.
     * @return {@link LiveData} con la lista de {@link UserAchievementEntity}.
     */
    @Query(
            "SELECT * " +
                    "FROM user_achievements " +
                    "WHERE userId = :userId " +
                    "ORDER BY familyId, level"
    )
    LiveData<List<UserAchievementEntity>> observeForUser(long userId);

    /**
     * Inserta o actualiza (REPLACE) un logro de usuario.
     *
     * @param ua entidad de logro de usuario a persistir.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(UserAchievementEntity ua);

    /**
     * Obtiene un logro concreto de un usuario por familia y nivel.
     *
     * @param userId   id del usuario.
     * @param familyId identificador normalizado de la familia del logro.
     * @param level    nivel del logro.
     * @return entidad encontrada o {@code null} si no existe.
     */
    @Query(
            "SELECT * " +
                    "FROM user_achievements " +
                    "WHERE userId = :userId " +
                    "  AND familyId = :familyId " +
                    "  AND level = :level"
    )
    UserAchievementEntity getForUserAndLevel(
            long userId,
            String familyId,
            Achievement.Level level
    );
}
