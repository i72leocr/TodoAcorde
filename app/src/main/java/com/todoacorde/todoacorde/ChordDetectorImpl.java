package com.todoacorde.todoacorde;

import android.annotation.SuppressLint;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Implementación de {@link IChordDetector} basada en análisis espectral (FFT) y
 * cromagrama (PCP). Captura audio en tiempo real usando {@link AudioRecord},
 * calcula magnitudes espectrales, deriva un vector PCP suavizado y lo compara
 * con perfiles de acordes para estimar el acorde más probable.
 *
 * Características principales:
 * - Frecuencia de muestreo: 44.1 kHz.
 * - Búfer de lectura: 8192 muestras (16 bits mono).
 * - Suavizado del PCP mediante media móvil de 3 ventanas.
 * - Coincidencia acorde–señal mediante similitud coseno.
 *
 * Requisitos:
 * - Permiso de grabación de audio concedido antes de invocar {@link #startDetection(ChordDetectionListener)}.
 * - Inyectable con Hilt/Dagger como {@link Singleton}.
 */
@Singleton
public class ChordDetectorImpl implements IChordDetector {
    private static final String TAG = "ChordDetector";
    /** Frecuencia de muestreo (Hz). */
    private static final int SAMPLE_RATE = 44100;
    /** Tamaño sugerido del búfer interno. */
    private static final int BUFFER_SIZE = 8192;
    /** Umbral RMS por debajo del cual se considera silencio/ruido bajo. */
    private static final float SILENCE_THRESHOLD = 3000f;

    /** Perfiles de acordes (PCP normalizados) contra los que se compara la entrada. */
    private List<Chord> chordProfiles = new ArrayList<>();
    /** Capturador de audio del sistema. */
    private AudioRecord audioRecord;
    /** Hilo de trabajo para lectura/procesado continuo. */
    private Thread recordingThread;
    /** Flag de ejecución segura entre hilos. */
    private final AtomicBoolean running = new AtomicBoolean(false);
    /** Ventanas deslizantes de PCP para suavizado (3 últimas). */
    private final float[][] pcpBuffers = new float[3][12];
    /** PCP promedio de las últimas ventanas. */
    private final float[] avgPcp = new float[12];
    /** Índice circular para escribir en {@link #pcpBuffers}. */
    private int currentIndex = 0;

    /**
     * Constructor por defecto para inyección.
     */
    @Inject
    public ChordDetectorImpl() {
    }

    /**
     * Establece los perfiles de acordes de referencia.
     *
     * @param profiles lista de {@link Chord} cuyos vectores {@code pcp} se usarán
     *                 como base de comparación. Si es {@code null}, se limpia.
     */
    @Override
    public void setChordProfiles(List<Chord> profiles) {
        this.chordProfiles = profiles != null ? profiles : new ArrayList<>();
        Log.d(TAG, "Set chordProfiles: " + chordProfiles.size() + " items");
    }

    /**
     * Inicia la captura y detección continua de acordes.
     * Debe llamarse con el permiso de micrófono concedido.
     *
     * Flujo:
     * 1) Inicializa y arranca {@link AudioRecord}.
     * 2) En un hilo, lee bloques PCM, descarta silencio según RMS,
     *    calcula magnitudes vía {@link FFT#performFFT(float[], float[], float[])},
     *    deriva PCP y decide acorde por similitud coseno.
     * 3) Notifica resultados mediante {@code listener}.
     *
     * @param listener receptor de eventos de acordes detectados.
     */
    @Override
    @SuppressLint("MissingPermission")
    public void startDetection(ChordDetectionListener listener) {
        if (running.get()) return;
        Log.d(TAG, "Starting detection with " + chordProfiles.size() + " profiles");

        int minBuffer = AudioRecord.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
        );

        audioRecord = new AudioRecord(
                MediaRecorder.AudioSource.DEFAULT,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                Math.max(BUFFER_SIZE, minBuffer)
        );

        if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
            Log.e(TAG, "AudioRecord initialization failed");
            return;
        }

        audioRecord.startRecording();
        running.set(true);

        recordingThread = new Thread(() -> {
            short[] rawBuffer = new short[BUFFER_SIZE / 2];
            while (running.get() && !Thread.currentThread().isInterrupted()) {
                int read = audioRecord.read(rawBuffer, 0, rawBuffer.length, AudioRecord.READ_BLOCKING);
                if (read > 0) {
                    // Conversión a float para procesado
                    float[] samples = new float[read];
                    for (int i = 0; i < read; i++) {
                        samples[i] = rawBuffer[i];
                    }

                    // Puerta por nivel (RMS)
                    float rms = calculateRMS(samples);
                    if (rms < SILENCE_THRESHOLD || chordProfiles.isEmpty()) {
                        listener.onChordDetected("No Chord");
                        continue;
                    }

                    // Espectro + PCP suavizado y decisión
                    float[] magnitudes = computeMagnitudes(samples);
                    updatePcpBuffers(magnitudes);
                    String chord = detectChord(avgPcp);
                    listener.onChordDetected(chord);
                }
            }
        }, "ChordDetectorThread");

        recordingThread.start();
    }

    /**
     * Detiene la detección, liberando el audio y deteniendo el hilo de trabajo.
     * Es seguro llamar varias veces; sólo tiene efecto si estaba en ejecución.
     */
    @Override
    public void stopDetection() {
        if (!running.getAndSet(false)) return;

        if (audioRecord != null) {
            audioRecord.stop();
            audioRecord.release();
            audioRecord = null;
        }
        if (recordingThread != null) {
            recordingThread.interrupt();
            recordingThread = null;
        }
    }

    /**
     * Calcula el nivel RMS del bloque de muestras.
     *
     * @param samples bloque PCM en coma flotante.
     * @return valor RMS del bloque.
     */
    private float calculateRMS(float[] samples) {
        float sum = 0f;
        for (float s : samples) {
            sum += s * s;
        }
        return (float) Math.sqrt(sum / samples.length);
    }

    /**
     * Calcula magnitudes espectrales a partir de un bloque temporal.
     * Aplica FFT real y retorna el semiespectro (magnitud del eje real).
     *
     * @param samples bloque temporal PCM.
     * @return vector de magnitudes (longitud n/2).
     */
    private float[] computeMagnitudes(float[] samples) {
        int n = samples.length;
        float[] real = new float[n];
        float[] imag = new float[n];
        FFT.performFFT(samples, real, imag);

        float[] mags = new float[n / 2];
        for (int i = 0; i < mags.length; i++) {
            // Uso de componente real como magnitud aproximada (rápido)
            mags[i] = Math.abs(real[i]);
        }
        return mags;
    }

    /**
     * Actualiza el buffer circular de PCP y recalcula el promedio
     * para suavizar fluctuaciones instantáneas.
     *
     * @param magnitudes espectro de magnitudes para la ventana actual.
     */
    private void updatePcpBuffers(float[] magnitudes) {
        // A4≈440, referencia C4≈261.6 Hz usada por PCPCalculator
        pcpBuffers[currentIndex] = PCPCalculator.calculatePCP(
                SAMPLE_RATE, 261.6f, magnitudes
        );
        currentIndex = (currentIndex + 1) % pcpBuffers.length;

        for (int i = 0; i < avgPcp.length; i++) {
            avgPcp[i] = (
                    pcpBuffers[0][i] + pcpBuffers[1][i] + pcpBuffers[2][i]
            ) / pcpBuffers.length;
        }
        Log.d(TAG, "avgPcp: " + Arrays.toString(avgPcp));
    }

    /**
     * Selecciona el acorde con mayor similitud coseno respecto al PCP promedio.
     *
     * @param pcp cromagrama promedio actual (12 bins).
     * @return nombre de acorde más probable o "No Chord" si no supera umbral.
     */
    private String detectChord(float[] pcp) {
        String best = "No Chord";
        double maxScore = 0;
        double threshold = 0.5; // umbral mínimo de similitud

        for (Chord chord : chordProfiles) {
            double score = computeScore(chord.getPcp(), pcp);
            Log.d(TAG, chord.getName() + " score=" + score);
            if (score > maxScore && score > threshold) {
                maxScore = score;
                best = chord.getName();
            }
        }
        return best;
    }

    /**
     * Similitud coseno entre el perfil de acorde y el PCP observado.
     *
     * @param profile perfil de acorde (vector doble de 12).
     * @param pcp     cromagrama observado (vector float de 12).
     * @return valor en [0,1], mayor es más parecido.
     */
    private double computeScore(double[] profile, float[] pcp) {
        double num = 0, den1 = 0, den2 = 0;
        for (int i = 0; i < profile.length; i++) {
            num += profile[i] * pcp[i];
            den1 += profile[i] * profile[i];
            den2 += pcp[i] * pcp[i];
        }
        if (den1 == 0 || den2 == 0) return 0;
        return num / (Math.sqrt(den1) * Math.sqrt(den2));
    }
}
