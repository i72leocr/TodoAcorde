package com.todoacorde.todoacorde.scales.domain;

import android.util.Log;

import androidx.annotation.NonNull;

import com.todoacorde.todoacorde.scales.data.PatternRepository;
import com.todoacorde.todoacorde.scales.data.entity.ScaleEntity;
import com.todoacorde.todoacorde.scales.domain.repository.ProgressionRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Calcula el progreso de escalas por niveles (tiers) y tonalidad.
 * Se apoya en:
 * - {@link PatternRepository} para conocer los patrones disponibles por tipo/raíz.
 * - {@link ProgressionRepository} para consultar el avance realizado por el usuario.
 *
 * No modifica estado en base de datos; únicamente lee y agrega información de progreso.
 */
public class ScaleProgressionCalculator {

    private static final String TAG = "ScaleProgressionCalc";

    private final PatternRepository patternRepo;
    private final ProgressionRepository progressionRepo;

    /**
     * Modelo de datos para la UI de un ítem de tier.
     * Contiene información de desbloqueo, disponibilidad de patrones,
     * cajas completadas/total y si está completado.
     */
    public static final class TierItem {
        /** Tipo de escala en inglés normalizado (por ejemplo, "Ionian"). */
        public final String typeEn;
        /** Nombre de escala en castellano tal y como está en BD. */
        public final String typeEs;
        /** Indica si el tier está desbloqueado para el usuario. */
        public final boolean unlocked;
        /** Indica si existen patrones disponibles para esta escala y raíz. */
        public final boolean hasPatterns;
        /** Número de cajas completadas para esta escala/raíz. */
        public final int completedBoxes;
        /** Número total de cajas requeridas para esta escala/raíz. */
        public final int totalBoxes;
        /** Indica si la escala se considera completada (completedBoxes >= totalBoxes). */
        public final boolean completed;

        public TierItem(String typeEn,
                        String typeEs,
                        boolean unlocked,
                        boolean hasPatterns,
                        int completedBoxes,
                        int totalBoxes,
                        boolean completed) {
            this.typeEn = typeEn;
            this.typeEs = typeEs;
            this.unlocked = unlocked;
            this.hasPatterns = hasPatterns;
            this.completedBoxes = completedBoxes;
            this.totalBoxes = totalBoxes;
            this.completed = completed;
        }
    }

    /**
     * Crea el calculador de progresión de escalas.
     *
     * @param patternRepo repositorio de patrones de escalas
     * @param progressionRepo repositorio de progreso del usuario
     */
    public ScaleProgressionCalculator(@NonNull PatternRepository patternRepo,
                                      @NonNull ProgressionRepository progressionRepo) {
        this.patternRepo = patternRepo;
        this.progressionRepo = progressionRepo;
    }

    /**
     * Determina si un tier está COMPLETO para una tonalidad y raíz dadas.
     * Un tier se considera completo si, para cada escala relevante del tier
     * (con patrones disponibles en esa raíz), el usuario ha completado tantas
     * cajas como variantes disponibles.
     *
     * @param userId id de usuario
     * @param tier nivel a evaluar
     * @param tonalityId id de la tonalidad
     * @param root raíz musical (por ejemplo "C", "G#", etc.)
     * @return true si el tier está completo para la raíz dada, false en caso contrario
     */
    public boolean isTierCompletedForRoot(long userId,
                                          int tier,
                                          long tonalityId,
                                          @NonNull String root) {
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

    /**
     * Calcula el mayor tier consecutivo completado para una raíz y tonalidad.
     * Empieza en 0 y avanza mientras existan escalas en el tier y esté completo.
     *
     * @param userId id de usuario
     * @param tonalityId id de la tonalidad
     * @param root raíz musical
     * @return índice del último tier completado, o -1 si ninguno
     */
    public int computeMaxCompletedTierForRoot(long userId,
                                              long tonalityId,
                                              @NonNull String root) {
        int t = 0, lastCompleted = -1;
        while (progressionRepo.hasAnyScaleInTier(t) &&
                isTierCompletedForRoot(userId, t, tonalityId, root)) {
            lastCompleted = t;
            t++;
        }
        return lastCompleted;
    }

    /**
     * Construye la lista de ítems de tier para pintar en UI usando userId=1 internamente.
     * Si necesitas otro usuario, usa la sobrecarga con userId.
     *
     * @param tier nivel
     * @param root raíz musical
     * @param tonalityId tonalidad
     * @param tierUnlocked si el tier está desbloqueado
     * @return lista de {@link TierItem}
     */
    public List<TierItem> buildTierItemsFor(int tier,
                                            @NonNull String root,
                                            long tonalityId,
                                            boolean tierUnlocked) {
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
                    ? progressionRepo.getMaxCompletedBoxOrZero(
                    1L /* caller sets userId via repo usage if needed */,
                    s.id,
                    tonalityId
            )
                    : 0;
            boolean completed = hasPatterns && maxHechas >= disponibles;

            out.add(new TierItem(en, s.name, tierUnlocked, hasPatterns, maxHechas, disponibles, completed));
        }
        return out;
    }

    /**
     * Construye la lista de ítems de tier para pintar en UI, especificando userId.
     *
     * @param tier nivel
     * @param root raíz musical
     * @param tonalityId tonalidad
     * @param tierUnlocked si el tier está desbloqueado
     * @param userId id de usuario
     * @return lista de {@link TierItem}
     */
    public List<TierItem> buildTierItemsFor(int tier,
                                            @NonNull String root,
                                            long tonalityId,
                                            boolean tierUnlocked,
                                            long userId) {
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

    /**
     * Devuelve la lista no nula (vacía si es null).
     */
    private static <T> List<T> nonNull(List<T> in) {
        return in == null ? Collections.emptyList() : in;
    }

    /**
     * Une en formato legible una lista para trazas de depuración.
     */
    private static String join(List<?> l) {
        if (l == null) return "[]";
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < l.size(); i++) {
            if (i > 0) sb.append(", ");
            Object x = l.get(i);
            sb.append(x == null ? "null" : String.valueOf(x));
        }
        sb.append("]");
        return sb.toString();
    }
}
