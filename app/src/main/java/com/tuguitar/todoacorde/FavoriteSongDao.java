package com.tuguitar.todoacorde;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface FavoriteSongDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insert(FavoriteSong favoriteSong);

    @Delete
    void delete(FavoriteSong favoriteSong);

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_songs WHERE userId = :userId AND songId = :songId)")
    LiveData<Boolean> isFavorite(int userId, int songId);

    @Query("SELECT songId FROM favorite_songs WHERE userId = :userId")
    LiveData<List<Integer>> getFavoriteSongIds(int userId);
}
