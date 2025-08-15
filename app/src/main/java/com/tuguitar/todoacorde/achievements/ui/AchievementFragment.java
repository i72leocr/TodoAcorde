package com.tuguitar.todoacorde.achievements.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.material.snackbar.Snackbar;
import com.tuguitar.todoacorde.achievements.data.Achievement;
import com.tuguitar.todoacorde.achievements.data.AchievementFamily;
import com.tuguitar.todoacorde.achievements.domain.AchievementViewModel;
import com.tuguitar.todoacorde.databinding.FragmentAchievementsBinding;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class AchievementFragment extends Fragment {

    private FragmentAchievementsBinding binding;
    private AchievementAdapter adapter;
    private AchievementViewModel viewModel;

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

        // placeholder si está vacío
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
