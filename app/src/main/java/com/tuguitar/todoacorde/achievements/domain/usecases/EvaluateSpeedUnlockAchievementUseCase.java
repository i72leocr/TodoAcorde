package com.tuguitar.todoacorde.achievements.domain.usecases;

import com.tuguitar.todoacorde.Achievement;
import com.tuguitar.todoacorde.AchievementDao;
import com.tuguitar.todoacorde.achievements.data.AchievementDefinitionDao;
import com.tuguitar.todoacorde.achievements.data.AchievementDefinitionEntity;
import com.tuguitar.todoacorde.AchievementEntity;
import com.tuguitar.todoacorde.AppExecutors;
import com.tuguitar.todoacorde.EvaluateAchievementUseCase;
import com.tuguitar.todoacorde.FamilyId;
import com.tuguitar.todoacorde.SessionManager;
import com.tuguitar.todoacorde.practice.data.SongUserSpeedDao;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EvaluateSpeedUnlockAchievementUseCase implements EvaluateAchievementUseCase {

    private static final String RAW_FAMILY_MODERATO = "Moderato Maestro";
    private static final String RAW_FAMILY_NORMAL  = "Norma Legendaria";

    private final AchievementDao achievementDao;
    private final AppExecutors appExecutors;
    private final SongUserSpeedDao songUserSpeedDao;
    private final SessionManager sessionManager;
    private final AchievementDefinitionDao definitionDao;

    // Cache de thresholds por familia para evitar reconsultas
    private final Map<String, int[]> thresholdsCache = new HashMap<>();

    @Inject
    public EvaluateSpeedUnlockAchievementUseCase(
            AchievementDao achievementDao,
            AppExecutors appExecutors,
            SongUserSpeedDao songUserSpeedDao,
            SessionManager sessionManager,
            AchievementDefinitionDao definitionDao
    ) {
        this.achievementDao      = achievementDao;
        this.appExecutors        = appExecutors;
        this.songUserSpeedDao    = songUserSpeedDao;
        this.sessionManager      = sessionManager;
        this.definitionDao       = definitionDao;
    }

    @Override
    public void evaluate() {
        appExecutors.diskIO().execute(() -> {
            int userId = (int) sessionManager.getCurrentUserId();

            int countModerato = songUserSpeedDao.countUnlockedModerato(userId);
            int countNormal   = songUserSpeedDao.countUnlockedNormal(userId);

            List<AchievementEntity> achievementsToPersist = new ArrayList<>();

            String familyModeratoId = FamilyId.of(RAW_FAMILY_MODERATO).asString();
            String familyNormalId   = FamilyId.of(RAW_FAMILY_NORMAL).asString();

            int[] moderatoThresholds = getCachedThresholds(familyModeratoId);
            int[] normalThresholds   = getCachedThresholds(familyNormalId);

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

    private boolean hasValidThresholds(int[] thresholds) {
        return thresholds != null
                && thresholds.length == 3
                && (thresholds[0] > 0 || thresholds[1] > 0 || thresholds[2] > 0);
    }

    private void processFamilyIfNeeded(String familyId, int[] thresholds, int progress, List<AchievementEntity> out) {
        // Comprueba si GOLD ya está completado y, si es así, no se reevalúa
        AchievementEntity existingGold = fetchExistingAchievement(familyId, Achievement.Level.GOLD);
        int goldThreshold = thresholds[2];
        if (existingGold != null && goldThreshold > 0) {
            if (existingGold.getProgress() >= goldThreshold) {
                return; // GOLD ya completo, saltamos
            }
        }

        out.addAll(getAchievementsForFamily(familyId, thresholds, progress));
    }

    private AchievementEntity fetchExistingAchievement(String familyId, Achievement.Level level) {
        try {
            return achievementDao.getByFamilyAndLevel(familyId, level);
        } catch (NoSuchMethodError | RuntimeException e) {
            return null;
        }
    }

    // Devuelve los thresholds cargados desde BD (bronze, silver, gold)
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

    private int[] getCachedThresholds(String familyId) {
        return thresholdsCache.computeIfAbsent(familyId, this::loadThresholds);
    }

    // Devuelve los logros alcanzados para una familia con progresión escalonada
    private List<AchievementEntity> getAchievementsForFamily(String familyId, int[] thresholds, int progress) {
        List<AchievementEntity> list = new ArrayList<>();

        int bronzeThreshold = thresholds[0];
        int silverThreshold = thresholds[1];
        int goldThreshold   = thresholds[2];

        // BRONZE siempre existe: progreso entre 0 y bronzeThreshold
        int bronzeProgress = Math.min(progress, bronzeThreshold);
        list.add(AchievementEntity.fromDomain(familyId, Achievement.Level.BRONZE, bronzeProgress));

        if (progress < bronzeThreshold) {
            return list; // aún no desbloqueado SILVER
        }

        // SILVER aparece cuando BRONZE completado
        int silverProgress = Math.min(progress, silverThreshold);
        list.add(AchievementEntity.fromDomain(familyId, Achievement.Level.SILVER, silverProgress));

        if (progress < silverThreshold) {
            return list; // aún no toca GOLD
        }

        // GOLD aparece cuando SILVER completado
        int goldProgress = Math.min(progress, goldThreshold);
        list.add(AchievementEntity.fromDomain(familyId, Achievement.Level.GOLD, goldProgress));

        return list;
    }
}
