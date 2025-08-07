package com.tuguitar.todoacorde.achievements.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.tuguitar.todoacorde.Achievement;
import com.tuguitar.todoacorde.AchievementFamily;
import com.tuguitar.todoacorde.databinding.ItemAchievementBinding;
import com.tuguitar.todoacorde.databinding.ItemSubAchievementBinding;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Adapter que muestra familias de logros y sus niveles. Se ha ajustado la lógica para que:
 * 1. El "nivel principal" (icono y barra) sea el siguiente nivel activo a progresar, no el último completado.
 * 2. Los niveles completados se muestran como sub-logros cuando se expande la familia.
 * 3. El icono del nivel activo se difumina (alpha) si aún no está completado.
 */
public class AchievementAdapter
        extends ListAdapter<AchievementFamily, AchievementAdapter.FamilyViewHolder> {

    private final Set<String> expandedFamilies = new HashSet<>();

    public AchievementAdapter() {
        super(DIFF_CALLBACK);
    }

    private static final DiffUtil.ItemCallback<AchievementFamily> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<AchievementFamily>() {
                @Override
                public boolean areItemsTheSame(
                        @NonNull AchievementFamily oldItem,
                        @NonNull AchievementFamily newItem
                ) {
                    return oldItem.getFamilyTitle().equals(newItem.getFamilyTitle());
                }

                @Override
                public boolean areContentsTheSame(
                        @NonNull AchievementFamily oldItem,
                        @NonNull AchievementFamily newItem
                ) {
                    return oldItem.getDescription().equals(newItem.getDescription())
                            && oldItem.getLevels().equals(newItem.getLevels());
                }
            };

    @NonNull
    @Override
    public FamilyViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {
        ItemAchievementBinding binding = ItemAchievementBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new FamilyViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(
            @NonNull FamilyViewHolder holder,
            int position
    ) {
        AchievementFamily family = getItem(position);
        boolean isExpanded = expandedFamilies.contains(family.getFamilyTitle());
        holder.bind(family, isExpanded);
    }

    class FamilyViewHolder extends RecyclerView.ViewHolder {
        private final ItemAchievementBinding binding;

        FamilyViewHolder(@NonNull ItemAchievementBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(@NonNull AchievementFamily family, boolean isExpanded) {
            binding.tvAchievementTitle.setText(family.getFamilyTitle());
            binding.tvAchievementDesc.setText(family.getDescription());

            List<Achievement> levels = family.getLevels();

            // Obtener thresholds y progresos por nivel
            int bronzeThreshold = getThreshold(levels, Achievement.Level.BRONZE);
            int silverThreshold = getThreshold(levels, Achievement.Level.SILVER);
            int goldThreshold   = getThreshold(levels, Achievement.Level.GOLD);

            int bronzeProgress = getProgressForLevel(levels, Achievement.Level.BRONZE);
            int silverProgress = getProgressForLevel(levels, Achievement.Level.SILVER);
            int goldProgress   = getProgressForLevel(levels, Achievement.Level.GOLD);

            // Determinar qué niveles están completados
            boolean bronzeCompleted = bronzeProgress >= bronzeThreshold && bronzeThreshold > 0;
            boolean silverCompleted = silverProgress >= silverThreshold && silverThreshold > 0;
            boolean goldCompleted   = goldProgress >= goldThreshold && goldThreshold > 0;

            // NUEVA LÓGICA: nivel activo = el siguiente que debe progresarse
            Achievement.Level activeLevel;
            if (!bronzeCompleted) {
                activeLevel = Achievement.Level.BRONZE;
            } else if (!silverCompleted) {
                activeLevel = Achievement.Level.SILVER;
            } else if (!goldCompleted) {
                activeLevel = Achievement.Level.GOLD;
            } else {
                activeLevel = Achievement.Level.GOLD; // todo completado
            }

            // Icono principal basado en el nivel activo
            Achievement levelForIcon = getAchievementForLevel(levels, activeLevel);
            binding.ivLevelIcon.setImageResource(levelForIcon.getIconResId());

            // Si el nivel activo no está completado, se atenúa para indicar "pendiente"
            boolean activeCompleted = (activeLevel == Achievement.Level.BRONZE && bronzeCompleted)
                    || (activeLevel == Achievement.Level.SILVER && silverCompleted)
                    || (activeLevel == Achievement.Level.GOLD && goldCompleted);
            binding.ivLevelIcon.setAlpha(activeCompleted ? 1f : 0.4f);

            // Card activada sólo si ORO completado totalmente
            binding.cardAchievement.setActivated(goldCompleted);

            // Barra de progreso principal va hacia el threshold del nivel activo
            int currentThreshold;
            int currentProgress;
            switch (activeLevel) {
                case BRONZE:
                    currentThreshold = bronzeThreshold;
                    currentProgress = bronzeProgress;
                    break;
                case SILVER:
                    currentThreshold = silverThreshold;
                    currentProgress = silverProgress;
                    break;
                case GOLD:
                default:
                    currentThreshold = goldThreshold;
                    currentProgress = goldProgress;
                    break;
            }

            binding.progAchievement.setMax(currentThreshold);
            binding.progAchievement.setProgress(Math.min(currentProgress, currentThreshold));
            binding.tvAchievementProgress.setText(Math.min(currentProgress, currentThreshold) + " / " + currentThreshold);

            // Flecha visible si al menos un nivel está completado (start de expansión)
            boolean anyCompleted = bronzeCompleted || silverCompleted || goldCompleted;
            binding.ivExpandArrow.setVisibility(anyCompleted ? View.VISIBLE : View.GONE);

            // Sublogros: mostrar los niveles completados en detalle cuando está expandido
            binding.layoutCompletedSubAchievements.removeAllViews();
            if (isExpanded && anyCompleted) {
                if (bronzeCompleted) {
                    addCompletedSubAchievement(binding, bronzeProgress, bronzeThreshold, Achievement.Level.BRONZE, levels);
                }
                if (silverCompleted) {
                    addCompletedSubAchievement(binding, silverProgress, silverThreshold, Achievement.Level.SILVER, levels);
                }
                if (goldCompleted) {
                    addCompletedSubAchievement(binding, goldProgress, goldThreshold, Achievement.Level.GOLD, levels);
                }
            } else {
                binding.layoutCompletedSubAchievements.setVisibility(View.GONE);
            }

            View.OnClickListener toggleExpand = v -> {
                if (!anyCompleted) return;
                String key = family.getFamilyTitle();
                if (expandedFamilies.contains(key)) {
                    expandedFamilies.remove(key);
                } else {
                    expandedFamilies.add(key);
                }
                notifyItemChanged(getBindingAdapterPosition());
            };
            binding.cardAchievement.setOnClickListener(toggleExpand);
            binding.ivExpandArrow.setOnClickListener(toggleExpand);
        }

        private void addCompletedSubAchievement(
                ItemAchievementBinding binding,
                int progress,
                int threshold,
                Achievement.Level level,
                List<Achievement> levels
        ) {
            Achievement achievement = getAchievementForLevel(levels, level);
            ItemSubAchievementBinding subBinding = ItemSubAchievementBinding.inflate(
                    LayoutInflater.from(binding.getRoot().getContext()),
                    binding.layoutCompletedSubAchievements,
                    false
            );
            subBinding.ivMedal.setImageResource(achievement.getIconResId());
            subBinding.ivMedal.setAlpha(1f);
            subBinding.progSubAchievement.setMax(threshold);
            subBinding.progSubAchievement.setProgress(Math.min(progress, threshold));
            subBinding.tvSubValue.setText(
                    Math.min(progress, threshold) + " / " + threshold
            );
            binding.layoutCompletedSubAchievements.addView(subBinding.getRoot());
            binding.layoutCompletedSubAchievements.setVisibility(View.VISIBLE);
        }

        private int getThreshold(List<Achievement> levels, Achievement.Level level) {
            for (Achievement a : levels) {
                if (a.getLevel() == level) return a.getThreshold();
            }
            return 0;
        }

        private int getProgressForLevel(List<Achievement> levels, Achievement.Level level) {
            for (Achievement a : levels) {
                if (a.getLevel() == level) return a.getProgress();
            }
            return 0;
        }

        private Achievement getAchievementForLevel(List<Achievement> levels, Achievement.Level level) {
            for (Achievement a : levels) {
                if (a.getLevel() == level) return a;
            }
            return levels.get(0);
        }
    }
}
