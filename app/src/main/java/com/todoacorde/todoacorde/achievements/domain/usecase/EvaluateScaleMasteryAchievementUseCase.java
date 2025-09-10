package com.todoacorde.todoacorde.achievements.domain.usecase;

import android.util.Log;

import androidx.annotation.NonNull;

import com.todoacorde.todoacorde.achievements.data.AchievementRepository;
import com.todoacorde.todoacorde.scales.data.PatternRepository;
import com.todoacorde.todoacorde.scales.data.entity.ScaleEntity;
import com.todoacorde.todoacorde.scales.domain.repository.ProgressionRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Caso de uso para evaluar hitos de maestría de escalas y actualizar logros asociados.
 *
 * Este caso de uso reacciona a la finalización de escalas por tonalidad y, cuando se cumplen
 * las condiciones, incrementa el progreso de las familias de logros:
 * - {@code scales_one_tonality_milestone}: completar todas las cajas requeridas para las escalas
 *   de un nivel de dificultad en una tonalidad concreta.
 * - {@code scales_all_tonalities_milestone}: completar todas las cajas requeridas para las escalas
 *   de un nivel de dificultad en todas las tonalidades.
 *
 * Determinación de dificultad:
 * - tier 0 → EASY, tier 1 → MEDIUM, tier ≥ 2 → HARD.
 * - El número de cajas requeridas se fija por {@link #requiredBoxesForTier(int)}.
 *
 * Esta clase no gestiona hilos explícitamente: delega en repositorios, que deberían realizar
 * el trabajo pesado fuera del hilo principal. Los errores se registran con {@link Log}.
 */
@Singleton
public class EvaluateScaleMasteryAchievementUseCase {

    private static final String TAG = "EvalScaleAchievements";

    /** Familia de logro para el hito “una tonalidad”. */
    public static final String FAMILY_ONE_TONALITY = "scales_one_tonality_milestone";
    /** Familia de logro para el hito “todas las tonalidades”. */
    public static final String FAMILY_ALL_TONALITIES = "scales_all_tonalities_milestone";

    /** Banda o dificultad agregada para agrupar escalas por nivel. */
    public enum Difficulty {EASY, MEDIUM, HARD}

    private final AchievementRepository achievementRepo;
    private final ProgressionRepository progressionRepo;
    private final PatternRepository patternRepo;

    /**
     * Crea el caso de uso con sus dependencias.
     *
     * @param achievementRepo repositorio de logros para incrementar progreso.
     * @param progressionRepo repositorio de progresiones/escala para consultar tiers, tonalidades y progreso por caja.
     * @param patternRepo     repositorio de patrones para contar variantes por tipo y tonalidad.
     */
    @Inject
    public EvaluateScaleMasteryAchievementUseCase(
            AchievementRepository achievementRepo,
            ProgressionRepository progressionRepo,
            PatternRepository patternRepo
    ) {
        this.achievementRepo = achievementRepo;
        this.progressionRepo = progressionRepo;
        this.patternRepo = patternRepo;
    }

    /**
     * Evalúa los hitos de escalas a partir de un evento de finalización de escala.
     * Si se cumple la condición para la tonalidad o para todas las tonalidades, incrementa
     * de forma segura la familia de logro correspondiente.
     *
     * @param userId                     identificador del usuario.
     * @param scaleId                    identificador de la escala completada.
     * @param tonalityId                 identificador de la tonalidad en la que se ha completado.
     * @param justCompletedForTonality   indica si esta acción ha completado la banda en esa tonalidad.
     * @param justCompletedAllTonalities indica si esta acción ha completado la banda en todas las tonalidades.
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

        try {
            if (justCompletedForTonality &&
                    isBandCompleteInTonality(userId, band, tonalityId)) {
                incrementSafe(FAMILY_ONE_TONALITY, 1);
            }
        } catch (Throwable t) {
            Log.e(TAG, "Error evaluando milestone 'una tonalidad': " + t.getMessage(), t);
        }
        try {
            if (isBandCompleteInAllTonalities(userId, band)) {
                incrementSafe(FAMILY_ALL_TONALITIES, 1);
            }
        } catch (Throwable t) {
            Log.e(TAG, "Error evaluando milestone 'todas las tonalidades': " + t.getMessage(), t);
        }
    }

    /**
     * Incrementa el progreso de una familia de logro, registrando errores si ocurren.
     *
     * @param familyId identificador de la familia de logro (ID o título normalizado).
     * @param delta    incremento a aplicar.
     */
    private void incrementSafe(@NonNull String familyId, int delta) {
        try {
            achievementRepo.incrementProgress(familyId, delta);
            Log.d(TAG, "Incremented achievement family=" + familyId + " by " + delta);
        } catch (Throwable t) {
            Log.e(TAG, "Error incrementing achievement " + familyId + ": " + t.getMessage(), t);
        }
    }

    /**
     * Verifica si una banda (dificultad) está completa en una tonalidad concreta.
     * Para considerarse completa, cada escala del tier correspondiente debe tener como mínimo
     * el número de cajas requeridas completadas en esa tonalidad.
     *
     * @param userId     identificador del usuario.
     * @param band       dificultad a verificar.
     * @param tonalityId identificador de la tonalidad.
     * @return true si la banda está completa en esa tonalidad; false en caso contrario.
     */
    private boolean isBandCompleteInTonality(long userId, Difficulty band, long tonalityId) {
        String root = nameForTonalityId(tonalityId);
        if (root == null || root.isEmpty()) return false;

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

    /**
     * Verifica si una banda (dificultad) está completa en todas las tonalidades.
     *
     * @param userId identificador del usuario.
     * @param band   dificultad a verificar.
     * @return true si la banda está completa en las 12 tonalidades; false en caso contrario.
     */
    private boolean isBandCompleteInAllTonalities(long userId, Difficulty band) {
        final String[] NOTE_NAMES = {"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"};
        for (String root : NOTE_NAMES) {
            long tonalityId = progressionRepo.findTonalityIdByName(root);
            if (tonalityId <= 0) return false;

            if (!isBandCompleteInTonalityByRoot(userId, band, root, tonalityId)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Variante de comprobación por tonalidad usando directamente el nombre de la nota raíz.
     *
     * @param userId     identificador del usuario.
     * @param band       dificultad a verificar.
     * @param root       nombre de la raíz (por ejemplo, "C", "F#", etc.).
     * @param tonalityId identificador de tonalidad correspondiente a la raíz.
     * @return true si la banda está completa en la tonalidad dada; false en caso contrario.
     */
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

    /**
     * Calcula de forma segura el número de variantes disponibles para un tipo de escala y raíz.
     *
     * @param englishType tipo de escala en inglés (se normaliza con {@link #normalizeEnglishTypeAlias(String)}).
     * @param root        nota raíz de la tonalidad.
     * @return número de variantes encontradas; 0 si hay error o no existen.
     */
    private int safeVariantsCount(@NonNull String englishType, @NonNull String root) {
        try {
            String canonical = normalizeEnglishTypeAlias(englishType);
            List<PatternRepository.PatternVariant> vs =
                    patternRepo.getVariantsByTypeAndRoot(canonical, root);
            return (vs != null) ? vs.size() : 0;
        } catch (Throwable t) {
            Log.e(TAG, "safeVariantsCount error for " + englishType + " / " + root + ": " + t.getMessage());
            return 0;
        }
    }

    /**
     * Obtiene el nombre de nota (raíz) para un identificador de tonalidad.
     *
     * @param tonalityId identificador de tonalidad.
     * @return nombre de la raíz si se encuentra; null en caso contrario.
     */
    private String nameForTonalityId(long tonalityId) {
        final String[] NOTE_NAMES = {"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"};
        for (String n : NOTE_NAMES) {
            long id = progressionRepo.findTonalityIdByName(n);
            if (id == tonalityId) return n;
        }
        return null;
    }

    /**
     * Mapea un nombre de escala en español proveniente de base de datos a su tipo inglés canónico.
     * La salida se normaliza posteriormente por {@link #normalizeEnglishTypeAlias(String)}.
     *
     * @param es nombre en español.
     * @return nombre canónico en inglés si existe mapeo; en su defecto el nombre recibido normalizado.
     */
    private String mapDbNameToEnglishType(String es) {
        if (es == null) return "";
        String s = es.trim().toLowerCase(Locale.ROOT);
        HashMap<String, String> inv = new HashMap<>();
        inv.put("pentatónica mayor", "Major Pentatonic");
        inv.put("pentatónica menor", "Minor Pentatonic");
        inv.put("blues", "Blues");
        inv.put("mayor", "Ionian");
        inv.put("menor natural", "Aeolian");
        inv.put("dórica", "Dorian");
        inv.put("mixolidia", "Mixolydian");
        inv.put("lidia", "Lydian");
        inv.put("frigia", "Phrygian");
        inv.put("locria", "Locrian");
        inv.put("frigia dominante", "Phrygian Dominant");
        inv.put("modo flamenco (mayor-frigio)", "Phrygian Dominant");
        inv.put("doble armónica mayor", "Double Harmonic Major");
        inv.put("española 8 tonos", "Spanish 8-Tone");
        inv.put("menor armónica", "Harmonic Minor");
        inv.put("menor melódica", "Melodic Minor (Asc)");

        String en = inv.get(s);
        return normalizeEnglishTypeAlias(en != null ? en : es);
    }

    /**
     * Normaliza alias comunes del tipo de escala en inglés hacia una forma canónica.
     * Mantiene la capitalización de la primera letra para entradas no reconocidas.
     *
     * @param st nombre de tipo en inglés o alias.
     * @return representación canónica (por ejemplo, "Phrygian Dominant", "Major Pentatonic").
     */
    private String normalizeEnglishTypeAlias(String st) {
        if (st == null) return "";
        String k = st.trim().toLowerCase(Locale.ROOT);
        if (k.equals("ionion")) return "Ionian";
        if (k.equals("flamenco mode (major-phrygian)")
                || k.equals("flamenco mode")
                || k.equals("major-phrygian")
                || k.equals("major phrygian")
                || k.equals("spanish phrygian")
                || k.equals("phrygian dominant (flamenco)")) {
            return "Phrygian Dominant";
        }
        if (k.equals("pentatonic major") || k.equals("major pentatonic")) return "Major Pentatonic";
        if (k.equals("pentatonic minor") || k.equals("minor pentatonic")) return "Minor Pentatonic";
        if (k.equals("spanish 8 tone") || k.equals("spanish 8-tone")) return "Spanish 8-Tone";
        if (k.equals("melodic minor (asc)") || k.equals("melodic minor asc") || k.equals("melodic minor")) {
            return "Melodic Minor (Asc)";
        }
        return st.substring(0, 1).toUpperCase(Locale.ROOT) + st.substring(1);
    }

    /**
     * Mapea un valor de tier a la dificultad agregada.
     *
     * @param tier nivel entero de dificultad (0, 1, 2...).
     * @return dificultad EASY para tier ≤ 0; MEDIUM para 1; HARD para el resto.
     */
    private static Difficulty mapTierToDifficulty(int tier) {
        if (tier <= 0) return Difficulty.EASY;
        if (tier == 1) return Difficulty.MEDIUM;
        return Difficulty.HARD;
    }

    /**
     * Mapea una dificultad agregada a su tier entero.
     *
     * @param d dificultad (EASY, MEDIUM, HARD).
     * @return 0 para EASY, 1 para MEDIUM, 2 para HARD.
     */
    private static int mapDifficultyToTier(Difficulty d) {
        switch (d) {
            case EASY:
                return 0;
            case MEDIUM:
                return 1;
            default:
                return 2;
        }
    }

    /**
     * Devuelve el número de cajas requeridas para considerar completada una escala de un tier.
     * Por el momento, la política es fija e independiente del tier.
     *
     * @param tier nivel entero de dificultad.
     * @return número de cajas requeridas (constante).
     */
    private int requiredBoxesForTier(int tier) {
        return 3;
    }
}
