package com.todoacorde.todoacorde.achievements.domain.usecase;

import com.todoacorde.todoacorde.AppExecutors;
import com.todoacorde.todoacorde.EvaluateAchievementUseCase;
import com.todoacorde.todoacorde.FamilyId;
import com.todoacorde.todoacorde.SessionManager;
import com.todoacorde.todoacorde.achievements.data.Achievement.Level;
import com.todoacorde.todoacorde.achievements.data.AchievementDao;
import com.todoacorde.todoacorde.achievements.data.AchievementDefinitionDao;
import com.todoacorde.todoacorde.achievements.data.AchievementDefinitionEntity;
import com.todoacorde.todoacorde.achievements.data.AchievementEntity;
import com.todoacorde.todoacorde.practice.data.PracticeSessionDao;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

/**
 * Caso de uso que evalúa logros asociados a conseguir puntuación perfecta en canciones.
 *
 * El cálculo obtiene el número total de canciones con puntuación perfecta del usuario
 * y lo aplica sobre los umbrales BRONZE, SILVER y GOLD definidos para la familia
 * {@code RAW_FAMILY_PERFECT}. Los umbrales se cargan desde definiciones persistidas
 * y se cachean en memoria para minimizar accesos a datos.
 *
 * Reglas:
 * - Si ya se alcanzó GOLD y el umbral GOLD es positivo, no se recalcula.
 * - El progreso por nivel se satura al correspondiente umbral.
 * - Se realiza en el ejecutor de disco de {@link AppExecutors}.
 */
public class EvaluatePerfectScoreAchievementUseCase implements EvaluateAchievementUseCase {

    /** Nombre crudo de la familia tal como se usa para derivar su {@code familyId}. */
    private static final String RAW_FAMILY_PERFECT = "Cien Por Ciento Rock";

    private final AchievementDao achievementDao;
    private final AppExecutors appExecutors;
    private final PracticeSessionDao sessionDao;
    private final SessionManager sessionManager;
    private final AchievementDefinitionDao definitionDao;

    /** Caché en memoria de umbrales por familia: índice 0=BRONZE, 1=SILVER, 2=GOLD. */
    private final Map<String, int[]> thresholdsCache = new HashMap<>();

    /**
     * Crea el caso de uso con sus dependencias.
     *
     * @param achievementDao DAO de progreso de logros.
     * @param appExecutors   ejecutores para trabajo off-main.
     * @param sessionDao     DAO de sesiones de práctica (fuente de puntuaciones).
     * @param sessionManager gestor de sesión para obtener el usuario actual.
     * @param definitionDao  DAO de definiciones de logros.
     */
    @Inject
    public EvaluatePerfectScoreAchievementUseCase(
            AchievementDao achievementDao,
            AppExecutors appExecutors,
            PracticeSessionDao sessionDao,
            SessionManager sessionManager,
            AchievementDefinitionDao definitionDao
    ) {
        this.achievementDao = achievementDao;
        this.appExecutors = appExecutors;
        this.sessionDao = sessionDao;
        this.sessionManager = sessionManager;
        this.definitionDao = definitionDao;
    }

    /**
     * Ejecuta la evaluación de la familia de logros de puntuación perfecta.
     * Obtiene el total de canciones perfectas del usuario actual y actualiza
     * los progresos BRONZE, SILVER y GOLD conforme a los umbrales definidos.
     */
    @Override
    public void evaluate() {
        appExecutors.diskIO().execute(() -> {
            long userId = sessionManager.getCurrentUserId();
            int totalPerfectSongs = sessionDao.getPerfectScoreSongIds(userId).size();

            String familyId = FamilyId.of(RAW_FAMILY_PERFECT).asString();

            int[] thresholds = getCachedThresholds(familyId);
            if (!hasValidThresholds(thresholds)) {
                return;
            }

            int bronzeThreshold = thresholds[0];
            int silverThreshold = thresholds[1];
            int goldThreshold = thresholds[2];

            // Si GOLD ya está completo y el umbral es válido, no recalcular.
            AchievementEntity existingGold = fetchExistingAchievement(familyId, Level.GOLD);
            if (existingGold != null && goldThreshold > 0 && existingGold.getProgress() >= goldThreshold) {
                return;
            }

            List<AchievementEntity> achievementsToPersist = new ArrayList<>();

            int bronzeProg = Math.min(totalPerfectSongs, bronzeThreshold);
            achievementsToPersist.add(
                    AchievementEntity.fromDomain(familyId, Level.BRONZE, bronzeProg)
            );
            if (totalPerfectSongs < bronzeThreshold) {
                achievementDao.upsertAll(achievementsToPersist);
                return;
            }

            int silverProg = Math.min(totalPerfectSongs, silverThreshold);
            achievementsToPersist.add(
                    AchievementEntity.fromDomain(familyId, Level.SILVER, silverProg)
            );
            if (totalPerfectSongs < silverThreshold) {
                achievementDao.upsertAll(achievementsToPersist);
                return;
            }

            int goldProg = Math.min(totalPerfectSongs, goldThreshold);
            achievementsToPersist.add(
                    AchievementEntity.fromDomain(familyId, Level.GOLD, goldProg)
            );

            if (!achievementsToPersist.isEmpty()) {
                achievementDao.upsertAll(achievementsToPersist);
            }
        });
    }

    /**
     * Verifica que el vector de umbrales es válido.
     *
     * @param thresholds arreglo con 3 posiciones: BRONZE, SILVER y GOLD.
     * @return true si el arreglo tiene longitud 3 y al menos uno de los umbrales es positivo.
     */
    private boolean hasValidThresholds(int[] thresholds) {
        return thresholds != null
                && thresholds.length == 3
                && (thresholds[0] > 0 || thresholds[1] > 0 || thresholds[2] > 0);
    }

    /**
     * Obtiene el logro existente para una familia y nivel, tolerando ausencias o
     * incompatibilidades de firma en tiempo de ejecución.
     *
     * @param familyId identificador de familia.
     * @param level    nivel del logro.
     * @return la entidad existente o null si no se puede recuperar.
     */
    private AchievementEntity fetchExistingAchievement(String familyId, Level level) {
        try {
            return achievementDao.getByFamilyAndLevel(familyId, level);
        } catch (NoSuchMethodError | RuntimeException e) {
            return null;
        }
    }

    /**
     * Carga los umbrales de la familia desde las definiciones persistidas.
     * El orden del arreglo devuelto es: índice 0 BRONZE, 1 SILVER, 2 GOLD.
     *
     * @param familyId identificador de familia.
     * @return arreglo con los umbrales de cada nivel; valores no definidos se devuelven como 0.
     */
    private int[] loadThresholds(String familyId) {
        List<AchievementDefinitionEntity> defs = definitionDao.getDefinitionsForFamily(familyId);
        int bronze = 0, silver = 0, gold = 0;
        if (defs != null) {
            for (AchievementDefinitionEntity def : defs) {
                Level level = def.getLevel();
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
     * @return arreglo de umbrales BRONZE, SILVER y GOLD.
     */
    private int[] getCachedThresholds(String familyId) {
        return thresholdsCache.computeIfAbsent(familyId, this::loadThresholds);
    }
}
