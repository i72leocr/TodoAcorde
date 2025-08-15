package com.tuguitar.todoacorde.metronome.domain;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;

import com.tuguitar.todoacorde.metronome.data.MetronomeSoundRepository;

import javax.inject.Inject;

/**
 * # MetronomeManager (Capa de Dominio)
 *
 * Motor de reloj del metrónomo. Programa y dispara los ticks de audio con baja latencia
 * usando un {@link Handler} sobre el hilo principal. Expone un callback de beat
 * para sincronizar la UI (iluminación de círculos, etc.).
 *
 * ## Responsabilidades
 * - Gestionar el estado de reproducción (start/stop).
 * - Calcular el intervalo entre golpes a partir del BPM.
 * - Llevar la cuenta del índice de beat dentro del compás y acentos.
 * - Disparar los sonidos (acento/beat) a través de {@link MetronomeSoundRepository}.
 * - Notificar a la capa de presentación cada vez que se produce un beat.
 *
 * ## Decisiones de diseño
 * - **Máximo 12 tiempos por compás** (requerimiento del TFG).
 * - **Bloqueo defensivo**: si el metrónomo está sonando, ignoramos cambios de compás
 *   desde el propio manager. Además lo reforzamos en ViewModel/UI.
 * - Intervalo en milisegundos calculado con precisión de doble y redondeo a long.
 *
 * ## Limitaciones
 * - Este motor programa los ticks en el hilo principal. Si necesitas jitter todavía
 *   más bajo, mueve la planificación a un HandlerThread dedicado y delega a main sólo
 *   la notificación de UI.
 */
public class MetronomeManager {

    /** BPM mínimo permitido para evitar intervalos excesivamente largos que degraden UX. */
    public static final int MIN_BPM = 20;

    /** BPM máximo (en línea con la limitación solicitada). */
    public static final int MAX_BPM = 218;

    /** Número máximo de tiempos por compás (requerimiento: hasta 12). */
    public static final int MAX_BEATS_PER_MEASURE = 12;

    /**
     * Callback de tick para notificar cada beat a la UI/VM.
     */
    public interface TickListener {
        /**
         * Notificado en cada beat del ciclo.
         *
         * @param beatIndex índice del beat actual dentro del compás (0 = primer tiempo).
         * @param beatsPerMeasure número total de tiempos del compás activo.
         */
        void onBeat(int beatIndex, int beatsPerMeasure);
    }

    // Dependencias
    private final MetronomeSoundRepository soundRepository;

    // Programación de ticks
    private final Handler handler;
    private Runnable beatRunnable;

    // Estado
    private boolean isRunning = false;
    private int bpm = 100;
    private int beatsPerMeasure = 4;
    private boolean accentFirst = true;
    private int currentBeat = 0;
    private TickListener tickListener;

    // Intervalo de tick en ms (derivado de BPM)
    private long intervalMs = 600; // 100 BPM ~ 600ms entre negras

    @Inject
    public MetronomeManager(@NonNull MetronomeSoundRepository soundRepository) {
        this.soundRepository = soundRepository;
        this.handler = new Handler(Looper.getMainLooper());
    }

    // ---------------------------------------------------------------------------------------------
    // API Pública
    // ---------------------------------------------------------------------------------------------

    /**
     * Arranca el metrónomo con los parámetros indicados.
     *
     * @param bpm               pulsaciones por minuto. Se clamp a [MIN_BPM, MAX_BPM].
     * @param beatsPerMeasure   número de tiempos por compás. Se clamp a [1, MAX_BEATS_PER_MEASURE].
     * @param accentFirst       si {@code true}, el primer tiempo del compás suena acentuado.
     * @param listener          callback para notificar cada beat (UI/VM).
     */
    @MainThread
    public void start(int bpm, int beatsPerMeasure, boolean accentFirst, @NonNull TickListener listener) {
        if (isRunning) {
            // Si ya está corriendo, paramos y reconfiguramos limpio.
            stop();
        }

        this.bpm = clamp(bpm, MIN_BPM, MAX_BPM);
        this.beatsPerMeasure = clamp(beatsPerMeasure, 1, MAX_BEATS_PER_MEASURE);
        this.accentFirst = accentFirst;
        this.tickListener = listener;
        this.currentBeat = 0;
        this.intervalMs = computeIntervalMs(this.bpm);

        isRunning = true;

        // Runnable que dispara el sonido y reprograma el siguiente tick.
        beatRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isRunning) return;

                // Sonido: acento en el primer tiempo si procede
                if (accentFirst && currentBeat == 0) {
                    soundRepository.playAccent();
                } else {
                    soundRepository.playTick();
                }

                // Notificación a la UI/VM
                if (tickListener != null) {
                    tickListener.onBeat(currentBeat, beatsPerMeasure);
                }

                // Avanzar beat (cíclico)
                currentBeat = (currentBeat + 1) % beatsPerMeasure;

                // Programar siguiente tick
                handler.postDelayed(this, intervalMs);
            }
        };

        // Primer tick inmediato para feedback instantáneo
        handler.post(beatRunnable);
    }

    /**
     * Detiene el metrónomo y limpia la programación de ticks.
     */
    @MainThread
    public void stop() {
        if (!isRunning) return;
        isRunning = false;
        handler.removeCallbacks(beatRunnable);
        currentBeat = 0;
        tickListener = null;
    }

    /**
     * Cambia el BPM. Si el metrónomo está en marcha, el cambio tiene efecto inmediato
     * reprogramando el intervalo de ticks sin cortar el flujo.
     *
     * @param bpm nuevo BPM (clamp a [MIN_BPM, MAX_BPM]).
     */
    @MainThread
    public void setBpm(int bpm) {
        int clamped = clamp(bpm, MIN_BPM, MAX_BPM);
        if (this.bpm == clamped) return;
        this.bpm = clamped;
        this.intervalMs = computeIntervalMs(this.bpm);

        // Si está corriendo, reprogramamos el siguiente tick con el nuevo intervalo.
        if (isRunning && beatRunnable != null) {
            handler.removeCallbacks(beatRunnable);
            handler.postDelayed(beatRunnable, intervalMs);
        }
    }

    /**
     * Cambia el número de tiempos por compás.
     * <p>
     * **Importante**: Para cumplir el requisito de “no permitir cambiar el compás
     * mientras suena”, este método **ignora** la petición si el metrónomo está corriendo.
     * La UI/VM también implementará el bloqueo para una UX coherente.
     *
     * @param beats nuevo número de tiempos; clamp a [1, MAX_BEATS_PER_MEASURE].
     */
    @MainThread
    public void setBeatsPerMeasure(int beats) {
        if (isRunning) {
            // Bloqueo defensivo: no cambiamos compás en caliente.
            return;
        }
        int clamped = clamp(beats, 1, MAX_BEATS_PER_MEASURE);
        if (this.beatsPerMeasure == clamped) return;
        this.beatsPerMeasure = clamped;
        this.currentBeat = 0;
    }

    /**
     * Activa o desactiva el acento en el primer tiempo del compás.
     * Puede aplicarse en caliente.
     *
     * @param accentFirst true para acentuar el primer tiempo.
     */
    @MainThread
    public void setAccentFirst(boolean accentFirst) {
        this.accentFirst = accentFirst;
    }

    /** @return {@code true} si el metrónomo está sonando. */
    public boolean isRunning() {
        return isRunning;
    }

    /** @return BPM actual. */
    public int getBpm() {
        return bpm;
    }

    /** @return tiempos por compás actual. */
    public int getBeatsPerMeasure() {
        return beatsPerMeasure;
    }

    /**
     * Libera recursos de audio (SoundPool, etc.). Invocar desde onCleared/onDestroy.
     */
    public void releaseSound() {
        soundRepository.release();
    }

    // ---------------------------------------------------------------------------------------------
    // Utilidades privadas
    // ---------------------------------------------------------------------------------------------

    /** Calcula el intervalo entre beats en milisegundos a partir del BPM. */
    private static long computeIntervalMs(int bpm) {
        // 60_000 ms por minuto / BPM = ms por negra
        double ms = 60000.0 / (double) bpm;
        return (long) Math.round(ms);
    }

    /** Clamp genérico para enteros. */
    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
