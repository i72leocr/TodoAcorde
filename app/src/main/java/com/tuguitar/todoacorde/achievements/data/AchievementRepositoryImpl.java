package com.tuguitar.todoacorde.achievements.data;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;

import com.tuguitar.todoacorde.achievements.domain.usecase.AchievementUseCaseRegistry;
import com.tuguitar.todoacorde.EvaluateAchievementUseCase;
import com.tuguitar.todoacorde.FamilyId;
import com.tuguitar.todoacorde.SessionManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale; // <-- añadido
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
        ioExecutor.execute(() -> {
            for (EvaluateAchievementUseCase useCase : AchievementUseCaseRegistry.getAll()) {
                useCase.evaluate();
            }
        });
    }

    /**
     * Incrementa el progreso:
     *  - Familias "milestone" (threshold=1 por nivel): solo incrementa el primer nivel NO completado (BRONZE→SILVER→GOLD).
     *  - Resto (legacy): incrementa todos los niveles a la vez (mismo contador con umbrales crecientes).
     *
     * El parámetro puede ser un familyId directo (recomendado) o un título (legacy con espacios).
     */
    @Override
    public void incrementProgress(@NonNull String familyIdOrTitle, int delta) {
        // Si parece título (tiene espacios), usamos FamilyId.of(...).asString().
        // Si no tiene espacios, lo tomamos como familyId directo (ideal para los nuevos milestones).
        final String familyId = familyIdOrTitle.contains(" ")
                ? FamilyId.of(familyIdOrTitle).asString()
                : familyIdOrTitle.toLowerCase(Locale.ROOT);

        ioExecutor.execute(() -> {
            // Identificar si es una familia "milestone" (las que creaste en el seeder)
            boolean isMilestone =
                    "scales_one_tonality_milestone".equalsIgnoreCase(familyId) ||
                            "scales_all_tonalities_milestone".equalsIgnoreCase(familyId);

            if (isMilestone) {
                // Política "milestone": avanzar SOLO el primer nivel no completado, clamp 0..1
                Achievement.Level[] order = new Achievement.Level[] {
                        Achievement.Level.BRONZE,
                        Achievement.Level.SILVER,
                        Achievement.Level.GOLD
                };
                for (Achievement.Level level : order) {
                    AchievementEntity current = achievementDao.getByFamilyAndLevel(familyId, level);
                    int prev = (current != null) ? current.getProgress() : 0;

                    // Threshold de milestones en tu seeder = 1. Clampeamos a 0..1
                    if (prev < 1) {
                        int next = prev + delta;
                        if (next < 0) next = 0;
                        if (next > 1) next = 1;

                        AchievementEntity updated = AchievementEntity.fromDomain(
                                familyId,
                                level,
                                next
                        );
                        achievementDao.upsert(updated);
                        // Solo avanzamos un nivel por llamada
                        break;
                    }
                }
            } else {
                // Política legacy (mismo contador en los 3 niveles, umbrales crecientes)
                for (Achievement.Level level : Achievement.Level.values()) {
                    AchievementEntity current = achievementDao.getByFamilyAndLevel(familyId, level);
                    int prev = (current != null) ? current.getProgress() : 0;
                    int next = Math.max(0, prev + delta);

                    AchievementEntity updated = AchievementEntity.fromDomain(
                            familyId,
                            level,
                            next
                    );
                    achievementDao.upsert(updated);
                }
            }
        });
    }

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
