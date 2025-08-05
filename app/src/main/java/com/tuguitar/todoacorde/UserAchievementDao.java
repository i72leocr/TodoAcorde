package com.tuguitar.todoacorde;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface UserAchievementDao {

    @Query(
            "SELECT * " +
                    "FROM user_achievements " +
                    "WHERE userId = :userId " +
                    "ORDER BY familyId, level"
    )
    LiveData<List<UserAchievementEntity>> observeForUser(long userId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(UserAchievementEntity ua);

    /**
     * Obtiene el progreso de un nivel concreto para el usuario.
     * Devuelve null si no existe aún.
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
