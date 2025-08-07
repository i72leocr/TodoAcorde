package com.tuguitar.todoacorde.songs.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import android.widget.SearchView;
import android.widget.ImageView;

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
import com.tuguitar.todoacorde.songs.domain.SongViewModel;
import com.tuguitar.todoacorde.practice.ui.PracticeChordsOptimizedFragment;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * Fragment que muestra la lista de canciones y navega al fragment de práctica.
 */
@AndroidEntryPoint
public class SongFragment extends Fragment {
    private static final String TAG       = "SongFragment";
    private static final String PREFS     = "SongPreferences";
    private static final String ARG_SONG_ID = "song_id";
    private static final int    USER_ID     = 1;

    private final List<Song> allSongs      = new ArrayList<>();
    private final List<Song> filteredSongs = new ArrayList<>();

    private SongAdapter songAdapter;
    private SearchView  searchView;
    private boolean     ascending;
    private String      sortCriterion;
    private boolean     showFavoritesOnly;
    private SharedPreferences prefs;
    private SongViewModel      songViewModel;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Aseguramos pasar userId al SavedStateHandle
        Bundle args = getArguments();
        if (args == null) {
            args = new Bundle();
        }
        if (!args.containsKey("userId")) {
            args.putInt("userId", USER_ID);
        }
        setArguments(args);
    }

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

        // --- 1) Preferencias ---
        prefs             = requireContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        showFavoritesOnly = prefs.getBoolean("show_favorites_only", false);
        ascending         = prefs.getBoolean("sort_order", true);
        sortCriterion     = prefs.getString("sort_criterion", "diff");
        Log.d(TAG, "Prefs loaded → showFav=" + showFavoritesOnly +
                " sort=" + sortCriterion + " asc=" + ascending);

        // --- 2) SearchView ---
        searchView = root.findViewById(R.id.searchView);
        setupSearchAppearance();
        setupSearchListener();

        // --- 3) RecyclerView + Adapters ---
        RecyclerView rv = root.findViewById(R.id.song_list);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        HeaderAdapter headerAdapter = new HeaderAdapter(requireContext());
        songAdapter = new SongAdapter(
                requireContext(),
                this::navigateToDetails,
                this::onFavoriteToggled
        );
        rv.setAdapter(new ConcatAdapter(
                new ConcatAdapter.Config.Builder().setIsolateViewTypes(true).build(),
                headerAdapter,
                songAdapter
        ));

        // --- 4) ViewModel ---
        songViewModel = new ViewModelProvider(this)
                .get(SongViewModel.class);

        // --- 5) Observamos cambios ---
        songViewModel.getSongsWithFav().observe(getViewLifecycleOwner(), list -> {
            allSongs.clear();
            if (list != null) allSongs.addAll(list);
            applyFilterSort();
            songAdapter.submitList(new ArrayList<>(filteredSongs));
        });
    }

    private void setupSearchAppearance() {
        int pad16 = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 16,
                getResources().getDisplayMetrics()
        );
        searchView.setPadding(pad16, searchView.getPaddingTop(), pad16, searchView.getPaddingBottom());

        View plate = searchView.findViewById(androidx.appcompat.R.id.search_plate);
        if (plate != null) {
            plate.setBackgroundColor(Color.TRANSPARENT);
            plate.setPadding(0, 0, 0, 0);
        }

        View src = searchView.findViewById(androidx.appcompat.R.id.search_src_text);
        if (src != null) {
            src.setBackground(null);
            src.setPadding(0, src.getPaddingTop(), src.getPaddingRight(), src.getPaddingBottom());
        }

        ImageView mag = searchView.findViewById(androidx.appcompat.R.id.search_mag_icon);
        if (mag != null) {
            mag.setPadding(0, 0, 0, 0);
            if (mag.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
                ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) mag.getLayoutParams();
                lp.setMarginStart(0);
                mag.setLayoutParams(lp);
            }
        }

        searchView.setQuery("", false);
    }

    private void setupSearchListener() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override public boolean onQueryTextSubmit(String query) { return false; }
            @Override public boolean onQueryTextChange(String query) {
                applyFilterSort();
                songAdapter.submitList(new ArrayList<>(filteredSongs));
                return true;
            }
        });
    }

    private void applyFilterSort() {
        List<Song> base = showFavoritesOnly
                ? allSongs.stream().filter(Song::isFavorite).collect(Collectors.toList())
                : new ArrayList<>(allSongs);

        String q = searchView.getQuery().toString().toLowerCase();
        if (!q.isEmpty()) {
            base = base.stream()
                    .filter(s -> s.getTitle().toLowerCase().contains(q)
                            || s.getAuthor().toLowerCase().contains(q))
                    .collect(Collectors.toList());
        }

        Comparator<Song> cmp = "title".equals(sortCriterion)
                ? Comparator.comparing(Song::getTitle, String.CASE_INSENSITIVE_ORDER)
                : Comparator.comparingInt(Song::getDifficulty);
        if (!ascending) cmp = cmp.reversed();

        base.sort(cmp);

        filteredSongs.clear();
        filteredSongs.addAll(base);
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
        boolean nowFav = !song.isFavorite();
        song.setFavorite(nowFav);
        songViewModel.toggleFavorite(song.getId(), nowFav);

        applyFilterSort();
        songAdapter.submitList(new ArrayList<>(filteredSongs));
    }

    // Adaptador para la cabecera
    private class HeaderAdapter extends RecyclerView.Adapter<HeaderAdapter.VH> {
        private final LayoutInflater inflater;
        HeaderAdapter(Context ctx) {
            inflater = LayoutInflater.from(ctx);
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = inflater.inflate(R.layout.partial_filters_row, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int position) {
            h.filterSwitch.setChecked(showFavoritesOnly);
            h.filterSwitch.setOnCheckedChangeListener((btn, checked) -> {
                showFavoritesOnly = checked;
                prefs.edit().putBoolean("show_favorites_only", checked).apply();
                applyFilterSort();
                songAdapter.submitList(new ArrayList<>(filteredSongs));
            });

            String label = ("title".equals(sortCriterion) ? "TITLE" : "DIFF")
                    + (ascending ? " (ASC)" : " (DESC)");
            h.sortButton.setText(label);
            h.sortButton.setOnClickListener(v -> {
                PopupMenu m = new PopupMenu(requireContext(), v);
                m.getMenuInflater().inflate(R.menu.menu_sort, m.getMenu());
                m.setOnMenuItemClickListener(item -> {
                    int id = item.getItemId();
                    if (id == R.id.sort_by_title_asc) {
                        sortCriterion = "title";
                        ascending     = true;
                    } else if (id == R.id.sort_by_title_desc) {
                        sortCriterion = "title";
                        ascending     = false;
                    } else if (id == R.id.sort_by_difficulty_asc) {
                        sortCriterion = "diff";
                        ascending     = true;
                    } else if (id == R.id.sort_by_difficulty_desc) {
                        sortCriterion = "diff";
                        ascending     = false;
                    }
                    prefs.edit()
                            .putString("sort_criterion", sortCriterion)
                            .putBoolean("sort_order",   ascending)
                            .apply();
                    applyFilterSort();
                    songAdapter.submitList(new ArrayList<>(filteredSongs));
                    return true;
                });

                m.show();
            });
        }

        @Override public int getItemCount() { return 1; }

        class VH extends RecyclerView.ViewHolder {
            final SwitchCompat filterSwitch;
            final AppCompatButton sortButton;
            VH(View itemView) {
                super(itemView);
                filterSwitch = itemView.findViewById(R.id.checkbox_favorites);
                sortButton   = itemView.findViewById(R.id.sort_button);
            }
        }
    }
}
