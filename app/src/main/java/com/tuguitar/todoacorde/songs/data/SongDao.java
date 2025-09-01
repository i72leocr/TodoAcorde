package com.tuguitar.todoacorde.songs.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

import com.tuguitar.todoacorde.Chord;
import com.tuguitar.todoacorde.ChordDao;
import com.tuguitar.todoacorde.SongWithDetails;


import java.util.List;

@Dao
public interface SongDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(Song song);

    @Update
    void updateSong(Song song);

    @Query("SELECT * FROM songs")
    LiveData<List<Song>> getAllSongs();

    @Query("SELECT * FROM songs WHERE title = :title LIMIT 1")
    LiveData<Song> getSongByTitle(String title);

    @Query("SELECT * FROM songs WHERE id = :songId LIMIT 1")
    LiveData<Song> getSongById(int songId);

    @Query("SELECT * FROM songs WHERE isFavorite = 1")
    LiveData<List<Song>> getFavoriteSongs();

    @Query("SELECT COUNT(*) FROM songs")
    LiveData<Integer> getTotalSongCount();
    @Transaction
    default void insertSongWithChords(Song song,
                                      List<Chord> chords,
                                      List<SongChord> songChords,
                                      ChordDao chordDao,
                                      SongChordDao songChordDao) {
        insert(song);
        for (SongChord sc : songChords) {
            songChordDao.insertSongChord(sc);
        }
    }
    @Transaction
    @Query("SELECT * FROM songs WHERE id = :songId")
    LiveData<SongWithDetails> getSongWithDetails(int songId);
}
