package com.tuguitar.todoacorde.songs.ui;

import android.content.Context;
import android.view.LayoutInflater;

import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.SwitchCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.tuguitar.todoacorde.R;
import com.tuguitar.todoacorde.songs.domain.PrefsKeys;

import java.util.Objects;

/**
 * Adapter para la cabecera de filtros y ordenación en SongFragment.
 */
public class HeaderAdapter extends RecyclerView.Adapter<HeaderAdapter.VH> {

    public interface FilterListener { void onFilterChanged(boolean showFav); }
    public interface SortListener   { void onSortChanged(String criterion, boolean asc); }

    private boolean showFavorites;
    private String  sortCriterion;
    private boolean ascending;
    private final FilterListener onFilterChanged;
    private final SortListener   onSortChanged;

    public HeaderAdapter(
            Context ctx,
            boolean initialShowFav,
            String  initialSortCrit,
            boolean initialAsc,
            FilterListener filterListener,
            SortListener   sortListener
    ) {
        this.showFavorites   = initialShowFav;
        this.sortCriterion   = initialSortCrit;
        this.ascending       = initialAsc;
        this.onFilterChanged = filterListener;
        this.onSortChanged   = sortListener;
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.partial_filters_row, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        h.filterSwitch.setChecked(showFavorites);
        h.filterSwitch.setOnCheckedChangeListener((btn, checked) -> {
            showFavorites = checked;
            onFilterChanged.onFilterChanged(checked);
        });
        int labelResId = Objects.equals(sortCriterion, PrefsKeys.VAL_SORT_TITLE)
                ? (ascending ? R.string.sort_title_asc : R.string.sort_title_desc)
                : (ascending ? R.string.sort_diff_asc  : R.string.sort_diff_desc);
        h.sortButton.setText(h.itemView.getContext().getString(labelResId));

        h.sortButton.setOnClickListener(v -> {
            PopupMenu menu = new PopupMenu(v.getContext(), v);
            menu.getMenuInflater().inflate(R.menu.menu_sort, menu.getMenu());
            menu.setOnMenuItemClickListener(item -> {
                int id = item.getItemId();
                if (id == R.id.sort_by_title_asc) {
                    sortCriterion = PrefsKeys.VAL_SORT_TITLE;
                    ascending     = true;
                } else if (id == R.id.sort_by_title_desc) {
                    sortCriterion = PrefsKeys.VAL_SORT_TITLE;
                    ascending     = false;
                } else if (id == R.id.sort_by_difficulty_asc) {
                    sortCriterion = PrefsKeys.VAL_SORT_DIFF;
                    ascending     = true;
                } else if (id == R.id.sort_by_difficulty_desc) {
                    sortCriterion = PrefsKeys.VAL_SORT_DIFF;
                    ascending     = false;
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

    @Override
    public int getItemCount() {
        return 1;
    }

    /**
     * Opcional: refleja en UI el número total de ítems.
     */
    public void updateCount(int count) {
        notifyItemChanged(0);
    }

    static class VH extends RecyclerView.ViewHolder {
        final SwitchCompat      filterSwitch;
        final AppCompatButton   sortButton;

        VH(@NonNull View itemView) {
            super(itemView);
            filterSwitch = itemView.findViewById(R.id.checkbox_favorites);
            sortButton   = itemView.findViewById(R.id.sort_button);
        }
    }
}
