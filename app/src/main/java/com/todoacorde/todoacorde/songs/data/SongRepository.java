package com.todoacorde.todoacorde.songs.data;

import androidx.lifecycle.LiveData;

import com.todoacorde.todoacorde.Difficulty;
import com.todoacorde.todoacorde.DifficultyDao;
import com.todoacorde.todoacorde.todoAcordeDatabase;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Repositorio de canciones.
 *
 * Responsabilidades:
 * - Exponer flujos reactivos (LiveData) para listar canciones y favoritos.
 * - Encapsular operaciones de marcado/desmarcado de favoritos en hilo de escritura.
 * - Proveer un mapa id→texto de dificultad desde la tabla de dificultades.
 *
 * Dependencias:
 * - {@link SongDao} CRUD de canciones.
 * - {@link FavoriteSongDao} relación usuario–canción favorita.
 * - {@link DifficultyDao} catálogo de dificultades.
 */
@Singleton
public class SongRepository {
    private final SongDao songDao;
    private final FavoriteSongDao favoriteDao;
    private final DifficultyDao difficultyDao;

    /**
     * Inyección de dependencias.
     *
     * @param songDao       DAO de canciones.
     * @param favoriteDao   DAO de favoritos.
     * @param difficultyDao DAO de dificultades.
     */
    @Inject
    public SongRepository(SongDao songDao, FavoriteSongDao favoriteDao, DifficultyDao difficultyDao) {
        this.songDao = songDao;
        this.favoriteDao = favoriteDao;
        this.difficultyDao = difficultyDao;
    }

    /**
     * Devuelve todas las canciones como LiveData.
     *
     * @return LiveData con lista de {@link Song}.
     */
    public LiveData<List<Song>> getAllSongs() {
        return songDao.getAllSongs();
    }

    /**
     * Devuelve los IDs de canciones favoritas de un usuario como LiveData.
     *
     * @param userId id del usuario.
     * @return LiveData con lista de IDs de canción favoritos.
     */
    public LiveData<List<Integer>> getFavoriteIdsLive(int userId) {
        return favoriteDao.getFavoriteSongIds(userId);
    }

    /**
     * Marca o desmarca una canción como favorita para un usuario.
     * Ejecuta la operación en el executor de escritura de Room.
     *
     * @param userId     id del usuario.
     * @param songId     id de la canción.
     * @param isFavorite true para marcar, false para desmarcar.
     */
    public void toggleFavorite(int userId, int songId, boolean isFavorite) {
        todoAcordeDatabase.databaseWriteExecutor.execute(() -> {
            if (isFavorite) {
                favoriteDao.insert(new FavoriteSong(userId, songId));
            } else {
                favoriteDao.delete(new FavoriteSong(userId, songId));
            }
        });
    }

    /**
     * Obtiene un mapa de dificultades id→descripción.
     *
     * @return mapa donde la clave es el id de la dificultad y el valor su texto.
     */
    public Map<Integer, String> getDifficultyMap() {
        List<Difficulty> difficulties = difficultyDao.getAllDifficulties();
        Map<Integer, String> map = new HashMap<>();
        for (Difficulty d : difficulties) {
            map.put(d.getId(), d.getDifficultyLevel());
        }
        return map;
    }
}
