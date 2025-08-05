package com.tuguitar.todoacorde;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface SongUserSpeedDao {

    @Query("SELECT * FROM SongUserSpeed WHERE songId = :songId AND userId = :userId LIMIT 1")
    LiveData<SongUserSpeed> getSongUserSpeed(int songId, int userId);

    @Query("SELECT * FROM SongUserSpeed WHERE songId = :songId AND userId = :userId LIMIT 1")
    SongUserSpeed getSongUserSpeedSync(int songId, int userId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrUpdate(SongUserSpeed sus);

    @Query("SELECT COUNT(*) FROM SongUserSpeed WHERE userId = :userId AND isUnlocked0_75x = 1")
    int countUnlockedModerato(int userId);

    @Query("SELECT COUNT(*) FROM SongUserSpeed WHERE userId = :userId AND isUnlocked1x = 1")
    int countUnlockedNormal(int userId);


    /** Obtiene la configuración de velocidad para una canción y usuario específicos. */
    @Query("SELECT * FROM SongUserSpeed WHERE songId = :songId AND userId = :userId LIMIT 1")
    LiveData<SongUserSpeed> getSongSpeedForUser(int songId, int userId);

    /** Obtiene todas las configuraciones de velocidad de un usuario. */
    @Query("SELECT * FROM SongUserSpeed WHERE userId = :userId")
    LiveData<List<SongUserSpeed>> getAllSpeedsForUser(int userId);

    /** Marca o desmarca el desbloqueo de 0.5x para una canción y usuario. */
    @Query("UPDATE SongUserSpeed SET isUnlocked0_5x = :isUnlocked WHERE songId = :songId AND userId = :userId")
    void updateUnlockedSpeed0_5x(int songId, int userId, boolean isUnlocked);

    /** Marca o desmarca el desbloqueo de 0.75x para una canción y usuario. */
    @Query("UPDATE SongUserSpeed SET isUnlocked0_75x = :isUnlocked WHERE songId = :songId AND userId = :userId")
    void updateUnlockedSpeed0_75x(int songId, int userId, boolean isUnlocked);

    /** Marca o desmarca el desbloqueo de 1x para una canción y usuario. */
    @Query("UPDATE SongUserSpeed SET isUnlocked1x = :isUnlocked WHERE songId = :songId AND userId = :userId")
    void updateUnlockedSpeed1x(int songId, int userId, boolean isUnlocked);

    /** Elimina la configuración de velocidad para una canción y usuario. */
    @Query("DELETE FROM SongUserSpeed WHERE userId = :userId AND songId = :songId")
    void deleteSpeedForSongAndUser(int songId, int userId);

    /** Elimina un registro completo de velocidad. */
    @Delete
    void deleteSpeed(SongUserSpeed songUserSpeed);
}
