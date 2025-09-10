package com.todoacorde.todoacorde.scales.data.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Entidad Room que representa una “caja” (box) de digitación asociada a una escala.
 *
 * Reglas y restricciones:
 * - Clave primaria autogenerada en {@link #id}.
 * - Clave foránea {@link #scaleId} → ScaleEntity.id con borrado en cascada.
 * - Índice único compuesto por (scaleId, boxOrder) para evitar duplicados
 *   de orden dentro de la misma escala e imponer un orden estable de las cajas.
 * - Índice adicional por scaleId para acelerar búsquedas por escala.
 */
@Entity(
        tableName = "ScaleBox",
        foreignKeys = @ForeignKey(
                entity = ScaleEntity.class,
                parentColumns = "id",
                childColumns = "scaleId",
                onDelete = ForeignKey.CASCADE
        ),
        indices = {
                @Index(value = {"scaleId", "boxOrder"}, unique = true),
                @Index(value = {"scaleId"})
        }
)
public class ScaleBoxEntity {

    /**
     * Identificador único autogenerado de la caja.
     */
    @PrimaryKey(autoGenerate = true)
    public long id;

    /**
     * Identificador de la escala a la que pertenece esta caja.
     * Referencia a ScaleEntity.id.
     */
    public long scaleId;

    /**
     * Orden de la caja dentro de la escala.
     * Debe ser único para cada valor de {@link #scaleId}.
     * Convención del proyecto: valores consecutivos empezando en 0 o 1 según la inserción.
     */
    public int boxOrder;
}
