package com.tuguitar.todoacorde.songs.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.tuguitar.todoacorde.R;
import com.tuguitar.todoacorde.songs.data.Song;

import java.util.Map;

public class SongAdapter extends ListAdapter<Song, SongAdapter.SongVH> {
    private OnSongClickListener onClick;
    private OnFavoriteClickListener onFavoriteToggle;
    private Map<Integer, String> difficultyMap;

    public interface OnSongClickListener {
        void onSongClick(Song song);
    }

    public interface OnFavoriteClickListener {
        void onFavoriteClick(Song song);
    }

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

    /** Setter para el Map de dificultades */
    public void setDifficultyMap(Map<Integer, String> map) {
        this.difficultyMap = map;
        notifyDataSetChanged(); // forzar refresco
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

    class SongVH extends RecyclerView.ViewHolder {
        final TextView tvTitle, tvAuthor, tvDifficulty;
        final ImageButton btnFavorite;

        SongVH(@NonNull View itemView) {
            super(itemView);
            tvTitle      = itemView.findViewById(R.id.song_title);
            tvAuthor     = itemView.findViewById(R.id.song_author);
            tvDifficulty = itemView.findViewById(R.id.song_difficulty);
            btnFavorite  = itemView.findViewById(R.id.song_favorite);

            itemView.setOnClickListener(v -> {
                int pos = getBindingAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) {
                    onClick.onSongClick(getItem(pos));
                }
            });
            btnFavorite.setOnClickListener(v -> {
                int pos = getBindingAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) {
                    onFavoriteToggle.onFavoriteClick(getItem(pos));
                }
            });
        }

        void bind(Song song) {
            tvTitle.setText(song.getTitle());
            tvAuthor.setText(song.getAuthor());

            String difficultyLabel = difficultyMap != null
                    ? difficultyMap.get(song.getDifficulty())
                    : String.valueOf(song.getDifficulty());
            tvDifficulty.setText("Dif: " + difficultyLabel);

            btnFavorite.setImageResource(
                    song.isFavorite()
                            ? R.drawable.heart_solid_full
                            : R.drawable.heart_regular_full
            );
        }
    }
}
