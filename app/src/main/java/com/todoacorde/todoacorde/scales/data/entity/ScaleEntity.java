package com.todoacorde.todoacorde.scales.data.entity;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Entidad Room que representa una escala musical.
 *
 * Detalles:
 * - Tabla: "Scale".
 * - Índice único por "name" para evitar duplicados.
 * - Índice por "tier" para optimizar consultas por nivel.
 */
@Entity(
        tableName = "Scale",
        indices = {
                @Index(value = {"name"}, unique = true),
                @Index(value = {"tier"})
        }
)
public class ScaleEntity {

    /**
     * Identificador único autogenerado de la escala.
     */
    @PrimaryKey(autoGenerate = true)
    public long id;

    /**
     * Nombre canónico de la escala (por ejemplo, "Ionian", "Minor Pentatonic").
     * Debe ser único en la tabla.
     */
    public String name;

    /**
     * Nivel o tier de la escala.
     * Convención sugerida: 0 = fácil, 1 = medio, 2 = difícil.
     */
    public int tier;

    /**
     * Devuelve el nombre de la escala para representación legible.
     */
    @Override
    public String toString() {
        return name;
    }
}
