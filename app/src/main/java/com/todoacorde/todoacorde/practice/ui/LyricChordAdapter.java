package com.todoacorde.todoacorde.practice.ui;

import android.graphics.Color;
import android.graphics.Typeface;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.todoacorde.todoacorde.R;
import com.todoacorde.todoacorde.practice.data.LineItem;
import com.todoacorde.todoacorde.practice.data.SpanInfo;

import java.util.List;
import java.util.Set;

/**
 * Adaptador para representar pares de líneas (acordes/letra) con acordes clicables
 * y realce de estado (acorde activo y acordes acertados).
 *
 * <p>Características principales:
 * <br>• Cada fila corresponde a un {@link LineItem} con dos TextViews: línea de acordes y de letra.
 * <br>• Los rangos de la línea de acordes definidos por {@link SpanInfo} se vuelven clicables.
 * <br>• Se resalta visualmente el acorde “activo” y los “acertados”.</p>
 *
 * <p>Uso: establecer opcionalmente un {@link OnChordClickListener} para recibir los clics
 * por índice global, actualizar el acorde activo mediante
 * {@link #setActiveChordGlobalIndex(int)} y el conjunto de índices correctos con
 * {@link #setCorrectIndices(Set)}.</p>
 */
public class LyricChordAdapter extends ListAdapter<LineItem, LyricChordAdapter.ViewHolder> {

    /* Índice global de acorde actualmente activo; -1 si ninguno */
    private int activeChordGlobalIndex = -1;

    /* Primer índice global que aparece en cada línea (para localizar rápidamente la fila de un índice) */
    private int[] firstGlobalIndexByLine = new int[0];

    /**
     * Callback para clic en acorde.
     */
    public interface OnChordClickListener {
        /**
         * Notifica el clic sobre un acorde identificado por su índice global.
         *
         * @param globalIndex índice global del acorde clicado
         */
        void onChordClick(int globalIndex);
    }

    /* Listener opcional de clic de acordes */
    private OnChordClickListener chordClickListener;

    /* Conjunto de índices globales marcados como “acertados” */
    private Set<Integer> correctIndices;

    /**
     * Crea el adaptador con su {@link DiffUtil.ItemCallback} por defecto.
     */
    public LyricChordAdapter() {
        super(DIFF_CALLBACK);
    }

    /**
     * Registra un listener para clics de acordes.
     *
     * @param listener implementación del callback; puede ser null para desactivar
     */
    public void setOnChordClickListener(OnChordClickListener listener) {
        this.chordClickListener = listener;
    }

    /**
     * Actualiza el conjunto de índices globales considerados “acertados”.
     * Fuerza un refresco de la lista para aplicar el estilo.
     *
     * @param correctIndices conjunto de índices acertados (puede ser null)
     */
    public void setCorrectIndices(Set<Integer> correctIndices) {
        this.correctIndices = correctIndices;
        notifyDataSetChanged();
    }

    /**
     * Sobrescribe la lista y recalcula el vector auxiliar con el primer índice global por línea.
     *
     * @param list nueva lista de {@link LineItem}
     */
    @Override
    public void submitList(List<LineItem> list) {
        super.submitList(list);
        if (list == null) {
            firstGlobalIndexByLine = new int[0];
        } else {
            firstGlobalIndexByLine = new int[list.size()];
            for (int i = 0; i < list.size(); i++) {
                LineItem li = list.get(i);
                int minStart = Integer.MAX_VALUE;
                int global = -1;
                for (SpanInfo span : li.spans) {
                    if (span.start < minStart) {
                        minStart = span.start;
                        global = span.globalIndex;
                    }
                }
                firstGlobalIndexByLine[i] = global;
            }
        }
        notifyDataSetChanged();
    }

    /**
     * Establece el acorde activo por índice global y solicita refrescar
     * únicamente las filas afectadas (la anterior y la nueva).
     *
     * @param idx índice global del acorde activo
     */
    public void setActiveChordGlobalIndex(int idx) {
        int old = activeChordGlobalIndex;
        activeChordGlobalIndex = idx;
        int oldLine = findLineForGlobal(old);
        int newLine = findLineForGlobal(idx);
        if (oldLine != RecyclerView.NO_POSITION) notifyItemChanged(oldLine);
        if (newLine != RecyclerView.NO_POSITION) notifyItemChanged(newLine);
    }

    /**
     * Localiza la fila que contiene un índice global de acorde.
     *
     * @param globalIdx índice global
     * @return posición de fila en el RecyclerView o {@link RecyclerView#NO_POSITION} si no se encuentra
     */
    private int findLineForGlobal(int globalIdx) {
        if (firstGlobalIndexByLine == null) return RecyclerView.NO_POSITION;
        for (int i = 0; i < firstGlobalIndexByLine.length; i++) {
            int start = firstGlobalIndexByLine[i];
            int next = (i + 1 < firstGlobalIndexByLine.length)
                    ? firstGlobalIndexByLine[i + 1] : Integer.MAX_VALUE;
            if (globalIdx >= start && globalIdx < next) return i;
        }
        return RecyclerView.NO_POSITION;
    }

    /**
     * Restablece el estado visual: desactiva acorde activo y limpia aciertos.
     */
    public void resetVisualState() {
        activeChordGlobalIndex = -1;
        if (correctIndices != null) {
            correctIndices.clear();
        }
        notifyDataSetChanged();
    }

    /**
     * Infla la vista de fila.
     */
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_lyric_chord, parent, false);
        return new ViewHolder(v);
    }

    /**
     * Enlaza los datos de un {@link LineItem} y aplica spans clicables/estilos.
     */
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int pos) {
        LineItem li = getItem(pos);
        SpannableString spannable = new SpannableString(li.chordLine);

        /* Configura spans para cada acorde de la línea */
        for (SpanInfo span : li.spans) {
            int gIdx = span.globalIndex;

            /* Span clicable para notificar al listener */
            spannable.setSpan(new ClickableSpan() {
                @Override
                public void onClick(@NonNull View widget) {
                    if (chordClickListener != null) chordClickListener.onChordClick(gIdx);
                }
            }, span.start, span.end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

            /* Estilo para acorde activo */
            if (gIdx == activeChordGlobalIndex) {
                spannable.setSpan(new StyleSpan(Typeface.BOLD),
                        span.start, span.end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                spannable.setSpan(new ForegroundColorSpan(Color.parseColor("#3E2723")),
                        span.start, span.end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

            /* Estilo para acordes acertados */
            if (correctIndices != null && correctIndices.contains(gIdx)) {
                spannable.setSpan(new ForegroundColorSpan(Color.GREEN),
                        span.start, span.end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }

        holder.chordLine.setText(spannable);
        holder.chordLine.setMovementMethod(LinkMovementMethod.getInstance());
        holder.chordLine.setHighlightColor(0x00000000);
        holder.lyricLine.setText(li.lyricLine);
    }

    /**
     * ViewHolder con referencias a las líneas de acordes y de letra.
     */
    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView chordLine, lyricLine;

        ViewHolder(View itemView) {
            super(itemView);
            chordLine = itemView.findViewById(R.id.tvChordLine);
            lyricLine = itemView.findViewById(R.id.tvLyricLine);
            chordLine.setMovementMethod(LinkMovementMethod.getInstance());
        }
    }

    /**
     * Estrategia de DiffUtil para optimizar actualizaciones del RecyclerView.
     */
    private static final DiffUtil.ItemCallback<LineItem> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<LineItem>() {
                @Override
                public boolean areItemsTheSame(@NonNull LineItem a, @NonNull LineItem b) {
                    return a.lyricLine.equals(b.lyricLine)
                            && a.chordLine.equals(b.chordLine);
                }

                @Override
                public boolean areContentsTheSame(@NonNull LineItem a, @NonNull LineItem b) {
                    return a.equals(b);
                }
            };
}
