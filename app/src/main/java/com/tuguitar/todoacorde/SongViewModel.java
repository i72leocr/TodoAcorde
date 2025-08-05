package com.tuguitar.todoacorde;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

public class SongViewModel extends ViewModel {
    private final SongRepository repository; // ← ahora es final y se asigna correctamente
    private final LiveData<List<Song>> songs;
    private final LiveData<List<Integer>> favoriteIds;
    private final MediatorLiveData<List<Song>> songsWithFav = new MediatorLiveData<>();

    public SongViewModel(SongRepository repository, int userId) {
        this.repository   = repository; // ← ESTA LÍNEA ERA CRUCIAL
        this.songs        = repository.getAllSongs();
        this.favoriteIds  = repository.getFavoriteIdsLive(userId);

        // Cuando cambie cualquiera de las dos fuentes, recombinamos
        songsWithFav.addSource(songs, list  -> combine(list, favoriteIds.getValue()));
        songsWithFav.addSource(favoriteIds, ids -> combine(songs.getValue(), ids));
    }

    /** LiveData que emite siempre la lista completa de canciones,
     *  con su flag `isFavorite` ya actualizado */
    public LiveData<List<Song>> getSongsWithFav() {
        return songsWithFav;
    }

    public void toggleFavorite(int userId, int songId, boolean isFavorite) {
        repository.toggleFavorite(userId, songId, isFavorite);
    }

    private void combine(List<Song> list, List<Integer> ids) {
        if (list == null || ids == null) return;
        List<Song> copy = new ArrayList<>(list);
        for (Song s : copy) {
            s.setFavorite(ids.contains(s.getId()));
        }
        songsWithFav.setValue(copy);
    }

    public static class Factory implements ViewModelProvider.Factory {
        private final SongRepository repository;
        private final int userId;

        public Factory(SongRepository repository, int userId) {
            this.repository = repository;
            this.userId     = userId;
        }

        @SuppressWarnings("unchecked")
        @Override @NonNull
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            if (modelClass.isAssignableFrom(SongViewModel.class)) {
                return (T) new SongViewModel(repository, userId);
            }
            throw new IllegalArgumentException("Unknown ViewModel class");
        }
    }
}
