package com.tuguitar.todoacorde;

import java.util.List;

public class Song {
    private String title;
    private String author;  // Nuevo campo para el autor
    private int difficulty;  // Dificultad de la canción
    private List<String> lyrics;  // Letra de la canción
    private List<String> chords;  // Acordes de la canción
    private List<Integer> chordDurations; // Duración de cada acorde (opcional si se usa)
    private int bpm;  // BPM de la canción (Beats per minute)
    private boolean isFavorite;  // Campo para marcar como favorita

    // Constructor con autor, bpm, duraciones de acordes y estado de favorito
    public Song(String title, String author, int difficulty, List<String> lyrics, List<String> chords, List<Integer> chordDurations, int bpm, boolean isFavorite) {
        this.title = title;
        this.author = author;  // Inicializa el autor
        this.difficulty = difficulty;
        this.lyrics = lyrics;
        this.chords = chords;
        this.chordDurations = chordDurations;  // Inicializa las duraciones de los acordes
        this.bpm = bpm;  // Inicializa el BPM
        this.isFavorite = isFavorite;  // Inicializa el estado de favorito
    }

    // Getter methods
    public String getTitle() {
        return title;
    }

    public String getAuthor() {
        return author;
    }

    public int getDifficulty() {
        return difficulty;
    }

    public List<String> getLyrics() {
        return lyrics;
    }

    public List<String> getChords() {
        return chords;
    }

    public List<Integer> getChordDurations() {
        return chordDurations;
    }

    public int getBpm() {
        return bpm;
    }

    public boolean isFavorite() {
        return isFavorite;
    }

    // Setter methods
    public void setTitle(String title) {
        this.title = title;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public void setDifficulty(int difficulty) {
        this.difficulty = difficulty;
    }

    public void setLyrics(List<String> lyrics) {
        this.lyrics = lyrics;
    }

    public void setChords(List<String> chords) {
        this.chords = chords;
    }

    public void setChordDurations(List<Integer> chordDurations) {
        this.chordDurations = chordDurations;
    }

    public void setBpm(int bpm) {
        this.bpm = bpm;
    }

    public void setFavorite(boolean isFavorite) {
        this.isFavorite = isFavorite;
    }

    // Método para mostrar información detallada de la canción
    public String getSongInfo() {
        return "Title: " + title + "\n" +
                "Author: " + author + "\n" +
                "Difficulty: " + difficulty + "\n" +
                "BPM: " + bpm + "\n" +
                "Is Favorite: " + (isFavorite ? "Yes" : "No");
    }
}
