package com.tuguitar.todoacorde.songs.data;

import androidx.lifecycle.LiveData;
import com.tuguitar.todoacorde.FavoriteSong;
import com.tuguitar.todoacorde.FavoriteSongDao;
import com.tuguitar.todoacorde.todoAcordeDatabase;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class SongRepository {
    private final SongDao songDao;
    private final FavoriteSongDao favoriteDao;

    @Inject
    public SongRepository(SongDao songDao, FavoriteSongDao favoriteDao) {
        this.songDao = songDao;
        this.favoriteDao = favoriteDao;
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
}
