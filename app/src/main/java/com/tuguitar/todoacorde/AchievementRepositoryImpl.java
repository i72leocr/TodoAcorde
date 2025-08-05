package com.tuguitar.todoacorde;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class AchievementRepositoryImpl implements AchievementRepository {

    private final AchievementDefinitionDao defDao;
    private final AchievementDao achievementDao;
    private final SessionManager sessionManager;
    private final Executor ioExecutor;

    @Inject
    public AchievementRepositoryImpl(
            @NonNull AchievementDefinitionDao defDao,
            @NonNull AchievementDao achievementDao,
            @NonNull SessionManager sessionManager
    ) {
        this.defDao         = defDao;
        this.achievementDao = achievementDao;
        this.sessionManager = sessionManager;
        this.ioExecutor     = Executors.newSingleThreadExecutor();
    }

    @Override
    public LiveData<List<Achievement>> observeAll() {
        MediatorLiveData<List<Achievement>> result = new MediatorLiveData<>();
        LiveData<List<AchievementDefinitionEntity>> defsLive = defDao.observeAllDefinitions();
        LiveData<List<AchievementEntity>> uaLive = achievementDao.observeAll();

        result.addSource(defsLive, defs ->
                combine(defs, uaLive.getValue(), result)
        );
        result.addSource(uaLive, uas ->
                combine(defsLive.getValue(), uas, result)
        );

        return result;
    }

    @Override
    public void updateAchievement(@NonNull Achievement achievement) {
        final String familyId = FamilyId.of(achievement.getTitle()).asString();
        final int progress    = achievement.getProgress();

        ioExecutor.execute(() -> {
            AchievementEntity entity = AchievementEntity.fromDomain(
                    familyId,
                    achievement.getLevel(),
                    progress
            );
            achievementDao.upsert(entity);
        });
    }

    // Método para batch update de logros
    @Override
    public void updateAchievements(@NonNull List<Achievement> achievements) {
        ioExecutor.execute(() -> {
            List<AchievementEntity> entities = new ArrayList<>();
            for (Achievement achievement : achievements) {
                String familyId = FamilyId.of(achievement.getTitle()).asString();
                AchievementEntity entity = AchievementEntity.fromDomain(
                        familyId,
                        achievement.getLevel(),
                        achievement.getProgress()
                );
                entities.add(entity);
            }
            achievementDao.upsertAll(entities);
        });
    }

    @Override
    public int getProgress(String familyTitle, Achievement.Level level) {
        final String familyId = FamilyId.of(familyTitle).asString();
        AchievementEntity entity = achievementDao.getByFamilyAndLevel(familyId, level);
        return entity != null ? entity.getProgress() : 0;
    }

    @Override
    public void evaluateAll() {
        // Ejecuta todos los UseCases registrados
        ioExecutor.execute(() -> {
            for (EvaluateAchievementUseCase useCase : AchievementUseCaseRegistry.getAll()) {
                useCase.evaluate();
            }
        });
    }

    // legacy: si alguna parte aún llama a slugify directamente podrías delegar aquí.
    // private String slugify(String input) {
    //     return FamilyId.of(input).asString();
    // }

    private void combine(
            List<AchievementDefinitionEntity> defs,
            List<AchievementEntity> uas,
            MediatorLiveData<List<Achievement>> output
    ) {
        if (defs == null) return;

        Map<String, Map<Achievement.Level, Integer>> progMap = new HashMap<>();
        if (uas != null) {
            for (AchievementEntity ua : uas) {
                progMap
                        .computeIfAbsent(ua.getFamilyId(), k -> new HashMap<>())
                        .put(ua.getLevel(), ua.getProgress());
            }
        }

        List<Achievement> list = new ArrayList<>(defs.size());
        for (AchievementDefinitionEntity def : defs) {
            Map<Achievement.Level, Integer> familyProgress = progMap.getOrDefault(def.getFamilyId(), new HashMap<>());
            int progress = familyProgress.getOrDefault(def.getLevel(), 0);
            Achievement a = Achievement.createLocked(
                    def.getTitle(),
                    def.getLevel(),
                    def.getIconResId(),
                    def.getThreshold()
            ).withAddedProgress(progress);
            list.add(a);
        }
        output.setValue(list);
    }
}
