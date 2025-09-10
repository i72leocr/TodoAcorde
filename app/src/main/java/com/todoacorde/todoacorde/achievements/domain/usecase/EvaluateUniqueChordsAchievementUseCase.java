package com.todoacorde.todoacorde.achievements.domain.usecase;

import com.todoacorde.todoacorde.AppExecutors;
import com.todoacorde.todoacorde.EvaluateAchievementUseCase;
import com.todoacorde.todoacorde.FamilyId;
import com.todoacorde.todoacorde.achievements.data.Achievement;
import com.todoacorde.todoacorde.achievements.data.AchievementDao;
import com.todoacorde.todoacorde.achievements.data.AchievementDefinitionDao;
import com.todoacorde.todoacorde.achievements.data.AchievementDefinitionEntity;
import com.todoacorde.todoacorde.achievements.data.AchievementEntity;
import com.todoacorde.todoacorde.practice.data.PracticeDetail;
import com.todoacorde.todoacorde.practice.data.PracticeDetailDao;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

/**
 * Caso de uso que evalúa y actualiza el logro de “acordes únicos” practicados.
 *
 * Recorre los detalles de práctica y cuenta cuántos acordes distintos
 * han sido acertados al menos una vez por el usuario, comparando el total
 * con los umbrales BRONZE/SILVER/GOLD definidos para la familia.
 *
 * Reglas:
 * - El progreso por nivel se satura a su umbral.
 * - Si GOLD ya está completo (y su umbral es positivo), se omite el recálculo.
 * - La evaluación se ejecuta en el ejecutor de disco de {@link AppExecutors}.
 */
public class EvaluateUniqueChordsAchievementUseCase implements EvaluateAchievementUseCase {

    /** Nombre crudo de la familia; se normaliza a ID con {@link FamilyId#of(String)}. */
    private static final String RAW_FAMILY_TITLE = "Acorde Único";

    private final AchievementDao achievementDao;
    private final AppExecutors appExecutors;
    private final PracticeDetailDao practiceDetailDao;
    private final AchievementDefinitionDao definitionDao;

    /** Caché de umbrales por familia. Índices: 0=BRONZE, 1=SILVER, 2=GOLD. */
    private final Map<String, int[]> thresholdsCache = new HashMap<>();

    /**
     * Crea el caso de uso con sus dependencias.
     *
     * @param achievementDao   DAO de estados/progresos de logros.
     * @param appExecutors     ejecutores para trabajo off-main.
     * @param practiceDetailDao DAO de detalles de práctica (aciertos por acorde).
     * @param definitionDao    DAO de definiciones de logros.
     */
    @Inject
    public EvaluateUniqueChordsAchievementUseCase(
            AchievementDao achievementDao,
            AppExecutors appExecutors,
            PracticeDetailDao practiceDetailDao,
            AchievementDefinitionDao definitionDao
    ) {
        this.achievementDao = achievementDao;
        this.appExecutors = appExecutors;
        this.practiceDetailDao = practiceDetailDao;
        this.definitionDao = definitionDao;
    }

    /**
     * Ejecuta la evaluación del logro de acordes únicos.
     * Cuenta cuántos {@link PracticeDetail} tienen aciertos positivos por acorde
     * y actualiza los progresos BRONZE, SILVER y GOLD conforme a los umbrales definidos.
     */
    @Override
    public void evaluate() {
        appExecutors.diskIO().execute(() -> {
            List<PracticeDetail> details = practiceDetailDao.getAllRaw();
            Set<Integer> uniqueIds = new HashSet<>();
            for (PracticeDetail detail : details) {
                if (detail.correctCount > 0) {
                    uniqueIds.add(detail.chordId);
                }
            }
            int totalUnique = uniqueIds.size();

            String familyId = FamilyId.of(RAW_FAMILY_TITLE).asString();

            int[] thresholds = getCachedThresholds(familyId);
            if (!hasValidThresholds(thresholds)) {
                return;
            }

            int bronzeThreshold = thresholds[0];
            int silverThreshold = thresholds[1];
            int goldThreshold = thresholds[2];

            // Si GOLD ya está completo y su umbral es válido, no recalcular.
            AchievementEntity existingGold = fetchExistingAchievement(familyId, Achievement.Level.GOLD);
            if (existingGold != null && goldThreshold > 0 && existingGold.getProgress() >= goldThreshold) {
                return;
            }

            List<AchievementEntity> achievementsToPersist = new ArrayList<>();

            int bronzeProg = Math.min(totalUnique, bronzeThreshold);
            achievementsToPersist.add(
                    AchievementEntity.fromDomain(familyId, Achievement.Level.BRONZE, bronzeProg)
            );
            if (totalUnique < bronzeThreshold) {
                achievementDao.upsertAll(achievementsToPersist);
                return;
            }

            int silverProg = Math.min(totalUnique, silverThreshold);
            achievementsToPersist.add(
                    AchievementEntity.fromDomain(familyId, Achievement.Level.SILVER, silverProg)
            );
            if (totalUnique < silverThreshold) {
                achievementDao.upsertAll(achievementsToPersist);
                return;
            }

            int goldProg = Math.min(totalUnique, goldThreshold);
            achievementsToPersist.add(
                    AchievementEntity.fromDomain(familyId, Achievement.Level.GOLD, goldProg)
            );

            if (!achievementsToPersist.isEmpty()) {
                achievementDao.upsertAll(achievementsToPersist);
            }
        });
    }

    /**
     * Comprueba que el arreglo de umbrales sea válido.
     *
     * @param thresholds arreglo de tres posiciones BRONZE/SILVER/GOLD.
     * @return true si tiene longitud 3 y al menos un umbral positivo.
     */
    private boolean hasValidThresholds(int[] thresholds) {
        return thresholds != null
                && thresholds.length == 3
                && (thresholds[0] > 0 || thresholds[1] > 0 || thresholds[2] > 0);
    }

    /**
     * Obtiene, tolerando errores en tiempo de ejecución, el estado actual de una familia/nivel.
     *
     * @param familyId identificador de familia.
     * @param level    nivel de logro.
     * @return entidad encontrada o null si no existe o hay incompatibilidades de firma.
     */
    private AchievementEntity fetchExistingAchievement(String familyId, Achievement.Level level) {
        try {
            return achievementDao.getByFamilyAndLevel(familyId, level);
        } catch (NoSuchMethodError | RuntimeException e) {
            return null;
        }
    }

    /**
     * Carga umbrales desde definiciones persistidas para una familia dada.
     * El orden del arreglo devuelto es 0=BRONZE, 1=SILVER, 2=GOLD.
     *
     * @param familyId identificador de familia.
     * @return arreglo de umbrales; valores no definidos se reportan como 0.
     */
    private int[] loadThresholds(String familyId) {
        List<AchievementDefinitionEntity> defs = definitionDao.getDefinitionsForFamily(familyId);
        int bronze = 0, silver = 0, gold = 0;
        if (defs != null) {
            for (AchievementDefinitionEntity def : defs) {
                Achievement.Level level = def.getLevel();
                if (level == null) continue;
                switch (level) {
                    case BRONZE:
                        bronze = def.getThreshold();
                        break;
                    case SILVER:
                        silver = def.getThreshold();
                        break;
                    case GOLD:
                        gold = def.getThreshold();
                        break;
                    default:
                        break;
                }
            }
        }
        return new int[]{bronze, silver, gold};
    }

    /**
     * Devuelve los umbrales de la familia desde caché si existen; si no, los carga y cachea.
     *
     * @param familyId identificador de familia.
     * @return arreglo de umbrales BRONZE/SILVER/GOLD.
     */
    private int[] getCachedThresholds(String familyId) {
        return thresholdsCache.computeIfAbsent(familyId, this::loadThresholds);
    }
}
