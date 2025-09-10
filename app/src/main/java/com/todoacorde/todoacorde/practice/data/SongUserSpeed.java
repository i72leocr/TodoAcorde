package com.todoacorde.todoacorde.practice.data;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import com.todoacorde.todoacorde.User;
import com.todoacorde.todoacorde.songs.data.Song;

/**
 * Entidad Room que almacena el estado de velocidades desbloqueadas
 * por usuario para una canción concreta.
 *
 * La clave lógica es (songId, userId), reforzada mediante un índice único.
 * Por defecto se asume 0.5x desbloqueada; el resto se va habilitando
 * conforme el usuario progresa.
 */
@Entity(
        tableName = "SongUserSpeed",
        foreignKeys = {
                @ForeignKey(
                        entity = Song.class,
                        parentColumns = "id",
                        childColumns = "songId",
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
                @Index(value = {"songId"}),
                @Index(value = {"userId"}),
                @Index(value = {"songId", "userId"}, unique = true)
        }
)
public class SongUserSpeed {

    /** Identificador interno autogenerado. */
    @PrimaryKey(autoGenerate = true)
    public int id;

    /** Identificador de la canción. */
    public int songId;

    /** Identificador del usuario. */
    public int userId;

    /** Indica si la velocidad 0.5x está desbloqueada. */
    public boolean isUnlocked0_5x;

    /** Indica si la velocidad 0.75x está desbloqueada. */
    public boolean isUnlocked0_75x;

    /** Indica si la velocidad 1.0x está desbloqueada. */
    public boolean isUnlocked1x;

    /**
     * Crea un registro de desbloqueo de velocidades para una pareja canción/usuario.
     *
     * @param songId          id de la canción
     * @param userId          id del usuario
     * @param isUnlocked0_5x  true si 0.5x está desbloqueada
     * @param isUnlocked0_75x true si 0.75x está desbloqueada
     * @param isUnlocked1x    true si 1.0x está desbloqueada
     */
    public SongUserSpeed(int songId, int userId, boolean isUnlocked0_5x, boolean isUnlocked0_75x, boolean isUnlocked1x) {
        this.songId = songId;
        this.userId = userId;
        this.isUnlocked0_5x = isUnlocked0_5x;
        this.isUnlocked0_75x = isUnlocked0_75x;
        this.isUnlocked1x = isUnlocked1x;
    }

    /**
     * Devuelve la mayor velocidad desbloqueada actualmente.
     *
     * @return 1.0f si está desbloqueada; en su defecto 0.75f si procede; si no, 0.5f.
     */
    public float getMaxUnlockedSpeed() {
        if (isUnlocked1x) return 1.0f;
        if (isUnlocked0_75x) return 0.75f;
        return 0.5f;
    }
}
