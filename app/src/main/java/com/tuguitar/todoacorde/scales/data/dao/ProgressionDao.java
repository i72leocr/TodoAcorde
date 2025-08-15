package com.tuguitar.todoacorde.scales.data.dao;

import androidx.room.Dao;
import androidx.room.Query;

@Dao
public interface ProgressionDao {

    // ¿Está completa una escala para TODAS sus tonalidades y TODAS sus cajas?
    @Query(
            "SELECT NOT EXISTS (" +
                    "   SELECT 1 " +
                    "   FROM Tonality t " +
                    "   JOIN ScaleBox sb ON sb.scaleId = :scaleId " +
                    "   LEFT JOIN UserScaleCompletion usc " +
                    "     ON usc.userId = :userId AND usc.scaleId = :scaleId AND usc.tonalityId = t.id AND usc.boxOrder = sb.boxOrder " +
                    "   WHERE usc.id IS NULL" +
                    ")"
    )
    boolean isScaleFullyCompletedAllTonalities(long userId, long scaleId);

    // ¿Cumple el usuario todas las escalas de un tier?
    @Query(
            "SELECT NOT EXISTS (" +
                    "   SELECT 1 FROM Scale s " +
                    "   WHERE s.tier = :tier " +
                    "     AND NOT (" +
                    "         SELECT NOT EXISTS (" +
                    "           SELECT 1 " +
                    "           FROM Tonality t " +
                    "           JOIN ScaleBox sb ON sb.scaleId = s.id " +
                    "           LEFT JOIN UserScaleCompletion usc " +
                    "             ON usc.userId = :userId AND usc.scaleId = s.id AND usc.tonalityId = t.id AND usc.boxOrder = sb.boxOrder " +
                    "           WHERE usc.id IS NULL" +
                    "         )" +
                    "     )" +
                    ")"
    )
    boolean isTierCompleted(long userId, int tier);
}
