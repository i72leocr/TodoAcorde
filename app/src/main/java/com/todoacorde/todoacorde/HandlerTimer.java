package com.todoacorde.todoacorde;

import android.os.Handler;
import android.os.Looper;

import java.util.List;

/**
 * Temporizador por pasos basado en {@link Handler} que recorre una secuencia de duraciones
 * expresadas en pulsos, sincronizadas a un BPM y a un factor de velocidad.
 *
 * Funcionamiento general:
 * - Cada elemento de {@code durations} representa un número de pulsos.
 * - La duración en milisegundos de cada paso se calcula como:
 *   {@code durationMs = (60000 / bpm) * pulses / speedFactor}.
 * - Durante cada paso se emiten actualizaciones de progreso en porcentaje.
 * - Al terminar un paso se invoca el callback de avance y se programa el siguiente.
 *
 * Uso típico:
 * - Construir con implementaciones de {@link ProgressCallback} y {@link StepCallback}.
 * - Llamar a {@link #startSequence(List, int, float)} para iniciar.
 * - Llamar a {@link #cancel()} para detener.
 */
public class HandlerTimer {

    /**
     * Notifica el progreso del paso actual en porcentaje entre 0 y 100.
     */
    public interface ProgressCallback {
        void onProgress(int percent);
    }

    /**
     * Notifica que ha finalizado un paso y se va a avanzar al siguiente.
     */
    public interface StepCallback {
        void onStep();
    }

    private final ProgressCallback progressCb;
    private final StepCallback stepCb;
    private final Handler handler = new Handler(Looper.getMainLooper());

    /** Secuencia de duraciones en número de pulsos. */
    private List<Integer> durations;
    /** Pulsos por minuto para el cálculo de milisegundos por pulso. */
    private int bpm;
    /** Factor multiplicador de velocidad. Valores mayores a 1 aceleran. */
    private float speedFactor;
    /** Índice del paso actual dentro de {@link #durations}. */
    private int currentIdx = 0;
    /** Marca de tiempo de inicio del paso actual en milisegundos. */
    private long startTime;
    /** Indica si el temporizador está activo. */
    private boolean running = false;

    /**
     * Crea un temporizador por pasos con callbacks de progreso y de avance.
     *
     * @param progressCb callback de progreso por porcentaje dentro del paso.
     * @param stepCb     callback invocado al finalizar cada paso.
     */
    public HandlerTimer(ProgressCallback progressCb, StepCallback stepCb) {
        this.progressCb = progressCb;
        this.stepCb = stepCb;
    }

    /**
     * Inicia la reproducción de una secuencia de pasos.
     *
     * @param durations   lista de duraciones por paso en pulsos.
     * @param bpm         pulsos por minuto para convertir pulsos a milisegundos.
     * @param speedFactor factor de velocidad aplicado a la duración resultante.
     */
    public void startSequence(List<Integer> durations, int bpm, float speedFactor) {
        this.durations = durations;
        this.bpm = bpm;
        this.speedFactor = speedFactor;
        this.currentIdx = 0;
        this.running = true;
        scheduleNext();
    }

    /**
     * Programa el siguiente paso en función del índice actual y calcula su duración en milisegundos.
     * Publica un runnable para actualizar el progreso y otro para avanzar al siguiente paso al expirar.
     */
    private void scheduleNext() {
        if (!running || currentIdx >= durations.size()) return;

        int pulses = durations.get(currentIdx);
        double beatMs = (60_000.0 / bpm);
        long durationMs = (long) (beatMs * pulses / speedFactor);

        startTime = System.currentTimeMillis();
        handler.post(progressRunnable(durationMs));
        handler.postDelayed(stepRunnable(), durationMs);
    }

    /**
     * Crea un runnable que emite progreso en intervalos fijos hasta completar el paso.
     *
     * @param durationMs duración total del paso en milisegundos.
     * @return runnable que actualiza el porcentaje de progreso.
     */
    private Runnable progressRunnable(final long durationMs) {
        return new Runnable() {
            @Override
            public void run() {
                if (!running) return;
                long elapsed = System.currentTimeMillis() - startTime;
                int pct = (int) (100.0 * elapsed / durationMs);
                if (pct > 100) pct = 100;

                progressCb.onProgress(pct);

                if (elapsed < durationMs) {
                    handler.postDelayed(this, 50); // resolución de actualización
                }
            }
        };
    }

    /**
     * Crea un runnable que avanza al siguiente paso al finalizar el actual.
     *
     * @return runnable que invoca {@link StepCallback#onStep()} y encadena el siguiente paso.
     */
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

    /**
     * Cancela la secuencia en curso y elimina las tareas pendientes en el {@link Handler}.
     */
    public void cancel() {
        running = false;
        handler.removeCallbacksAndMessages(null);
    }

    /**
     * Devuelve el tiempo transcurrido del paso actual en milisegundos.
     *
     * @return milisegundos desde el inicio del paso actual.
     */
    public long elapsed() {
        return System.currentTimeMillis() - startTime;
    }
}
