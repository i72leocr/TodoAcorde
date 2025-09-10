package com.todoacorde.todoacorde.achievements.data;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;

import com.todoacorde.todoacorde.EvaluateAchievementUseCase;
import com.todoacorde.todoacorde.FamilyId;
import com.todoacorde.todoacorde.SessionManager;
import com.todoacorde.todoacorde.achievements.domain.usecase.AchievementUseCaseRegistry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Implementación del repositorio de logros.
 *
 * Orquesta el acceso a definiciones ({@link AchievementDefinitionDao}) y estados
 * de logro ({@link AchievementDao}), combinándolos en modelos de dominio
 * {@link Achievement}. Expone observación reactiva, actualización de progreso
 * y evaluación de reglas de logro.
 *
 * Notas:
 * - Utiliza un {@link Executor} de E/S monohilo para operaciones Room fuera del hilo principal.
 * - Está anotado como {@link Singleton} para compartir instancia en el grafo de DI.
 */
@Singleton
public class AchievementRepositoryImpl implements AchievementRepository {

    private final AchievementDefinitionDao defDao;
    private final AchievementDao achievementDao;
    private final SessionManager sessionManager;
    private final Executor ioExecutor;

    /**
     * Inyecta dependencias del repositorio.
     *
     * @param defDao          DAO de definiciones de logros.
     * @param achievementDao  DAO de estados/progreso de logros.
     * @param sessionManager  gestor de sesión (reservado para usos de contexto de usuario).
     */
    @Inject
    public AchievementRepositoryImpl(
            @NonNull AchievementDefinitionDao defDao,
            @NonNull AchievementDao achievementDao,
            @NonNull SessionManager sessionManager
    ) {
        this.defDao = defDao;
        this.achievementDao = achievementDao;
        this.sessionManager = sessionManager;
        this.ioExecutor = Executors.newSingleThreadExecutor();
    }

    /**
     * Observa todos los logros combinando definiciones y progreso persistido.
     * La combinación se actualiza de forma reactiva ante cambios en cualquiera de las fuentes.
     *
     * @return {@link LiveData} que emite listas de {@link Achievement}.
     */
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

    /**
     * Actualiza (upsert) un logro concreto en almacenamiento persistente,
     * traduciendo el modelo de dominio a {@link AchievementEntity}.
     * Se ejecuta en hilo de E/S.
     *
     * @param achievement logro a persistir.
     */
    @Override
    public void updateAchievement(@NonNull Achievement achievement) {
        final String familyId = FamilyId.of(achievement.getTitle()).asString();
        final int progress = achievement.getProgress();

        ioExecutor.execute(() -> {
            AchievementEntity entity = AchievementEntity.fromDomain(
                    familyId,
                    achievement.getLevel(),
                    progress
            );
            achievementDao.upsert(entity);
        });
    }

    /**
     * Actualiza en lote una lista de logros (upsert), convirtiéndolos a entidades Room.
     * Se ejecuta en hilo de E/S.
     *
     * @param achievements lista de logros a persistir.
     */
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

    /**
     * Recupera el progreso asociado a una familia/título y nivel.
     *
     * @param familyTitle título de la familia (se normaliza a {@code familyId}).
     * @param level       nivel del logro.
     * @return el progreso actual; 0 si no existe registro.
     */
    @Override
    public int getProgress(String familyTitle, Achievement.Level level) {
        final String familyId = FamilyId.of(familyTitle).asString();
        AchievementEntity entity = achievementDao.getByFamilyAndLevel(familyId, level);
        return entity != null ? entity.getProgress() : 0;
    }

    /**
     * Evalúa todas las reglas de logro registradas y aplica sus efectos.
     * Se ejecuta en hilo de E/S.
     */
    @Override
    public void evaluateAll() {
        ioExecutor.execute(() -> {
            for (EvaluateAchievementUseCase useCase : AchievementUseCaseRegistry.getAll()) {
                useCase.evaluate();
            }
        });
    }

    /**
     * Incrementa el progreso de una familia identificada por ID o por título.
     * Si el parámetro contiene espacios, se interpreta como título y se normaliza a ID.
     * Para familias de tipo "milestone" (binarias), se respeta el rango [0,1] y
     * se incrementa por orden BRONZE → SILVER → GOLD hasta completar.
     * En caso general, se incrementan todos los niveles.
     * Se ejecuta en hilo de E/S.
     *
     * @param familyIdOrTitle identificador lógico (ID) o título de la familia.
     * @param delta           incremento a aplicar (se satura a valores no negativos).
     */
    @Override
    public void incrementProgress(@NonNull String familyIdOrTitle, int delta) {
        final String familyId = familyIdOrTitle.contains(" ")
                ? FamilyId.of(familyIdOrTitle).asString()
                : familyIdOrTitle.toLowerCase(Locale.ROOT);

        ioExecutor.execute(() -> {
            boolean isMilestone =
                    "scales_one_tonality_milestone".equalsIgnoreCase(familyId) ||
                            "scales_all_tonalities_milestone".equalsIgnoreCase(familyId);

            if (isMilestone) {
                Achievement.Level[] order = new Achievement.Level[]{
                        Achievement.Level.BRONZE,
                        Achievement.Level.SILVER,
                        Achievement.Level.GOLD
                };
                for (Achievement.Level level : order) {
                    AchievementEntity current = achievementDao.getByFamilyAndLevel(familyId, level);
                    int prev = (current != null) ? current.getProgress() : 0;
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
                        break;
                    }
                }
            } else {
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

    /**
     * Combina definiciones y estados de logro en modelos de dominio y publica el resultado.
     * Si no hay definiciones, no emite cambios. El progreso ausente se interpreta como 0.
     *
     * @param defs   lista de definiciones (puede ser null en la notificación inicial).
     * @param uas    lista de estados/progreso de usuario (puede ser null).
     * @param output {@link MediatorLiveData} destino para publicar la combinación.
     */
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
            Map<Achievement.Level, Integer> familyProgress =
                    progMap.getOrDefault(def.getFamilyId(), new HashMap<>());
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
