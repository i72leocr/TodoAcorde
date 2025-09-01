package com.tuguitar.todoacorde;

import android.os.Handler;
import android.os.Looper;
import java.util.List;

/**
 * Ejecuta una secuencia de duraciones (en milisegundos),
 * actualiza el progreso y notifica al completar cada uno.
 */
public class HandlerTimer {
    public interface ProgressCallback {
        /** porcentaje de 0 a 100 del acorde actual */
        void onProgress(int percent);
    }
    public interface StepCallback {
        /** se llama cuando finaliza el acorde y se debe avanzar índice */
        void onStep();
    }

    private final ProgressCallback progressCb;
    private final StepCallback stepCb;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private List<Integer> durations;
    private int bpm;
    private float speedFactor;
    private int currentIdx = 0;
    private long startTime;
    private boolean running = false;

    public HandlerTimer(ProgressCallback progressCb, StepCallback stepCb) {
        this.progressCb = progressCb;
        this.stepCb     = stepCb;
    }

    /**
     * @param durations lista de pulsos (número de ticks, no ms)
     * @param bpm tempo de la canción
     * @param speedFactor factor de velocidad (0.5x, 0.75x, 1x)
     */
    public void startSequence(List<Integer> durations, int bpm, float speedFactor) {
        this.durations   = durations;
        this.bpm         = bpm;
        this.speedFactor = speedFactor;
        this.currentIdx  = 0;
        this.running     = true;
        scheduleNext();
    }

    private void scheduleNext() {
        if (!running || currentIdx >= durations.size()) return;
        int pulses = durations.get(currentIdx);
        double beatMs = (60_000.0 / bpm);
        long durationMs = (long)(beatMs * pulses / speedFactor);

        startTime = System.currentTimeMillis();
        handler.post(progressRunnable(durationMs));
        handler.postDelayed(stepRunnable(), durationMs);
    }

    private Runnable progressRunnable(final long durationMs) {
        return new Runnable() {
            @Override
            public void run() {
                if (!running) return;
                long elapsed = System.currentTimeMillis() - startTime;
                int pct = (int)(100.0 * elapsed / durationMs);
                if (pct > 100) pct = 100;
                progressCb.onProgress(pct);
                if (elapsed < durationMs) {
                    handler.postDelayed(this, 50);
                }
            }
        };
    }

    private Runnable stepRunnable() {
        return new Runnable() {
            @Override
            public void run() {
                if (!running) return;
                stepCb.onStep();
                currentIdx++;
                if (running) {
                    scheduleNext();
                }
            }
        };
    }

    /** Cancela la secuencia */
    public void cancel() {
        running = false;
        handler.removeCallbacksAndMessages(null);
    }

    /** Tiempo transcurrido en el acorde actual (ms) */
    public long elapsed() {
        return System.currentTimeMillis() - startTime;
    }
}
