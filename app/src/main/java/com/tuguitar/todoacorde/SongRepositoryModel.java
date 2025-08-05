package com.tuguitar.todoacorde;

import java.util.List;

// Modelo que se espera leer desde song_repo.json (no es la entidad Room Song)
public class SongRepositoryModel {
    public int id;
    public String title;
    public String author;
    public int difficulty;
    public List<String> lyrics;
    public List<String> chords;
    public List<Integer> chordDurations;
    public int bpm;
    public String measure;
    public boolean isFavorite;
}
