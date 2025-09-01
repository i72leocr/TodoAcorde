package com.tuguitar.todoacorde.scales.domain.repository;

import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;
import androidx.room.Transaction;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.tuguitar.todoacorde.scales.data.dao.ProgressionDao;
import com.tuguitar.todoacorde.scales.data.dao.ScaleBoxDao;
import com.tuguitar.todoacorde.scales.data.dao.ScaleDao;
import com.tuguitar.todoacorde.scales.data.dao.TonalityDao;
import com.tuguitar.todoacorde.scales.data.dao.UserScaleCompletionDao;
import com.tuguitar.todoacorde.scales.data.entity.ScaleEntity;
import com.tuguitar.todoacorde.scales.data.entity.TonalityEntity;
import com.tuguitar.todoacorde.scales.data.entity.UserScaleCompletionEntity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private volatile Map<String, Long> cacheTonalityNameToId = Collections.emptyMap(); // key=UPPER
    private volatile Map<String, Long> cacheScaleNameEsToId  = Collections.emptyMap(); // key=LOWER
    private volatile Map<Long, Integer> cacheScaleIdToTier   = Collections.emptyMap();
    private volatile Map<Integer, List<ScaleEntity>> cacheScalesByTier = Collections.emptyMap();
    private volatile Map<Integer, Integer> cacheCountByTier = Collections.emptyMap();

    /**
     * Carga/recarga las cachés. Llama a esto SIEMPRE desde un hilo de IO
     * (p.ej., en ViewModel.onInit dentro de ioExecutor, antes de emitir UI).
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
        cacheScaleNameEsToId  = Collections.unmodifiableMap(scaleNameToId);
        cacheScaleIdToTier    = Collections.unmodifiableMap(scaleIdToTier);
        cacheScalesByTier     = Collections.unmodifiableMap(byTier);
        cacheCountByTier      = Collections.unmodifiableMap(countByTier);
    }

    /** Devuelve el id de tonalidad desde caché (sin tocar Room). */
    public long findTonalityIdByNameCached(String root) {
        if (root == null) return -1;
        Long v = cacheTonalityNameToId.get(root.trim().toUpperCase());
        return v != null ? v : -1;
    }

    /** Devuelve el id de escala por nombre ES (desde caché). */
    public long findScaleIdByNameCached(String nameEs) {
        if (nameEs == null) return -1;
        Long v = cacheScaleNameEsToId.get(nameEs.trim().toLowerCase());
        return v != null ? v : -1;
    }

    /** Devuelve el tier de una escala por id (desde caché). */
    public int findScaleTierByIdCached(long scaleId) {
        Integer v = cacheScaleIdToTier.get(scaleId);
        return v != null ? v : 0;
    }

    /** Lista de escalas por tier (desde caché, lista inmutable). */
    public List<ScaleEntity> getScalesByTierCached(int tier) {
        List<ScaleEntity> list = cacheScalesByTier.get(tier);
        return list != null ? list : Collections.emptyList();
    }

    /** ¿Existe alguna escala en el tier? (desde caché). */
    public boolean hasAnyScaleInTierCached(int tier) {
        Integer c = cacheCountByTier.get(tier);
        return c != null && c > 0;
    }

    /**
     * Marca completada una caja y devuelve el próximo boxOrder desbloqueado para esa {escala, tonalidad}
     * SOLO si esa siguiente caja aún no estaba hecha. Si ya estaba hecha (o no hay siguiente), devuelve null.
     * No duplica registros si la caja actual ya se había completado antes.
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
        if (next > maxBox) return null; // no hay más
        boolean nextAlreadyCompleted = completionDao.isBoxCompleted(userId, scaleId, tonalityId, next);
        return nextAlreadyCompleted ? null : next;
    }

    /** Devuelve el mayor boxOrder completado en esta {escala, tonalidad} o 0 si ninguno. */
    public int getMaxCompletedBoxOrZero(long userId, long scaleId, long tonalityId) {
        Integer v = completionDao.getMaxCompletedBox(userId, scaleId, tonalityId);
        return v == null ? 0 : v;
    }

    /** ¿Está completada una caja concreta? */
    public boolean isBoxCompleted(long userId, long scaleId, long tonalityId, int boxOrder) {
        return completionDao.isBoxCompleted(userId, scaleId, tonalityId, boxOrder);
    }

    /** ¿Está completada una escala entera (todas sus cajas del catálogo) para ESTA tonalidad y usuario? */
    public boolean isScaleCompletedForTonality(long userId, long scaleId, long tonalityId) {
        Integer maxBox = scaleBoxDao.getMaxBoxOrder(scaleId);
        if (maxBox == null || maxBox <= 0) return false;
        return completionDao.isBoxCompleted(userId, scaleId, tonalityId, maxBox);
    }

    /**
     * ¿Está completo un tier ENTERO para el usuario en ESTA tonalidad?
     * (Todas las escalas del tier con TODAS sus cajas del catálogo completadas en la tonalidad dada).
     * Nota: esta versión NO considera el nº real de variantes por tonalidad.
     * (Úsala desde IO si la necesitas; para UI usa cálculos cacheados).
     */
    public boolean isTierCompletedForTonality(long userId, int tier, long tonalityId) {
        List<ScaleEntity> tierScales = scaleDao.getByTier(tier);
        if (tierScales == null || tierScales.isEmpty()) return false; // si no hay escalas, consideramos no completado
        for (ScaleEntity s : tierScales) {
            if (!isScaleCompletedForTonality(userId, s.id, tonalityId)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Variante que considera las variantes REALES por tonalidad.
     *
     * @param requiredByScale Mapa {scaleId -> requiredBoxes} calculado por el caller como:
     *                        required = min( cajasCatálogo(=3) , variantesDisponibles(type+root) ).
     *                        Si una escala no aparece en el mapa o el valor es <= 0, se IGNORA
     *                        para esta tonalidad (no bloquea la completitud del tier).
     * @return true si TODAS las escalas relevantes (con required>0) cumplen maxCompletas>=required.
     *         Si ninguna escala es relevante (todas required<=0), devuelve false (evita falsos positivos).
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
     * Devuelve true SOLO si el tier ha pasado de "incompleto" a "completo" tras registrar una nueva caja.
     * Úsalo para NO repetir el mensaje “¡Nuevas escalas desbloqueadas!” cuando el tier ya estaba completo.
     *
     * @param requiredByScale  {scaleId -> cajas requeridas} (ver isTierCompletedForTonalityFiltered)
     * @param justScaleId      escala en la que se acaba de registrar la caja
     * @param justBoxWasAlreadyCompleted si la caja ya existía (no hubo cambio), devuelve false directamente
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

    /** ¿Está completa una escala en TODAS las tonalidades? (global, no por tonalidad) */
    public boolean isScaleFullyCompletedAllTonalities(long userId, long scaleId) {
        return progressionDao.isScaleFullyCompletedAllTonalities(userId, scaleId);
    }

    /** ¿Hay alguna escala en el tier dado? (para saber si existe siguiente tier) */
    public boolean hasAnyScaleInTier(int tier) {
        return scaleDao.countByTier(tier) > 0;
    }

    /** Escalas por tier (para construir el conjunto permitido por tiers habilitados). */
    public List<ScaleEntity> getScalesByTier(int tier) {
        return scaleDao.getByTier(tier);
    }

    /** Máximo número de caja definido para una escala en catálogo. */
    @Nullable
    public Integer getMaxBoxOrder(long scaleId) {
        return scaleBoxDao.getMaxBoxOrder(scaleId);
    }

    /** Encuentra id de escala por nombre (ES). */
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

    /** Encuentra id de tonalidad por nombre ("C","C#",...). */
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

    /** Devuelve el tier al que pertenece una escala por id, o 0 si no se encuentra. */
    public int findScaleTierById(long scaleId) {
        List<ScaleEntity> all = scaleDao.getAll();
        if (all != null) {
            for (ScaleEntity s : all) if (s.id == scaleId) return s.tier;
        }
        return 0;
    }
}
