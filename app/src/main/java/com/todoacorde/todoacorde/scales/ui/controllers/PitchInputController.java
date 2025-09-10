package com.todoacorde.todoacorde.scales.ui.controllers;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Build;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.todoacorde.todoacorde.scales.data.NoteUtils;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/**
 * Controlador de entrada de tono (pitch) desde el micrófono.
 * - Lee audio con {@link AudioRecord}, estima el tono fundamental con un enfoque tipo NSDF,
 *   y emite notas estables a través de {@link Listener}.
 * - También permite un modo de auto-alimentación para test (simula notas sin usar el micrófono).
 */
public class PitchInputController {

    /**
     * Callbacks de salida del detector de tono.
     */
    public interface Listener {
        /**
         * Notificación de una nota estable detectada (nombre normalizado, desviación en cents).
         *
         * @param noteName nombre de la nota (ej. "C", "F#", "A#")
         * @param centsOff desviación en cents respecto al semitono más cercano
         */
        void onStableNote(String noteName, double centsOff);

        /**
         * Variante con frecuencia estimada. Por defecto no hace nada.
         *
         * @param noteName   nombre de la nota
         * @param frequencyHz frecuencia estimada en Hz
         * @param centsOff   desviación en cents
         */
        default void onStablePitch(String noteName, double frequencyHz, double centsOff) { }

        /**
         * Permiso de micrófono denegado o imposible inicializar el grabador.
         */
        void onPermissionDenied();
    }

    private final Context context;
    private final Listener listener;

    /* Parámetros de captura/ventaneo. */
    private static final int SR = 44100;
    private static final int FRAME_SIZE = 2048;
    private static final int HOP_SIZE = 256;

    private AudioRecord recorder;
    private Thread worker;
    private volatile boolean running = false;

    /* Hilo de pruebas (auto-feed). */
    private Thread testThread;
    private volatile boolean feeding = false;

    /* Puertas/umbrales de detección. */
    private static final double RMS_DB_GATE = -45.0;
    private static final double MIN_CLARITY = 0.6;
    private static final double STABLE_CENTS = 25.0;
    private static final int STABLE_HITS = 2;

    /* Historial corto para validar estabilidad. */
    private final Deque<Detected> history = new ArrayDeque<>(3);
    private static final Detected DETECTED_INVALID = Detected.invalid();

    /* Rango esperado de frecuencia (opcional). */
    private volatile double expectMinHz = 0.0;
    private volatile double expectMaxHz = 0.0;

    /**
     * @param ctx      contexto de aplicación
     * @param listener receptor de eventos de nota estable y permisos
     */
    public PitchInputController(@NonNull Context ctx, @NonNull Listener listener) {
        this.context = ctx.getApplicationContext();
        this.listener = listener;
    }

    /**
     * Define un rango esperado de frecuencias en Hz para filtrar detecciones.
     * Si los valores no son válidos, se desactiva el filtro.
     *
     * @param minHz frecuencia mínima
     * @param maxHz frecuencia máxima
     */
    public void setExpectedRangeHz(double minHz, double maxHz) {
        if (minHz > 0 && maxHz > minHz) {
            this.expectMinHz = minHz;
            this.expectMaxHz = maxHz;
        } else {
            this.expectMinHz = 0.0;
            this.expectMaxHz = 0.0;
        }
    }

    /**
     * Inicia el flujo de captura y detección usando el micrófono.
     * Equivalente a {@link #startMic()}.
     */
    @MainThread
    public void start() {
        startMic();
    }

    /**
     * Inicia el micrófono y el hilo de trabajo de detección.
     * Si no hay permiso, notifica {@link Listener#onPermissionDenied()}.
     */
    @MainThread
    public void startMic() {
        cancelAutoFeed(); /* Asegura que el modo test esté detenido. */
        if (!hasMicPermission()) {
            if (listener != null) listener.onPermissionDenied();
            return;
        }
        if (running) return;

        int minBuf = AudioRecord.getMinBufferSize(
                SR, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        int recBuf = Math.max(minBuf, FRAME_SIZE * 2);
        int source = (Build.VERSION.SDK_INT >= 24)
                ? MediaRecorder.AudioSource.UNPROCESSED
                : MediaRecorder.AudioSource.MIC;

        recorder = new AudioRecord(
                source,
                SR,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                recBuf
        );
        if (recorder.getState() != AudioRecord.STATE_INITIALIZED) {
            try { recorder.release(); } catch (Throwable ignore) { }
            recorder = null;
            if (listener != null) listener.onPermissionDenied();
            return;
        }

        running = true;
        recorder.startRecording();

        worker = new Thread(this::loop, "PitchWorker");
        worker.setPriority(Thread.NORM_PRIORITY + 1);
        worker.start();
    }

    /**
     * Activa el modo de auto-alimentación de notas (sin micrófono), útil para tests/demo.
     *
     * @param noteNames lista de nombres de nota (se normalizan a sostenidos)
     * @param delayMs   retardo entre notas en milisegundos
     */
    @MainThread
    public void startAutoFeed(@NonNull List<String> noteNames, long delayMs) {
        stopMic();
        cancelAutoFeed();

        feeding = true;
        testThread = new Thread(() -> {
            try {
                for (String raw : noteNames) {
                    if (!feeding) break;
                    String n = NoteUtils.normalizeToSharp(NoteUtils.stripOctave(raw));
                    if (listener != null) {
                        listener.onStableNote(n, 0.0);
                    }
                    try {
                        Thread.sleep(Math.max(0, delayMs));
                    } catch (InterruptedException ie) {
                        break;
                    }
                }
            } finally {
                feeding = false;
                testThread = null;
            }
        }, "PitchTestFeeder");
        testThread.start();
    }

    /**
     * Detiene el modo de auto-alimentación de notas de test.
     */
    @MainThread
    public void cancelAutoFeed() {
        feeding = false;
        if (testThread != null) {
            try { testThread.join(200); } catch (InterruptedException ignore) { }
            testThread = null;
        }
    }

    /**
     * Detiene cualquier modo activo (micrófono o auto-feed) y limpia estado interno.
     */
    @MainThread
    public void stop() {
        cancelAutoFeed();
        stopMic();
        history.clear();
    }

    /* Detiene el micrófono y su hilo de procesamiento. */
    private void stopMic() {
        running = false;
        if (worker != null) {
            try { worker.join(300); } catch (InterruptedException ignore) { }
            worker = null;
        }
        if (recorder != null) {
            try { recorder.stop(); } catch (Throwable ignore) { }
            try { recorder.release(); } catch (Throwable ignore) { }
            recorder = null;
        }
    }

    /* Verifica el permiso de RECORD_AUDIO. */
    private boolean hasMicPermission() {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED;
    }

    /* Bucle de captura: ventana deslizante con saltos HOP_SIZE hasta tener FRAME_SIZE. */
    private void loop() {
        short[] in = new short[HOP_SIZE];
        float[] frame = new float[FRAME_SIZE];
        int filled = 0;

        while (running) {
            int n = recorder.read(in, 0, in.length);
            if (n <= 0) continue;

            int neededShift = Math.max(0, filled + n - FRAME_SIZE);
            if (neededShift > 0) {
                int remain = filled - neededShift;
                if (remain > 0) System.arraycopy(frame, neededShift, frame, 0, remain);
                filled = Math.max(0, remain);
            }

            int canCopy = Math.min(n, FRAME_SIZE - filled);
            for (int i = 0; i < canCopy; i++) {
                frame[filled + i] = in[i] / 32768f;
            }
            filled = Math.min(FRAME_SIZE, filled + canCopy);

            if (filled < FRAME_SIZE) continue;
            analyzeFrame(frame);
        }
    }

    /**
     * Analiza una ventana de audio y estima el tono fundamental usando NSDF y pico parabólico.
     * Aplica puertas de RMS y claridad, rango esperado, y valida estabilidad sobre un historial corto.
     */
    private void analyzeFrame(float[] x) {
        /* RMS en dB para puerta de silencio/ruido. */
        double rms = 0;
        for (float v : x) rms += v * v;
        rms = Math.sqrt(rms / x.length);
        double rmsDb = 20.0 * Math.log10(rms + 1e-12);
        if (rmsDb < RMS_DB_GATE) {
            pushHistoryInvalid();
            return;
        }

        /* NSDF para tau en [minTau, maxTau]. */
        int size = x.length;
        int maxTau = Math.min(size - 3, SR / 50);      /* ~50 Hz */
        int minTau = Math.max(2, SR / 1200);          /* ~1200 Hz */
        double[] nsdf = new double[maxTau + 1];
        double maxAtTau = -1.0;
        int tauAtMax = -1;

        for (int tau = minTau; tau <= maxTau; tau++) {
            double acf = 0;
            double m = 0;
            int L = size - tau;
            for (int i = 0; i < L; i++) {
                float xi = x[i];
                float xt = x[i + tau];
                acf += xi * xt;
                m += xi * xi + xt * xt;
            }
            double denom = m + 1e-12;
            double val = 2.0 * acf / denom;
            nsdf[tau] = val;
            if (val > maxAtTau) {
                maxAtTau = val;
                tauAtMax = tau;
            }
        }

        if (tauAtMax <= 0 || maxAtTau < MIN_CLARITY) {
            pushHistoryInvalid();
            return;
        }

        /* Refinamiento parabólico del pico y frecuencia. */
        double tau = parabolicPeak(nsdf, tauAtMax);
        double freq = SR / tau;

        /* Filtro por rango esperado. */
        if (expectMinHz > 0 && expectMaxHz > expectMinHz) {
            if (freq < expectMinHz || freq > expectMaxHz) {
                pushHistoryInvalid();
                return;
            }
        }

        /* Conversión a nota cercana y cents de error. */
        double midi = 69.0 + 12.0 * log2(freq / 440.0);
        int nearest = (int) Math.round(midi);
        double cents = (midi - nearest) * 100.0;

        String name = NoteUtils.normalizeToSharp(
                NoteUtils.stripOctave(NoteUtils.midiToNoteName(nearest)));

        Detected d = Detected.valid(name, freq, cents);
        if (isStable(d)) {
            if (listener != null) {
                listener.onStableNote(d.noteName, d.centsOff);
                listener.onStablePitch(d.noteName, d.freqHz, d.centsOff);
            }
            history.clear();
        }
    }

    /* Interpolación parabólica sobre el máximo de la NSDF. */
    private static double parabolicPeak(double[] y, int i) {
        int i0 = Math.max(1, i - 1);
        int i2 = Math.min(y.length - 2, i + 1);
        double y0 = y[i0], y1 = y[i], y2 = y[i2];
        double denom = (y0 - 2 * y1 + y2);
        if (Math.abs(denom) < 1e-12) return i;
        double x = i + 0.5 * (y0 - y2) / denom;
        return Math.max(1.0, Math.min(y.length - 1.0, x));
    }

    /* Añade lectura al historial con tope de tamaño. */
    private void pushHistory(@NonNull Detected d) {
        if (history.size() == 3) history.removeFirst();
        history.addLast(d);
    }

    /* Marca lectura inválida en historial. */
    private void pushHistoryInvalid() {
        pushHistory(DETECTED_INVALID);
    }

    /**
     * Comprueba estabilidad de nota a partir del historial.
     * Reglas:
     * - Al menos dos lecturas válidas recientes.
     * - Mismo nombre de nota y dentro de STABLE_CENTS.
     */
    private boolean isStable(Detected newest) {
        pushHistory(newest == null ? DETECTED_INVALID : newest);
        int ok = 0, totalValid = 0;
        String ref = null;
        for (Detected d : history) {
            if (d == null || !d.valid) continue;
            totalValid++;
            if (ref == null) ref = d.noteName;
            if (ref != null && ref.equals(d.noteName) && Math.abs(d.centsOff) <= STABLE_CENTS) ok++;
        }
        return totalValid >= 2 && ok >= STABLE_HITS;
    }

    /* Log base 2. */
    private static double log2(double x) {
        return Math.log(x) / Math.log(2.0);
    }

    /**
     * Estructura interna de una detección puntual.
     */
    private static class Detected {
        final boolean valid;
        final String noteName;
        final double freqHz;
        final double centsOff;

        private Detected(boolean valid, String noteName, double freqHz, double centsOff) {
            this.valid = valid;
            this.noteName = noteName;
            this.freqHz = freqHz;
            this.centsOff = centsOff;
        }

        static Detected invalid() {
            return new Detected(false, null, 0, 0);
        }

        static Detected valid(String name, double f, double c) {
            return new Detected(true, name, f, c);
        }
    }
}
