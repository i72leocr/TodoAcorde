package com.todoacorde.todoacorde.tuner.data;

import android.annotation.SuppressLint;
import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import com.todoacorde.todoacorde.FFT;
import com.todoacorde.todoacorde.tuner.domain.PitchDetectorCallback;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Detector de tono por FFT y HPS (Harmonic Product Spectrum) con refinamiento
 * por interpolación parabólica. Captura audio en tiempo real con {@link AudioRecord}
 * y reporta las detecciones mediante {@link PitchDetectorCallback}.
 */
public class PitchDetector {

    /** Frecuencia de muestreo del capturador de audio (Hz). */
    private static final int SAMPLE_RATE = 44100;

    /** Tamaño del búfer de entrada para AudioRecord (muestras). */
    private static final int BUFFER_SIZE = 8192;

    private static final String TAG = "PitchDetector";

    /** Frecuencia mínima aceptada para detección (Hz). */
    private static final int MIN_PITCH = 50;

    /** Frecuencia máxima aceptada para detección (Hz). */
    private static final int MAX_PITCH = 400;

    /** Ventana temporal (ms) para agrupar y filtrar detecciones. */
    private static final int DETECTION_PERIOD_MS = 100;

    /** Umbral de cambio (Hz) para considerar variación significativa del tono. */
    private static final double FREQUENCY_THRESHOLD = 10.0;

    /** Recurso de captura de audio. */
    private AudioRecord audioRecord;

    /** Hilo de lectura y procesado de audio. */
    private Thread recordingThread;

    /** Indicador de ejecución del ciclo de detección. */
    private boolean isRunning = false;

    /** Frecuencia de referencia usada para filtrar cambios bruscos. */
    private double referenceFrequency = -1;

    /** Marca temporal de la última detección válida. */
    private long lastDetectionTime = 0;

    /** Callback de notificación de detecciones. */
    private final PitchDetectorCallback callback;

    /** Contexto Android. */
    private final Context context;

    /**
     * Crea un detector de tono.
     *
     * @param callback receptor de eventos de detección.
     * @param context  contexto de la aplicación.
     */
    public PitchDetector(PitchDetectorCallback callback, Context context) {
        this.callback = callback;
        this.context = context;
    }

    /**
     * Inicia la captura de audio y el bucle de detección.
     * Debe haberse concedido el permiso de grabación.
     */
    @SuppressLint("MissingPermission")
    public void startDetection() {
        if (isRunning) return;
        try {
            audioRecord = new AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    BUFFER_SIZE
            );

            if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "AudioRecord initialization failed.");
                return;
            }

            audioRecord.startRecording();
            isRunning = true;

            recordingThread = new Thread(() -> {
                short[] buffer = new short[BUFFER_SIZE / 2];
                List<Double> pitchBuffer = new ArrayList<>();

                while (isRunning) {
                    int readResult = audioRecord.read(buffer, 0, BUFFER_SIZE / 2);
                    if (readResult > 0) {
                        Log.d(TAG, "Audio Buffer: " + Arrays.toString(buffer));

                        double pitch = detectPitch(buffer);
                        if (pitch > 0) {
                            // Notificación inmediata de la medida puntual
                            callback.onPitchDetected(pitch);

                            long currentTime = System.currentTimeMillis();

                            // Control de referencia y ventana temporal de agrupado
                            if (referenceFrequency == -1 || isSignificantFrequencyChange(pitch)) {
                                if (pitch >= MIN_PITCH && pitch <= MAX_PITCH) {
                                    referenceFrequency = pitch;
                                    lastDetectionTime = currentTime;
                                    pitchBuffer.clear();
                                }
                            }

                            // Acumula dentro de la ventana temporal; al expirar, calcula modo filtrado
                            if (currentTime - lastDetectionTime <= DETECTION_PERIOD_MS) {
                                pitchBuffer.add(pitch);
                            } else {
                                if (pitchBuffer.size() > 1) {
                                    double proximityThreshold = 3.0;
                                    double averagePitch = calculateMode(pitchBuffer, proximityThreshold);
                                    Log.d(TAG, "Detected pitch: " + averagePitch);
                                } else {
                                    Log.d(TAG, "Not enough pitches in buffer, skipping average calculation.");
                                    Log.d(TAG, "Current buffer size: " + pitchBuffer.size());
                                }
                                referenceFrequency = -1;
                                pitchBuffer.clear();
                            }
                        }
                    }
                }
            }, "Recording Thread");

            recordingThread.start();
        } catch (Exception e) {
            Log.e(TAG, "Error initializing AudioRecord", e);
        }
    }

    /**
     * Detiene la captura y libera los recursos asociados.
     */
    public void stopDetection() {
        if (!isRunning) return;

        isRunning = false;

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
     * Comprueba si existe variación significativa respecto a la referencia.
     *
     * @param pitch frecuencia actual (Hz).
     * @return true si la diferencia supera el umbral configurado.
     */
    private boolean isSignificantFrequencyChange(double pitch) {
        return Math.abs(pitch - referenceFrequency) > FREQUENCY_THRESHOLD;
    }

    /**
     * Calcula la media aritmética de una lista de frecuencias.
     *
     * @param pitches lista de frecuencias (Hz).
     * @return media aritmética o 0.0 si la lista está vacía.
     */
    private double calculateAverage(List<Double> pitches) {
        double sum = 0.0;
        for (double pitch : pitches) {
            sum += pitch;
        }
        return pitches.size() > 0 ? sum / pitches.size() : 0.0;
    }

    /**
     * Estima la moda de una lista de frecuencias tras un filtrado por proximidad.
     * Primero determina los valores más frecuentes; usa su media como referencia
     * y filtra el conjunto por un umbral de cercanía. Después calcula la moda del
     * conjunto filtrado.
     *
     * @param pitches            lista original de frecuencias (Hz).
     * @param proximityThreshold tolerancia de proximidad en Hz respecto a la referencia.
     * @return moda del conjunto filtrado o 0.0 si no hay datos válidos.
     */
    private double calculateMode(List<Double> pitches, double proximityThreshold) {
        if (pitches == null || pitches.isEmpty()) {
            return 0.0;
        }

        Map<Double, Integer> pitchFrequency = new HashMap<>();
        for (double pitch : pitches) {
            pitchFrequency.put(pitch, pitchFrequency.getOrDefault(pitch, 0) + 1);
        }

        int maxFrequency = Collections.max(pitchFrequency.values());
        List<Double> mostFrequentPitches = new ArrayList<>();
        for (Map.Entry<Double, Integer> entry : pitchFrequency.entrySet()) {
            if (entry.getValue() == maxFrequency) {
                mostFrequentPitches.add(entry.getKey());
            }
        }

        double referencePitch = calculateAverage(mostFrequentPitches);

        List<Double> filteredPitches = new ArrayList<>();
        for (double pitch : pitches) {
            if (Math.abs(pitch - referencePitch) <= proximityThreshold) {
                filteredPitches.add(pitch);
            }
        }
        if (filteredPitches.isEmpty()) {
            return 0.0;
        }

        pitchFrequency.clear();
        for (double pitch : filteredPitches) {
            pitchFrequency.put(pitch, pitchFrequency.getOrDefault(pitch, 0) + 1);
        }

        double mode = 0.0;
        maxFrequency = 0;
        for (Map.Entry<Double, Integer> entry : pitchFrequency.entrySet()) {
            if (entry.getValue() > maxFrequency) {
                maxFrequency = entry.getValue();
                mode = entry.getKey();
            }
        }
        return mode;
    }

    /**
     * Pipeline de detección de tono para un bloque de audio.
     * Normaliza, aplica ventana de Hann, rellena con ceros, calcula FFT,
     * aplica HPS, detecta pico y refina con interpolación parabólica.
     *
     * @param signal bloque PCM de 16 bits con signo.
     * @return frecuencia estimada en Hz; 0 si no se estima.
     */
    private double detectPitch(short[] signal) {
        float[] normalizedSignal = normalizeSignal(signal);
        float[] windowedSignal = applyHannWindow(normalizedSignal);
        float[] paddedSignal = zeroPad(windowedSignal, BUFFER_SIZE * 4);

        float[] real = new float[paddedSignal.length];
        float[] imag = new float[paddedSignal.length];
        FFT.performFFT(paddedSignal, real, imag);

        double[] magnitudes = calculateMagnitudes(real, imag);
        double[] hps = harmonicProductSpectrum(magnitudes);
        int peakIndex = findPeakIndex(hps);
        double interpolatedPeakIndex = parabolicInterpolation(hps, peakIndex);

        double frequency = (double) SAMPLE_RATE * interpolatedPeakIndex / paddedSignal.length;
        Log.d(TAG, "Frequency: " + frequency);
        return frequency;
    }

    /**
     * Normaliza la señal corta [-32768, 32767] a flotante [-1, 1].
     * Completa con ceros hasta {@link #BUFFER_SIZE}.
     *
     * @param signal entrada PCM 16-bit.
     * @return vector de tamaño BUFFER_SIZE normalizado.
     */
    private float[] normalizeSignal(short[] signal) {
        float[] normalizedSignal = new float[BUFFER_SIZE];
        for (int i = 0; i < BUFFER_SIZE; i++) {
            if (i < signal.length) {
                normalizedSignal[i] = signal[i] / 32768.0f;
            } else {
                normalizedSignal[i] = 0.0f;
            }
        }
        Log.d(TAG, "Normalized Signal: " + Arrays.toString(normalizedSignal));
        return normalizedSignal;
    }

    /**
     * Aplica ventana de Hann a la señal.
     *
     * @param signal vector de entrada.
     * @return señal ventana de Hann aplicada.
     */
    private float[] applyHannWindow(float[] signal) {
        float[] windowedSignal = new float[signal.length];
        for (int i = 0; i < signal.length; i++) {
            windowedSignal[i] =
                    signal[i] * (0.5f - 0.5f * (float) Math.cos(2 * Math.PI * i / (signal.length - 1)));
        }
        Log.d(TAG, "Windowed Signal: " + Arrays.toString(windowedSignal));
        return windowedSignal;
    }

    /**
     * Rellena con ceros hasta el tamaño indicado.
     *
     * @param signal  señal base.
     * @param newSize tamaño final deseado.
     * @return vector de tamaño newSize con cola de ceros.
     */
    private float[] zeroPad(float[] signal, int newSize) {
        float[] paddedSignal = new float[newSize];
        System.arraycopy(signal, 0, paddedSignal, 0, signal.length);
        return paddedSignal;
    }

    /**
     * Calcula el módulo espectral a partir de componentes real e imaginaria.
     *
     * @param real parte real de la FFT.
     * @param imag parte imaginaria de la FFT.
     * @return magnitudes espectrales.
     */
    private double[] calculateMagnitudes(float[] real, float[] imag) {
        double[] magnitudes = new double[real.length];
        for (int i = 0; i < real.length; i++) {
            magnitudes[i] = Math.sqrt(real[i] * real[i] + imag[i] * imag[i]);
        }
        Log.d(TAG, "Magnitudes: " + Arrays.toString(magnitudes));
        return magnitudes;
    }

    /**
     * Espectro por Producto Armónico (HPS).
     * Multiplica y acumula magnitudes en índices armónicos para realzar la fundamental.
     *
     * @param magnitudes magnitudes espectrales.
     * @return vector HPS.
     */
    private double[] harmonicProductSpectrum(double[] magnitudes) {
        int length = magnitudes.length;
        int harmonics = 5;
        double[] hps = new double[length];

        for (int i = 0; i < length; i++) {
            hps[i] = magnitudes[i];
            for (int h = 2; h <= harmonics; h++) {
                if (i * h < length) {
                    hps[i] *= magnitudes[i * h];
                }
            }
        }

        // Componente aditiva para robustez frente a ceros
        for (int i = 0; i < length; i++) {
            for (int h = 2; h <= harmonics; h++) {
                if (i * h < length) {
                    hps[i] += magnitudes[i * h];
                }
            }
        }

        Log.d(TAG, "HPS: " + Arrays.toString(hps));
        return hps;
    }

    /**
     * Busca el índice de pico principal en la mitad baja del espectro.
     *
     * @param magnitudes vector de entrada (p. ej., HPS).
     * @return índice del máximo local principal.
     */
    private int findPeakIndex(double[] magnitudes) {
        int peakIndex = 0;
        double peakValue = 0;
        for (int i = 1; i < magnitudes.length / 2; i++) {
            if (magnitudes[i] > peakValue) {
                peakValue = magnitudes[i];
                peakIndex = i;
            }
        }
        Log.d(TAG, "Peak Index: " + peakIndex);
        return peakIndex;
    }

    /**
     * Interpolación parabólica alrededor de un pico discreto para afinar la estimación
     * submuestral del índice de frecuencia.
     *
     * @param array vector espectral.
     * @param x     índice entero del máximo detectado.
     * @return índice real interpolado.
     */
    private double parabolicInterpolation(double[] array, int x) {
        if (x > 0 && x < array.length - 1) {
            double alpha = array[x - 1];
            double beta = array[x];
            double gamma = array[x + 1];
            double offset = (gamma - alpha) / (2 * (2 * beta - alpha - gamma));
            return x + offset;
        } else {
            return x;
        }
    }
}
