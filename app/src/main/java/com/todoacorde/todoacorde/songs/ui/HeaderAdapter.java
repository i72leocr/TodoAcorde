package com.todoacorde.todoacorde.songs.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.SwitchCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.todoacorde.todoacorde.R;
import com.todoacorde.todoacorde.songs.domain.PrefsKeys;

import java.util.Objects;

/**
 * Adaptador de cabecera para la lista de canciones.
 * Muestra y gestiona los controles de filtrado por favoritos y ordenación.
 *
 * Estado que mantiene:
 * - showFavorites: si se muestran solo favoritas.
 * - sortCriterion: criterio de ordenación (ver {@link PrefsKeys}).
 * - ascending: sentido de ordenación ascendente o descendente.
 *
 * Notifica cambios a través de {@link FilterListener} y {@link SortListener}.
 * Diseñado para renderizar una única fila de cabecera.
 */
public class HeaderAdapter extends RecyclerView.Adapter<HeaderAdapter.VH> {

    /**
     * Callback cuando cambia el filtro de favoritos.
     */
    public interface FilterListener {
        void onFilterChanged(boolean showFav);
    }

    /**
     * Callback cuando cambia el criterio o sentido de ordenación.
     */
    public interface SortListener {
        void onSortChanged(String criterion, boolean asc);
    }

    /** Flag: mostrar solo favoritas. */
    private boolean showFavorites;
    /** Criterio de ordenación actual. */
    private String sortCriterion;
    /** Sentido de ordenación: true si ascendente. */
    private boolean ascending;

    /** Listener para cambios de filtro. */
    private final FilterListener onFilterChanged;
    /** Listener para cambios de ordenación. */
    private final SortListener onSortChanged;

    /**
     * Crea el adaptador de cabecera con el estado inicial y los listeners.
     *
     * @param ctx              contexto.
     * @param initialShowFav   estado inicial del filtro de favoritas.
     * @param initialSortCrit  criterio de ordenación inicial.
     * @param initialAsc       sentido inicial de ordenación.
     * @param filterListener   receptor de cambios de filtro.
     * @param sortListener     receptor de cambios de ordenación.
     */
    public HeaderAdapter(
            Context ctx,
            boolean initialShowFav,
            String initialSortCrit,
            boolean initialAsc,
            FilterListener filterListener,
            SortListener sortListener
    ) {
        this.showFavorites = initialShowFav;
        this.sortCriterion = initialSortCrit;
        this.ascending = initialAsc;
        this.onFilterChanged = filterListener;
        this.onSortChanged = sortListener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.partial_filters_row, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        // Filtro por favoritas
        h.filterSwitch.setChecked(showFavorites);
        h.filterSwitch.setOnCheckedChangeListener((btn, checked) -> {
            showFavorites = checked;
            onFilterChanged.onFilterChanged(checked);
        });

        // Etiqueta del botón de ordenación según criterio y sentido
        int labelResId = Objects.equals(sortCriterion, PrefsKeys.VAL_SORT_TITLE)
                ? (ascending ? R.string.sort_title_asc : R.string.sort_title_desc)
                : (ascending ? R.string.sort_diff_asc : R.string.sort_diff_desc);
        h.sortButton.setText(h.itemView.getContext().getString(labelResId));

        // Menú emergente para selección de ordenación
        h.sortButton.setOnClickListener(v -> {
            PopupMenu menu = new PopupMenu(v.getContext(), v);
            menu.getMenuInflater().inflate(R.menu.menu_sort, menu.getMenu());
            menu.setOnMenuItemClickListener(item -> {
                int id = item.getItemId();
                if (id == R.id.sort_by_title_asc) {
                    sortCriterion = PrefsKeys.VAL_SORT_TITLE;
                    ascending = true;
                } else if (id == R.id.sort_by_title_desc) {
                    sortCriterion = PrefsKeys.VAL_SORT_TITLE;
                    ascending = false;
                } else if (id == R.id.sort_by_difficulty_asc) {
                    sortCriterion = PrefsKeys.VAL_SORT_DIFF;
                    ascending = true;
                } else if (id == R.id.sort_by_difficulty_desc) {
                    sortCriterion = PrefsKeys.VAL_SORT_DIFF;
                    ascending = false;
                } else {
                    return false;
                }
                onSortChanged.onSortChanged(sortCriterion, ascending);
                notifyItemChanged(0);
                return true;
            });
            menu.show();
        });
    }

    /**
     * Este adaptador representa una única fila de cabecera.
     */
    @Override
    public int getItemCount() {
        return 1;
    }

    /**
     * Permite refrescar la fila cuando cambie el total mostrado en la lista principal.
     * El parámetro se deja por compatibilidad con llamados externos.
     *
     * @param count total actual (no usado para el render de esta cabecera).
     */
    public void updateCount(int count) {
        notifyItemChanged(0);
    }

    /**
     * ViewHolder de la cabecera con los controles de filtro y ordenación.
     */
    static class VH extends RecyclerView.ViewHolder {
        final SwitchCompat filterSwitch;
        final AppCompatButton sortButton;

        VH(@NonNull View itemView) {
            super(itemView);
            filterSwitch = itemView.findViewById(R.id.checkbox_favorites);
            sortButton = itemView.findViewById(R.id.sort_button);
        }
    }
}
