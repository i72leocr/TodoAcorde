package com.todoacorde.todoacorde.songs.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.todoacorde.todoacorde.R;
import com.todoacorde.todoacorde.songs.data.Song;

import java.util.Map;

/**
 * Adaptador para la lista de canciones basado en {@link ListAdapter} con DiffUtil.
 * - Muestra título, autor, dificultad y un botón de favorito.
 * - Expone callbacks para clic en elemento y para alternar favorito.
 */
public class SongAdapter extends ListAdapter<Song, SongAdapter.SongVH> {

    /** Callback cuando se pulsa una canción. */
    public interface OnSongClickListener {
        void onSongClick(Song song);
    }

    /** Callback cuando se pulsa el icono de favorito. */
    public interface OnFavoriteClickListener {
        void onFavoriteClick(Song song);
    }

    /** Listener para clic en fila. */
    private final OnSongClickListener onClick;
    /** Listener para alternar favorito. */
    private final OnFavoriteClickListener onFavoriteToggle;
    /** Mapa de id de dificultad → texto descriptivo. */
    private Map<Integer, String> difficultyMap;

    /**
     * Crea el adaptador con sus listeners.
     */
    public SongAdapter(OnSongClickListener onClick,
                       OnFavoriteClickListener onFavoriteToggle) {
        super(new DiffUtil.ItemCallback<Song>() {
            @Override
            public boolean areItemsTheSame(@NonNull Song a, @NonNull Song b) {
                return a.getId() == b.getId();
            }
            @Override
            public boolean areContentsTheSame(@NonNull Song a, @NonNull Song b) {
                return a.getTitle().equals(b.getTitle())
                        && a.getAuthor().equals(b.getAuthor())
                        && a.getDifficulty() == b.getDifficulty()
                        && a.isFavorite() == b.isFavorite();
            }
        });
        this.onClick = onClick;
        this.onFavoriteToggle = onFavoriteToggle;
    }

    /**
     * Inyecta el mapa de dificultades (id → etiqueta) y refresca el listado.
     */
    public void setDifficultyMap(Map<Integer, String> map) {
        this.difficultyMap = map;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public SongVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_item_song, parent, false);
        return new SongVH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull SongVH holder, int position) {
        Song song = getItem(position);
        holder.bind(song);
    }

    /**
     * ViewHolder de una fila de canción.
     * Contiene referencias a vistas y gestiona los clics.
     */
    class SongVH extends RecyclerView.ViewHolder {
        final TextView tvTitle;
        final TextView tvAuthor;
        final TextView tvDifficulty;
        final ImageButton btnFavorite;

        SongVH(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.song_title);
            tvAuthor = itemView.findViewById(R.id.song_author);
            tvDifficulty = itemView.findViewById(R.id.song_difficulty);
            btnFavorite = itemView.findViewById(R.id.song_favorite);

            // Clic en toda la fila → detalle de canción
            itemView.setOnClickListener(v -> {
                int pos = getBindingAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) {
                    onClick.onSongClick(getItem(pos));
                }
            });

            // Clic en el icono de favorito → alternar estado
            btnFavorite.setOnClickListener(v -> {
                int pos = getBindingAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) {
                    onFavoriteToggle.onFavoriteClick(getItem(pos));
                }
            });
        }

        /**
         * Enlaza datos de Song con las vistas.
         */
        void bind(Song song) {
            tvTitle.setText(song.getTitle());
            tvAuthor.setText(song.getAuthor());

            // Etiqueta de dificultad: usa el mapa si está disponible
            String difficultyLabel = (difficultyMap != null)
                    ? difficultyMap.get(song.getDifficulty())
                    : String.valueOf(song.getDifficulty());
            tvDifficulty.setText("Dif: " + difficultyLabel);

            // Icono de favorito según estado
            btnFavorite.setImageResource(
                    song.isFavorite()
                            ? R.drawable.heart_solid_full
                            : R.drawable.heart_regular_full
            );
        }
    }
}
