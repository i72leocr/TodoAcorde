package com.tuguitar.todoacorde.scales.domain.usecase;

import androidx.annotation.Nullable;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.tuguitar.todoacorde.scales.domain.repository.ProgressionRepository;

/**
 * Marca como completada una caja y devuelve información de desbloqueos/resultados
 * teniendo en cuenta la tonalidad actual y el tier de la escala.
 */
@Singleton
public class CompleteScaleBoxUseCase {

    private final ProgressionRepository progressionRepo;

    @Inject
    public CompleteScaleBoxUseCase(ProgressionRepository progressionRepo) {
        this.progressionRepo = progressionRepo;
    }

    public static final class Result {
        /** Próxima caja desbloqueada (solo si realmente es NUEVA) o null si no hay / ya estaba hecha. */
        @Nullable public final Integer nextBoxOrder;

        /** true si tras esta acción la escala ha quedado completa en ESTA tonalidad (estado actual). */
        public final boolean scaleCompletedForTonality;

        /** true si tras esta acción la escala ha quedado completa en TODAS las tonalidades (estado actual). */
        public final boolean scaleCompletedAllTonalities;

        /** true si tras esta acción el tier queda completo para ESTA tonalidad (estado actual). */
        public final boolean tierCompletedForTonality;

        /** Alias de compatibilidad con código existente (equivale a tierCompletedForTonality). */
        public final boolean tierCompleted;

        /** NUEVO: true si la escala ha quedado completa PARA ESTA TONALIDAD JUSTO AHORA (antes no lo estaba). */
        public final boolean justCompletedForTonality;

        /** NUEVO: true si la escala ha quedado completa EN TODAS LAS TONALIDADES JUSTO AHORA (antes no lo estaba). */
        public final boolean justCompletedAllTonalities;

        public Result(@Nullable Integer nextBoxOrder,
                      boolean scaleCompletedForTonality,
                      boolean scaleCompletedAllTonalities,
                      boolean tierCompletedForTonality,
                      boolean justCompletedForTonality,
                      boolean justCompletedAllTonalities) {
            this.nextBoxOrder = nextBoxOrder;
            this.scaleCompletedForTonality = scaleCompletedForTonality;
            this.scaleCompletedAllTonalities = scaleCompletedAllTonalities;
            this.tierCompletedForTonality = tierCompletedForTonality;
            this.tierCompleted = tierCompletedForTonality; // compatibilidad con ViewModel
            this.justCompletedForTonality = justCompletedForTonality;
            this.justCompletedAllTonalities = justCompletedAllTonalities;
        }
    }

    /**
     * @param userId Usuario
     * @param scaleId Escala (catálogo)
     * @param tonalityId Tonalidad (C, C#, ...), afecta a los desbloqueos
     * @param boxOrderJustCompleted Caja recién completada (1..max)
     * @param scaleTier Tier de la escala (para comprobar cierre de tier por tonalidad)
     * @param nowUtc timestamp
     */
    public Result execute(long userId,
                          long scaleId,
                          long tonalityId,
                          int boxOrderJustCompleted,
                          int scaleTier,
                          long nowUtc) {

        // Estado PREVIO (para detectar “justo ahora”)
        boolean wasScaleCompletedForTonality =
                progressionRepo.isScaleCompletedForTonality(userId, scaleId, tonalityId);
        boolean wasScaleCompletedAllTonalities =
                progressionRepo.isScaleFullyCompletedAllTonalities(userId, scaleId);

        // Inserta si procede y calcula la "siguiente caja" SOLO si es nueva.
        Integer nextUnlocked = progressionRepo.completeBoxAndGetNext(
                userId, scaleId, tonalityId, boxOrderJustCompleted, nowUtc
        );

        // Estado ACTUAL tras guardar
        boolean scaleCompletedForTonality =
                progressionRepo.isScaleCompletedForTonality(userId, scaleId, tonalityId);
        boolean scaleCompletedAllTonalities =
                progressionRepo.isScaleFullyCompletedAllTonalities(userId, scaleId);

        boolean justCompletedForTonality =
                !wasScaleCompletedForTonality && scaleCompletedForTonality;
        boolean justCompletedAllTonalities =
                !wasScaleCompletedAllTonalities && scaleCompletedAllTonalities;

        // ¿El TIER entero queda completo en ESTA tonalidad?
        boolean tierCompletedForTonality = false;
        if (scaleCompletedForTonality) {
            tierCompletedForTonality = progressionRepo.isTierCompletedForTonality(
                    userId, scaleTier, tonalityId
            );
        }

        return new Result(
                nextUnlocked,
                scaleCompletedForTonality,
                scaleCompletedAllTonalities,
                tierCompletedForTonality,
                justCompletedForTonality,
                justCompletedAllTonalities
        );
    }
}
