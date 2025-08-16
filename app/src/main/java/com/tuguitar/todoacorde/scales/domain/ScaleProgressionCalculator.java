package com.tuguitar.todoacorde.scales.domain;

import android.util.Log;

import androidx.annotation.NonNull;

import com.tuguitar.todoacorde.scales.data.PatternRepository;
import com.tuguitar.todoacorde.scales.data.entity.ScaleEntity;
import com.tuguitar.todoacorde.scales.domain.repository.ProgressionRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Servicio de dominio para cálculos de progresión y construcción de ítems por tier. */
public class ScaleProgressionCalculator {

    private static final String TAG = "ScaleProgressionCalc";

    private final PatternRepository patternRepo;
    private final ProgressionRepository progressionRepo;

    public static final class TierItem {
        public final String typeEn;        // "Ionian", "Phrygian", ...
        public final String typeEs;        // "Mayor", "Frigia", ...
        public final boolean unlocked;     // tier desbloqueado para la root actual
        public final boolean hasPatterns;  // hay variantes para esta root
        public final int completedBoxes;   // maxHechas para esta root
        public final int totalBoxes;       // variantes disponibles en esta root
        public final boolean completed;    // completedBoxes >= totalBoxes

        public TierItem(String typeEn, String typeEs, boolean unlocked, boolean hasPatterns,
                        int completedBoxes, int totalBoxes, boolean completed) {
            this.typeEn = typeEn;
            this.typeEs = typeEs;
            this.unlocked = unlocked;
            this.hasPatterns = hasPatterns;
            this.completedBoxes = completedBoxes;
            this.totalBoxes = totalBoxes;
            this.completed = completed;
        }
    }

    public ScaleProgressionCalculator(@NonNull PatternRepository patternRepo,
                                      @NonNull ProgressionRepository progressionRepo) {
        this.patternRepo = patternRepo;
        this.progressionRepo = progressionRepo;
    }

    /** Tier completo para ESTA tonalidad si todas las escalas relevantes del tier están completas. */
    public boolean isTierCompletedForRoot(long userId, int tier, long tonalityId, @NonNull String root) {
        List<ScaleEntity> tierScales = progressionRepo.getScalesByTier(tier);
        if (tierScales == null || tierScales.isEmpty()) {
            Log.d(TAG, "isTierCompletedForRoot tier=" + tier + " -> vacío => false");
            return false;
        }

        int relevantes = 0;
        List<String> debug = new ArrayList<>();

        for (ScaleEntity s : tierScales) {
            String en = ScaleTypeMapper.mapDbNameToEnglishType(s.name);
            List<PatternRepository.PatternVariant> variants =
                    nonNull(patternRepo.getVariantsByTypeAndRoot(en, root));
            int disponibles = variants.size();

            if (disponibles <= 0) {
                debug.add(" - " + s.name + " [sin patrones para " + root + "] -> IGNORAR");
                continue;
            }

            relevantes++;
            int maxHechas = progressionRepo.getMaxCompletedBoxOrZero(userId, s.id, tonalityId);
            boolean done = maxHechas >= disponibles;

            debug.add(" - " + s.name + " [patrones " + root + ": " + disponibles + "] -> "
                    + (done ? "COMPLETA" : "PENDIENTE") + " (" + maxHechas + "/" + disponibles + ")");

            if (!done) {
                Log.d(TAG, "isTierCompletedForRoot tier=" + tier + " -> NO completo. Detalle:\n" + join(debug));
                return false;
            }
        }

        if (relevantes == 0) {
            Log.d(TAG, "isTierCompletedForRoot tier=" + tier + " -> sin escalas relevantes para " + root + ", NO completo.");
            return false;
        }

        Log.d(TAG, "isTierCompletedForRoot tier=" + tier + " -> COMPLETO. Detalle:\n" + join(debug));
        return true;
    }

    /** Tier máximo COMPLETADO para una root. Devuelve -1 si ninguno. */
    public int computeMaxCompletedTierForRoot(long userId, long tonalityId, @NonNull String root) {
        int t = 0, lastCompleted = -1;
        while (progressionRepo.hasAnyScaleInTier(t) &&
                isTierCompletedForRoot(userId, t, tonalityId, root)) {
            lastCompleted = t;
            t++;
        }
        return lastCompleted;
    }

    /** Construye la lista de items para un tier. */
    public List<TierItem> buildTierItemsFor(int tier, @NonNull String root, long tonalityId, boolean tierUnlocked) {
        List<TierItem> out = new ArrayList<>();
        List<ScaleEntity> scales = progressionRepo.getScalesByTier(tier);
        if (scales == null) return out;

        for (ScaleEntity s : scales) {
            String en = ScaleTypeMapper.mapDbNameToEnglishType(s.name);
            List<PatternRepository.PatternVariant> variants =
                    nonNull(patternRepo.getVariantsByTypeAndRoot(en, root));
            int disponibles = variants.size();
            boolean hasPatterns = disponibles > 0;
            int maxHechas = (tonalityId > 0)
                    ? progressionRepo.getMaxCompletedBoxOrZero(1L /* caller sets userId via repo usage if needed */, s.id, tonalityId)
                    : 0;
            // OJO: Si quieres el usuario real, pásalo desde el ViewModel en otro método (wrapper) o añade userId como parámetro.
            boolean completed = hasPatterns && maxHechas >= disponibles;

            out.add(new TierItem(en, s.name, tierUnlocked, hasPatterns, maxHechas, disponibles, completed));
        }
        return out;
    }

    // Variante que recibe userId explícito para completedBoxes correctos por usuario:
    public List<TierItem> buildTierItemsFor(int tier, @NonNull String root, long tonalityId, boolean tierUnlocked, long userId) {
        List<TierItem> out = new ArrayList<>();
        List<ScaleEntity> scales = progressionRepo.getScalesByTier(tier);
        if (scales == null) return out;

        for (ScaleEntity s : scales) {
            String en = ScaleTypeMapper.mapDbNameToEnglishType(s.name);
            List<PatternRepository.PatternVariant> variants =
                    nonNull(patternRepo.getVariantsByTypeAndRoot(en, root));
            int disponibles = variants.size();
            boolean hasPatterns = disponibles > 0;
            int maxHechas = (tonalityId > 0)
                    ? progressionRepo.getMaxCompletedBoxOrZero(userId, s.id, tonalityId)
                    : 0;
            boolean completed = hasPatterns && maxHechas >= disponibles;

            out.add(new TierItem(en, s.name, tierUnlocked, hasPatterns, maxHechas, disponibles, completed));
        }
        return out;
    }

    // ===== Utils internos =====
    private static <T> List<T> nonNull(List<T> in) { return in == null ? Collections.emptyList() : in; }

    private static String join(List<?> l) {
        if (l == null) return "[]";
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < l.size(); i++) {
            if (i>0) sb.append(", ");
            Object x = l.get(i);
            sb.append(x == null ? "null" : String.valueOf(x));
        }
        sb.append("]");
        return sb.toString();
    }
}
