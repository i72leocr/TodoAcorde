package com.tuguitar.todoacorde;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Transaction;

import java.util.List;

@Dao
public interface ProgressionChordDao {
    @Insert
    long insertProgressionChord(ProgressionChord progressionChord);

    @Query("DELETE FROM progression_chords WHERE progression_id = :progressionId")
    void deleteProgressionChordsByProgression(int progressionId);

    @Query("SELECT * FROM progression_chords WHERE progression_id = :progressionId ORDER BY order_in_progression ASC")
    List<ProgressionChord> getChordsForProgression(int progressionId);

    @Query("SELECT * FROM progression_chords WHERE chord_id = :chordId")
    List<ProgressionChord> getProgressionsForChord(int chordId);

    @Query("SELECT * FROM progression_chords WHERE progression_id = :progressionId AND chord_id = :chordId")
    ProgressionChord getSpecificProgressionChord(int progressionId, int chordId);
}