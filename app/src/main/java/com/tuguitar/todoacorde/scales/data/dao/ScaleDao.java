package com.tuguitar.todoacorde.scales.data.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.tuguitar.todoacorde.scales.data.entity.ScaleEntity;

import java.util.List;

@Dao
public interface ScaleDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insert(ScaleEntity scale);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    List<Long> insertAll(List<ScaleEntity> scales);

    @Query("SELECT * FROM Scale ORDER BY tier ASC, name ASC")
    List<ScaleEntity> getAll();

    @Query("SELECT * FROM Scale WHERE tier = :tier ORDER BY name ASC")
    List<ScaleEntity> getByTier(int tier);

    @Query("SELECT COUNT(*) FROM Scale WHERE tier = :tier")
    int countByTier(int tier);
}
