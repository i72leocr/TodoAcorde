package com.todoacorde.todoacorde.scales.domain.repository;

import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;
import androidx.room.Transaction;

import com.todoacorde.todoacorde.scales.data.dao.ProgressionDao;
import com.todoacorde.todoacorde.scales.data.dao.ScaleBoxDao;
import com.todoacorde.todoacorde.scales.data.dao.ScaleDao;
import com.todoacorde.todoacorde.scales.data.dao.TonalityDao;
import com.todoacorde.todoacorde.scales.data.dao.UserScaleCompletionDao;
import com.todoacorde.todoacorde.scales.data.entity.ScaleEntity;
import com.todoacorde.todoacorde.scales.data.entity.TonalityEntity;
import com.todoacorde.todoacorde.scales.data.entity.UserScaleCompletionEntity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Repositorio de progresión de escalas.
 *
 * Responsabilidades:
 * - Consultas y actualizaciones relacionadas con el progreso del usuario en escalas/tonalidades.
 * - Cálculos de siguiente caja pendiente, completitud de escala y de tier.
 * - Cachés en memoria para acelerar búsquedas por nombre y agregados por tier.
 *
 * Notas de implementación:
 * - Los métodos que acceden a Room directamente se marcan como @WorkerThread cuando procede.
 * - Se usan mapas inmutables para las cachés, reemplazando referencias de forma atómica.
 */
@Singleton
public class ProgressionRepository {

    private final ScaleDao scaleDao;
    private final ScaleBoxDao scaleBoxDao;
    private final TonalityDao tonalityDao;
    private final UserScaleCompletionDao completionDao;
    private final ProgressionDao progressionDao;

    @Inject
    public ProgressionRepository(
            ScaleDao scaleDao,
            ScaleBoxDao scaleBoxDao,
            TonalityDao tonalityDao,
            UserScaleCompletionDao completionDao,
            ProgressionDao progressionDao
    ) {
        this.scaleDao = scaleDao;
        this.scaleBoxDao = scaleBoxDao;
        this.tonalityDao = tonalityDao;
        this.completionDao = completionDao;
        this.progressionDao = progressionDao;
    }

    /* Cachés in-memory */
    private volatile Map<String, Long> cacheTonalityNameToId = Collections.emptyMap();
    private volatile Map<String, Long> cacheScaleNameEsToId = Collections.emptyMap();
    private volatile Map<Long, Integer> cacheScaleIdToTier = Collections.emptyMap();
    private volatile Map<Integer, List<ScaleEntity>> cacheScalesByTier = Collections.emptyMap();
    private volatile Map<Integer, Integer> cacheCountByTier = Collections.emptyMap();

    /**
     * Precarga las cachés en memoria desde la base de datos.
     * Debe ejecutarse en hilo de trabajo.
     */
    @WorkerThread
    public void warmUpCaches() {
        Map<String, Long> tonMap = new HashMap<>();
        List<TonalityEntity> tones = tonalityDao.getAll();
        if (tones != null) {
            for (TonalityEntity t : tones) {
                if (t != null && t.name != null) {
                    tonMap.put(t.name.trim().toUpperCase(), t.id);
                }
            }
        }

        Map<String, Long> scaleNameToId = new HashMap<>();
        Map<Long, Integer> scaleIdToTier = new HashMap<>();
        Map<Integer, List<ScaleEntity>> byTier = new HashMap<>();
        Map<Integer, Integer> countByTier = new HashMap<>();

        List<ScaleEntity> allScales = scaleDao.getAll();
        if (allScales != null) {
            for (ScaleEntity s : allScales) {
                if (s == null || s.name == null) continue;
                scaleNameToId.put(s.name.trim().toLowerCase(), s.id);
                scaleIdToTier.put(s.id, s.tier);
                List<ScaleEntity> bucket = byTier.computeIfAbsent(s.tier, k -> new ArrayList<>());
                bucket.add(s);
            }
        }
        for (Map.Entry<Integer, List<ScaleEntity>> e : byTier.entrySet()) {
            countByTier.put(e.getKey(), e.getValue() != null ? e.getValue().size() : 0);
            e.setValue(Collections.unmodifiableList(e.getValue()));
        }

        cacheTonalityNameToId = Collections.unmodifiableMap(tonMap);
        cacheScaleNameEsToId = Collections.unmodifiableMap(scaleNameToId);
        cacheScaleIdToTier = Collections.unmodifiableMap(scaleIdToTier);
        cacheScalesByTier = Collections.unmodifiableMap(byTier);
        cacheCountByTier = Collections.unmodifiableMap(countByTier);
    }

    /**
     * Busca id de tonalidad en caché por nombre (case-insensitive).
     */
    public long findTonalityIdByNameCached(String root) {
        if (root == null) return -1;
        Long v = cacheTonalityNameToId.get(root.trim().toUpperCase());
        return v != null ? v : -1;
    }

    /**
     * Busca id de escala en caché por nombre en español (case-insensitive).
     */
    public long findScaleIdByNameCached(String nameEs) {
        if (nameEs == null) return -1;
        Long v = cacheScaleNameEsToId.get(nameEs.trim().toLowerCase());
        return v != null ? v : -1;
    }

    /**
     * Devuelve el tier de una escala desde la caché.
     */
    public int findScaleTierByIdCached(long scaleId) {
        Integer v = cacheScaleIdToTier.get(scaleId);
        return v != null ? v : 0;
    }

    /**
     * Devuelve las escalas por tier desde la caché.
     */
    public List<ScaleEntity> getScalesByTierCached(int tier) {
        List<ScaleEntity> list = cacheScalesByTier.get(tier);
        return list != null ? list : Collections.emptyList();
    }

    /**
     * Indica si hay escalas en el tier indicado (usando caché).
     */
    public boolean hasAnyScaleInTierCached(int tier) {
        Integer c = cacheCountByTier.get(tier);
        return c != null && c > 0;
    }

    /**
     * Marca una caja como completada (si no lo estaba) y devuelve el siguiente boxOrder pendiente.
     * Si no hay siguiente, devuelve null.
     *
     * Operación transaccional.
     *
     * @param userId       id usuario
     * @param scaleId      id escala
     * @param tonalityId   id tonalidad
     * @param boxOrder     caja completada
     * @param nowUtc       timestamp UTC de completado
     * @return siguiente boxOrder pendiente o null si no hay más
     */
    @Transaction
    public @Nullable Integer completeBoxAndGetNext(long userId, long scaleId, long tonalityId, int boxOrder, long nowUtc) {
        boolean alreadyCompleted = completionDao.isBoxCompleted(userId, scaleId, tonalityId, boxOrder);
        if (!alreadyCompleted) {
            UserScaleCompletionEntity c = new UserScaleCompletionEntity();
            c.userId = userId;
            c.scaleId = scaleId;
            c.tonalityId = tonalityId;
            c.boxOrder = boxOrder;
            c.completedAtUtc = nowUtc;
            completionDao.insert(c);
        }

        Integer maxBox = scaleBoxDao.getMaxBoxOrder(scaleId);
        if (maxBox == null) return null;

        int next = boxOrder + 1;
        if (next > maxBox) return null;

        boolean nextAlreadyCompleted = completionDao.isBoxCompleted(userId, scaleId, tonalityId, next);
        return nextAlreadyCompleted ? null : next;
    }

    /**
     * Devuelve el mayor boxOrder completado o 0 si no hay registros.
     */
    public int getMaxCompletedBoxOrZero(long userId, long scaleId, long tonalityId) {
        Integer v = completionDao.getMaxCompletedBox(userId, scaleId, tonalityId);
        return v == null ? 0 : v;
    }

    /**
     * Indica si una caja concreta está completada.
     */
    public boolean isBoxCompleted(long userId, long scaleId, long tonalityId, int boxOrder) {
        return completionDao.isBoxCompleted(userId, scaleId, tonalityId, boxOrder);
    }

    /**
     * Una escala se considera completada para una tonalidad cuando su última caja está marcada.
     */
    public boolean isScaleCompletedForTonality(long userId, long scaleId, long tonalityId) {
        Integer maxBox = scaleBoxDao.getMaxBoxOrder(scaleId);
        if (maxBox == null || maxBox <= 0) return false;
        return completionDao.isBoxCompleted(userId, scaleId, tonalityId, maxBox);
    }

    /**
     * Comprueba si todas las escalas de un tier están completadas para una tonalidad.
     */
    public boolean isTierCompletedForTonality(long userId, int tier, long tonalityId) {
        List<ScaleEntity> tierScales = scaleDao.getByTier(tier);
        if (tierScales == null || tierScales.isEmpty()) return false;
        for (ScaleEntity s : tierScales) {
            if (!isScaleCompletedForTonality(userId, s.id, tonalityId)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Variante filtrada: cada escala del tier puede requerir un número mínimo de cajas.
     * Solo se evalúan las escalas con requisito > 0. Devuelve true si todas las relevantes
     * cumplen el mínimo.
     */
    public boolean isTierCompletedForTonalityFiltered(long userId,
                                                      int tier,
                                                      long tonalityId,
                                                      @Nullable Map<Long, Integer> requiredByScale) {
        List<ScaleEntity> tierScales = scaleDao.getByTier(tier);
        if (tierScales == null || tierScales.isEmpty()) return false;

        int relevantes = 0;
        for (ScaleEntity s : tierScales) {
            int required = (requiredByScale != null && requiredByScale.containsKey(s.id))
                    ? Math.max(0, requiredByScale.get(s.id))
                    : 0;

            if (required <= 0) {
                continue;
            }
            relevantes++;

            int maxDone = getMaxCompletedBoxOrZero(userId, s.id, tonalityId);
            if (maxDone < required) {
                return false;
            }
        }
        return relevantes > 0;
    }

    /**
     * Indica si, tras completar una nueva caja, el tier acaba de quedar cumplido.
     * Se ignora si la caja recién “completada” ya estaba completada.
     */
    public boolean didTierJustCompleteAfterNewBox(long userId,
                                                  int tier,
                                                  long tonalityId,
                                                  @Nullable Map<Long, Integer> requiredByScale,
                                                  long justScaleId,
                                                  boolean justBoxWasAlreadyCompleted) {
        if (justBoxWasAlreadyCompleted) {
            return false;
        }
        boolean after = isTierCompletedForTonalityFiltered(userId, tier, tonalityId, requiredByScale);
        if (!after) return false;

        List<ScaleEntity> tierScales = scaleDao.getByTier(tier);
        if (tierScales == null || tierScales.isEmpty()) return false;

        int relevantes = 0;
        for (ScaleEntity s : tierScales) {
            int required = (requiredByScale != null && requiredByScale.containsKey(s.id))
                    ? Math.max(0, requiredByScale.get(s.id))
                    : 0;

            if (required <= 0) {
                continue;
            }
            relevantes++;

            int maxDone = getMaxCompletedBoxOrZero(userId, s.id, tonalityId);
            if (s.id == justScaleId) {
                maxDone = Math.max(0, maxDone - 1);
            }
            if (maxDone < required) {
                return true;
            }
        }
        return false;
    }

    /**
     * Comprueba si una escala está totalmente completada en todas las tonalidades.
     */
    public boolean isScaleFullyCompletedAllTonalities(long userId, long scaleId) {
        return progressionDao.isScaleFullyCompletedAllTonalities(userId, scaleId);
    }

    /**
     * Indica si existen escalas en el tier dado (consulta directa a DB).
     */
    public boolean hasAnyScaleInTier(int tier) {
        return scaleDao.countByTier(tier) > 0;
    }

    /**
     * Obtiene las escalas de un tier (consulta directa a DB).
     */
    public List<ScaleEntity> getScalesByTier(int tier) {
        return scaleDao.getByTier(tier);
    }

    /**
     * Devuelve el máximo boxOrder existente para la escala o null si no hay cajas.
     */
    @Nullable
    public Integer getMaxBoxOrder(long scaleId) {
        return scaleBoxDao.getMaxBoxOrder(scaleId);
    }

    /**
     * Busca el id de escala por nombre exacto (consulta directa a DB).
     */
    public long findScaleIdByName(String nameEs) {
        if (nameEs == null || nameEs.isEmpty()) return -1;
        List<ScaleEntity> all = scaleDao.getAll();
        if (all != null) {
            for (ScaleEntity s : all) {
                if (s.name != null && s.name.equalsIgnoreCase(nameEs)) return s.id;
            }
        }
        return -1;
    }

    /**
     * Busca el id de tonalidad por nombre exacto (consulta directa a DB).
     */
    public long findTonalityIdByName(String root) {
        if (root == null || root.isEmpty()) return -1;
        List<TonalityEntity> all = tonalityDao.getAll();
        if (all != null) {
            for (TonalityEntity t : all) {
                if (t.name != null && t.name.equalsIgnoreCase(root)) return t.id;
            }
        }
        return -1;
    }

    /**
     * Devuelve el tier de una escala por id (consulta directa a DB).
     */
    public int findScaleTierById(long scaleId) {
        List<ScaleEntity> all = scaleDao.getAll();
        if (all != null) {
            for (ScaleEntity s : all) if (s.id == scaleId) return s.tier;
        }
        return 0;
    }
}
