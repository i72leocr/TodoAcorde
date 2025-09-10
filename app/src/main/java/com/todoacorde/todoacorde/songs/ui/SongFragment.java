package com.todoacorde.todoacorde.songs.ui;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SearchView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ConcatAdapter;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.todoacorde.todoacorde.R;
import com.todoacorde.todoacorde.practice.ui.PracticeChordsOptimizedFragment;
import com.todoacorde.todoacorde.songs.data.Song;
import com.todoacorde.todoacorde.songs.domain.PrefsKeys;
import com.todoacorde.todoacorde.songs.domain.SongViewModel;

import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * Fragmento de listado de canciones con:
 * - Cabecera de filtros/ordenaciones (ConcatAdapter).
 * - Cuadro de búsqueda reactivo.
 * - Observación del {@link SongViewModel} para datos filtrados y mapa de dificultades.
 * Navega al detalle de práctica al pulsar una canción.
 */
@AndroidEntryPoint
public class SongFragment extends Fragment {

    /** Argumento para pasar el id de canción al fragmento de práctica. */
    private static final String ARG_SONG_ID = "song_id";

    /** Preferencias inyectadas (Hilt) para estado de filtros/orden. */
    @Inject SharedPreferences prefs;

    /** ViewModel que gestiona filtros, favoritos y ordenación. */
    private SongViewModel songViewModel;

    /** Adaptador de filas de canciones. */
    private SongAdapter songAdapter;

    /** Barra de búsqueda. */
    private SearchView searchView;

    /** Adaptador de cabecera con UI de filtro/orden. */
    private HeaderAdapter headerAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_song, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View root,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(root, savedInstanceState);

        // RecyclerView con layout vertical.
        RecyclerView rv = root.findViewById(R.id.song_list);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));

        // Cabecera: estado inicial leído de SharedPreferences.
        headerAdapter = new HeaderAdapter(
                requireContext(),
                prefs.getBoolean(PrefsKeys.KEY_SHOW_FAV, false),
                prefs.getString(PrefsKeys.KEY_SORT_CRIT, PrefsKeys.VAL_SORT_TITLE),
                prefs.getBoolean(PrefsKeys.KEY_SORT_ASC, true),
                this::onFilterChanged,
                this::onSortChanged
        );

        // Adaptador principal de canciones.
        songAdapter = new SongAdapter(
                this::navigateToDetails,
                this::onFavoriteToggled
        );

        // Concatena cabecera + lista.
        rv.setAdapter(new ConcatAdapter(headerAdapter, songAdapter));

        // Configuración visual y comportamiento de la búsqueda.
        searchView = root.findViewById(R.id.searchView);
        int pad16 = getResources().getDimensionPixelSize(R.dimen.search_padding_horizontal);
        searchView.setPadding(
                pad16,
                searchView.getPaddingTop(),
                pad16,
                searchView.getPaddingBottom()
        );
        searchView.setQueryHint(getString(R.string.search_hint));
        setupSearchAppearance();
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override public boolean onQueryTextSubmit(String query) { return false; }
            @Override public boolean onQueryTextChange(String query) {
                songViewModel.setSearchQuery(query);
                return true;
            }
        });

        // ViewModel: fuentes y observadores.
        songViewModel = new ViewModelProvider(this).get(SongViewModel.class);
        songViewModel.getFilteredSongs().observe(getViewLifecycleOwner(), this::submitSongs);
        songViewModel.getDifficultyMap().observe(getViewLifecycleOwner(), map -> {
            songAdapter.setDifficultyMap(map);
        });
    }

    /**
     * Callback de la cabecera: cambia filtro “solo favoritos”.
     */
    private void onFilterChanged(boolean showFav) {
        songViewModel.setShowFavoritesOnly(showFav);
    }

    /**
     * Callback de la cabecera: cambia criterio y orden de ordenación.
     */
    private void onSortChanged(String criterion, boolean asc) {
        songViewModel.setSortCriterion(criterion);
        songViewModel.setAscending(asc);
    }

    /**
     * Entrega la lista al adaptador y refresca contador de cabecera.
     */
    private void submitSongs(List<Song> songs) {
        songAdapter.submitList(songs);
        RecyclerView rv = requireView().findViewById(R.id.song_list);
        rv.post(() -> headerAdapter.updateCount(songs != null ? songs.size() : 0));
    }

    /**
     * Navega al fragmento de práctica de acordes con el id de canción.
     */
    private void navigateToDetails(Song song) {
        PracticeChordsOptimizedFragment f = new PracticeChordsOptimizedFragment();
        Bundle b = new Bundle();
        b.putInt(ARG_SONG_ID, song.getId());
        f.setArguments(b);
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, f)
                .addToBackStack(null)
                .commit();
    }

    /**
     * Alterna el estado de favorito a través del ViewModel.
     */
    private void onFavoriteToggled(Song song) {
        songViewModel.toggleFavorite(song.getId(), !song.isFavorite());
    }

    /**
     * Ajustes de aspecto del SearchView (márgenes y fondo).
     */
    private void setupSearchAppearance() {
        ImageView mag = searchView.findViewById(androidx.appcompat.R.id.search_mag_icon);
        if (mag != null) {
            ViewGroup.MarginLayoutParams lp =
                    (ViewGroup.MarginLayoutParams) mag.getLayoutParams();
            lp.setMarginStart(
                    getResources().getDimensionPixelSize(R.dimen.search_icon_margin)
            );
            mag.setLayoutParams(lp);
        }
        View plate = searchView.findViewById(androidx.appcompat.R.id.search_plate);
        if (plate != null) {
            plate.setBackgroundColor(
                    getResources().getColor(R.color.search_plate_bg, requireContext().getTheme())
            );
        }
    }
}
