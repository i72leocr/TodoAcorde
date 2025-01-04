package com.tuguitar.todoacorde;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import java.util.List;

public class SongAdapter extends BaseAdapter {
    private Context context;
    private List<Song> songs;
    private SharedPreferences sharedPreferences;
    private static final String TAG = "SongAdapter";

    public SongAdapter(Context context, List<Song> songs) {
        this.context = context;
        this.songs = songs;
        this.sharedPreferences = context.getSharedPreferences("SongPreferences", Context.MODE_PRIVATE);
    }

    @Override
    public int getCount() {
        return songs.size();
    }

    @Override
    public Object getItem(int position) {
        return songs.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.list_item_song, parent, false);
        }

        TextView titleTextView = convertView.findViewById(R.id.song_title);
        TextView authorTextView = convertView.findViewById(R.id.song_author);
        TextView difficultyTextView = convertView.findViewById(R.id.song_difficulty);
        ImageView favoriteIcon = convertView.findViewById(R.id.favorite_icon);

        Song song = songs.get(position);
        titleTextView.setText(song.getTitle());
        authorTextView.setText(song.getAuthor());

        // Restaurar el estado de favorito desde SharedPreferences para cada canción
        boolean isFavorite = sharedPreferences.getBoolean(song.getTitle(), false);
        song.setFavorite(isFavorite);

        Log.d(TAG, "Restaurado estado favorito para canción: " + song.getTitle() + " -> " + isFavorite);

        // Actualizar el ícono del corazón en función del estado de favorito de la canción
        updateFavoriteIcon(favoriteIcon, song.isFavorite());

        // Alternar el estado de favorito cuando se hace clic en el ícono del corazón
        favoriteIcon.setOnClickListener(v -> {
            boolean newFavoriteStatus = !song.isFavorite();
            song.setFavorite(newFavoriteStatus);
            saveFavoriteStatus(song);
            updateFavoriteIcon(favoriteIcon, newFavoriteStatus);
            Log.d(TAG, "Nuevo estado favorito guardado para canción: " + song.getTitle() + " -> " + newFavoriteStatus);
        });


        // Set difficulty text as Alta, Media, Baja based on difficulty level
        switch (song.getDifficulty()) {
            case 3:
                difficultyTextView.setText("Alta");
                break;
            case 2:
                difficultyTextView.setText("Media");
                break;
            case 1:
            default:
                difficultyTextView.setText("Baja");
                break;
        }

        return convertView;
    }

    // Método auxiliar para actualizar el ícono de favorito
    private void updateFavoriteIcon(ImageView favoriteIcon, boolean isFavorite) {
        if (isFavorite) {
            favoriteIcon.setImageResource(android.R.drawable.btn_star_big_on); // Usar ícono de corazón lleno
        } else {
            favoriteIcon.setImageResource(android.R.drawable.btn_star_big_off); // Usar ícono de corazón vacío
        }
    }

    // Método auxiliar para configurar la visibilidad de las estrellas según la dificultad
    private void setStarVisibility(ImageView star1, ImageView star2, ImageView star3, int difficulty) {
        star1.setVisibility(difficulty >= 1 ? View.VISIBLE : View.GONE);
        star2.setVisibility(difficulty >= 2 ? View.VISIBLE : View.GONE);
        star3.setVisibility(difficulty >= 3 ? View.VISIBLE : View.GONE);
    }

    // Guardar el estado de favorito en SharedPreferences para cada canción
    private void saveFavoriteStatus(Song song) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(song.getTitle(), song.isFavorite());  // Almacenar el estado de favorito por título de la canción
        editor.apply();
        Log.d(TAG, "Guardado estado favorito para canción: " + song.getTitle() + " -> " + song.isFavorite());
    }

    // Método para actualizar la lista de canciones en el adaptador
    public void updateSongs(List<Song> newSongs) {
        this.songs = newSongs;
        notifyDataSetChanged();
        Log.d(TAG, "Lista de canciones actualizada. Total canciones: " + newSongs.size());
    }
}
