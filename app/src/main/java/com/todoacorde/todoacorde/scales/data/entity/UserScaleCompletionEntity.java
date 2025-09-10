package com.todoacorde.todoacorde.scales.data.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import com.todoacorde.todoacorde.User;

/**
 * Entidad Room que registra la finalización de una caja (box) de una escala por un usuario
 * en una tonalidad específica.
 *
 * Reglas y restricciones:
 * - Clave primaria autogenerada en {@link #id}.
 * - Claves foráneas:
 *   - {@link #scaleId} → {@code ScaleEntity.id} (borrado en cascada).
 *   - {@link #tonalityId} → {@code TonalityEntity.id} (borrado en cascada).
 *   - {@link #userId} → {@code User.id} (borrado en cascada).
 * - Índice único compuesto por {@code (userId, scaleId, tonalityId, boxOrder)} para evitar
 *   duplicados de finalización de la misma caja por el mismo usuario en la misma tonalidad.
 * - Índices adicionales por {@code scaleId}, {@code tonalityId} y {@code userId} para acelerar
 *   consultas filtradas.
 */
@Entity(
        tableName = "UserScaleCompletion",
        foreignKeys = {
                @ForeignKey(
                        entity = ScaleEntity.class,
                        parentColumns = "id",
                        childColumns = "scaleId",
                        onDelete = ForeignKey.CASCADE
                ),
                @ForeignKey(
                        entity = TonalityEntity.class,
                        parentColumns = "id",
                        childColumns = "tonalityId",
                        onDelete = ForeignKey.CASCADE
                ),
                @ForeignKey(
                        entity = User.class,
                        parentColumns = "id",
                        childColumns = "userId",
                        onDelete = ForeignKey.CASCADE
                )
        },
        indices = {
                @Index(value = {"userId", "scaleId", "tonalityId", "boxOrder"}, unique = true),
                @Index(value = {"scaleId"}),
                @Index(value = {"tonalityId"}),
                @Index(value = {"userId"})
        }
)
public class UserScaleCompletionEntity {

    /**
     * Identificador único autogenerado del registro de finalización.
     */
    @PrimaryKey(autoGenerate = true)
    public long id;

    /**
     * Identificador del usuario que completó la caja.
     * Referencia a {@code User.id}.
     */
    public long userId;

    /**
     * Identificador de la escala a la que pertenece la caja completada.
     * Referencia a {@code ScaleEntity.id}.
     */
    public long scaleId;

    /**
     * Identificador de la tonalidad en la que se completó la caja.
     * Referencia a {@code TonalityEntity.id}.
     */
    public long tonalityId;

    /**
     * Orden de la caja (box) dentro de la escala.
     * Debe ser único por combinación de usuario, escala y tonalidad.
     */
    public int boxOrder;

    /**
     * Marca temporal (UTC, en milisegundos desde epoch) indicando cuándo se registró la finalización.
     */
    public long completedAtUtc;
}
