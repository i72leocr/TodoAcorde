package com.todoacorde.todoacorde.scales.domain.usecase;

import androidx.annotation.Nullable;

import com.todoacorde.todoacorde.scales.data.PatternRepository;
import com.todoacorde.todoacorde.scales.data.entity.ScaleEntity;
import com.todoacorde.todoacorde.scales.domain.repository.ProgressionRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Caso de uso para completar una caja (box) de una escala en una tonalidad.
 *
 * Flujo principal:
 * 1) Se consultan estados previos (escala/tonalidad/tier completados).
 * 2) Se marca la caja como completada (si no lo estaba) y se obtiene la siguiente pendiente.
 * 3) Se vuelven a consultar estados para determinar si se han producido “just completed”.
 */
@Singleton
public class CompleteScaleBoxUseCase {

    private final ProgressionRepository progressionRepo;
    private final PatternRepository patternRepo;

    @Inject
    public CompleteScaleBoxUseCase(ProgressionRepository progressionRepo,
                                   PatternRepository patternRepo) {
        this.progressionRepo = progressionRepo;
        this.patternRepo = patternRepo;
    }

    /**
     * Resultado del caso de uso tras intentar completar una caja.
     */
    public static final class Result {
        /* Próxima caja pendiente (boxOrder) o null si no hay más. */
        @Nullable
        public final Integer nextBoxOrder;

        /* Indica si la escala quedó completada para la tonalidad. */
        public final boolean scaleCompletedForTonality;

        /* Indica si la escala quedó completada en todas las tonalidades. */
        public final boolean scaleCompletedAllTonalities;

        /* Indica si el tier quedó completado para la tonalidad tras esta acción. */
        public final boolean tierCompletedForTonality;

        /* Alias del campo anterior para compatibilidad (mismo valor). */
        public final boolean tierCompleted;

        /* “Just completed” de escala para la tonalidad. */
        public final boolean justCompletedForTonality;

        /* “Just completed” de escala en todas las tonalidades. */
        public final boolean justCompletedAllTonalities;

        public Result(@Nullable Integer nextBoxOrder,
                      boolean scaleCompletedForTonality,
                      boolean scaleCompletedAllTonalities,
                      boolean tierCompletedForTonality_justNow,
                      boolean justCompletedForTonality,
                      boolean justCompletedAllTonalities) {
            this.nextBoxOrder = nextBoxOrder;
            this.scaleCompletedForTonality = scaleCompletedForTonality;
            this.scaleCompletedAllTonalities = scaleCompletedAllTonalities;
            this.tierCompletedForTonality = tierCompletedForTonality_justNow;
            this.tierCompleted = tierCompletedForTonality_justNow;
            this.justCompletedForTonality = justCompletedForTonality;
            this.justCompletedAllTonalities = justCompletedAllTonalities;
        }
    }

    /**
     * Ejecuta el proceso de completar una caja para un usuario/escala/tonalidad concretos.
     *
     * @param userId                 identificador de usuario
     * @param scaleId                identificador de la escala
     * @param tonalityId             identificador de la tonalidad
     * @param boxOrderJustCompleted  caja (orden) que se acaba de completar
     * @param scaleTier              tier de la escala
     * @param nowUtc                 marca temporal UTC
     * @return                       objeto Result con los flags y la siguiente caja pendiente
     */
    public Result execute(long userId,
                          long scaleId,
                          long tonalityId,
                          int boxOrderJustCompleted,
                          int scaleTier,
                          long nowUtc) {

        /* Estado previo: escala completada para tonalidad y en todas las tonalidades. */
        boolean wasScaleCompletedForTonality =
                progressionRepo.isScaleCompletedForTonality(userId, scaleId, tonalityId);
        boolean wasScaleCompletedAllTonalities =
                progressionRepo.isScaleFullyCompletedAllTonalities(userId, scaleId);

        /* Mapa de requisitos por escala para el tier y tonalidad (antes de completar). */
        final String root = nameForTonalityId(tonalityId);
        final Map<Long, Integer> requiredByScale_BEFORE =
                computeRequiredByScaleForTierAndRoot(scaleTier, root);

        /* Estado previo: tier completado para tonalidad según requisitos. */
        boolean wasTierCompletedForTonality =
                progressionRepo.isTierCompletedForTonalityFiltered(
                        userId, scaleTier, tonalityId, requiredByScale_BEFORE
                );

        /* Completa caja y obtiene la siguiente pendiente. */
        Integer nextUnlocked = progressionRepo.completeBoxAndGetNext(
                userId, scaleId, tonalityId, boxOrderJustCompleted, nowUtc
        );

        /* Estado posterior: escala completada para tonalidad y global. */
        boolean scaleCompletedForTonality =
                progressionRepo.isScaleCompletedForTonality(userId, scaleId, tonalityId);
        boolean scaleCompletedAllTonalities =
                progressionRepo.isScaleFullyCompletedAllTonalities(userId, scaleId);

        /* Flags “just completed”. */
        boolean justCompletedForTonality =
                !wasScaleCompletedForTonality && scaleCompletedForTonality;
        boolean justCompletedAllTonalities =
                !wasScaleCompletedAllTonalities && scaleCompletedAllTonalities;

        /* Estado posterior del tier con el mismo mapa de requisitos. */
        boolean nowTierCompletedForTonality =
                progressionRepo.isTierCompletedForTonalityFiltered(
                        userId, scaleTier, tonalityId, requiredByScale_BEFORE
                );
        boolean tierJustCompleted = !wasTierCompletedForTonality && nowTierCompletedForTonality;

        return new Result(
                nextUnlocked,
                scaleCompletedForTonality,
                scaleCompletedAllTonalities,
                tierJustCompleted,
                justCompletedForTonality,
                justCompletedAllTonalities
        );
    }

    /**
     * Calcula el número de cajas requeridas por escala para un tier y una tonalidad dados.
     * El requisito es min(maxBoxesCatalogadas, variantesDisponiblesPorPatrones).
     *
     * @param tier  tier de las escalas
     * @param root  nombre de la tonalidad (p. ej. C, C#, D...)
     * @return      mapa scaleId → cajas requeridas (> 0)
     */
    private Map<Long, Integer> computeRequiredByScaleForTierAndRoot(int tier, @Nullable String root) {
        Map<Long, Integer> out = new HashMap<>();
        if (root == null || root.isEmpty()) return out;

        List<ScaleEntity> scales = progressionRepo.getScalesByTier(tier);
        if (scales == null || scales.isEmpty()) return out;

        for (ScaleEntity s : scales) {
            /* Traducción de nombre en ES a tipo en EN para buscar patrones. */
            String typeEn = mapDbNameToEnglishType(s.name);

            /* Número de variantes disponibles en patrones para la raíz dada. */
            int variants = 0;
            try {
                variants = patternRepo.getVariantsByTypeAndRoot(typeEn, root).size();
            } catch (Throwable ignore) {
                variants = 0;
            }

            /* Número máximo de cajas catalogadas para esa escala. */
            Integer catalogMax = progressionRepo.getMaxBoxOrder(s.id);
            int maxBoxes = (catalogMax == null || catalogMax <= 0) ? 3 : catalogMax;

            /* Requisito final por escala. */
            int required = Math.min(maxBoxes, Math.max(0, variants));
            if (required > 0) {
                out.put(s.id, required);
            }
        }
        return out;
    }

    /**
     * Devuelve el nombre de la tonalidad a partir del id, buscando por lista fija de nombres.
     * Si no encuentra coincidencia exacta, devuelve null.
     */
    @Nullable
    private String nameForTonalityId(long tonalityId) {
        final String[] NOTE_NAMES = {"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"};
        for (String n : NOTE_NAMES) {
            long id = progressionRepo.findTonalityIdByName(n);
            if (id == tonalityId) return n;
        }
        return null;
    }

    /**
     * Mapea nombre de escala en español (DB) a tipo en inglés esperado por el repositorio de patrones.
     * Aplica además normalización de alias en inglés.
     */
    private String mapDbNameToEnglishType(String esRaw) {
        if (esRaw == null) return "";
        String s = esRaw.trim().toLowerCase(Locale.ROOT);

        Map<String, String> inv = new HashMap<>();
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
        return normalizeEnglishTypeAlias(en != null ? en : esRaw);
    }

    /**
     * Normaliza alias frecuentes de tipos de escala en inglés.
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

        if (k.equals("pentatonic major")) return "Major Pentatonic";
        if (k.equals("pentatonic minor")) return "Minor Pentatonic";
        if (k.equals("spanish 8 tone")) return "Spanish 8-Tone";
        if (k.equals("melodic minor (asc)") || k.equals("melodic minor asc") || k.equals("melodic minor")) {
            return "Melodic Minor (Asc)";
        }

        /* Capitaliza por defecto si no coincide con alias conocidos. */
        return st.substring(0, 1).toUpperCase(Locale.ROOT) + st.substring(1);
    }
}
