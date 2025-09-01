package com.tuguitar.todoacorde.songs.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface SongLyricDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertLyrics(List<SongLyric> lyrics);

    @Query("SELECT * FROM song_lyrics WHERE songId = :songId ORDER BY verseOrder")
    LiveData<List<SongLyric>> getLyricsForSong(int songId);


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(SongLyric lyric);

    @Query("DELETE FROM song_lyrics WHERE songId = :songId")
    void deleteLyricsForSong(int songId);

    @Query("SELECT * FROM song_lyrics WHERE songId = :songId AND verseOrder = :verseOrder LIMIT 1")
    LiveData<SongLyric> getLyricByVerseOrder(long songId, int verseOrder);

    @Query("SELECT * FROM song_lyrics WHERE songId = :songId ORDER BY verseOrder")
    LiveData<List<SongLyric>> getLyricsBySongId(int songId);
}

