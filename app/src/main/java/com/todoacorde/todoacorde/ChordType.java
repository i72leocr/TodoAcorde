package com.todoacorde.todoacorde;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Entidad Room que representa un tipo/clase de acorde.
 * Se almacena en la tabla {@code chord_types}.
 *
 * Cada registro define una categoría de acorde (por ejemplo: mayor, menor, séptima),
 * con un nombre de tipo y una descripción opcional.
 */
@Entity(tableName = "chord_types")
public class ChordType {

    /**
     * Identificador autogenerado del tipo de acorde.
     */
    @PrimaryKey(autoGenerate = true)
    public int id;

    /**
     * Nombre del tipo de acorde.
     * No puede ser nulo y se persiste en la columna {@code type_name}.
     */
    @NonNull
    @ColumnInfo(name = "type_name")
    public String typeName;

    /**
     * Descripción opcional del tipo de acorde.
     */
    @ColumnInfo(name = "description")
    public String description;

    /**
     * Crea un nuevo tipo de acorde.
     *
     * @param typeName    nombre del tipo (no nulo).
     * @param description descripción opcional.
     */
    public ChordType(@NonNull String typeName, String description) {
        this.typeName = typeName;
        this.description = description;
    }

    /**
     * Devuelve el nombre del tipo de acorde.
     *
     * @return nombre del tipo (no nulo).
     */
    @NonNull
    public String getTypeName() {
        return typeName;
    }

    /**
     * Establece el nombre del tipo de acorde.
     *
     * @param typeName nombre del tipo (no nulo).
     */
    public void setTypeName(@NonNull String typeName) {
        this.typeName = typeName;
    }

    /**
     * Devuelve la descripción del tipo de acorde.
     *
     * @return descripción o {@code null} si no está definida.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Establece la descripción del tipo de acorde.
     *
     * @param description texto descriptivo; puede ser {@code null}.
     */
    public void setDescription(String description) {
        this.description = description;
    }
}
