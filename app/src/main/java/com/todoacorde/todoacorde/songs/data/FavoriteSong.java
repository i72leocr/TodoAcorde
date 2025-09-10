package com.todoacorde.todoacorde.songs.data;

import static androidx.room.ForeignKey.CASCADE;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;

import com.todoacorde.todoacorde.User;
import com.todoacorde.todoacorde.songs.data.Song; // Import explícito de Song

/**
 * Entidad Room que modela la relación “favorita” entre un usuario y una canción.
 *
 * Características principales:
 * - Clave primaria compuesta por {@code userId} y {@code songId}.
 * - Dos claves foráneas con borrado en cascada:
 *   - {@code userId} → {@link User#id}
 *   - {@code songId} → {@link Song#id}
 * - Índice adicional por {@code songId} para acelerar consultas por canción.
 *
 * Semántica:
 * - Cada fila representa que un usuario ha marcado una canción como favorita.
 * - La clave primaria compuesta impide duplicados para la misma pareja usuario–canción.
 */
@Entity(
        tableName = "favorite_songs",
        primaryKeys = {"userId", "songId"},
        indices = {
                @Index(value = "songId")
        },
        foreignKeys = {
                @ForeignKey(
                        entity = User.class,
                        parentColumns = "id",
                        childColumns = "userId",
                        onDelete = CASCADE
                ),
                @ForeignKey(
                        entity = Song.class,
                        parentColumns = "id",
                        childColumns = "songId",
                        onDelete = CASCADE
                )
        }
)
public class FavoriteSong {

    /**
     * Identificador del usuario que marca como favorita la canción.
     * Clave foránea hacia {@link User#id}.
     */
    public final int userId;

    /**
     * Identificador de la canción marcada como favorita.
     * Clave foránea hacia {@link Song#id}.
     */
    public final int songId;

    /**
     * Crea una relación de canción favorita para un usuario.
     *
     * @param userId id del usuario.
     * @param songId id de la canción.
     */
    public FavoriteSong(int userId, int songId) {
        this.userId = userId;
        this.songId = songId;
    }
}
