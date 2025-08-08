package com.tuguitar.todoacorde.songs.ui;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SearchView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ConcatAdapter;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.tuguitar.todoacorde.R;
import com.tuguitar.todoacorde.songs.data.Song;
import com.tuguitar.todoacorde.songs.domain.PrefsKeys;
import com.tuguitar.todoacorde.songs.domain.SongViewModel;
import com.tuguitar.todoacorde.practice.ui.PracticeChordsOptimizedFragment;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class SongFragment extends Fragment {
    private static final String ARG_SONG_ID = "song_id";

    @Inject SharedPreferences prefs;
    private SongViewModel songViewModel;
    private SongAdapter songAdapter;
    private SearchView searchView;
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

        // Configurar RecyclerView y adapters
        RecyclerView rv = root.findViewById(R.id.song_list);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));

        headerAdapter = new HeaderAdapter(
                requireContext(),
                prefs.getBoolean(PrefsKeys.KEY_SHOW_FAV, false),
                prefs.getString(PrefsKeys.KEY_SORT_CRIT, PrefsKeys.VAL_SORT_TITLE),
                prefs.getBoolean(PrefsKeys.KEY_SORT_ASC, true),
                this::onFilterChanged,
                this::onSortChanged
        );
        songAdapter = new SongAdapter(
                this::navigateToDetails,
                this::onFavoriteToggled
        );
        rv.setAdapter(new ConcatAdapter(headerAdapter, songAdapter));

        // Configurar SearchView
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

        // Inyectar ViewModel
        songViewModel = new ViewModelProvider(this).get(SongViewModel.class);
        songViewModel.getFilteredSongs().observe(getViewLifecycleOwner(), this::submitSongs);

        // NUEVO: Observar el mapa de dificultad y pasarlo al adaptador
        songViewModel.getDifficultyMap().observe(getViewLifecycleOwner(), map -> {
            songAdapter.setDifficultyMap(map);
        });
    }

    private void onFilterChanged(boolean showFav) {
        songViewModel.setShowFavoritesOnly(showFav);
    }

    private void onSortChanged(String criterion, boolean asc) {
        songViewModel.setSortCriterion(criterion);
        songViewModel.setAscending(asc);
    }

    private void submitSongs(List<Song> songs) {
        songAdapter.submitList(songs);
        RecyclerView rv = requireView().findViewById(R.id.song_list);
        rv.post(() -> headerAdapter.updateCount(songs != null ? songs.size() : 0));
    }

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

    private void onFavoriteToggled(Song song) {
        songViewModel.toggleFavorite(song.getId(), !song.isFavorite());
    }

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
