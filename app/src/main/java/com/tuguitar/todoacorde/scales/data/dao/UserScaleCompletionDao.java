package com.tuguitar.todoacorde.scales.data.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.tuguitar.todoacorde.scales.data.entity.UserScaleCompletionEntity;

@Dao
public interface UserScaleCompletionDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insert(UserScaleCompletionEntity completion);

    @Query("SELECT COUNT(*) FROM UserScaleCompletion WHERE userId = :userId AND scaleId = :scaleId AND tonalityId = :tonalityId")
    int countBoxesCompletedForTonality(long userId, long scaleId, long tonalityId);

    @Query("SELECT MAX(boxOrder) FROM UserScaleCompletion WHERE userId = :userId AND scaleId = :scaleId AND tonalityId = :tonalityId")
    Integer getMaxCompletedBox(long userId, long scaleId, long tonalityId);

    @Query("SELECT COUNT(*) FROM UserScaleCompletion WHERE userId = :userId AND scaleId = :scaleId")
    int countBoxesCompletedForScaleAllTonalities(long userId, long scaleId);

    @Query("SELECT EXISTS(SELECT 1 FROM UserScaleCompletion WHERE userId = :userId AND scaleId = :scaleId AND tonalityId = :tonalityId AND boxOrder = :boxOrder)")
    boolean isBoxCompleted(long userId, long scaleId, long tonalityId, int boxOrder);
}
