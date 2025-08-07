package com.tuguitar.todoacorde.songs.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "songs")
public class Song {
    @PrimaryKey(autoGenerate = true)
    private int id;

    private String title;
    private String author;
    private int difficulty;
    private String measure;
    private int bpm;
    private boolean isFavorite;

    // Constructor principal
    public Song(String title, String author, int difficulty,
                int bpm, String measure, boolean isFavorite) {
        this.title      = title;
        this.author     = author;
        this.difficulty = difficulty;
        this.bpm        = bpm;
        this.measure    = measure;
        this.isFavorite = isFavorite;
    }

    // --- NUEVO: constructor de copia ---
    public Song(Song other) {
        this.id         = other.id;
        this.title      = other.title;
        this.author     = other.author;
        this.difficulty = other.difficulty;
        this.bpm        = other.bpm;
        this.measure    = other.measure;
        this.isFavorite = other.isFavorite;
    }

    // Getters y setters
    public int getId()               { return id; }
    public String getTitle()         { return title; }
    public String getAuthor()        { return author; }
    public int getDifficulty()       { return difficulty; }
    public String getMeasure()       { return measure; }
    public int getBpm()              { return bpm; }
    public boolean isFavorite()      { return isFavorite; }

    public void setId(int id)               { this.id = id; }
    public void setTitle(String title)      { this.title = title; }
    public void setAuthor(String author)    { this.author = author; }
    public void setDifficulty(int difficulty){ this.difficulty = difficulty; }
    public void setMeasure(String measure)  { this.measure = measure; }
    public void setBpm(int bpm)             { this.bpm = bpm; }
    public void setFavorite(boolean fav)    { this.isFavorite = fav; }
}
