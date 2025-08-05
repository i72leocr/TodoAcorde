package com.tuguitar.todoacorde;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;
@Dao
public interface ChordTypeDao {
    @Query("SELECT * FROM chord_types")
    List<ChordType> getAllChordTypes();


    @Insert
    void insert(ChordType chordType);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<ChordType> chordTypes);
}
