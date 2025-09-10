package com.todoacorde.todoacorde.practice.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

/**
 * DAO para el estado de velocidades desbloqueadas por usuario y canción.
 *
 * Expone operaciones de lectura reactiva/síncrona, conteos agregados,
 * actualización de flags y eliminación.
 */
@Dao
public interface SongUserSpeedDao {

    /**
     * Obtiene de forma reactiva el registro de velocidad para una pareja (songId, userId).
     *
     * @param songId id de la canción.
     * @param userId id del usuario.
     * @return LiveData con el registro si existe; null en caso contrario.
     */
    @Query("SELECT * FROM SongUserSpeed WHERE songId = :songId AND userId = :userId LIMIT 1")
    LiveData<SongUserSpeed> getSongUserSpeed(int songId, int userId);

    /**
     * Obtiene de forma síncrona el registro de velocidad para una pareja (songId, userId).
     *
     * @param songId id de la canción.
     * @param userId id del usuario.
     * @return entidad si existe; null en caso contrario.
     */
    @Query("SELECT * FROM SongUserSpeed WHERE songId = :songId AND userId = :userId LIMIT 1")
    SongUserSpeed getSongUserSpeedSync(int songId, int userId);

    /**
     * Inserta o actualiza un registro de velocidad.
     *
     * @param sus entidad a persistir.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrUpdate(SongUserSpeed sus);

    /**
     * Cuenta cuántas canciones tienen desbloqueada la velocidad 0.75x para un usuario.
     *
     * @param userId id del usuario.
     * @return número de canciones con 0.75x desbloqueada.
     */
    @Query("SELECT COUNT(*) FROM SongUserSpeed WHERE userId = :userId AND isUnlocked0_75x = 1")
    int countUnlockedModerato(int userId);

    /**
     * Cuenta cuántas canciones tienen desbloqueada la velocidad 1.0x para un usuario.
     *
     * @param userId id del usuario.
     * @return número de canciones con 1.0x desbloqueada.
     */
    @Query("SELECT COUNT(*) FROM SongUserSpeed WHERE userId = :userId AND isUnlocked1x = 1")
    int countUnlockedNormal(int userId);

    /**
     * Variante reactiva para obtener el registro de velocidad por canción y usuario.
     *
     * @param songId id de la canción.
     * @param userId id del usuario.
     * @return LiveData de la entidad.
     */
    @Query("SELECT * FROM SongUserSpeed WHERE songId = :songId AND userId = :userId LIMIT 1")
    LiveData<SongUserSpeed> getSongSpeedForUser(int songId, int userId);

    /**
     * Devuelve todas las entradas de velocidad para un usuario.
     *
     * @param userId id del usuario.
     * @return LiveData con la lista de registros.
     */
    @Query("SELECT * FROM SongUserSpeed WHERE userId = :userId")
    LiveData<List<SongUserSpeed>> getAllSpeedsForUser(int userId);

    /**
     * Actualiza el flag de desbloqueo para 0.5x.
     *
     * @param songId     id de la canción.
     * @param userId     id del usuario.
     * @param isUnlocked nuevo estado.
     */
    @Query("UPDATE SongUserSpeed SET isUnlocked0_5x = :isUnlocked WHERE songId = :songId AND userId = :userId")
    void updateUnlockedSpeed0_5x(int songId, int userId, boolean isUnlocked);

    /**
     * Actualiza el flag de desbloqueo para 0.75x.
     *
     * @param songId     id de la canción.
     * @param userId     id del usuario.
     * @param isUnlocked nuevo estado.
     */
    @Query("UPDATE SongUserSpeed SET isUnlocked0_75x = :isUnlocked WHERE songId = :songId AND userId = :userId")
    void updateUnlockedSpeed0_75x(int songId, int userId, boolean isUnlocked);

    /**
     * Actualiza el flag de desbloqueo para 1.0x.
     *
     * @param songId     id de la canción.
     * @param userId     id del usuario.
     * @param isUnlocked nuevo estado.
     */
    @Query("UPDATE SongUserSpeed SET isUnlocked1x = :isUnlocked WHERE songId = :songId AND userId = :userId")
    void updateUnlockedSpeed1x(int songId, int userId, boolean isUnlocked);

    /**
     * Elimina el registro de velocidad para una pareja (songId, userId).
     *
     * @param songId id de la canción.
     * @param userId id del usuario.
     */
    @Query("DELETE FROM SongUserSpeed WHERE userId = :userId AND songId = :songId")
    void deleteSpeedForSongAndUser(int songId, int userId);

    /**
     * Elimina una entidad concreta.
     *
     * @param songUserSpeed entidad a eliminar.
     */
    @Delete
    void deleteSpeed(SongUserSpeed songUserSpeed);
}
