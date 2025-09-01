package com.tuguitar.todoacorde;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "User")
public class User {
    @PrimaryKey(autoGenerate = true)
    public int id;
    @ColumnInfo(name = "username", collate = ColumnInfo.NOCASE)
    @NonNull
    public String username; // Nombre del usuario, debe ser único
    @ColumnInfo(name = "created_at")
    @NonNull
    public long createdAt; // Fecha y hora de creación

    @ColumnInfo(name = "last_active")
    public long lastActive; // Última vez que estuvo activo
}
