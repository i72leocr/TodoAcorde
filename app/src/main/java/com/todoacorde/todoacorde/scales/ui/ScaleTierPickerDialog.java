package com.todoacorde.todoacorde.scales.ui;

import android.app.Dialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.todoacorde.todoacorde.R;
import com.todoacorde.todoacorde.scales.domain.ScaleProgressionCalculator.TierItem;
import com.todoacorde.todoacorde.scales.domain.ScaleTrainerViewModel;

import java.util.List;

/**
 * Diálogo que muestra las escalas agrupadas por dificultad (fácil, media, difícil)
 * y permite seleccionar un tipo de escala. No usa etiquetas HTML en los comentarios.
 *
 * Flujo:
 * 1) Observa el UiState del ViewModel compartido para renderizar las tres secciones.
 * 2) Cada fila muestra el estado de desbloqueo, progreso (cajas completadas) y
 *    permite la selección si está disponible.
 */
public class ScaleTierPickerDialog extends DialogFragment {

    private static final String TAG = "ScaleTierDialog";

    private ScaleTrainerViewModel viewModel;
    private LinearLayout tierEasyContainer, tierMediumContainer, tierHardContainer;

    /**
     * Crea el diálogo con el contenido inflado y los observers del ViewModel.
     */
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View content = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_scale_tiers, null, false);

        TextView title = content.findViewById(R.id.sheetTitle);
        ImageButton btnClose = content.findViewById(R.id.btnCloseSheet);
        tierEasyContainer = content.findViewById(R.id.tierEasyContainer);
        tierMediumContainer = content.findViewById(R.id.tierMediumContainer);
        tierHardContainer = content.findViewById(R.id.tierHardContainer);

        title.setText(getString(R.string.escalas_por_dificultad));
        btnClose.setOnClickListener(v -> dismiss());

        viewModel = obtainSharedVm();
        Log.d(TAG, "onCreateDialog -> VM instance @" + System.identityHashCode(viewModel));

        // Observa el estado de UI y re-renderiza cada sección cuando cambie.
        viewModel.getUiState().observe(this, s -> {
            if (s == null) return;
            Log.d(TAG, "onState -> easy=" + (s.easyItems == null ? -1 : s.easyItems.size())
                    + " medium=" + (s.mediumItems == null ? -1 : s.mediumItems.size())
                    + " hard=" + (s.hardItems == null ? -1 : s.hardItems.size()));

            renderSection(tierEasyContainer, getString(R.string.difficulty_easy), s.easyItems);
            renderSection(tierMediumContainer, getString(R.string.difficulty_medium), s.mediumItems);
            renderSection(tierHardContainer, getString(R.string.difficulty_hard), s.hardItems);
        });

        return new MaterialAlertDialogBuilder(requireContext())
                .setView(content)
                .create();
    }

    /**
     * Obtiene el ViewModel compartido, preferentemente del parent Fragment si existe,
     * en caso contrario de la Activity.
     */
    private ScaleTrainerViewModel obtainSharedVm() {
        Fragment parent = getParentFragment();
        ViewModelProvider provider;
        String owner;
        if (parent != null) {
            provider = new ViewModelProvider(parent);
            owner = "parentFragment";
        } else {
            provider = new ViewModelProvider(requireActivity());
            owner = "activity";
        }
        ScaleTrainerViewModel vm = provider.get(ScaleTrainerViewModel.class);
        Log.d(TAG, "obtainSharedVm -> owner=" + owner + " vm@" + System.identityHashCode(vm));
        return vm;
    }

    /**
     * Renderiza una sección (fácil, media o difícil).
     * Si la lista está vacía o nula, muestra un mensaje de “no hay escalas”.
     */
    private void renderSection(@Nullable LinearLayout container, String headerText, @Nullable List<TierItem> items) {
        if (container == null) return;
        container.removeAllViews();

        // Cabecera de la sección.
        TextView header = new TextView(requireContext());
        header.setText(headerText);
        header.setAllCaps(true);
        header.setPadding(dp(8), dp(4), dp(8), dp(4));
        header.setTextColor(getColor(android.R.color.white));
        header.setBackgroundColor(getColor(android.R.color.darker_gray));
        container.addView(header, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // Contenido vacío.
        if (items == null || items.isEmpty()) {
            TextView empty = new TextView(requireContext());
            empty.setText(getString(R.string.no_scales_in_difficulty));
            empty.setPadding(dp(12), dp(12), dp(12), dp(12));
            empty.setGravity(android.view.Gravity.CENTER);
            container.addView(empty, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            return;
        }

        // Fila por cada tipo de escala.
        for (TierItem it : items) {
            container.addView(buildTierRow(it), new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        }
    }

    /**
     * Construye la fila para un tipo de escala dentro de la sección.
     * Incluye nombre, estado (desbloqueo/completado), barra de progreso y click si procede.
     */
    private View buildTierRow(TierItem it) {
        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(dp(12), dp(10), dp(12), dp(10));

        // Color de fondo en función de desbloqueo y completado.
        int bg = it.unlocked
                ? (it.completed ? getColor(android.R.color.holo_green_light) : getColor(android.R.color.white))
                : getColor(android.R.color.darker_gray);
        row.setBackgroundColor(bg);

        // Fila superior con nombre y estado.
        LinearLayout top = new LinearLayout(requireContext());
        top.setOrientation(LinearLayout.HORIZONTAL);

        TextView name = new TextView(requireContext());
        name.setText(it.typeEs + " (" + it.typeEn + ")");
        name.setTextColor(getColor(android.R.color.black));
        name.setTextSize(16f);
        name.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView state = new TextView(requireContext());
        String lock = it.unlocked ? "🔓" : "🔒";
        String patterns = it.hasPatterns ? "" : " · sin patrones";
        state.setText(lock + (it.completed ? " · completo" : "") + patterns);
        state.setTextColor(getColor(android.R.color.black));

        top.addView(name);
        top.addView(state);
        row.addView(top);

        // Fila inferior con progreso de cajas completadas.
        LinearLayout bottom = new LinearLayout(requireContext());
        bottom.setOrientation(LinearLayout.HORIZONTAL);
        bottom.setPadding(0, dp(6), 0, 0);

        ProgressBar pb = new ProgressBar(requireContext(), null, android.R.attr.progressBarStyleHorizontal);
        pb.setMax(Math.max(1, it.totalBoxes));
        pb.setProgress(Math.max(0, Math.min(it.totalBoxes, it.completedBoxes)));
        pb.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView counts = new TextView(requireContext());
        counts.setText(it.completedBoxes + "/" + it.totalBoxes);
        counts.setPadding(dp(8), 0, 0, 0);
        counts.setTextColor(getColor(android.R.color.black));

        bottom.addView(pb);
        bottom.addView(counts);
        row.addView(bottom);

        // Habilita click solo si está desbloqueado y hay patrones.
        boolean clickable = it.unlocked && it.hasPatterns;
        row.setAlpha(clickable ? 1f : 0.6f);
        row.setClickable(clickable);
        if (clickable) {
            row.setOnClickListener(v -> {
                Log.d(TAG, "click item -> " + it.typeEs + " / " + it.typeEn);
                viewModel.onTypeSelected(it.typeEn);
                dismiss();
            });
        }

        // Separador visual bajo la fila.
        View sep = new View(requireContext());
        sep.setBackgroundColor(getColor(android.R.color.darker_gray));
        LinearLayout.LayoutParams sepLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(1));
        sepLp.topMargin = dp(8);
        sepLp.bottomMargin = dp(8);
        row.addView(sep, sepLp);

        // Wrapper con márgenes verticales para espaciar filas.
        LinearLayout wrapper = new LinearLayout(requireContext());
        wrapper.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(6);
        lp.bottomMargin = dp(6);
        wrapper.addView(row, lp);

        return wrapper;
    }

    /**
     * Convierte dp a píxeles según densidad actual.
     */
    private int dp(int v) {
        float d = requireContext().getResources().getDisplayMetrics().density;
        return Math.round(v * d);
    }

    /**
     * Obtiene un color compat a partir de recursos.
     */
    private int getColor(int resId) {
        return ContextCompat.getColor(requireContext(), resId);
    }
}
