package com.todoacorde.todoacorde.scales.data.entity;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Entidad Room que representa una tonalidad musical (por ejemplo: C, D#, F, etc.).
 *
 * Restricciones e índices:
 * - Clave primaria autogenerada en {@link #id}.
 * - Índice único por {@code name} para evitar duplicados y acelerar búsquedas.
 */
@Entity(
        tableName = "Tonality",
        indices = @Index(value = {"name"}, unique = true)
)
public class TonalityEntity {

    /**
     * Identificador único autogenerado de la tonalidad.
     */
    @PrimaryKey(autoGenerate = true)
    public long id;

    /**
     * Nombre legible de la tonalidad (ej.: "C", "C#", "D", ...).
     */
    public String name;

    /**
     * Representación textual: devuelve el nombre de la tonalidad.
     */
    @Override
    public String toString() {
        return name;
    }
}
