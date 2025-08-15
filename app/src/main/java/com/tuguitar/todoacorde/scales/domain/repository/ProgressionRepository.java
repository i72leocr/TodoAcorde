package com.tuguitar.todoacorde.scales.domain.repository;

import androidx.annotation.Nullable;
import androidx.room.Transaction;

import com.tuguitar.todoacorde.scales.data.dao.ProgressionDao;
import com.tuguitar.todoacorde.scales.data.dao.ScaleBoxDao;
import com.tuguitar.todoacorde.scales.data.dao.ScaleDao;
import com.tuguitar.todoacorde.scales.data.dao.TonalityDao;
import com.tuguitar.todoacorde.scales.data.dao.UserScaleCompletionDao;
import com.tuguitar.todoacorde.scales.data.entity.ScaleEntity;
import com.tuguitar.todoacorde.scales.data.entity.UserScaleCompletionEntity;

import java.util.List;

public class ProgressionRepository {

    private final ScaleDao scaleDao;
    private final ScaleBoxDao scaleBoxDao;
    private final TonalityDao tonalityDao;
    private final UserScaleCompletionDao completionDao;
    private final ProgressionDao progressionDao;

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

    /** Marca completada una caja y devuelve el próximo boxOrder desbloqueado para esa {escala, tonalidad}. */
    @Transaction
    public @Nullable Integer completeBoxAndGetNext(long userId, long scaleId, long tonalityId, int boxOrder, long nowUtc) {
        // Evitar duplicados por la UNIQUE del DAO
        UserScaleCompletionEntity c = new UserScaleCompletionEntity();
        c.userId = userId;
        c.scaleId = scaleId;
        c.tonalityId = tonalityId;
        c.boxOrder = boxOrder;
        c.completedAtUtc = nowUtc;
        completionDao.insert(c);

        // Descubrir si existe siguiente caja
        Integer maxBox = scaleBoxDao.getMaxBoxOrder(scaleId);
        if (maxBox == null) return null;
        int next = boxOrder + 1;
        if (next <= maxBox) {
            // Se considera "desbloqueado" por derivación (UI).
            return next;
        }
        return null; // no hay siguiente caja
    }

    /** ¿Está completa una escala en todas sus tonalidades y cajas para el usuario? */
    public boolean isScaleFullyCompletedAllTonalities(long userId, long scaleId) {
        return progressionDao.isScaleFullyCompletedAllTonalities(userId, scaleId);
    }

    /** ¿Está completo un tier entero para el usuario? */
    public boolean isTierCompleted(long userId, int tier) {
        return progressionDao.isTierCompleted(userId, tier);
    }

    /** Devuelve el mayor boxOrder completado en esta {escala, tonalidad}. */
    public int getMaxCompletedBoxOrZero(long userId, long scaleId, long tonalityId) {
        Integer v = completionDao.getMaxCompletedBox(userId, scaleId, tonalityId);
        return v == null ? 0 : v;
    }

    /** ¿Está completada una caja concreta? */
    public boolean isBoxCompleted(long userId, long scaleId, long tonalityId, int boxOrder) {
        return completionDao.isBoxCompleted(userId, scaleId, tonalityId, boxOrder);
    }

    /** ¿Qué escalas hay por tier? */
    public List<ScaleEntity> getScalesByTier(int tier) {
        return scaleDao.getByTier(tier);
    }
}
