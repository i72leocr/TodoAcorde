package com.tuguitar.todoacorde;

import androidx.lifecycle.LiveData;

import java.util.List;

/**
 * Repositorio para canciones, agrupa acceso a DAO de Song y a DAO de favoritos.
 */
public class SongRepository {
    private final SongDao songDao;
    private final FavoriteSongDao favoriteDao;

    public SongRepository(SongDao songDao, FavoriteSongDao favoriteDao) {
        this.songDao     = songDao;
        this.favoriteDao = favoriteDao;
    }

    /** Lista de todas las canciones (LiveData de Room). */
    public LiveData<List<Song>> getAllSongs() {
        return songDao.getAllSongs();
    }

    /**
     * Recupera IDs de favoritos para un usuario de forma síncrona.
     * Está permitido porque se llama desde un hilo de ViewModel (no UI).
     */

    public LiveData<List<Integer>> getFavoriteIdsLive(int userId) {
        return favoriteDao.getFavoriteSongIds(userId);
    }

    /**
     * Marca o desmarca como favorito de forma síncrona.
     * Se usa en ViewModel por simplicidad; en una versión más avanzada podríamos hacerlo en background.
     */
    // Repository
    public void toggleFavorite(int userId, int songId, boolean isFavorite) {
        todoAcordeDatabase.databaseWriteExecutor.execute(() -> {
            if (isFavorite) {
                favoriteDao.insert(new FavoriteSong(userId, songId));
            } else {
                favoriteDao.delete(new FavoriteSong(userId, songId));
            }
        });
    }

}
