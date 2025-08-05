package com.tuguitar.todoacorde;

import static androidx.room.OnConflictStrategy.IGNORE;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface AchievementDefinitionDao {

    @Query("SELECT * FROM achievement_definitions ORDER BY familyId, level")
    LiveData<List<AchievementDefinitionEntity>> observeAllDefinitions();

    @Insert(onConflict = IGNORE)
    void insertAllDefinitions(List<AchievementDefinitionEntity> defs);

    /**
     * Lee las definiciones para una familia concreta (BRONZE, SILVER, GOLD) en orden de nivel.
     * Se usa desde los use cases para extraer sus thresholds dinámicamente.
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
