package com.todoacorde.todoacorde.songs.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface FavoriteSongDao {

    /**
     * Inserta una relación de favorito (userId, songId).
     * Si ya existe el par, la operación se ignora para evitar error de conflicto.
     *
     * @param favoriteSong entidad con userId y songId.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insert(FavoriteSong favoriteSong);

    /**
     * Elimina la relación de favorito indicada por la entidad (userId, songId).
     *
     * @param favoriteSong entidad con userId y songId a eliminar.
     */
    @Delete
    void delete(FavoriteSong favoriteSong);

    /**
     * Comprueba si una canción es favorita para un usuario.
     *
     * @param userId id del usuario.
     * @param songId id de la canción.
     * @return LiveData que emite true si existe el favorito, false en caso contrario.
     */
    @Query("SELECT EXISTS(SELECT 1 FROM favorite_songs WHERE userId = :userId AND songId = :songId)")
    LiveData<Boolean> isFavorite(int userId, int songId);

    /**
     * Obtiene los IDs de canciones favoritas de un usuario.
     *
     * @param userId id del usuario.
     * @return LiveData que emite una lista de songId marcados como favoritos.
     */
    @Query("SELECT songId FROM favorite_songs WHERE userId = :userId")
    LiveData<List<Integer>> getFavoriteSongIds(int userId);
}
