package com.todoacorde.todoacorde.scales.data.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.todoacorde.todoacorde.scales.data.entity.UserScaleCompletionEntity;

/**
 * DAO para acceder y modificar los registros de completado de cajas (boxes) de escalas por usuario.
 *
 * Gestiona inserciones idempotentes y diversas consultas agregadas para verificar progreso por
 * tonalidad y a nivel global de una escala.
 */
@Dao
public interface UserScaleCompletionDao {

    /**
     * Inserta un registro de completado de una caja (box) de una escala para un usuario/tonalidad.
     *
     * Estrategia de conflicto: {@link OnConflictStrategy#IGNORE}. Si el registro ya existe,
     * la operación se ignora y devuelve -1.
     *
     * @param completion entidad {@link UserScaleCompletionEntity} a insertar.
     * @return id autogenerado de la fila insertada o -1 si la inserción se ignoró por conflicto.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insert(UserScaleCompletionEntity completion);

    /**
     * Cuenta cuántas cajas de una escala han sido completadas por un usuario en una tonalidad dada.
     *
     * @param userId     identificador del usuario.
     * @param scaleId    identificador de la escala.
     * @param tonalityId identificador de la tonalidad.
     * @return número de cajas completadas para esa combinación usuario/escala/tonalidad.
     */
    @Query("SELECT COUNT(*) FROM UserScaleCompletion WHERE userId = :userId AND scaleId = :scaleId AND tonalityId = :tonalityId")
    int countBoxesCompletedForTonality(long userId, long scaleId, long tonalityId);

    /**
     * Obtiene el índice máximo de caja (boxOrder) completada por un usuario para una escala en una tonalidad.
     *
     * @param userId     identificador del usuario.
     * @param scaleId    identificador de la escala.
     * @param tonalityId identificador de la tonalidad.
     * @return mayor valor de {@code boxOrder} registrado o {@code null} si no hay registros.
     */
    @Query("SELECT MAX(boxOrder) FROM UserScaleCompletion WHERE userId = :userId AND scaleId = :scaleId AND tonalityId = :tonalityId")
    Integer getMaxCompletedBox(long userId, long scaleId, long tonalityId);

    /**
     * Cuenta el total de cajas completadas por un usuario para una escala, sumando todas las tonalidades.
     *
     * @param userId  identificador del usuario.
     * @param scaleId identificador de la escala.
     * @return total de filas de completado asociadas a la escala para ese usuario (todas las tonalidades).
     */
    @Query("SELECT COUNT(*) FROM UserScaleCompletion WHERE userId = :userId AND scaleId = :scaleId")
    int countBoxesCompletedForScaleAllTonalities(long userId, long scaleId);

    /**
     * Verifica si una caja concreta (boxOrder) está marcada como completada por un usuario
     * para una escala y tonalidad específicas.
     *
     * @param userId     identificador del usuario.
     * @param scaleId    identificador de la escala.
     * @param tonalityId identificador de la tonalidad.
     * @param boxOrder   orden/índice de la caja dentro de la escala.
     * @return {@code true} si existe el registro, {@code false} en caso contrario.
     */
    @Query("SELECT EXISTS(SELECT 1 FROM UserScaleCompletion WHERE userId = :userId AND scaleId = :scaleId AND tonalityId = :tonalityId AND boxOrder = :boxOrder)")
    boolean isBoxCompleted(long userId, long scaleId, long tonalityId, int boxOrder);
}
