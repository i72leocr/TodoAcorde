// SongRecyclerAdapter.java
package com.tuguitar.todoacorde;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class SongRecyclerAdapter extends RecyclerView.Adapter<SongRecyclerAdapter.VH> {
    private static final String TAG = "SongAdapter";

    public interface OnSongClick {
        void onSongClick(Song song);
    }

    public interface OnFavoriteToggleListener {
        void onFavoriteToggled(Song song);
    }

    /** Ahora mantenemos nuestra propia copia de la lista */
    private final List<Song> songs;
    private final OnSongClick clickListener;
    private final OnFavoriteToggleListener favoriteToggleListener;
    private final FavoriteSongDao favoriteDao;
    private final int userId = 1;

    public SongRecyclerAdapter(List<Song> initialSongs,
                               OnSongClick clickListener,
                               OnFavoriteToggleListener favoriteToggleListener,
                               Context context) {
        // Creamos una copia para no compartir la misma instancia con filteredSongs
        this.songs = new ArrayList<>(initialSongs);
        this.clickListener = clickListener;
        this.favoriteToggleListener = favoriteToggleListener;
        this.favoriteDao = todoAcordeDatabase
                .getInstance(context)
                .favoriteSongDao();
        Log.d(TAG, "Adapter created, initial songs list size = " + this.songs.size());
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Log.d(TAG, "onCreateViewHolder(viewType=" + viewType + ")");
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_item_song, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Log.d(TAG, "onBindViewHolder(position=" + position + ")");
        Song song = songs.get(position);
        h.title.setText(song.getTitle());
        h.author.setText(song.getAuthor());
        String diffLabel;
        switch (song.getDifficulty()) {
            case 1: diffLabel = "Fácil"; break;
            case 2: diffLabel = "Medio"; break;
            case 3: diffLabel = "Difícil"; break;
            default: diffLabel = String.valueOf(song.getDifficulty());
        }
        h.difficulty.setText("Dif: " + diffLabel);
        h.star.setImageResource(
                song.isFavorite()
                        ? R.drawable.heart_solid_full
                        : R.drawable.heart_regular_full
        );

        h.itemView.setOnClickListener(v -> clickListener.onSongClick(song));
        h.star.setOnClickListener(v -> {
            boolean nextFav = !song.isFavorite();
            song.setFavorite(nextFav);
            Log.d(TAG, "Toggling favorite for id=" + song.getId() + " to " + nextFav);
            favoriteToggleListener.onFavoriteToggled(song);
            notifyItemChanged(position);
            Executors.newSingleThreadExecutor().execute(() -> {
                if (nextFav) {
                    favoriteDao.insert(new FavoriteSong(userId, song.getId()));
                } else {
                    favoriteDao.delete(new FavoriteSong(userId, song.getId()));
                }
            });
        });
    }

    @Override public int getItemCount() {
        int count = songs.size();
        Log.d(TAG, "getItemCount() = " + count);
        return count;
    }

    public static class VH extends RecyclerView.ViewHolder {
        final TextView title, author, difficulty;
        final ImageButton star;
        public VH(View itemView) {
            super(itemView);
            title      = itemView.findViewById(R.id.song_title);
            author     = itemView.findViewById(R.id.song_author);
            difficulty = itemView.findViewById(R.id.song_difficulty);
            star       = itemView.findViewById(R.id.song_favorite);
        }
    }

    /** Actualiza la lista de canciones mostradas y refresca la vista. */
    public void setSongs(List<Song> newSongs) {
        Log.d(TAG, "setSongs(newSize=" + newSongs.size() + ")");
        this.songs.clear();
        this.songs.addAll(newSongs);
        notifyDataSetChanged();
    }
}
