package com.todoacorde.todoacorde.scales.data.dao;

import androidx.room.Dao;
import androidx.room.Query;

/**
 * DAO para consultas de progresión de escalas.
 *
 * Proporciona métodos booleanos que indican si una escala o un tier
 * han sido completados por un usuario según los registros de
 * UserScaleCompletion.
 */
@Dao
public interface ProgressionDao {

    /**
     * Comprueba si una escala está completamente finalizada en todas las
     * tonalidades para un usuario.
     *
     * Lógica: devuelve true si no existe ninguna combinación de
     * (tonalidad, caja) sin registro de finalización en UserScaleCompletion.
     *
     * @param userId  identificador del usuario.
     * @param scaleId identificador de la escala.
     * @return true si la escala está completa en todas las tonalidades; false en caso contrario.
     */
    @Query(
            "SELECT NOT EXISTS (" +
                    "   SELECT 1 " +
                    "   FROM Tonality t " +
                    "   JOIN ScaleBox sb ON sb.scaleId = :scaleId " +
                    "   LEFT JOIN UserScaleCompletion usc " +
                    "     ON usc.userId = :userId " +
                    "    AND usc.scaleId = :scaleId " +
                    "    AND usc.tonalityId = t.id " +
                    "    AND usc.boxOrder = sb.boxOrder " +
                    "   WHERE usc.id IS NULL" +
                    ")"
    )
    boolean isScaleFullyCompletedAllTonalities(long userId, long scaleId);

    /**
     * Comprueba si todas las escalas de un tier están completamente finalizadas
     * (todas sus cajas en todas las tonalidades) para un usuario.
     *
     * Lógica: para cada escala del tier, se verifica que no existan
     * combinaciones (tonalidad, caja) pendientes en UserScaleCompletion.
     *
     * @param userId identificador del usuario.
     * @param tier   nivel o grupo de dificultad de la escala.
     * @return true si el usuario ha completado el tier; false en caso contrario.
     */
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
                    "             ON usc.userId = :userId " +
                    "            AND usc.scaleId = s.id " +
                    "            AND usc.tonalityId = t.id " +
                    "            AND usc.boxOrder = sb.boxOrder " +
                    "           WHERE usc.id IS NULL" +
                    "         )" +
                    "     )" +
                    ")"
    )
    boolean isTierCompleted(long userId, int tier);
}
