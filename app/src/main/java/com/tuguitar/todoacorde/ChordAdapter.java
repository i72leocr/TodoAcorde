package com.tuguitar.todoacorde;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class ChordAdapter extends RecyclerView.Adapter<ChordAdapter.ChordViewHolder> {

    private List<String> chords;
    private OnItemClickListener listener;

    public ChordAdapter(List<String> chords) {
        this.chords = chords;
    }

    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ChordViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chord_type, parent, false);
        return new ChordViewHolder(view, listener);  // Pass the listener to the ViewHolder
    }

    @Override
    public void onBindViewHolder(@NonNull ChordViewHolder holder, int position) {
        String chord = chords.get(position);
        holder.chordName.setText(chord);
    }

    @Override
    public int getItemCount() {
        return chords.size();
    }

    public static class ChordViewHolder extends RecyclerView.ViewHolder {
        public TextView chordName;

        public ChordViewHolder(@NonNull View itemView, final OnItemClickListener listener) {
            super(itemView);
            chordName = itemView.findViewById(R.id.chord_name);
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (listener != null) {
                        int position = getAdapterPosition();
                        if (position != RecyclerView.NO_POSITION) {
                            listener.onItemClick(position);
                        }
                    }
                }
            });
        }
    }
}
