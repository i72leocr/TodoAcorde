package com.tuguitar.todoacorde.scales.data.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.tuguitar.todoacorde.scales.data.entity.TonalityEntity;

import java.util.List;

@Dao
public interface TonalityDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insert(TonalityEntity t);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    List<Long> insertAll(List<TonalityEntity> tonalities);

    @Query("SELECT * FROM Tonality ORDER BY name ASC")
    List<TonalityEntity> getAll();

    @Query("SELECT COUNT(*) FROM Tonality")
    int countAll();
}
