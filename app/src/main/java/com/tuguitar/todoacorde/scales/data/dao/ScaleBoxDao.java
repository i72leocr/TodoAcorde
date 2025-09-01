package com.tuguitar.todoacorde.scales.data.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.tuguitar.todoacorde.scales.data.entity.ScaleBoxEntity;

import java.util.List;

@Dao
public interface ScaleBoxDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insert(ScaleBoxEntity box);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    List<Long> insertAll(List<ScaleBoxEntity> boxes);

    @Query("SELECT * FROM ScaleBox WHERE scaleId = :scaleId ORDER BY boxOrder ASC")
    List<ScaleBoxEntity> getByScale(long scaleId);

    @Query("SELECT MAX(boxOrder) FROM ScaleBox WHERE scaleId = :scaleId")
    Integer getMaxBoxOrder(long scaleId);
}
