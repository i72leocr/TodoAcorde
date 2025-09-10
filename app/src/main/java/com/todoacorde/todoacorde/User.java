package com.todoacorde.todoacorde;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Entidad Room que representa a un usuario de la aplicación.
 *
 * Tabla: {@code User}
 * - {@code id}: clave primaria autogenerada.
 * - {@code username}: nombre de usuario, con colación {@link ColumnInfo#NOCASE}
 *   para búsquedas insensibles a mayúsculas/minúsculas.
 * - {@code createdAt}: instante de creación en milisegundos desde época Unix.
 * - {@code lastActive}: último instante de actividad en milisegundos.
 */
@Entity(tableName = "User")
public class User {

    /** Identificador interno autogenerado. */
    @PrimaryKey(autoGenerate = true)
    public int id;

    /** Nombre de usuario (colación NOCASE para comparaciones insensibles a mayúsculas). */
    @ColumnInfo(name = "username", collate = ColumnInfo.NOCASE)
    @NonNull
    public String username;

    /** Marca temporal de creación (epoch millis). */
    @ColumnInfo(name = "created_at")
    @NonNull
    public long createdAt;

    /** Marca temporal de última actividad (epoch millis). */
    @ColumnInfo(name = "last_active")
    public long lastActive;
}
