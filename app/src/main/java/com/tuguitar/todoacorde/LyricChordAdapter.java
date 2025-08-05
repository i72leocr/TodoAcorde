package com.tuguitar.todoacorde;

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

import java.util.List;
import java.util.Set;

/**
 * Adapter que muestra líneas de acordes y letra, resalta acorde activo,
 * permite pulsar sobre spans de acordes y pinta de verde los acordes acertados.
 */
public class LyricChordAdapter
        extends ListAdapter<LineItem, LyricChordAdapter.ViewHolder> {

    /** Índice global del acorde activo. */
    private int activeChordGlobalIndex = -1;
    /** Primer índice global por línea (recalculado en submitList). */
    private int[] firstGlobalIndexByLine = new int[0];
    /** Listener para clicks en acordes. */
    public interface OnChordClickListener { void onChordClick(int globalIndex); }
    private OnChordClickListener chordClickListener;

    /** Conjunto de índices de acordes acertados. */
    private Set<Integer> correctIndices;

    public LyricChordAdapter() {
        super(DIFF_CALLBACK);
    }

    /** Set listener para clicks en acordes. */
    public void setOnChordClickListener(OnChordClickListener listener) {
        this.chordClickListener = listener;
    }

    /** Set del conjunto de índices acertados y refresca todo. */
    public void setCorrectIndices(Set<Integer> correctIndices) {
        this.correctIndices = correctIndices;
        notifyDataSetChanged();
    }

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

    /** Actualiza acorde activo y refresca líneas afectadas. */
    public void setActiveChordGlobalIndex(int idx) {
        int old = activeChordGlobalIndex;
        activeChordGlobalIndex = idx;
        int oldLine = findLineForGlobal(old);
        int newLine = findLineForGlobal(idx);
        if (oldLine != RecyclerView.NO_POSITION) notifyItemChanged(oldLine);
        if (newLine != RecyclerView.NO_POSITION) notifyItemChanged(newLine);
    }

    /** Encuentra línea que contiene el índice global. */
    private int findLineForGlobal(int globalIdx) {
        if (firstGlobalIndexByLine == null) return RecyclerView.NO_POSITION;
        for (int i = 0; i < firstGlobalIndexByLine.length; i++) {
            int start = firstGlobalIndexByLine[i];
            int next = (i+1 < firstGlobalIndexByLine.length)
                    ? firstGlobalIndexByLine[i+1] : Integer.MAX_VALUE;
            if (globalIdx >= start && globalIdx < next) return i;
        }
        return RecyclerView.NO_POSITION;
    }

    @NonNull @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_lyric_chord, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int pos) {
        LineItem li = getItem(pos);
        SpannableString spannable = new SpannableString(li.chordLine);

        for (SpanInfo span : li.spans) {
            int gIdx = span.globalIndex;
            // click
            spannable.setSpan(new ClickableSpan() {
                @Override public void onClick(@NonNull View widget) {
                    if (chordClickListener != null) chordClickListener.onChordClick(gIdx);
                }
            }, span.start, span.end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

            // bold si es activo
            if (gIdx == activeChordGlobalIndex) {
                spannable.setSpan(
                        new StyleSpan(Typeface.BOLD),
                        span.start, span.end,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                );
            }

            // verde si está en correctIndices
            if (correctIndices != null && correctIndices.contains(gIdx)) {
                spannable.setSpan(
                        new ForegroundColorSpan(Color.GREEN),
                        span.start, span.end,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                );
            }
        }

        holder.chordLine.setText(spannable);
        holder.chordLine.setMovementMethod(LinkMovementMethod.getInstance());
        holder.chordLine.setHighlightColor(0x00000000);
        holder.lyricLine.setText(li.lyricLine);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView chordLine, lyricLine;
        ViewHolder(View itemView) {
            super(itemView);
            chordLine = itemView.findViewById(R.id.tvChordLine);
            lyricLine = itemView.findViewById(R.id.tvLyricLine);
            chordLine.setMovementMethod(LinkMovementMethod.getInstance());
        }
    }

    private static final DiffUtil.ItemCallback<LineItem> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<LineItem>() {
                @Override public boolean areItemsTheSame(@NonNull LineItem a, @NonNull LineItem b) {
                    return a.lyricLine.equals(b.lyricLine)
                            && a.chordLine.equals(b.chordLine);
                }
                @Override public boolean areContentsTheSame(@NonNull LineItem a, @NonNull LineItem b) {
                    return a.equals(b);
                }
            };
}
