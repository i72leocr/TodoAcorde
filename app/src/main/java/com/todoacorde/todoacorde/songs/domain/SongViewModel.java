package com.todoacorde.todoacorde.songs.domain;

import android.content.SharedPreferences;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;

import com.todoacorde.todoacorde.AppExecutors;
import com.todoacorde.todoacorde.songs.data.Song;
import com.todoacorde.todoacorde.songs.data.SongRepository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

/**
 * ViewModel responsable de:
 * - Combinar el listado base de canciones con el estado de favorito por usuario.
 * - Aplicar filtrado (búsqueda, solo favoritos) y ordenación (título/dificultad).
 * - Persistir preferencias de UI en {@link SharedPreferences}.
 * - Exponer metadatos de dificultad (id → nombre).
 */
@HiltViewModel
public class SongViewModel extends ViewModel {

    /** Claves de preferencias persistidas. */
    private static final String KEY_SHOW_FAV = "show_favorites_only";
    private static final String KEY_SORT_CRIT = "sort_criterion";
    private static final String KEY_SORT_ASC = "sort_order";

    /** Acceso a datos de canciones y favoritos. */
    private final SongRepository repository;
    /** Preferencias para recordar los ajustes de listado. */
    private final SharedPreferences prefs;
    /** Usuario actual asociado al estado de favoritos. */
    private final int userId;

    /**
     * Lista de canciones con el flag de favorito “inyectado”.
     * Orígenes combinados: listado base + ids de favoritos.
     */
    private final MediatorLiveData<List<Song>> songsWithFav = new MediatorLiveData<>();

    /**
     * Lista final tras aplicar búsqueda, filtro de favoritos y ordenación.
     */
    private final MediatorLiveData<List<Song>> filteredSongs = new MediatorLiveData<>();

    /** Mapa de dificultad (id → etiqueta legible). */
    private final MutableLiveData<Map<Integer, String>> difficultyMap = new MutableLiveData<>();

    /** Texto de búsqueda libre. */
    private final MutableLiveData<String> searchQuery = new MutableLiveData<>("");

    /** Flag UI: mostrar solo favoritos. */
    private final MutableLiveData<Boolean> showFavorites = new MutableLiveData<>();

    /** Criterio de ordenación: "title" o "diff". */
    private final MutableLiveData<String> sortCriterion = new MutableLiveData<>();

    /** Sentido de ordenación: ascendente si true. */
    private final MutableLiveData<Boolean> ascending = new MutableLiveData<>();

    /**
     * Crea el ViewModel configurando los orígenes de datos y las fuentes reactivas.
     *
     * @param repository repositorio de canciones/favoritos/dificultades.
     * @param prefs preferencias persistentes de la pantalla.
     * @param savedStateHandle estado con parámetros de inicialización (p.ej. userId).
     */
    @Inject
    public SongViewModel(
            SongRepository repository,
            SharedPreferences prefs,
            SavedStateHandle savedStateHandle
    ) {
        this.repository = repository;
        this.prefs = prefs;

        // Usuario actual (fallback a 1 si no está presente).
        Integer uid = savedStateHandle.get("userId");
        this.userId = (uid != null) ? uid : 1;

        // Estado inicial desde preferencias.
        showFavorites.setValue(prefs.getBoolean(KEY_SHOW_FAV, false));
        sortCriterion.setValue(prefs.getString(KEY_SORT_CRIT, "title"));
        ascending.setValue(prefs.getBoolean(KEY_SORT_ASC, true));

        // Fuentes base.
        LiveData<List<Song>> baseSongs = repository.getAllSongs();
        LiveData<List<Integer>> favIds = repository.getFavoriteIdsLive(userId);

        // Combina lista base con los favoritos (bidireccional).
        songsWithFav.addSource(baseSongs, list -> combineFlags(list, favIds.getValue(), songsWithFav));
        songsWithFav.addSource(favIds, ids -> combineFlags(baseSongs.getValue(), ids, songsWithFav));

        // Recalcula la lista final cuando cambia cualquiera de las dependencias.
        filteredSongs.addSource(songsWithFav, list -> applyAll());
        filteredSongs.addSource(searchQuery, q -> applyAll());
        filteredSongs.addSource(showFavorites, f -> applyAll());
        filteredSongs.addSource(sortCriterion, c -> applyAll());
        filteredSongs.addSource(ascending, a -> applyAll());

        // Carga las etiquetas de dificultad.
        loadDifficultyMap();
    }

    /** Expone la lista filtrada y ordenada para la UI. */
    public LiveData<List<Song>> getFilteredSongs() {
        return filteredSongs;
    }

    /** Expone el mapa de dificultades (id → etiqueta). */
    public LiveData<Map<Integer, String>> getDifficultyMap() {
        return difficultyMap;
    }

    /** Actualiza el texto de búsqueda. */
    public void setSearchQuery(String query) {
        searchQuery.setValue(query != null ? query : "");
    }

    /** Activa/desactiva el filtro de solo favoritos y lo persiste. */
    public void setShowFavoritesOnly(boolean show) {
        prefs.edit().putBoolean(KEY_SHOW_FAV, show).apply();
        showFavorites.setValue(show);
    }

    /** Cambia el criterio de ordenación y lo persiste. */
    public void setSortCriterion(String criterion) {
        prefs.edit().putString(KEY_SORT_CRIT, criterion).apply();
        sortCriterion.setValue(criterion);
    }

    /** Cambia el sentido de ordenación y lo persiste. */
    public void setAscending(boolean asc) {
        prefs.edit().putBoolean(KEY_SORT_ASC, asc).apply();
        ascending.setValue(asc);
    }

    /**
     * Alterna el estado de favorito para una canción y actualiza la lista actual en memoria.
     *
     * @param songId     id de la canción.
     * @param isFavorite nuevo estado de favorito.
     */
    public void toggleFavorite(int songId, boolean isFavorite) {
        repository.toggleFavorite(userId, songId, isFavorite);

        List<Song> current = songsWithFav.getValue();
        if (current == null) return;

        List<Song> updated = new ArrayList<>(current.size());
        for (Song s : current) {
            if (s.getId() == songId) {
                Song copy = new Song(s);
                copy.setFavorite(isFavorite);
                updated.add(copy);
            } else {
                updated.add(s);
            }
        }
        songsWithFav.setValue(updated);
    }

    /**
     * Inyecta el flag de favorito en copias de la lista base de canciones.
     *
     * @param songs   lista base.
     * @param favIds  ids marcados como favoritos del usuario.
     * @param output  destino combinado.
     */
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

    /**
     * Aplica, en hilo de trabajo, los pasos de filtrado y ordenación sobre la lista combinada.
     * - Filtro por favoritos (opcional).
     * - Filtro por texto (título o autor).
     * - Ordenación por título o dificultad, asc/desc.
     */
    private void applyAll() {
        AppExecutors.getInstance().diskIO().execute(() -> {
            List<Song> list = songsWithFav.getValue();
            if (list == null) {
                filteredSongs.postValue(null);
                return;
            }

            boolean showFav = Boolean.TRUE.equals(showFavorites.getValue());
            String q = searchQuery.getValue() != null
                    ? searchQuery.getValue().toLowerCase()
                    : "";

            // Filtrado
            List<Song> temp = new ArrayList<>();
            for (Song s : list) {
                boolean matchFav = !showFav || s.isFavorite();
                boolean matchQuery = q.isEmpty()
                        || s.getTitle().toLowerCase().contains(q)
                        || s.getAuthor().toLowerCase().contains(q);
                if (matchFav && matchQuery) {
                    temp.add(s);
                }
            }

            // Ordenación
            Comparator<Song> cmp = "title".equals(sortCriterion.getValue())
                    ? Comparator.comparing(Song::getTitle, String.CASE_INSENSITIVE_ORDER)
                    : Comparator.comparingInt(Song::getDifficulty);

            if (Boolean.FALSE.equals(ascending.getValue())) {
                cmp = cmp.reversed();
            }
            temp.sort(cmp);

            filteredSongs.postValue(temp);
        });
    }

    /** Recupera el mapa de dificultades desde el repositorio y lo publica. */
    private void loadDifficultyMap() {
        AppExecutors.getInstance().diskIO().execute(() -> {
            Map<Integer, String> map = repository.getDifficultyMap();
            difficultyMap.postValue(map);
        });
    }
}
