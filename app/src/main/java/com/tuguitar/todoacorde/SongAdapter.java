package com.tuguitar.todoacorde;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import java.util.function.Consumer;

/**
 * Adapter para la lista de canciones.
 * Usa ListAdapter + DiffUtil para actualizaciones eficientes.
 */
public class SongAdapter extends ListAdapter<Song, SongAdapter.SongVH> {
    private final Context context;
    private final Consumer<Song> onClick;
    private final Consumer<Song> onFavoriteToggle;

    public SongAdapter(Context context,
                       Consumer<Song> onClick,
                       Consumer<Song> onFavoriteToggle) {
        super(new DiffUtil.ItemCallback<Song>() {
            @Override public boolean areItemsTheSame(@NonNull Song a, @NonNull Song b) {
                return a.getId() == b.getId();
            }
            @Override public boolean areContentsTheSame(@NonNull Song a, @NonNull Song b) {
                return a.equals(b);
            }
        });
        this.context = context;
        this.onClick = onClick;
        this.onFavoriteToggle = onFavoriteToggle;
    }

    @NonNull @Override
    public SongVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context)
                .inflate(R.layout.list_item_song, parent, false);
        return new SongVH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull SongVH holder, int position) {
        Song song = getItem(position);
        holder.bind(song);
    }

    class SongVH extends RecyclerView.ViewHolder {
        final TextView tvTitle, tvAuthor, tvDifficulty;
        final ImageButton btnFavorite;

        SongVH(@NonNull View itemView) {
            super(itemView);
            tvTitle      = itemView.findViewById(R.id.song_title);
            tvAuthor     = itemView.findViewById(R.id.song_author);
            tvDifficulty = itemView.findViewById(R.id.song_difficulty);
            btnFavorite  = itemView.findViewById(R.id.song_favorite);

            itemView.setOnClickListener(v -> onClick.accept(getItem(getAdapterPosition())));
            btnFavorite.setOnClickListener(v -> onFavoriteToggle.accept(getItem(getAdapterPosition())));
        }

        void bind(Song song) {
            tvTitle.setText(song.getTitle());
            tvAuthor.setText(song.getAuthor());
            tvDifficulty.setText(String.valueOf(song.getDifficulty()));
            btnFavorite.setImageResource(
                    song.isFavorite()
                            ? R.drawable.heart_solid_full
                            : R.drawable.heart_regular_full
            );
        }
    }
}
