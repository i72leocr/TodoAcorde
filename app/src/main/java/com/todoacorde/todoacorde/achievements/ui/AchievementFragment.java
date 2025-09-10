package com.todoacorde.todoacorde.achievements.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.snackbar.Snackbar;
import com.todoacorde.todoacorde.achievements.data.Achievement;
import com.todoacorde.todoacorde.achievements.data.AchievementFamily;
import com.todoacorde.todoacorde.achievements.domain.AchievementViewModel;
import com.todoacorde.todoacorde.databinding.FragmentAchievementsBinding;

import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * Fragmento responsable de mostrar el listado de familias de logros y
 * notificar al usuario cuando se desbloquea un logro.
 *
 * Configura el {@link RecyclerView} con {@link AchievementAdapter}, observa el
 * {@link AchievementViewModel} para renderizar familias y escucha eventos de
 * desbloqueo para emitir un {@link Snackbar}.
 */
@AndroidEntryPoint
public class AchievementFragment extends Fragment {

    /** View binding del layout {@code fragment_achievements.xml}. */
    private FragmentAchievementsBinding binding;
    /** Adaptador para la lista de familias de logros. */
    private AchievementAdapter adapter;
    /** ViewModel que orquesta datos de logros. */
    private AchievementViewModel viewModel;

    /**
     * Infla la vista principal del fragmento usando ViewBinding.
     *
     * @param inflater  inflador de layouts.
     * @param container contenedor padre.
     * @param savedInstanceState estado previo si existe.
     * @return raíz de la vista del fragmento.
     */
    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        binding = FragmentAchievementsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    /**
     * Configura la UI y las observaciones al crearse la vista.
     *
     * @param view               raíz de la vista ya creada.
     * @param savedInstanceState estado previo si existe.
     */
    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState
    ) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this)
                .get(AchievementViewModel.class);

        adapter = new AchievementAdapter();
        binding.rvAchievements.setLayoutManager(
                new LinearLayoutManager(requireContext())
        );
        binding.rvAchievements.setAdapter(adapter);
        binding.tvEmpty.setVisibility(View.GONE);

        viewModel.getAchievementFamilies().observe(
                getViewLifecycleOwner(),
                this::renderFamilies
        );

        viewModel.getUnlockEvent().observe(
                getViewLifecycleOwner(),
                event -> {
                    Achievement ach = event.getIfNotHandled();
                    if (ach != null) {
                        String message = String.format("¡Logro desbloqueado: %s (%s)!",
                                ach.getTitle(),
                                ach.getLevel().name());
                        Snackbar.make(
                                binding.getRoot(),
                                message,
                                Snackbar.LENGTH_LONG
                        ).show();
                    }
                }
        );
    }

    /**
     * Renderiza la lista de familias en la UI y actualiza el contador de logros completados.
     *
     * @param families lista de familias de logros; puede ser vacía o null.
     */
    private void renderFamilies(List<AchievementFamily> families) {
        if (families == null || families.isEmpty()) {
            adapter.submitList(List.of());
            binding.tvTotalUnlocked.setText("0 / 0");
            binding.tvEmpty.setVisibility(View.VISIBLE);
            return;
        } else {
            binding.tvEmpty.setVisibility(View.GONE);
        }

        adapter.submitList(families);

        int total = 0;
        int done = 0;
        for (AchievementFamily fam : families) {
            total += fam.getLevels().size();
            for (Achievement a : fam.getLevels()) {
                if (a.getState() == Achievement.State.COMPLETED) {
                    done++;
                }
            }
        }
        binding.tvTotalUnlocked.setText(done + " / " + total);
    }

    /**
     * Libera la referencia al binding para evitar memory leaks.
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
