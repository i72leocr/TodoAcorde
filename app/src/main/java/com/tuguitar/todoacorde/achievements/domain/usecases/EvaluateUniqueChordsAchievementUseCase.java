package com.tuguitar.todoacorde.achievements.domain.usecases;

import com.tuguitar.todoacorde.achievements.data.Achievement;
import com.tuguitar.todoacorde.achievements.data.AchievementDao;
import com.tuguitar.todoacorde.achievements.data.AchievementDefinitionDao;
import com.tuguitar.todoacorde.achievements.data.AchievementDefinitionEntity;
import com.tuguitar.todoacorde.achievements.data.AchievementEntity;
import com.tuguitar.todoacorde.AppExecutors;
import com.tuguitar.todoacorde.EvaluateAchievementUseCase;
import com.tuguitar.todoacorde.FamilyId;
import com.tuguitar.todoacorde.practice.data.PracticeDetail;
import com.tuguitar.todoacorde.practice.data.PracticeDetailDao;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class EvaluateUniqueChordsAchievementUseCase implements EvaluateAchievementUseCase {

    private static final String RAW_FAMILY_TITLE = "Acorde Único";

    private final AchievementDao achievementDao;
    private final AppExecutors appExecutors;
    private final PracticeDetailDao practiceDetailDao;
    private final AchievementDefinitionDao definitionDao;

    // Cache de thresholds por familia
    private final Map<String, int[]> thresholdsCache = new HashMap<>();

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
                return; // no hay definiciones válidas
            }

            int bronzeThreshold = thresholds[0];
            int silverThreshold = thresholds[1];
            int goldThreshold = thresholds[2];

            // Short-circuit: si GOLD ya está completo no se reevalúa
            AchievementEntity existingGold = fetchExistingAchievement(familyId, Achievement.Level.GOLD);
            if (existingGold != null && goldThreshold > 0 && existingGold.getProgress() >= goldThreshold) {
                return;
            }

            List<AchievementEntity> achievementsToPersist = new ArrayList<>();

            // BRONZE siempre aparece
            int bronzeProg = Math.min(totalUnique, bronzeThreshold);
            achievementsToPersist.add(
                    AchievementEntity.fromDomain(familyId, Achievement.Level.BRONZE, bronzeProg)
            );
            if (totalUnique < bronzeThreshold) {
                achievementDao.upsertAll(achievementsToPersist);
                return;
            }

            // SILVER aparece cuando BRONZE completado
            int silverProg = Math.min(totalUnique, silverThreshold);
            achievementsToPersist.add(
                    AchievementEntity.fromDomain(familyId, Achievement.Level.SILVER, silverProg)
            );
            if (totalUnique < silverThreshold) {
                achievementDao.upsertAll(achievementsToPersist);
                return;
            }

            // GOLD aparece cuando SILVER completado
            int goldProg = Math.min(totalUnique, goldThreshold);
            achievementsToPersist.add(
                    AchievementEntity.fromDomain(familyId, Achievement.Level.GOLD, goldProg)
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

    private AchievementEntity fetchExistingAchievement(String familyId, Achievement.Level level) {
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
}
