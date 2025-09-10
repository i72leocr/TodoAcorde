package com.todoacorde.todoacorde.metronome.domain;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;

import com.todoacorde.todoacorde.metronome.data.MetronomeSoundRepository;

import javax.inject.Inject;

/**
 * Gestor del metrónomo que programa ticks a intervalos regulares según BPM
 * y compás, reproduciendo acentos y notificando los beats a un oyente.
 *
 * Características:
 * - Programa la ejecución en el hilo principal mediante {@link Handler}.
 * - Reproduce sonidos a través de {@link MetronomeSoundRepository}.
 * - Permite ajustar BPM en caliente; el compás solo se modifica en reposo.
 */
public class MetronomeManager {

    /** Límite inferior de BPM permitido. */
    public static final int MIN_BPM = 20;

    /** Límite superior de BPM permitido. */
    public static final int MAX_BPM = 218;

    /** Máximo de pulsos por compás admitido. */
    public static final int MAX_BEATS_PER_MEASURE = 12;

    /**
     * Oyente para recibir notificaciones de cada pulso.
     */
    public interface TickListener {
        /**
         * Notificado en cada beat.
         *
         * @param beatIndex        índice del beat dentro del compás, empezando en 0.
         * @param beatsPerMeasure  número de beats por compás vigente.
         */
        void onBeat(int beatIndex, int beatsPerMeasure);
    }

    /** Fuente de sonidos del metrónomo. */
    private final MetronomeSoundRepository soundRepository;
    /** Programador sobre el hilo principal para los ticks. */
    private final Handler handler;

    /** Tarea periódica que ejecuta y reprograma los beats. */
    private Runnable beatRunnable;
    /** Estado de ejecución actual del metrónomo. */
    private boolean isRunning = false;

    /** BPM actual (clamp en {@link #MIN_BPM}..{@link #MAX_BPM}). */
    private int bpm = 100;
    /** Beats por compás actual (clamp en 1..{@link #MAX_BEATS_PER_MEASURE}). */
    private int beatsPerMeasure = 4;
    /** Si el primer beat del compás se acentúa. */
    private boolean accentFirst = true;
    /** Índice del beat actual dentro del compás [0..beatsPerMeasure). */
    private int currentBeat = 0;
    /** Oyente de ticks; puede ser null cuando el metrónomo está parado. */
    private TickListener tickListener;
    /** Intervalo actual entre beats en milisegundos (derivado de BPM). */
    private long intervalMs = 600;

    /**
     * Crea un gestor de metrónomo sobre el hilo principal.
     *
     * @param soundRepository repositorio para reproducir ticks y acentos.
     */
    @Inject
    public MetronomeManager(@NonNull MetronomeSoundRepository soundRepository) {
        this.soundRepository = soundRepository;
        this.handler = new Handler(Looper.getMainLooper());
    }

    /**
     * Inicia el metrónomo con los parámetros indicados.
     * Si ya estaba corriendo, lo detiene antes de reiniciar.
     *
     * @param bpm              pulsos por minuto (se ajusta a los límites).
     * @param beatsPerMeasure  número de beats por compás (se ajusta a los límites).
     * @param accentFirst      si el primer beat del compás debe acentuarse.
     * @param listener         oyente a notificar en cada beat.
     */
    @MainThread
    public void start(int bpm, int beatsPerMeasure, boolean accentFirst, @NonNull TickListener listener) {
        if (isRunning) {
            stop();
        }

        this.bpm = clamp(bpm, MIN_BPM, MAX_BPM);
        this.beatsPerMeasure = clamp(beatsPerMeasure, 1, MAX_BEATS_PER_MEASURE);
        this.accentFirst = accentFirst;
        this.tickListener = listener;
        this.currentBeat = 0;
        this.intervalMs = computeIntervalMs(this.bpm);

        isRunning = true;
        beatRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isRunning) return;

                if (accentFirst && currentBeat == 0) {
                    soundRepository.playAccent();
                } else {
                    soundRepository.playTick();
                }

                if (tickListener != null) {
                    tickListener.onBeat(currentBeat, beatsPerMeasure);
                }

                currentBeat = (currentBeat + 1) % beatsPerMeasure;
                handler.postDelayed(this, intervalMs);
            }
        };
        handler.post(beatRunnable);
    }

    /**
     * Detiene el metrónomo y limpia el estado de ejecución.
     * No libera los recursos de audio (ver {@link #releaseSound()}).
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
     * Ajusta el BPM en caliente. Si el metrónomo está en ejecución, reprograma
     * el siguiente tick con el nuevo intervalo.
     *
     * @param bpm nuevo valor de BPM (se ajusta a los límites).
     */
    @MainThread
    public void setBpm(int bpm) {
        int clamped = clamp(bpm, MIN_BPM, MAX_BPM);
        if (this.bpm == clamped) return;
        this.bpm = clamped;
        this.intervalMs = computeIntervalMs(this.bpm);
        if (isRunning && beatRunnable != null) {
            handler.removeCallbacks(beatRunnable);
            handler.postDelayed(beatRunnable, intervalMs);
        }
    }

    /**
     * Ajusta el número de beats por compás. Por simplicidad, solo admite
     * el cambio cuando el metrónomo está parado.
     *
     * @param beats nuevo número de beats por compás (1..MAX_BEATS_PER_MEASURE).
     */
    @MainThread
    public void setBeatsPerMeasure(int beats) {
        if (isRunning) {
            return;
        }
        int clamped = clamp(beats, 1, MAX_BEATS_PER_MEASURE);
        if (this.beatsPerMeasure == clamped) return;
        this.beatsPerMeasure = clamped;
        this.currentBeat = 0;
    }

    /**
     * Activa o desactiva el acento en el primer beat del compás.
     *
     * @param accentFirst true para acentuar el primer beat; false en caso contrario.
     */
    @MainThread
    public void setAccentFirst(boolean accentFirst) {
        this.accentFirst = accentFirst;
    }

    /** @return true si el metrónomo está en ejecución. */
    public boolean isRunning() {
        return isRunning;
    }

    /** @return BPM actual. */
    public int getBpm() {
        return bpm;
    }

    /** @return beats por compás actual. */
    public int getBeatsPerMeasure() {
        return beatsPerMeasure;
    }

    /**
     * Libera los recursos de audio asociados al metrónomo.
     * Debe invocarse cuando el componente de UI se destruye.
     */
    public void releaseSound() {
        soundRepository.release();
    }

    /**
     * Calcula el intervalo en milisegundos para un BPM dado.
     *
     * @param bpm pulsos por minuto.
     * @return intervalo aproximado en milisegundos entre beats.
     */
    private static long computeIntervalMs(int bpm) {
        double ms = 60000.0 / (double) bpm;
        return Math.round(ms);
    }

    /**
     * Restringe un valor entero a un rango [min, max].
     *
     * @param value valor a restringir.
     * @param min   mínimo permitido.
     * @param max   máximo permitido.
     * @return valor dentro del rango.
     */
    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
