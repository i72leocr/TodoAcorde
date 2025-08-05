package com.tuguitar.todoacorde;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;

import java.util.List;

@Dao
public interface SongChordDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertSongChord(SongChord songChord);

    @Query("SELECT * FROM song_chords " +
                "WHERE songId = :songId " +
                "ORDER BY lyricId, positionInVerse")
        LiveData<List<SongChord>> getChordsForSong(int songId);
    @Transaction
    @Query("SELECT sc.*, c.* "
            + "FROM song_chords sc "
            + "JOIN chords c ON sc.chordId = c.id "
            + "WHERE sc.songId = :songId "
            + "ORDER BY sc.lyricId, sc.positionInVerse")
    LiveData<List<SongChordWithInfo>> getChordsWithInfoForSong(int songId);
}


