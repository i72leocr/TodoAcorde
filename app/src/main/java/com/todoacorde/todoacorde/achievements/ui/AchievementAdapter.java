package com.todoacorde.todoacorde.achievements.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.todoacorde.todoacorde.achievements.data.Achievement;
import com.todoacorde.todoacorde.achievements.data.AchievementFamily;
import com.todoacorde.todoacorde.databinding.ItemAchievementBinding;
import com.todoacorde.todoacorde.databinding.ItemSubAchievementBinding;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Adaptador de lista para mostrar familias de logros y sus niveles asociados.
 *
 * Utiliza {@link ListAdapter} con {@link DiffUtil} para optimizar actualizaciones de la UI
 * y mantiene el estado de expansión por familia para revelar sublogros completados.
 */
public class AchievementAdapter
        extends ListAdapter<AchievementFamily, AchievementAdapter.FamilyViewHolder> {

    /** Conjunto de títulos de familias actualmente expandidas. */
    private final Set<String> expandedFamilies = new HashSet<>();

    /** Crea el adaptador con el callback de diferencias por defecto. */
    public AchievementAdapter() {
        super(DIFF_CALLBACK);
    }

    /** Callback de comparación de elementos y contenidos para DiffUtil. */
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

    /**
     * Infla la vista de un ítem de familia y crea su {@link FamilyViewHolder}.
     *
     * @param parent  contenedor padre.
     * @param viewType tipo de vista (no se usa en este adaptador).
     * @return un nuevo {@link FamilyViewHolder}.
     */
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

    /**
     * Vincula los datos de una familia de logros a su ViewHolder.
     *
     * @param holder   ViewHolder de familia.
     * @param position posición del elemento en la lista.
     */
    @Override
    public void onBindViewHolder(
            @NonNull FamilyViewHolder holder,
            int position
    ) {
        AchievementFamily family = getItem(position);
        boolean isExpanded = expandedFamilies.contains(family.getFamilyTitle());
        holder.bind(family, isExpanded);
    }

    /**
     * ViewHolder para la tarjeta de una familia de logros.
     *
     * Encapsula la lógica de presentación: cálculo del nivel activo,
     * progreso y renderizado de sublogros completados al expandir.
     */
    class FamilyViewHolder extends RecyclerView.ViewHolder {
        private final ItemAchievementBinding binding;

        /**
         * Crea el ViewHolder con el binding de la vista.
         *
         * @param binding binding generado de {@code item_achievement.xml}.
         */
        FamilyViewHolder(@NonNull ItemAchievementBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        /**
         * Vincula una familia de logros y su estado de expansión.
         *
         * @param family     familia a mostrar.
         * @param isExpanded indica si la familia está expandida para mostrar sublogros.
         */
        void bind(@NonNull AchievementFamily family, boolean isExpanded) {
            binding.tvAchievementTitle.setText(family.getFamilyTitle());
            binding.tvAchievementDesc.setText(family.getDescription());

            List<Achievement> levels = family.getLevels();
            int bronzeThreshold = getThreshold(levels, Achievement.Level.BRONZE);
            int silverThreshold = getThreshold(levels, Achievement.Level.SILVER);
            int goldThreshold = getThreshold(levels, Achievement.Level.GOLD);

            int bronzeProgress = getProgressForLevel(levels, Achievement.Level.BRONZE);
            int silverProgress = getProgressForLevel(levels, Achievement.Level.SILVER);
            int goldProgress = getProgressForLevel(levels, Achievement.Level.GOLD);

            boolean bronzeCompleted = bronzeProgress >= bronzeThreshold && bronzeThreshold > 0;
            boolean silverCompleted = silverProgress >= silverThreshold && silverThreshold > 0;
            boolean goldCompleted = goldProgress >= goldThreshold && goldThreshold > 0;

            // Determinar el nivel activo (el primero no completado; si todos completos, GOLD).
            Achievement.Level activeLevel;
            if (!bronzeCompleted) {
                activeLevel = Achievement.Level.BRONZE;
            } else if (!silverCompleted) {
                activeLevel = Achievement.Level.SILVER;
            } else if (!goldCompleted) {
                activeLevel = Achievement.Level.GOLD;
            } else {
                activeLevel = Achievement.Level.GOLD;
            }

            // Icono del nivel activo y estado visual (opacidad)
            Achievement levelForIcon = getAchievementForLevel(levels, activeLevel);
            binding.ivLevelIcon.setImageResource(levelForIcon.getIconResId());
            boolean activeCompleted = (activeLevel == Achievement.Level.BRONZE && bronzeCompleted)
                    || (activeLevel == Achievement.Level.SILVER && silverCompleted)
                    || (activeLevel == Achievement.Level.GOLD && goldCompleted);
            binding.ivLevelIcon.setAlpha(activeCompleted ? 1f : 0.4f);

            // Activar tarjeta si GOLD está completado
            binding.cardAchievement.setActivated(goldCompleted);

            // Progreso actual mostrado en la barra principal
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

            // Mostrar flecha de expansión solo si hay algún nivel completado
            boolean anyCompleted = bronzeCompleted || silverCompleted || goldCompleted;
            binding.ivExpandArrow.setVisibility(anyCompleted ? View.VISIBLE : View.GONE);

            // Renderizar sublogros completados al expandir
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

            // Toggle de expansión al pulsar tarjeta o flecha
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

        /**
         * Añade a la vista el sublogro completado con su progreso e icono.
         *
         * @param binding   binding de la tarjeta principal.
         * @param progress  progreso alcanzado.
         * @param threshold umbral del sublogro.
         * @param level     nivel del sublogro.
         * @param levels    lista de niveles de la familia.
         */
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

        /**
         * Obtiene el umbral de un nivel concreto.
         *
         * @param levels lista de niveles de la familia.
         * @param level  nivel a consultar.
         * @return umbral del nivel o 0 si no se encuentra.
         */
        private int getThreshold(List<Achievement> levels, Achievement.Level level) {
            for (Achievement a : levels) {
                if (a.getLevel() == level) return a.getThreshold();
            }
            return 0;
        }

        /**
         * Obtiene el progreso asociado a un nivel concreto.
         *
         * @param levels lista de niveles de la familia.
         * @param level  nivel a consultar.
         * @return progreso del nivel o 0 si no se encuentra.
         */
        private int getProgressForLevel(List<Achievement> levels, Achievement.Level level) {
            for (Achievement a : levels) {
                if (a.getLevel() == level) return a.getProgress();
            }
            return 0;
        }

        /**
         * Devuelve el logro correspondiente a un nivel concreto.
         *
         * @param levels lista de niveles.
         * @param level  nivel a buscar.
         * @return logro del nivel; si no se encuentra, devuelve el primero de la lista.
         */
        private Achievement getAchievementForLevel(List<Achievement> levels, Achievement.Level level) {
            for (Achievement a : levels) {
                if (a.getLevel() == level) return a;
            }
            return levels.get(0);
        }
    }
}
