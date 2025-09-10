package com.todoacorde.todoacorde.songs.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Entidad Room que representa una canción del catálogo.
 *
 * Tabla: songs
 * - id (PK autogenerada)
 * - title (título de la canción)
 * - author (autor o banda)
 * - difficulty (nivel de dificultad numérico)
 * - measure (compás, por ejemplo "4/4")
 * - bpm (tempo en beats por minuto)
 * - isFavorite (bandera de favorito para vistas/uso local)
 *
 * Nota: El “favorito” persistente por usuario se modela en la tabla
 *       de relación {@code favorite_songs}. Este campo puede usarse
 *       como estado de UI o para listas precargadas si lo necesitas.
 */
@Entity(tableName = "songs")
public class Song {

    /**
     * Identificador único autogenerado de la canción.
     */
    @PrimaryKey(autoGenerate = true)
    private int id;

    /**
     * Título de la canción.
     */
    private String title;

    /**
     * Autor o grupo.
     */
    private String author;

    /**
     * Dificultad (escala numérica definida por la app).
     */
    private int difficulty;

    /**
     * Compás (por ejemplo "4/4", "3/4").
     */
    private String measure;

    /**
     * Tempo en beats por minuto.
     */
    private int bpm;

    /**
     * Bandera de favorito (uso de UI/local).
     * Para favoritos por usuario usa {@code favorite_songs}.
     */
    private boolean isFavorite;

    /**
     * Constructor principal para Room/inserciones manuales.
     *
     * @param title      título.
     * @param author     autor/grupo.
     * @param difficulty dificultad numérica.
     * @param bpm        tempo en BPM.
     * @param measure    compás (ej. "4/4").
     * @param isFavorite favorito local (no por usuario).
     */
    public Song(String title, String author, int difficulty,
                int bpm, String measure, boolean isFavorite) {
        this.title = title;
        this.author = author;
        this.difficulty = difficulty;
        this.bpm = bpm;
        this.measure = measure;
        this.isFavorite = isFavorite;
    }

    /**
     * Constructor de copia.
     *
     * @param other instancia a copiar.
     */
    public Song(Song other) {
        this.id = other.id;
        this.title = other.title;
        this.author = other.author;
        this.difficulty = other.difficulty;
        this.bpm = other.bpm;
        this.measure = other.measure;
        this.isFavorite = other.isFavorite;
    }

    /** @return id autogenerado. */
    public int getId() {
        return id;
    }

    /** @return título. */
    public String getTitle() {
        return title;
    }

    /** @return autor/grupo. */
    public String getAuthor() {
        return author;
    }

    /** @return dificultad. */
    public int getDifficulty() {
        return difficulty;
    }

    /** @return compás. */
    public String getMeasure() {
        return measure;
    }

    /** @return tempo en BPM. */
    public int getBpm() {
        return bpm;
    }

    /** @return true si está marcado como favorito local. */
    public boolean isFavorite() {
        return isFavorite;
    }

    /** @param id valor del identificador. */
    public void setId(int id) {
        this.id = id;
    }

    /** @param title nuevo título. */
    public void setTitle(String title) {
        this.title = title;
    }

    /** @param author nuevo autor/grupo. */
    public void setAuthor(String author) {
        this.author = author;
    }

    /** @param difficulty nueva dificultad. */
    public void setDifficulty(int difficulty) {
        this.difficulty = difficulty;
    }

    /** @param measure nuevo compás. */
    public void setMeasure(String measure) {
        this.measure = measure;
    }

    /** @param bpm nuevo tempo en BPM. */
    public void setBpm(int bpm) {
        this.bpm = bpm;
    }

    /** @param fav marca o desmarca como favorito local. */
    public void setFavorite(boolean fav) {
        this.isFavorite = fav;
    }
}
