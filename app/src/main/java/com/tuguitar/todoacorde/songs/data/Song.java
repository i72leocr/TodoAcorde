package com.tuguitar.todoacorde.songs.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "songs")
public class Song {
    @PrimaryKey(autoGenerate = true)
    private int id;

    public String title;
    public String author;
    private int difficulty; // Ajustado a string para facilitar su interpretación
    private String measure;
    private int bpm;
    private boolean isFavorite;

    public Song(String title, String author, int difficulty, int bpm, String measure, boolean isFavorite) {
        this.title = title;
        this.author = author;
        this.difficulty = difficulty;
        this.bpm = bpm;
        this.measure = measure;
        this.isFavorite = isFavorite;
    }

    // Métodos getter y setter
    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getAuthor() {
        return author;
    }

    public int getDifficulty() {
        return difficulty;
    }

    public String getMeasure() {
        return measure;
    }

    public int getBpm() {
        return bpm;
    }

    public boolean isFavorite() {
        return isFavorite;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public void setDifficulty(int difficulty) {
        this.difficulty = difficulty;
    }

    public void setMeasure(String measure) {
        this.measure = measure;
    }

    public void setBpm(int bpm) {
        this.bpm = bpm;
    }

    public void setFavorite(boolean isFavorite) {
        this.isFavorite = isFavorite;
    }

}
