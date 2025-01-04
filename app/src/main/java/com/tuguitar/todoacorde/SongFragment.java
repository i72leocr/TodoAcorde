package com.tuguitar.todoacorde;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.View;
import android.widget.TextView;

import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.SearchView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Arrays;

public class SongFragment extends Fragment {

    private static final int SONGS_PER_PAGE = 10;

    private List<Song> allSongs;
    private List<Song> filteredSongs;
    private SongAdapter adapter;
    private String currentSortCriterion;
    private boolean ascending;
    private boolean showFavoritesOnly;
    private SharedPreferences sharedPreferences;
    private Button sortButton;
    private CheckBox favoriteCheckBox;
    private Button nextPageButton;
    private Button prevPageButton;
    private TextView pageIndicator;
    private SearchView searchView;  // Initialize searchView
    private String currentQuery = "";  // Initialize currentQuery
    private int currentPage;

    private static final String TAG = "SongFragment";

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Listen for results from SongDetailsFragment
        getParentFragmentManager().setFragmentResultListener("requestKey", this, (requestKey, bundle) -> {
            currentPage = bundle.getInt("current_page", 0);
            Log.d(TAG, "Page restored from SongDetailsFragment: " + currentPage);
            updatePage();
        });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_song, container, false);

        // Inicializar SharedPreferences
        sharedPreferences = requireContext().getSharedPreferences("SongPreferences", Context.MODE_PRIVATE);

        // Cargar preferencias guardadas
        currentSortCriterion = sharedPreferences.getString("sort_criterion", "title");
        ascending = sharedPreferences.getBoolean("sort_order", true);
        showFavoritesOnly = sharedPreferences.getBoolean("show_favorites_only", false);

        // Inicializar componentes
        allSongs = getSongs();  // Lista original de canciones
        filteredSongs = new ArrayList<>(allSongs);  // Lista filtrada y ordenada

        Log.d(TAG, "Canciones originales cargadas: " + allSongs.size());  // Log de canciones iniciales

        adapter = new SongAdapter(getContext(), filteredSongs);

        // Configurar ListView
        ListView listView = view.findViewById(R.id.song_list);

        // Inflar y agregar el encabezado al ListView
        View headerView = inflater.inflate(R.layout.list_header, null);
        listView.addHeaderView(headerView);  // Añadir el encabezado

        listView.setAdapter(adapter);

        // Configurar SearchView
        SearchView searchView = view.findViewById(R.id.searchView);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterSongs(newText);
                return true;
            }
        });

        // Configurar botones de paginación
        nextPageButton = view.findViewById(R.id.next_page_button);
        prevPageButton = view.findViewById(R.id.prev_page_button);
        pageIndicator = view.findViewById(R.id.page_indicator);

        nextPageButton.setOnClickListener(v -> {
            currentPage++;
            updatePage();
        });

        prevPageButton.setOnClickListener(v -> {
            if (currentPage > 0) currentPage--;
            updatePage();
        });

        // Configurar botón de ordenar
        sortButton = view.findViewById(R.id.sort_button);
        updateSortButtonText(); // Actualizar texto del botón de ordenación
        sortButton.setOnClickListener(v -> showPopupMenu(v));

        // Configurar CheckBox para favoritos
        favoriteCheckBox = view.findViewById(R.id.checkbox_favorites);
        favoriteCheckBox.setChecked(showFavoritesOnly);
        favoriteCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            showFavoritesOnly = isChecked;
            applySortingAndFiltering();  // Aplicar filtros y ordenamiento
            saveFavoriteState();  // Guardar estado de favoritos
        });

        listView.setOnItemClickListener((parent, view1, position, id) -> {
            int headerViewsCount = listView.getHeaderViewsCount(); // Account for header views
            int actualPosition = position - headerViewsCount;  // Get the position within the visible items

            // Ensure actualPosition is valid within the paginated list
            if (actualPosition >= 0 && actualPosition < adapter.getCount()) {
                // Cast the returned object to a Song type
                Song selectedSong = (Song) adapter.getItem(actualPosition);
                openSongDetails(selectedSong);
            } else {
                Log.e(TAG, "Invalid position: " + actualPosition + ", size of visible list: " + adapter.getCount());
            }
        });

        // Aplicar preferencias de orden y filtrado
        applySortingAndFiltering();
        updatePage();
        return view;
    }
    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        if (savedInstanceState != null) {
            currentPage = savedInstanceState.getInt("current_page");
            Log.d(TAG, "recibido2: " + currentPage);
            updatePage();  // Restore the page state
        }
    }
    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("current_page", currentPage);
        Log.d(TAG, "onSaveInstanceState: " + currentPage);
        outState.putString("search_query", currentQuery);
    }

    private void showPopupMenu(View view) {
        PopupMenu popup = new PopupMenu(getContext(), view);
        MenuInflater inflater = popup.getMenuInflater();
        inflater.inflate(R.menu.menu_sort, popup.getMenu());

        popup.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.sort_by_title_asc) {
                currentSortCriterion = "title";
                ascending = true;
            } else if (itemId == R.id.sort_by_title_desc) {
                currentSortCriterion = "title";
                ascending = false;
            } else if (itemId == R.id.sort_by_difficulty_asc) {
                currentSortCriterion = "difficulty";
                ascending = true;
            } else if (itemId == R.id.sort_by_difficulty_desc) {
                currentSortCriterion = "difficulty";
                ascending = false;
            }

            applySortingAndFiltering();
            updateSortButtonText();
            saveSortState();
            return true;
        });

        popup.show();
    }

    private void sortSongs() {
        if (currentSortCriterion.equals("title")) {
            filteredSongs.sort(ascending ? Comparator.comparing(Song::getTitle) : Comparator.comparing(Song::getTitle).reversed());
        } else if (currentSortCriterion.equals("difficulty")) {
            filteredSongs.sort(ascending ? Comparator.comparingInt(Song::getDifficulty) : Comparator.comparingInt(Song::getDifficulty).reversed());
        }
    }

    private void filterFavorites() {
        if (showFavoritesOnly) {
            filteredSongs = allSongs.stream()
                    .filter(Song::isFavorite)
                    .collect(Collectors.toList());

            Log.d(TAG, "Filtradas canciones favoritas. Total favoritos: " + filteredSongs.size());

        } else {
            filteredSongs = new ArrayList<>(allSongs); // Restaurar lista completa
            Log.d(TAG, "Mostrar todas las canciones. Total canciones: " + filteredSongs.size());
        }
    }

    private void applySortingAndFiltering() {
        filterFavorites();  // Apply favorite filtering
        sortSongs();  // Apply sorting

        // Ensure currentPage is within the bounds of the filtered list
        int totalPages = (filteredSongs.size() + SONGS_PER_PAGE - 1) / SONGS_PER_PAGE;
        if (currentPage >= totalPages) {
            currentPage = totalPages - 1;
        }

        // Prevent currentPage from being negative
        if (currentPage < 0) {
            currentPage = 0;
        }

        updatePage();  // Refresh the pagination
        Log.d(TAG, "Lista actualizada después de aplicar filtros y ordenamiento. Total canciones: " + filteredSongs.size());
    }


    private void filterSongs(String query) {
        // Always start filtering from the full list of songs, not the already filtered one
        List<Song> searchFilteredSongs;

        // If the query is empty, reset to all songs (filtered by favorites if necessary)
        if (query == null || query.trim().isEmpty()) {
            applySortingAndFiltering();  // Reset filteredSongs to all or favorites
        } else {
            searchFilteredSongs = allSongs.stream()
                    .filter(song -> song.getTitle().toLowerCase().contains(query.toLowerCase()) ||
                            song.getAuthor().toLowerCase().contains(query.toLowerCase()))  // Filter by title or author
                    .collect(Collectors.toList());

            filteredSongs = searchFilteredSongs;  // Update the filtered songs based on the search query
            updatePage();  // Update the list and pagination
        }

        Log.d(TAG, "Aplicado filtro de búsqueda. Resultados: " + filteredSongs.size());
    }


    private void updateSortButtonText() {
        String sortOrderText = ascending ? "Asc" : "Desc";
        if (currentSortCriterion.equals("title")) {
            sortButton.setText("Sorted by Title (" + sortOrderText + ")");
        } else if (currentSortCriterion.equals("difficulty")) {
            sortButton.setText("Sorted by Difficulty (" + sortOrderText + ")");
        }
    }

    private void saveSortState() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("sort_criterion", currentSortCriterion);
        editor.putBoolean("sort_order", ascending);
        editor.apply();
    }

    private void saveFavoriteState() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("show_favorites_only", showFavoritesOnly);
        editor.apply();
    }

    private void openSongDetails(Song selectedSong) {
        SongDetailsFragment songDetailsFragment = new SongDetailsFragment();
        Bundle bundle = new Bundle();
        bundle.putString("song_title", selectedSong.getTitle());
        bundle.putString("song_author", selectedSong.getAuthor());
        bundle.putStringArrayList("song_lyrics", new ArrayList<>(selectedSong.getLyrics()));
        bundle.putStringArrayList("song_chords", new ArrayList<>(selectedSong.getChords()));
        bundle.putInt("current_page", currentPage);
        Log.d(TAG, "Pagina enviada: " + currentPage);
        bundle.putString("search_query", currentQuery);// Pass the current page
        songDetailsFragment.setArguments(bundle);

        getParentFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, songDetailsFragment)
                .addToBackStack(null)
                .commit();
    }

    private List<Song> getSongs() {
        List<Song> songs = new ArrayList<>();

        // House of the Rising Sun
        String houseOfRisingSunLyrics = "There is a house in New Orleans\n" +
                "They call the rising sun\n" +
                "And it's been the ruin\n" +
                "of many a poor boy\n" +
                "And God, I know, I'm one";

        String houseOfRisingSunChords = "      Am    C        D           F  \n" +
                "      Am       C      E \n" +
                "          Am       C  \n" +
                "    D           F \n" +
                "     Am      E       Am   C  D  F";

        List<String> houseOfRisingSunLyricsList = Arrays.asList(houseOfRisingSunLyrics.split("\n"));
        List<String> houseOfRisingSunChordsList = Arrays.asList(houseOfRisingSunChords.split("\n"));
        List<Integer> houseOfRisingSunDurations = Arrays.asList(12, 12, 12, 12, 12, 12, 12, 12);  // Duraciones de los acordes

        songs.add(new Song("House of the Rising Sun", "Traditional", 3, houseOfRisingSunLyricsList, houseOfRisingSunChordsList, houseOfRisingSunDurations, 78, false));

        // New song 1: Example Song
        String exampleSongLyrics = "This is the first verse\n" +
                "Here is the chorus line\n" +
                "Another verse appears\n" +
                "Ending with a chorus";

        String exampleSongChords = "      G        D        Em    \n" +
                "      C       G      D   \n" +
                "      G       D     Em    \n" +
                "      C        G    D    G";

        List<String> exampleSongLyricsList = Arrays.asList(exampleSongLyrics.split("\n"));
        List<String> exampleSongChordsList = Arrays.asList(exampleSongChords.split("\n"));
        List<Integer> exampleSongDurations = Arrays.asList(8, 8, 8, 8);

        songs.add(new Song("Example Song", "Artist Name", 2, exampleSongLyricsList, exampleSongChordsList, exampleSongDurations, 120, false));

        // New song 2: Another Song
        String anotherSongLyrics = "Verse 1 of the new song\n" +
                "Followed by the chorus\n" +
                "Another verse and\n" +
                "Finally a chorus";

        String anotherSongChords = "      C       F       G     \n" +
                "      C       F      G    \n" +
                "      C       F       G    \n" +
                "      C       F     G    C";

        List<String> anotherSongLyricsList = Arrays.asList(anotherSongLyrics.split("\n"));
        List<String> anotherSongChordsList = Arrays.asList(anotherSongChords.split("\n"));
        List<Integer> anotherSongDurations = Arrays.asList(10, 10, 10, 10);

        songs.add(new Song("Another Song", "Another Artist", 4, anotherSongLyricsList, anotherSongChordsList, anotherSongDurations, 100, true));

        // Retrieve favorite status from SharedPreferences
        SharedPreferences prefs = requireContext().getSharedPreferences("SongPreferences", Context.MODE_PRIVATE);

        // Loop to create additional songs with different names (you can keep this to create a large number of generic songs)
        for (int i = 1; i <= 100; i++) {
            String songTitle = "Song " + i;
            String songAuthor = "Anonymous " + i;
            boolean isFavorite = prefs.getBoolean(songTitle, false);

            songs.add(new Song(songTitle, songAuthor, i % 5 + 1, houseOfRisingSunLyricsList, houseOfRisingSunChordsList, houseOfRisingSunDurations, 78, isFavorite));
        }

        return songs;
    }





    private void updatePage() {
        int start = currentPage * SONGS_PER_PAGE;

        // Ensure the end index does not exceed the size of the filteredSongs list
        int end = Math.min(start + SONGS_PER_PAGE, filteredSongs.size());

        // Avoid invalid sublist ranges
        if (start <= end && start >= 0) {
            List<Song> paginatedSongs = filteredSongs.subList(start, end);
            adapter.updateSongs(paginatedSongs);

            pageIndicator.setText("Page " + (currentPage + 1) + " of " + ((filteredSongs.size() + SONGS_PER_PAGE - 1) / SONGS_PER_PAGE));

            prevPageButton.setEnabled(currentPage > 0);
            nextPageButton.setEnabled(end < filteredSongs.size());
        } else {
            Log.e(TAG, "Invalid pagination range: start=" + start + ", end=" + end);
        }
    }
}

