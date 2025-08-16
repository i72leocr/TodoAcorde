package com.tuguitar.todoacorde.scales.domain.repository;

import androidx.annotation.Nullable;
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
import java.util.List;

/**
 * Repositorio de progresión:
 * - Inserta completados (sin duplicar).
 * - Calcula siguiente caja desbloqueada (solo si realmente es nueva).
 * - Consultas de estado por tier/escala/tonalidad.
 * - Helpers de mapeo por nombre e IDs.
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

    // =============================================================================================
    // Escrituras / cálculo inmediato
    // =============================================================================================

    /**
     * Marca completada una caja y devuelve el próximo boxOrder desbloqueado para esa {escala, tonalidad}
     * SOLO si esa siguiente caja aún no estaba hecha. Si ya estaba hecha (o no hay siguiente), devuelve null.
     * No duplica registros si la caja actual ya se había completado antes.
     */
    @Transaction
    public @Nullable Integer completeBoxAndGetNext(long userId, long scaleId, long tonalityId, int boxOrder, long nowUtc) {
        // Evitar duplicados: si ya estaba esta caja, no insertamos otra fila.
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

        // Calcular “siguiente caja” real.
        Integer maxBox = scaleBoxDao.getMaxBoxOrder(scaleId);
        if (maxBox == null) return null;

        int next = boxOrder + 1;
        if (next > maxBox) return null; // no hay más

        // Si la "siguiente" ya estaba hecha, no devolvemos nada (no mostrar mensaje fantasma).
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

    /** ¿Está completada una escala entera (todas sus cajas) para ESTA tonalidad y usuario? */
    public boolean isScaleCompletedForTonality(long userId, long scaleId, long tonalityId) {
        Integer maxBox = scaleBoxDao.getMaxBoxOrder(scaleId);
        if (maxBox == null || maxBox <= 0) return false;
        return completionDao.isBoxCompleted(userId, scaleId, tonalityId, maxBox);
    }

    // =============================================================================================
    // Desbloqueo por TIER (teniendo en cuenta la tonalidad)
    // =============================================================================================

    /**
     * ¿Está completo un tier ENTERO para el usuario en ESTA tonalidad?
     * (Todas las escalas del tier con TODAS sus cajas completadas en la tonalidad dada).
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

    // =============================================================================================
    // Helpers de mapeo / catálogo
    // =============================================================================================

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
