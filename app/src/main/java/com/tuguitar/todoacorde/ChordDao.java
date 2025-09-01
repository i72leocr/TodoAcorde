package com.tuguitar.todoacorde;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.tuguitar.todoacorde.songs.data.SongChord;

import java.util.List;

@Dao
public interface ChordDao {

    @Query("SELECT * FROM chords WHERE name = :name LIMIT 1")
    LiveData<Chord> getChordByName(String name);

    @Query("SELECT * FROM chords")
    LiveData<List<Chord>> getAllChords();

    @Query("SELECT * FROM chords WHERE id = :chordId")
    LiveData<Chord> getChordById(int chordId);
    /** Búsqueda síncrona por nombre, para usar en el seeder */
    @Query("SELECT * FROM chords WHERE name = :name LIMIT 1")
    Chord findByNameSync(String name);

    @Query("SELECT * FROM chords WHERE type_id = :typeId AND difficulty_id = :difficultyId")
    LiveData<List<Chord>> getChordsByTypeAndDifficulty(int typeId, int difficultyId);

    @Query("SELECT * FROM chords WHERE difficulty_id = :difficulty")
    LiveData<List<Chord>> getChordsByDifficulty(int difficulty);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<Chord> chords);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Chord chord);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertSongChord(SongChord songChord);
}
