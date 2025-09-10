package com.todoacorde.todoacorde;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

/**
 * Adaptador de {@link RecyclerView} para mostrar una lista de nombres de acordes (String).
 * Cada ítem se representa mediante el layout {@code R.layout.item_chord_type}.
 *
 * Funcionamiento:
 * - Recibe una lista inmutable de cadenas con los nombres de los acordes.
 * - Expone una interfaz {@link OnItemClickListener} para notificar pulsaciones en los ítems.
 */
public class ChordAdapter extends RecyclerView.Adapter<ChordAdapter.ChordViewHolder> {

    /** Lista de nombres de acordes a renderizar. */
    private final List<String> chords;

    /** Listener opcional para manejar clics sobre los ítems. */
    private OnItemClickListener listener;

    /**
     * Crea el adaptador con la lista de acordes a mostrar.
     *
     * @param chords lista de nombres de acordes.
     */
    public ChordAdapter(List<String> chords) {
        this.chords = chords;
    }

    /**
     * Interfaz para notificar el clic de un ítem al exterior.
     */
    public interface OnItemClickListener {
        /**
         * Notificación de clic sobre la posición indicada.
         *
         * @param position posición del ítem clicado en el adaptador.
         */
        void onItemClick(int position);
    }

    /**
     * Establece el listener para los clics de ítem.
     *
     * @param listener implementación de {@link OnItemClickListener}.
     */
    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ChordViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Infla la vista del ítem a partir del layout.
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_chord_type, parent, false);
        return new ChordViewHolder(view, listener);
    }

    @Override
    public void onBindViewHolder(@NonNull ChordViewHolder holder, int position) {
        // Enlaza el nombre del acorde a la vista del ViewHolder.
        String chord = chords.get(position);
        holder.chordName.setText(chord);
    }

    @Override
    public int getItemCount() {
        return chords.size();
    }

    /**
     * ViewHolder que mantiene referencias a las vistas de cada ítem y
     * gestiona el clic para notificarlo mediante el listener.
     */
    public static class ChordViewHolder extends RecyclerView.ViewHolder {
        /** TextView que muestra el nombre del acorde. */
        public TextView chordName;

        /**
         * Construye el ViewHolder y registra el manejador de clics.
         *
         * @param itemView vista raíz del ítem.
         * @param listener listener de clics (puede ser null).
         */
        public ChordViewHolder(@NonNull View itemView, final OnItemClickListener listener) {
            super(itemView);
            chordName = itemView.findViewById(R.id.chord_name);

            // Manejo de clic sobre todo el ítem.
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
