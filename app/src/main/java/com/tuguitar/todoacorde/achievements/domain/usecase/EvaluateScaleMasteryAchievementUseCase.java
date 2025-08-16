package com.tuguitar.todoacorde.achievements.domain.usecase;

import androidx.annotation.NonNull;

import javax.inject.Inject;
import javax.inject.Singleton;

import android.util.Log;

import com.tuguitar.todoacorde.achievements.data.AchievementRepository;
import com.tuguitar.todoacorde.scales.data.PatternRepository;
import com.tuguitar.todoacorde.scales.data.entity.ScaleEntity;
import com.tuguitar.todoacorde.scales.domain.repository.ProgressionRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;

/**
 * Incrementa los logros de "Completar escalas (una tonalidad)" y
 * "Completar escalas (todas las tonalidades)" cuando se cumple la
 * condición global por banda (fácil/medio/difícil).
 *
 * Criterio de completitud por (escala + tonalidad):
 *   required = min( REQUIRED_BOXES_PER_TIER(tier)=3 , variantesDisponiblesEnPatrones )
 *   Se considera completa si maxHechas >= required.
 *
 * Notas:
 *  - Si una escala/tier NO tiene variantes para esa tonalidad (0), se ignora.
 *  - Si NINGUNA escala del tier tiene variantes para esa tonalidad, no se
 *    considera completo (evita falsos positivos).
 *  - Thresholds de estas familias en el seeder: 1/1/1.
 */
@Singleton
public class EvaluateScaleMasteryAchievementUseCase {

    private static final String TAG = "EvalScaleAchievements";

    // Familias definidas en el seeder
    public static final String FAMILY_ONE_TONALITY   = "scales_one_tonality_milestone";
    public static final String FAMILY_ALL_TONALITIES = "scales_all_tonalities_milestone";

    public enum Difficulty { EASY, MEDIUM, HARD }

    private final AchievementRepository achievementRepo;
    private final ProgressionRepository progressionRepo;
    private final PatternRepository patternRepo;

    @Inject
    public EvaluateScaleMasteryAchievementUseCase(
            AchievementRepository achievementRepo,
            ProgressionRepository progressionRepo,
            PatternRepository patternRepo
    ) {
        this.achievementRepo = achievementRepo;
        this.progressionRepo = progressionRepo;
        this.patternRepo     = patternRepo;
    }

    /**
     * Llamar DESPUÉS de persistir la caja completada.
     *
     * @param userId                     Usuario
     * @param scaleId                    Escala recién practicada
     * @param tonalityId                 Tonalidad actual
     * @param justCompletedForTonality   true si ESTA escala acaba de quedar completa en esta tonalidad
     * @param justCompletedAllTonalities true si ESTA escala acaba de quedar completa en TODAS las tonalidades
     */
    public void onScaleCompletionEvaluated(long userId,
                                           long scaleId,
                                           long tonalityId,
                                           boolean justCompletedForTonality,
                                           boolean justCompletedAllTonalities) {

        int tier = progressionRepo.findScaleTierById(scaleId);
        Difficulty band = mapTierToDifficulty(tier);

        Log.d(TAG, "onScaleCompletionEvaluated user=" + userId
                + " scaleId=" + scaleId + " tonalityId=" + tonalityId
                + " tier=" + tier + " band=" + band
                + " justTonality=" + justCompletedForTonality
                + " justAllTonalities=" + justCompletedAllTonalities);

        // === MILESTONE: "una tonalidad" ===
        try {
            if (justCompletedForTonality &&
                    isBandCompleteInTonality(userId, band, tonalityId)) {
                incrementSafe(FAMILY_ONE_TONALITY, 1);
            }
        } catch (Throwable t) {
            Log.e(TAG, "Error evaluando milestone 'una tonalidad': " + t.getMessage(), t);
        }

        // === MILESTONE: "todas las tonalidades" ===
        try {
            if (isBandCompleteInAllTonalities(userId, band)) {
                incrementSafe(FAMILY_ALL_TONALITIES, 1);
            }
        } catch (Throwable t) {
            Log.e(TAG, "Error evaluando milestone 'todas las tonalidades': " + t.getMessage(), t);
        }
    }

    private void incrementSafe(@NonNull String familyId, int delta) {
        try {
            achievementRepo.incrementProgress(familyId, delta);
            Log.d(TAG, "Incremented achievement family=" + familyId + " by " + delta);
        } catch (Throwable t) {
            Log.e(TAG, "Error incrementing achievement " + familyId + ": " + t.getMessage(), t);
        }
    }

    // ===== Comprobaciones =====

    private boolean isBandCompleteInTonality(long userId, Difficulty band, long tonalityId) {
        String root = nameForTonalityId(tonalityId);
        if (root == null || root.isEmpty()) return false;

        int tier = mapDifficultyToTier(band);
        List<ScaleEntity> scales = progressionRepo.getScalesByTier(tier);
        if (scales == null || scales.isEmpty()) return false;

        boolean hadRelevant = false;

        for (ScaleEntity s : scales) {
            String en = mapDbNameToEnglishType(s.name); // clave canónica del PatternRepository
            int variantes = safeVariantsCount(en, root);
            if (variantes <= 0) {
                // No hay patrones para esta escala/root → no bloquea ni cuenta
                continue;
            }
            hadRelevant = true;

            int required = Math.min(requiredBoxesForTier(tier), variantes);
            int maxHechas = progressionRepo.getMaxCompletedBoxOrZero(userId, s.id, tonalityId);
            if (maxHechas < required) {
                return false;
            }
        }
        return hadRelevant;
    }

    private boolean isBandCompleteInAllTonalities(long userId, Difficulty band) {
        final String[] NOTE_NAMES = {"C","C#","D","D#","E","F","F#","G","G#","A","A#","B"};
        for (String root : NOTE_NAMES) {
            long tonalityId = progressionRepo.findTonalityIdByName(root);
            if (tonalityId <= 0) return false;

            if (!isBandCompleteInTonalityByRoot(userId, band, root, tonalityId)) {
                return false;
            }
        }
        return true;
    }

    private boolean isBandCompleteInTonalityByRoot(long userId, Difficulty band, String root, long tonalityId) {
        int tier = mapDifficultyToTier(band);
        List<ScaleEntity> scales = progressionRepo.getScalesByTier(tier);
        if (scales == null || scales.isEmpty()) return false;

        boolean hadRelevant = false;

        for (ScaleEntity s : scales) {
            String en = mapDbNameToEnglishType(s.name);
            int variantes = safeVariantsCount(en, root);
            if (variantes <= 0) {
                continue;
            }
            hadRelevant = true;

            int required = Math.min(requiredBoxesForTier(tier), variantes);
            int maxHechas = progressionRepo.getMaxCompletedBoxOrZero(userId, s.id, tonalityId);
            if (maxHechas < required) {
                return false;
            }
        }
        return hadRelevant;
    }

    // ===== Helpers =====

    private int safeVariantsCount(@NonNull String englishType, @NonNull String root) {
        try {
            List<PatternRepository.PatternVariant> vs =
                    patternRepo.getVariantsByTypeAndRoot(englishType, root);
            return (vs != null) ? vs.size() : 0;
        } catch (Throwable t) {
            Log.e(TAG, "safeVariantsCount error for " + englishType + " / " + root + ": " + t.getMessage());
            return 0;
        }
    }

    /** Deducción del nombre de root a partir del id, usando el propio repo (inversión por búsqueda). */
    private String nameForTonalityId(long tonalityId) {
        final String[] NOTE_NAMES = {"C","C#","D","D#","E","F","F#","G","G#","A","A#","B"};
        for (String n : NOTE_NAMES) {
            long id = progressionRepo.findTonalityIdByName(n);
            if (id == tonalityId) return n;
        }
        return null;
    }

    /**
     * Mapeo ES → EN a los nombres canónicos que usa PatternRepository/JSON.
     * (Devuelve el nombre original si no encuentra mapeo).
     */
    private String mapDbNameToEnglishType(String es) {
        if (es == null) return "";
        String s = es.trim().toLowerCase(Locale.ROOT);
        HashMap<String,String> inv = new HashMap<>();

        // Pentatónicas / Blues
        inv.put("pentatónica mayor", "Major Pentatonic");
        inv.put("pentatónica menor", "Minor Pentatonic");
        inv.put("blues",             "Blues");

        // Modos mayores/menores
        inv.put("mayor",             "Ionian");
        inv.put("menor natural",     "Aeolian");
        inv.put("dórica",            "Dorian");
        inv.put("mixolidia",         "Mixolydian");
        inv.put("lidia",             "Lydian");
        inv.put("frigia",            "Phrygian");
        inv.put("locria",            "Locrian");

        // Flamenco / exóticos
        inv.put("frigia dominante",  "Phrygian Dominant");
        inv.put("modo flamenco (mayor-frigio)", "Flamenco Mode (Major-Phrygian)");
        inv.put("doble armónica mayor", "Double Harmonic Major");
        inv.put("española 8 tonos",  "Spanish 8-Tone");

        // Menores compuestas
        inv.put("menor armónica",    "Harmonic Minor");
        inv.put("menor melódica",    "Melodic Minor (Asc)");

        String en = inv.get(s);
        return en != null ? en : es;
    }

    private static Difficulty mapTierToDifficulty(int tier) {
        if (tier <= 0) return Difficulty.EASY;
        if (tier == 1) return Difficulty.MEDIUM;
        return Difficulty.HARD;
    }
    private static int mapDifficultyToTier(Difficulty d) {
        switch (d) {
            case EASY:   return 0;
            case MEDIUM: return 1;
            default:     return 2;
        }
    }

    /** Ahora todas las bandas requieren 3 cajas por tonalidad. */
    private int requiredBoxesForTier(int tier) {
        return 3;
    }
}
