package com.tuguitar.todoacorde.songs.domain;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;

import com.tuguitar.todoacorde.songs.data.Song;
import com.tuguitar.todoacorde.songs.data.SongRepository;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class SongViewModel extends ViewModel {
    private final SongRepository repository;
    private final int userId;
    private final LiveData<List<Song>> songs;
    private final LiveData<List<Integer>> favoriteIds;
    private final MediatorLiveData<List<Song>> songsWithFav = new MediatorLiveData<>();

    @Inject
    public SongViewModel(
            SongRepository repository,
            SavedStateHandle savedStateHandle
    ) {
        this.repository = repository;
        this.userId = savedStateHandle.get("userId");
        this.songs = repository.getAllSongs();
        this.favoriteIds = repository.getFavoriteIdsLive(userId);

        songsWithFav.addSource(songs, list  -> combine(list, favoriteIds.getValue()));
        songsWithFav.addSource(favoriteIds, ids -> combine(songs.getValue(), ids));
    }

    public LiveData<List<Song>> getSongsWithFav() {
        return songsWithFav;
    }

    public void toggleFavorite(int songId, boolean isFavorite) {
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
}
