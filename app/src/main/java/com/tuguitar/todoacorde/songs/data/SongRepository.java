package com.tuguitar.todoacorde.songs.data;

import androidx.lifecycle.LiveData;

import com.tuguitar.todoacorde.Difficulty;
import com.tuguitar.todoacorde.DifficultyDao;
import com.tuguitar.todoacorde.todoAcordeDatabase;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class SongRepository {
    private final SongDao songDao;
    private final FavoriteSongDao favoriteDao;
    private final DifficultyDao difficultyDao;

    @Inject
    public SongRepository(SongDao songDao, FavoriteSongDao favoriteDao, DifficultyDao difficultyDao) {
        this.songDao = songDao;
        this.favoriteDao = favoriteDao;
        this.difficultyDao = difficultyDao;
    }

    public LiveData<List<Song>> getAllSongs() {
        return songDao.getAllSongs();
    }

    public LiveData<List<Integer>> getFavoriteIdsLive(int userId) {
        return favoriteDao.getFavoriteSongIds(userId);
    }

    public void toggleFavorite(int userId, int songId, boolean isFavorite) {
        todoAcordeDatabase.databaseWriteExecutor.execute(() -> {
            if (isFavorite) {
                favoriteDao.insert(new FavoriteSong(userId, songId));
            } else {
                favoriteDao.delete(new FavoriteSong(userId, songId));
            }
        });
    }

    public Map<Integer, String> getDifficultyMap() {
        List<Difficulty> difficulties = difficultyDao.getAllDifficulties();
        Map<Integer, String> map = new HashMap<>();
        for (Difficulty d : difficulties) {
            map.put(d.getId(), d.getDifficultyLevel());
        }
        return map;
    }
}
