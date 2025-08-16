package com.tuguitar.todoacorde.scales.domain.usecase;

import androidx.annotation.Nullable;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.tuguitar.todoacorde.scales.data.PatternRepository;
import com.tuguitar.todoacorde.scales.data.entity.ScaleEntity;
import com.tuguitar.todoacorde.scales.domain.repository.ProgressionRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Marca como completada una caja y devuelve información de desbloqueos/resultados
 * teniendo en cuenta la tonalidad actual y el tier de la escala.
 *
 * Notas importantes:
 * - A partir de esta versión, el campo {@code tierCompletedForTonality} (y su alias {@code tierCompleted})
 *   es TRUE solo si el tier se ha completado JUSTO AHORA (antes no lo estaba). Así evitamos
 *   mostrar repetidamente el toast de “¡Nuevas escalas desbloqueadas!”.
 * - La comprobación de completitud por tier usa las variantes reales disponibles para la
 *   tonalidad (required = min(cajasCatálogo, variantes(typeEN, root))).
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

    public static final class Result {
        /** Próxima caja desbloqueada (solo si realmente es NUEVA) o null si no hay / ya estaba hecha. */
        @Nullable public final Integer nextBoxOrder;

        /** true si tras esta acción la escala ha quedado completa en ESTA tonalidad (estado actual). */
        public final boolean scaleCompletedForTonality;

        /** true si tras esta acción la escala ha quedado completa en TODAS las tonalidades (estado actual). */
        public final boolean scaleCompletedAllTonalities;

        /**
         * true SOLO si el tier ha quedado COMPLETO EN ESTA TONALIDAD JUSTO AHORA
         * (antes de registrar la caja no estaba completo).
         *
         * Alias de compatibilidad: {@link #tierCompleted}
         */
        public final boolean tierCompletedForTonality;

        /** Alias para compatibilidad con el ViewModel (mismo valor que {@link #tierCompletedForTonality}). */
        public final boolean tierCompleted;

        /** true si la escala ha quedado completa PARA ESTA TONALIDAD JUSTO AHORA. */
        public final boolean justCompletedForTonality;

        /** true si la escala ha quedado completa EN TODAS LAS TONALIDADES JUSTO AHORA. */
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
            this.tierCompleted = tierCompletedForTonality_justNow; // compatibilidad con el VM
            this.justCompletedForTonality = justCompletedForTonality;
            this.justCompletedAllTonalities = justCompletedAllTonalities;
        }
    }

    /**
     * @param userId Usuario
     * @param scaleId Escala (catálogo)
     * @param tonalityId Tonalidad (C, C#, ...), afecta a los desbloqueos
     * @param boxOrderJustCompleted Caja recién completada (1..max)
     * @param scaleTier Tier de la escala (para comprobar cierre de tier por tonalidad)
     * @param nowUtc timestamp
     */
    public Result execute(long userId,
                          long scaleId,
                          long tonalityId,
                          int boxOrderJustCompleted,
                          int scaleTier,
                          long nowUtc) {

        // =========================
        // Estado PREVIO (“just now”)
        // =========================
        boolean wasScaleCompletedForTonality =
                progressionRepo.isScaleCompletedForTonality(userId, scaleId, tonalityId);
        boolean wasScaleCompletedAllTonalities =
                progressionRepo.isScaleFullyCompletedAllTonalities(userId, scaleId);

        // Para detectar si el TIER se completa “justo ahora” usamos la versión FILTRADA por variantes reales.
        final String root = nameForTonalityId(tonalityId);
        final Map<Long, Integer> requiredByScale_BEFORE =
                computeRequiredByScaleForTierAndRoot(scaleTier, root);

        boolean wasTierCompletedForTonality =
                progressionRepo.isTierCompletedForTonalityFiltered(
                        userId, scaleTier, tonalityId, requiredByScale_BEFORE
                );

        // ==========================================
        // Guardar la caja y calcular “siguiente caja”
        // ==========================================
        Integer nextUnlocked = progressionRepo.completeBoxAndGetNext(
                userId, scaleId, tonalityId, boxOrderJustCompleted, nowUtc
        );

        // =========================
        // Estado ACTUAL tras guardar
        // =========================
        boolean scaleCompletedForTonality =
                progressionRepo.isScaleCompletedForTonality(userId, scaleId, tonalityId);
        boolean scaleCompletedAllTonalities =
                progressionRepo.isScaleFullyCompletedAllTonalities(userId, scaleId);

        boolean justCompletedForTonality =
                !wasScaleCompletedForTonality && scaleCompletedForTonality;
        boolean justCompletedAllTonalities =
                !wasScaleCompletedAllTonalities && scaleCompletedAllTonalities;

        // Reutilizamos el mismo mapa (las variantes no cambian por completar una caja)
        boolean nowTierCompletedForTonality =
                progressionRepo.isTierCompletedForTonalityFiltered(
                        userId, scaleTier, tonalityId, requiredByScale_BEFORE
                );

        // “Justo ahora”: true solo si antes NO estaba completo y ahora SÍ lo está.
        boolean tierJustCompleted = !wasTierCompletedForTonality && nowTierCompletedForTonality;

        return new Result(
                nextUnlocked,
                scaleCompletedForTonality,
                scaleCompletedAllTonalities,
                tierJustCompleted,            // <- cambia semántica para el VM (solo “just now”)
                justCompletedForTonality,
                justCompletedAllTonalities
        );
    }

    // =============================================================================================
    // Soporte: construcción del mapa requiredByScale para el tier y tonalidad
    // =============================================================================================

    /**
     * requiredByScale[scaleId] = min( cajasCatálogo(=getMaxBoxOrder), variantesDisponibles(EN, root) )
     * Si una escala no tiene variantes para esa tonalidad → no participa (required=0/omitida).
     */
    private Map<Long, Integer> computeRequiredByScaleForTierAndRoot(int tier, @Nullable String root) {
        Map<Long, Integer> out = new HashMap<>();
        if (root == null || root.isEmpty()) return out;

        List<ScaleEntity> scales = progressionRepo.getScalesByTier(tier);
        if (scales == null || scales.isEmpty()) return out;

        for (ScaleEntity s : scales) {
            String typeEn = mapDbNameToEnglishType(s.name);  // ES → EN canónico
            int variants = 0;
            try {
                variants = patternRepo.getVariantsByTypeAndRoot(typeEn, root).size();
            } catch (Throwable ignore) {
                variants = 0;
            }
            Integer catalogMax = progressionRepo.getMaxBoxOrder(s.id);
            int maxBoxes = (catalogMax == null || catalogMax <= 0) ? 3 : catalogMax;
            int required = Math.min(maxBoxes, Math.max(0, variants));
            if (required > 0) {
                out.put(s.id, required);
            }
        }
        return out;
    }

    // =============================================================================================
    // Helpers: mapeos y nombre de la tonalidad
    // =============================================================================================

    /** Deducción del nombre de root a partir del id, reusando el repositorio. */
    @Nullable
    private String nameForTonalityId(long tonalityId) {
        final String[] NOTE_NAMES = {"C","C#","D","D#","E","F","F#","G","G#","A","A#","B"};
        for (String n : NOTE_NAMES) {
            long id = progressionRepo.findTonalityIdByName(n);
            if (id == tonalityId) return n;
        }
        return null;
    }

    /** ES → EN canónico para PatternRepository (coherente con VM/UseCases). */
    private String mapDbNameToEnglishType(String esRaw) {
        if (esRaw == null) return "";
        String s = esRaw.trim().toLowerCase(Locale.ROOT);
        Map<String, String> inv = new HashMap<>();

        // Pentatónicas / Blues
        inv.put("pentatónica mayor", "Major Pentatonic");
        inv.put("pentatónica menor", "Minor Pentatonic");
        inv.put("blues",             "Blues");

        // Modos
        inv.put("mayor",          "Ionian");
        inv.put("menor natural",  "Aeolian");
        inv.put("dórica",         "Dorian");
        inv.put("mixolidia",      "Mixolydian");
        inv.put("lidia",          "Lydian");
        inv.put("frigia",         "Phrygian");
        inv.put("locria",         "Locrian");

        // Flamenco / exóticos
        inv.put("frigia dominante",             "Phrygian Dominant");
        inv.put("modo flamenco (mayor-frigio)", "Phrygian Dominant"); // alias
        inv.put("doble armónica mayor",         "Double Harmonic Major");
        inv.put("española 8 tonos",             "Spanish 8-Tone");

        // Menores compuestas
        inv.put("menor armónica",  "Harmonic Minor");
        inv.put("menor melódica",  "Melodic Minor (Asc)");

        String en = inv.get(s);
        return normalizeEnglishTypeAlias(en != null ? en : esRaw);
    }

    /** Normaliza alias/typos ingleses a canónicos, alineado con el VM. */
    private String normalizeEnglishTypeAlias(String st) {
        if (st == null) return "";
        String k = st.trim().toLowerCase(Locale.ROOT);

        // Typos comunes
        if (k.equals("ionion")) return "Ionian";

        // Flamenco / Frigio mayor
        if (k.equals("flamenco mode (major-phrygian)")
                || k.equals("flamenco mode")
                || k.equals("major-phrygian")
                || k.equals("major phrygian")
                || k.equals("spanish phrygian")
                || k.equals("phrygian dominant (flamenco)")) {
            return "Phrygian Dominant";
        }

        // Pentatónicas
        if (k.equals("pentatonic major")) return "Major Pentatonic";
        if (k.equals("pentatonic minor")) return "Minor Pentatonic";

        // Spanish 8-Tone
        if (k.equals("spanish 8 tone")) return "Spanish 8-Tone";

        // Melodic minor
        if (k.equals("melodic minor (asc)") || k.equals("melodic minor asc") || k.equals("melodic minor")) {
            return "Melodic Minor (Asc)";
        }

        // Capitaliza primera letra por defecto
        return st.substring(0,1).toUpperCase(Locale.ROOT) + st.substring(1);
    }
}
