package com.tuguitar.todoacorde.achievements.domain.usecase;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.tuguitar.todoacorde.achievements.data.AchievementDao;
import com.tuguitar.todoacorde.achievements.data.AchievementDefinitionDao;
import com.tuguitar.todoacorde.achievements.data.AchievementDefinitionEntity;
import com.tuguitar.todoacorde.achievements.data.AchievementEntity;
import com.tuguitar.todoacorde.AppExecutors;
import com.tuguitar.todoacorde.EvaluateAchievementUseCase;
import com.tuguitar.todoacorde.FamilyId;
import com.tuguitar.todoacorde.SessionManager;
import com.tuguitar.todoacorde.practice.data.PracticeSessionDao;
import com.tuguitar.todoacorde.achievements.data.Achievement.Level;

public class EvaluatePerfectScoreAchievementUseCase implements EvaluateAchievementUseCase {

    private static final String RAW_FAMILY_PERFECT = "Cien Por Ciento Rock";

    private final AchievementDao achievementDao;
    private final AppExecutors appExecutors;
    private final PracticeSessionDao sessionDao;
    private final SessionManager sessionManager;
    private final AchievementDefinitionDao definitionDao;

    // Cache de thresholds por familia
    private final Map<String, int[]> thresholdsCache = new HashMap<>();

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

    @Override
    public void evaluate() {
        appExecutors.diskIO().execute(() -> {
            long userId = sessionManager.getCurrentUserId();
            int totalPerfectSongs = sessionDao.getPerfectScoreSongIds(userId).size();

            String familyId = FamilyId.of(RAW_FAMILY_PERFECT).asString();

            int[] thresholds = getCachedThresholds(familyId);
            if (!hasValidThresholds(thresholds)) {
                return; // No hay definiciones válidas, no procesamos
            }

            int bronzeThreshold = thresholds[0];
            int silverThreshold = thresholds[1];
            int goldThreshold = thresholds[2];

            // Short-circuit: si GOLD ya está completo no se reevalúa
            AchievementEntity existingGold = fetchExistingAchievement(familyId, Level.GOLD);
            if (existingGold != null && goldThreshold > 0 && existingGold.getProgress() >= goldThreshold) {
                return;
            }

            List<AchievementEntity> achievementsToPersist = new ArrayList<>();

            // BRONZE siempre aparece
            int bronzeProg = Math.min(totalPerfectSongs, bronzeThreshold);
            achievementsToPersist.add(
                    AchievementEntity.fromDomain(familyId, Level.BRONZE, bronzeProg)
            );
            if (totalPerfectSongs < bronzeThreshold) {
                achievementDao.upsertAll(achievementsToPersist);
                return;
            }

            // SILVER aparece cuando BRONZE completado
            int silverProg = Math.min(totalPerfectSongs, silverThreshold);
            achievementsToPersist.add(
                    AchievementEntity.fromDomain(familyId, Level.SILVER, silverProg)
            );
            if (totalPerfectSongs < silverThreshold) {
                achievementDao.upsertAll(achievementsToPersist);
                return;
            }

            // GOLD aparece cuando SILVER completado
            int goldProg = Math.min(totalPerfectSongs, goldThreshold);
            achievementsToPersist.add(
                    AchievementEntity.fromDomain(familyId, Level.GOLD, goldProg)
            );

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

    private AchievementEntity fetchExistingAchievement(String familyId, Level level) {
        try {
            return achievementDao.getByFamilyAndLevel(familyId, level);
        } catch (NoSuchMethodError | RuntimeException e) {
            return null;
        }
    }

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

    private int[] getCachedThresholds(String familyId) {
        return thresholdsCache.computeIfAbsent(familyId, this::loadThresholds);
    }
}
