package com.todoacorde.todoacorde.achievements.domain.usecase;

import com.todoacorde.todoacorde.AppExecutors;
import com.todoacorde.todoacorde.EvaluateAchievementUseCase;
import com.todoacorde.todoacorde.FamilyId;
import com.todoacorde.todoacorde.SessionManager;
import com.todoacorde.todoacorde.achievements.data.Achievement;
import com.todoacorde.todoacorde.achievements.data.AchievementDao;
import com.todoacorde.todoacorde.achievements.data.AchievementDefinitionDao;
import com.todoacorde.todoacorde.achievements.data.AchievementDefinitionEntity;
import com.todoacorde.todoacorde.achievements.data.AchievementEntity;
import com.todoacorde.todoacorde.practice.data.SongUserSpeedDao;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

/**
 * Caso de uso que evalúa y actualiza logros de desbloqueo por velocidad alcanzada.
 *
 * Obtiene, para el usuario actual, el número de canciones con tempo desbloqueado
 * en los modos "Moderato" y "Normal" y actualiza el progreso de las familias
 * correspondientes. Los umbrales BRONZE/SILVER/GOLD se leen de definiciones
 * persistidas y se cachean en memoria.
 *
 * Reglas:
 * - El progreso por nivel se satura a su umbral.
 * - Si GOLD ya está completo (y su umbral es positivo), se omite el recálculo.
 * - La evaluación se ejecuta en el ejecutor de disco de {@link AppExecutors}.
 */
public class EvaluateSpeedUnlockAchievementUseCase implements EvaluateAchievementUseCase {

    /** Nombre crudo de la familia “Moderato”; se normaliza a ID con {@link FamilyId#of(String)}. */
    private static final String RAW_FAMILY_MODERATO = "Moderato Maestro";
    /** Nombre crudo de la familia “Normal”; se normaliza a ID con {@link FamilyId#of(String)}. */
    private static final String RAW_FAMILY_NORMAL = "Norma Legendaria";

    private final AchievementDao achievementDao;
    private final AppExecutors appExecutors;
    private final SongUserSpeedDao songUserSpeedDao;
    private final SessionManager sessionManager;
    private final AchievementDefinitionDao definitionDao;

    /** Caché de umbrales por familia. Índices: 0=BRONZE, 1=SILVER, 2=GOLD. */
    private final Map<String, int[]> thresholdsCache = new HashMap<>();

    /**
     * Crea el caso de uso con sus dependencias.
     *
     * @param achievementDao   DAO de estados/progresos de logros.
     * @param appExecutors     ejecutores para trabajo off-main.
     * @param songUserSpeedDao DAO para métricas de velocidad por canción/usuario.
     * @param sessionManager   gestor de sesión para obtener el usuario actual.
     * @param definitionDao    DAO de definiciones de logros.
     */
    @Inject
    public EvaluateSpeedUnlockAchievementUseCase(
            AchievementDao achievementDao,
            AppExecutors appExecutors,
            SongUserSpeedDao songUserSpeedDao,
            SessionManager sessionManager,
            AchievementDefinitionDao definitionDao
    ) {
        this.achievementDao = achievementDao;
        this.appExecutors = appExecutors;
        this.songUserSpeedDao = songUserSpeedDao;
        this.sessionManager = sessionManager;
        this.definitionDao = definitionDao;
    }

    /**
     * Ejecuta la evaluación de logros de velocidad para el usuario actual.
     * Agrega los conteos de desbloqueos en “Moderato” y “Normal”, compara con
     * umbrales por familia y persiste los progresos resultantes.
     */
    @Override
    public void evaluate() {
        appExecutors.diskIO().execute(() -> {
            int userId = (int) sessionManager.getCurrentUserId();

            int countModerato = songUserSpeedDao.countUnlockedModerato(userId);
            int countNormal = songUserSpeedDao.countUnlockedNormal(userId);

            List<AchievementEntity> achievementsToPersist = new ArrayList<>();

            String familyModeratoId = FamilyId.of(RAW_FAMILY_MODERATO).asString();
            String familyNormalId = FamilyId.of(RAW_FAMILY_NORMAL).asString();

            int[] moderatoThresholds = getCachedThresholds(familyModeratoId);
            int[] normalThresholds = getCachedThresholds(familyNormalId);

            if (hasValidThresholds(moderatoThresholds)) {
                processFamilyIfNeeded(familyModeratoId, moderatoThresholds, countModerato, achievementsToPersist);
            }
            if (hasValidThresholds(normalThresholds)) {
                processFamilyIfNeeded(familyNormalId, normalThresholds, countNormal, achievementsToPersist);
            }

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
     * Calcula y añade al lote los progresos de una familia si procede.
     * Evita recomputar si GOLD ya está completo.
     *
     * @param familyId   identificador de familia.
     * @param thresholds umbrales BRONZE/SILVER/GOLD.
     * @param progress   progreso agregado observado.
     * @param out        lista de entidades a persistir.
     */
    private void processFamilyIfNeeded(String familyId, int[] thresholds, int progress, List<AchievementEntity> out) {
        AchievementEntity existingGold = fetchExistingAchievement(familyId, Achievement.Level.GOLD);
        int goldThreshold = thresholds[2];
        if (existingGold != null && goldThreshold > 0) {
            if (existingGold.getProgress() >= goldThreshold) {
                return;
            }
        }
        out.addAll(getAchievementsForFamily(familyId, thresholds, progress));
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

    /**
     * Genera la lista de entidades de logro a persistir para una familia dada,
     * saturando el progreso en cada umbral y cortocircuitando cuando aún no se
     * alcanza el siguiente nivel.
     *
     * @param familyId   identificador de familia.
     * @param thresholds umbrales BRONZE/SILVER/GOLD.
     * @param progress   progreso agregado observado.
     * @return lista de entidades en orden BRONZE → SILVER → GOLD.
     */
    private List<AchievementEntity> getAchievementsForFamily(String familyId, int[] thresholds, int progress) {
        List<AchievementEntity> list = new ArrayList<>();

        int bronzeThreshold = thresholds[0];
        int silverThreshold = thresholds[1];
        int goldThreshold = thresholds[2];

        int bronzeProgress = Math.min(progress, bronzeThreshold);
        list.add(AchievementEntity.fromDomain(familyId, Achievement.Level.BRONZE, bronzeProgress));
        if (progress < bronzeThreshold) {
            return list;
        }

        int silverProgress = Math.min(progress, silverThreshold);
        list.add(AchievementEntity.fromDomain(familyId, Achievement.Level.SILVER, silverProgress));
        if (progress < silverThreshold) {
            return list;
        }

        int goldProgress = Math.min(progress, goldThreshold);
        list.add(AchievementEntity.fromDomain(familyId, Achievement.Level.GOLD, goldProgress));

        return list;
    }
}
