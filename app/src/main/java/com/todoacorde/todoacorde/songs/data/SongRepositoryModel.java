package com.todoacorde.todoacorde.songs.data;

import java.util.List;

/**
 * Modelo de datos para exponer una canción desde el repositorio hacia la capa de UI.
 *
 * Campos incluidos:
 * - id: identificador único de la canción.
 * - title: título de la canción.
 * - author: autor o intérprete.
 * - difficulty: nivel de dificultad (referencia a catálogo).
 * - lyrics: líneas de la letra ordenadas.
 * - chords: nombres de acordes alineados con la estructura de la canción.
 * - chordDurations: duración de cada acorde en compases o unidades definidas.
 * - bpm: tempo en golpes por minuto.
 * - measure: métrica o compás (por ejemplo, "4/4").
 * - isFavorite: indicador de favorito para el usuario actual.
 */
public class SongRepositoryModel {

    /** Identificador único de la canción. */
    public int id;

    /** Título de la canción. */
    public String title;

    /** Autor o intérprete. */
    public String author;

    /** Nivel de dificultad (id del catálogo de dificultades). */
    public int difficulty;

    /** Letra de la canción, cada elemento corresponde a una línea. */
    public List<String> lyrics;

    /** Secuencia de acordes en la canción. */
    public List<String> chords;

    /** Duración de cada acorde, alineada con la lista de acordes. */
    public List<Integer> chordDurations;

    /** Tempo en BPM. */
    public int bpm;

    /** Compás o métrica, por ejemplo "4/4". */
    public String measure;

    /** Indicador de favorito para el usuario actual. */
    public boolean isFavorite;
}
