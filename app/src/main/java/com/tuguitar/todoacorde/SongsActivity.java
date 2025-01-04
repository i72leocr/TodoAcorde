/*
package com.tuguitar.todoacorde;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SearchView;
import com.google.android.material.navigation.NavigationView;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SongsActivity extends BaseActivity {
    private List<Song> songs;
    private SongAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set the specific content for this activity
        setContentView(R.layout.activity_songs);
        setActivityContent(R.layout.activity_songs);

        // Get the list of songs
        songs = getSongs();

        // Set up the adapter
        adapter = new SongAdapter(this, songs);

        // Get the ListView and assign the adapter
        ListView listView = findViewById(R.id.song_list);
        listView.setAdapter(adapter);

        // Set up the SearchView to filter the list of songs
        SearchView searchView = findViewById(R.id.searchView);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false; // We don't need to do anything on submit
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterSongs(newText);
                return true;
            }
        });

        // Set up the click listener for list items
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // Get the selected song
                Song selectedSong = songs.get(position);

                // Start PracticeChordsActivity with the "SONG" mode
                Intent intent = new Intent(SongsActivity.this, PracticeChordsActivity.class);
                intent.putExtra("ACCESS_MODE", "SONG");
                // You can pass additional data if needed, such as the song name or chords
                startActivity(intent);
            }
        });
    }

    private List<Song> getSongs() {
        // Create a list with example songs
        List<Song> songs = new ArrayList<>();
        songs.add(new Song("House of the Rising Sun - The Animals", 1)); // 1-star difficulty
        songs.add(new Song("Another Song", 2)); // Additional example
        return songs;
    }

    private void filterSongs(String query) {
        List<Song> filteredSongs = songs.stream()
                .filter(song -> song.getTitle().toLowerCase().contains(query.toLowerCase()))
                .collect(Collectors.toList());

        adapter.updateSongs(filteredSongs);
    }
}
*/