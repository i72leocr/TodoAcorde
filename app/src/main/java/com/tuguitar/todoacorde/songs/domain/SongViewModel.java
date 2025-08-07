package com.tuguitar.todoacorde.songs.domain;

import android.content.SharedPreferences;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;

import com.tuguitar.todoacorde.AppExecutors;
import com.tuguitar.todoacorde.songs.data.Song;
import com.tuguitar.todoacorde.songs.data.SongRepository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class SongViewModel extends ViewModel {
    private static final String KEY_SHOW_FAV  = "show_favorites_only";
    private static final String KEY_SORT_CRIT = "sort_criterion";
    private static final String KEY_SORT_ASC  = "sort_order";

    private final SongRepository repository;
    private final SharedPreferences prefs;
    private final int userId;
    private final MediatorLiveData<List<Song>> songsWithFav = new MediatorLiveData<>();

    // Expuesto al UI
    private final MediatorLiveData<List<Song>> filteredSongs = new MediatorLiveData<>();

    // Estado local
    private final MutableLiveData<String>  searchQuery    = new MutableLiveData<>("");
    private final MutableLiveData<Boolean> showFavorites = new MutableLiveData<>();
    private final MutableLiveData<String>  sortCriterion  = new MutableLiveData<>();
    private final MutableLiveData<Boolean> ascending     = new MutableLiveData<>();

    @Inject
    public SongViewModel(
            SongRepository repository,
            SharedPreferences prefs,
            SavedStateHandle savedStateHandle
    ) {
        this.repository = repository;
        this.prefs      = prefs;

        Integer uid = savedStateHandle.get("userId");
        this.userId = (uid != null) ? uid : 1;

        // Inicializar filtros desde prefs
        showFavorites.setValue(prefs.getBoolean(KEY_SHOW_FAV, false));
        sortCriterion.setValue(prefs.getString(KEY_SORT_CRIT, "title"));
        ascending.setValue(prefs.getBoolean(KEY_SORT_ASC, true));

        // 1) Observamos baseSongs y favIds para construir songsWithFav
        LiveData<List<Song>> baseSongs = repository.getAllSongs();
        LiveData<List<Integer>> favIds  = repository.getFavoriteIdsLive(userId);

        songsWithFav.addSource(baseSongs, list ->
                combineFlags(list, favIds.getValue(), songsWithFav)
        );
        songsWithFav.addSource(favIds, ids ->
                combineFlags(baseSongs.getValue(), ids, songsWithFav)
        );

        // 2) Observamos songsWithFav + filtros para calcular filteredSongs
        filteredSongs.addSource(songsWithFav, list -> applyAll());
        filteredSongs.addSource(searchQuery,    q    -> applyAll());
        filteredSongs.addSource(showFavorites,  f    -> applyAll());
        filteredSongs.addSource(sortCriterion,  c    -> applyAll());
        filteredSongs.addSource(ascending,      a    -> applyAll());
    }

    /** Resultado final que observa la UI */
    public LiveData<List<Song>> getFilteredSongs() {
        return filteredSongs;
    }

    public void setSearchQuery(String query) {
        searchQuery.setValue(query != null ? query : "");
    }

    public void setShowFavoritesOnly(boolean show) {
        prefs.edit().putBoolean(KEY_SHOW_FAV, show).apply();
        showFavorites.setValue(show);
    }

    public void setSortCriterion(String criterion) {
        prefs.edit().putString(KEY_SORT_CRIT, criterion).apply();
        sortCriterion.setValue(criterion);
    }

    public void setAscending(boolean asc) {
        prefs.edit().putBoolean(KEY_SORT_ASC, asc).apply();
        ascending.setValue(asc);
    }

    /**
     * Toggle optimista: actualiza Room y modifica la lista en memoria inmediatamente.
     */
    public void toggleFavorite(int songId, boolean isFavorite) {
        // 1) Persistencia en background
        repository.toggleFavorite(userId, songId, isFavorite);

        // 2) Optimistic update
        List<Song> current = songsWithFav.getValue();
        if (current == null) return;

        List<Song> updated = new ArrayList<>(current.size());
        for (Song s : current) {
            if (s.getId() == songId) {
                // Usamos el constructor de copia y luego cambiamos el flag
                Song copy = new Song(s);
                copy.setFavorite(isFavorite);
                updated.add(copy);
            } else {
                updated.add(s);
            }
        }
        songsWithFav.setValue(updated);
    }

    private void combineFlags(
            List<Song> songs,
            List<Integer> favIds,
            MediatorLiveData<List<Song>> output
    ) {
        if (songs == null || favIds == null) return;
        List<Song> list = new ArrayList<>(songs.size());
        for (Song s : songs) {
            Song copy = new Song(s);
            copy.setFavorite(favIds.contains(s.getId()));
            list.add(copy);
        }
        output.setValue(list);
    }

    private void applyAll() {
        AppExecutors.getInstance().diskIO().execute(() -> {
            List<Song> list = songsWithFav.getValue();
            if (list == null) {
                ((MediatorLiveData<List<Song>>) filteredSongs).postValue(null);
                return;
            }

            boolean showFav = Boolean.TRUE.equals(showFavorites.getValue());
            String q = searchQuery.getValue() != null
                    ? searchQuery.getValue().toLowerCase()
                    : "";

            List<Song> temp = new ArrayList<>();
            for (Song s : list) {
                if ((!showFav || s.isFavorite()) &&
                        (q.isEmpty()
                                || s.getTitle().toLowerCase().contains(q)
                                || s.getAuthor().toLowerCase().contains(q))) {
                    temp.add(s);
                }
            }

            Comparator<Song> cmp = "title".equals(sortCriterion.getValue())
                    ? Comparator.comparing(Song::getTitle, String.CASE_INSENSITIVE_ORDER)
                    : Comparator.comparingInt(Song::getDifficulty);
            if (Boolean.FALSE.equals(ascending.getValue())) {
                cmp = cmp.reversed();
            }
            temp.sort(cmp);

            // Publicamos el resultado de vuelta en el hilo principal
            ((MediatorLiveData<List<Song>>) filteredSongs).postValue(temp);
        });
    }
}
