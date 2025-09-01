package com.tuguitar.todoacorde.scales.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Transaction;

import java.util.List;

@Dao
public interface ScalePatternDao {

    @Insert
    long insertPattern(ScalePatternEntity pattern);

    @Insert
    void insertNotes(List<ScaleNoteEntity> notes);

    @Transaction
    default void insertPatternWithNotes(ScalePatternEntity pattern, List<ScaleNoteEntity> notes) {
        long id = insertPattern(pattern);
        for (ScaleNoteEntity n : notes) n.patternId = id;
        insertNotes(notes);
    }

    @Query("DELETE FROM scale_patterns")
    void clearAllPatterns();

    @Query("DELETE FROM scale_notes")
    void clearAllNotes();

    default void deleteAllPatterns() { clearAllPatterns(); }
    default void deleteAllNotes()    { clearAllNotes(); }

    @Transaction
    @Query("SELECT * FROM scale_patterns ORDER BY scaleType, rootNote, positionIndex ASC, startFret ASC, id ASC")
    List<PatternWithNotes> getAllPatternsWithNotes();
    @Transaction
    @Query("SELECT * FROM scale_patterns " +
            "WHERE scaleType = :scaleType COLLATE NOCASE " +
            "AND   rootNote  = :rootNote  COLLATE NOCASE " +
            "ORDER BY positionIndex ASC, startFret ASC, id ASC")
    List<PatternWithNotes> findByScaleTypeAndRoot(String scaleType, String rootNote);
    @Query("SELECT DISTINCT TRIM(scaleType) " +
            "FROM scale_patterns " +
            "WHERE scaleType IS NOT NULL AND TRIM(scaleType) <> '' " +
            "ORDER BY TRIM(scaleType) COLLATE NOCASE ASC")
    List<String> getAllScaleTypesDistinct();

    @Query("SELECT DISTINCT UPPER(TRIM(rootNote)) " +
            "FROM scale_patterns " +
            "WHERE scaleType = :scaleType COLLATE NOCASE " +
            "AND   rootNote IS NOT NULL AND TRIM(rootNote) <> '' " +
            "ORDER BY UPPER(TRIM(rootNote)) COLLATE NOCASE ASC")
    List<String> getRootsForType(String scaleType);

    @Query("SELECT COUNT(*) FROM scale_patterns")
    int countPatterns();
}
