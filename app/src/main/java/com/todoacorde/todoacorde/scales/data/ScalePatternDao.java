package com.todoacorde.todoacorde.scales.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Transaction;

import java.util.List;

/**
 * DAO para patrones de escala y sus notas asociadas.
 *
 * Incluye operaciones de inserción y consultas compuestas (patrón + notas).
 * Evita HTML/markup no soportado; comentarios en formato estándar Java/Android.
 */
@Dao
public interface ScalePatternDao {

    /**
     * Inserta un patrón de escala.
     *
     * @param pattern entidad del patrón
     * @return id autogenerado del patrón
     */
    @Insert
    long insertPattern(ScalePatternEntity pattern);

    /**
     * Inserta una lista de notas asociadas a patrones.
     *
     * @param notes lista de notas
     */
    @Insert
    void insertNotes(List<ScaleNoteEntity> notes);

    /**
     * Inserta un patrón junto con sus notas en una única transacción.
     * Asigna el {@code patternId} generado a cada nota antes de insertarla.
     *
     * @param pattern patrón a insertar
     * @param notes   notas pertenecientes al patrón
     */
    @Transaction
    default void insertPatternWithNotes(ScalePatternEntity pattern, List<ScaleNoteEntity> notes) {
        long id = insertPattern(pattern);
        for (ScaleNoteEntity n : notes) {
            n.patternId = id;
        }
        insertNotes(notes);
    }

    /**
     * Elimina todos los patrones.
     */
    @Query("DELETE FROM scale_patterns")
    void clearAllPatterns();

    /**
     * Elimina todas las notas.
     */
    @Query("DELETE FROM scale_notes")
    void clearAllNotes();

    /**
     * Alias legible para {@link #clearAllPatterns()}.
     */
    default void deleteAllPatterns() {
        clearAllPatterns();
    }

    /**
     * Alias legible para {@link #clearAllNotes()}.
     */
    default void deleteAllNotes() {
        clearAllNotes();
    }

    /**
     * Obtiene todos los patrones con sus notas asociadas, ordenados por tipo de escala,
     * nota raíz, índice de posición, traste inicial e id.
     *
     * @return lista de patrones con notas
     */
    @Transaction
    @Query("SELECT * FROM scale_patterns " +
            "ORDER BY scaleType, rootNote, positionIndex ASC, startFret ASC, id ASC")
    List<PatternWithNotes> getAllPatternsWithNotes();

    /**
     * Busca patrones por tipo de escala y nota raíz (sin sensibilidad a mayúsculas),
     * devolviendo también sus notas.
     *
     * @param scaleType tipo de escala normalizado
     * @param rootNote  nota raíz normalizada
     * @return lista de patrones con notas
     */
    @Transaction
    @Query("SELECT * FROM scale_patterns " +
            "WHERE scaleType = :scaleType COLLATE NOCASE " +
            "AND   rootNote  = :rootNote  COLLATE NOCASE " +
            "ORDER BY positionIndex ASC, startFret ASC, id ASC")
    List<PatternWithNotes> findByScaleTypeAndRoot(String scaleType, String rootNote);

    /**
     * Devuelve todos los tipos de escala distintos no vacíos, ordenados alfabéticamente.
     *
     * @return lista de tipos de escala
     */
    @Query("SELECT DISTINCT TRIM(scaleType) " +
            "FROM scale_patterns " +
            "WHERE scaleType IS NOT NULL AND TRIM(scaleType) <> '' " +
            "ORDER BY TRIM(scaleType) COLLATE NOCASE ASC")
    List<String> getAllScaleTypesDistinct();

    /**
     * Devuelve las notas raíz disponibles para un tipo de escala dado,
     * ordenadas alfabéticamente.
     *
     * @param scaleType tipo de escala (case-insensitive)
     * @return lista de raíces disponibles
     */
    @Query("SELECT DISTINCT UPPER(TRIM(rootNote)) " +
            "FROM scale_patterns " +
            "WHERE scaleType = :scaleType COLLATE NOCASE " +
            "AND   rootNote IS NOT NULL AND TRIM(rootNote) <> '' " +
            "ORDER BY UPPER(TRIM(rootNote)) COLLATE NOCASE ASC")
    List<String> getRootsForType(String scaleType);

    /**
     * Cuenta cuántos patrones existen en la tabla.
     *
     * @return número de patrones
     */
    @Query("SELECT COUNT(*) FROM scale_patterns")
    int countPatterns();
}
