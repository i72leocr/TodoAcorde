package com.tuguitar.todoacorde.scales.domain.usecase;


import com.tuguitar.todoacorde.scales.domain.repository.ProgressionRepository;

public class CompleteScaleBoxUseCase {

    private final ProgressionRepository repo;

    public static class Result {
        public final Integer nextBoxOrder; // null si no hay siguiente
        public final boolean scaleCompletedAllTonalities; // si con este evento ya cerraste toda la escala
        public final boolean tierCompleted; // si con esto cerraste el tier y por tanto desbloqueas el siguiente

        public Result(Integer nextBoxOrder, boolean scaleCompletedAllTonalities, boolean tierCompleted) {
            this.nextBoxOrder = nextBoxOrder;
            this.scaleCompletedAllTonalities = scaleCompletedAllTonalities;
            this.tierCompleted = tierCompleted;
        }
    }

    public CompleteScaleBoxUseCase(ProgressionRepository repo) {
        this.repo = repo;
    }

    public Result execute(long userId, long scaleId, long tonalityId, int boxOrder, int scaleTier, long nowUtc) {
        Integer next = repo.completeBoxAndGetNext(userId, scaleId, tonalityId, boxOrder, nowUtc);
        boolean scaleFull = repo.isScaleFullyCompletedAllTonalities(userId, scaleId);
        boolean tierFull = repo.isTierCompleted(userId, scaleTier);
        return new Result(next, scaleFull, tierFull);
    }
}
