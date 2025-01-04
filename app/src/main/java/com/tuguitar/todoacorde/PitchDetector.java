package com.tuguitar.todoacorde;

import android.annotation.SuppressLint;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import android.content.Context;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;

public class PitchDetector {
    private static final int SAMPLE_RATE = 44100;
    private static final int BUFFER_SIZE = 8192; // Ajuste para que coincida con Pitch.java
    private static final String TAG = "PitchDetector";
    private static final int MIN_PITCH = 50;  // Frecuencia mínima para una guitarra (aproximadamente Hz)
    private static final int MAX_PITCH = 400; // Frecuencia máxima para una guitarra (aproximadamente Hz)
    private static final int DETECTION_PERIOD_MS = 100;
    private static final double FREQUENCY_THRESHOLD = 10.0;

    private AudioRecord audioRecord;
    private Thread recordingThread;
    private boolean isRunning = false;
    private double referenceFrequency = -1;
    private long lastDetectionTime = 0;
    private PitchDetectorCallback callback;
    private Context context;

    public PitchDetector(PitchDetectorCallback callback, Context context) {
        this.callback = callback;
        this.context = context;
    }

    @SuppressLint("MissingPermission")
    public void startDetection() {
        if (isRunning) return;

        // Initialize AudioRecord
        try {
            audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE,
                    AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, BUFFER_SIZE);

            if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "AudioRecord initialization failed.");
                return;
            }

            audioRecord.startRecording();
            isRunning = true;

        recordingThread = new Thread(new Runnable() {
            @Override
            public void run() {
                short[] buffer = new short[BUFFER_SIZE/2];
                List<Double> pitchBuffer = new ArrayList<>();

                while (isRunning) {
                    int readResult = audioRecord.read(buffer, 0, BUFFER_SIZE/2);
                    if (readResult > 0) {
                        Log.d(TAG, "Audio Buffer: " + Arrays.toString(buffer));
                        double pitch = detectPitch(buffer);

                        if (pitch > 0) {
                            callback.onPitchDetected(pitch);
                            long currentTime = System.currentTimeMillis();
                            if (referenceFrequency == -1 || isSignificantFrequencyChange(pitch)) {
                                if (pitch >= MIN_PITCH && pitch <= MAX_PITCH) {
                                    referenceFrequency = pitch;
                                    lastDetectionTime = currentTime;
                                    pitchBuffer.clear();
                                }
                            }

                            if (currentTime - lastDetectionTime <= DETECTION_PERIOD_MS) {
                                pitchBuffer.add(pitch);
                            } else {
                                if (pitchBuffer.size() > 1) {  // Asegúrate de tener más de 1 valor antes de calcular la moda
                                    // Asumiendo que tienes un valor de umbral de proximidad definido
                                    double proximityThreshold = 3.0;  // Este es un valor de ejemplo, ajusta según lo que necesites
                                    double averagePitch = calculateMode(pitchBuffer, proximityThreshold);  // Llamada a calculateMode
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
            }
        }, "Recording Thread");

        recordingThread.start();
        } catch (Exception e) {
            Log.e(TAG, "Error initializing AudioRecord", e);
        }
    }

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

    private boolean isSignificantFrequencyChange(double pitch) {
        return Math.abs(pitch - referenceFrequency) > FREQUENCY_THRESHOLD;
    }

    private double calculateAverage(List<Double> pitches) {
        double sum = 0.0;
        for (double pitch : pitches) {
            sum += pitch;
        }
        return pitches.size() > 0 ? sum / pitches.size() : 0.0;
    }
    private double calculateMode(List<Double> pitches, double proximityThreshold) {
        // Verificamos si la lista está vacía
        if (pitches == null || pitches.isEmpty()) {
            return 0.0;
        }

        // Creamos un mapa para contar las ocurrencias de cada tono
        Map<Double, Integer> pitchFrequency = new HashMap<>();
        for (double pitch : pitches) {
            pitchFrequency.put(pitch, pitchFrequency.getOrDefault(pitch, 0) + 1);
        }

        // Encontramos la frecuencia máxima
        int maxFrequency = Collections.max(pitchFrequency.values());

        // Filtramos los valores que tienen la frecuencia máxima
        List<Double> mostFrequentPitches = new ArrayList<>();
        for (Map.Entry<Double, Integer> entry : pitchFrequency.entrySet()) {
            if (entry.getValue() == maxFrequency) {
                mostFrequentPitches.add(entry.getKey());
            }
        }

        // Calculamos la media de los valores más frecuentes como referencia
        double referencePitch = calculateAverage(mostFrequentPitches);

        // Filtramos los valores que están dentro del umbral de proximidad respecto a la media de los más frecuentes
        List<Double> filteredPitches = new ArrayList<>();
        for (double pitch : pitches) {
            if (Math.abs(pitch - referencePitch) <= proximityThreshold) {
                filteredPitches.add(pitch);
            }
        }

        // Si no quedan valores después del filtrado, retornamos 0.0
        if (filteredPitches.isEmpty()) {
            return 0.0;
        }

        // Volvemos a contar las ocurrencias dentro del rango filtrado para calcular la moda final
        pitchFrequency.clear();
        for (double pitch : filteredPitches) {
            pitchFrequency.put(pitch, pitchFrequency.getOrDefault(pitch, 0) + 1);
        }

        // Buscamos la moda final dentro de los valores filtrados
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


    private double detectPitch(short[] signal) {
        float[] normalizedSignal = normalizeSignal(signal);
        float[] windowedSignal = applyHannWindow(normalizedSignal);

        // Aplicar zero-padding
        float[] paddedSignal = zeroPad(windowedSignal, BUFFER_SIZE * 4);

        float[] real = new float[paddedSignal.length];
        float[] imag = new float[paddedSignal.length];

        // Utilizar la función nativa de FFT desde la clase FFT
        FFT.performFFT(paddedSignal, real, imag);

        double[] magnitudes = calculateMagnitudes(real, imag);
        double[] hps = harmonicProductSpectrum(magnitudes);
        int peakIndex = findPeakIndex(hps);
        double interpolatedPeakIndex = parabolicInterpolation(hps, peakIndex);
        double frequency = (double) SAMPLE_RATE * interpolatedPeakIndex / paddedSignal.length;

        Log.d(TAG, "Frequency: " + frequency);

        return frequency;
    }

    private float[] normalizeSignal(short[] signal) {
        float[] normalizedSignal = new float[BUFFER_SIZE];
        for (int i = 0; i < BUFFER_SIZE; i++) {
            if (i < signal.length) {
                normalizedSignal[i] = signal[i] / 32768.0f; // Normalización a rango [-1, 1]
            } else {
                normalizedSignal[i] = 0.0f;
            }
        }
        Log.d(TAG, "Normalized Signal: " + Arrays.toString(normalizedSignal));
        return normalizedSignal;
    }

    private float[] applyHannWindow(float[] signal) {
        float[] windowedSignal = new float[signal.length];
        for (int i = 0; i < signal.length; i++) {
            windowedSignal[i] = signal[i] * (0.5f - 0.5f * (float) Math.cos(2 * Math.PI * i / (signal.length - 1)));
        }
        Log.d(TAG, "Windowed Signal: " + Arrays.toString(windowedSignal));
        return windowedSignal;
    }

    private float[] zeroPad(float[] signal, int newSize) {
        float[] paddedSignal = new float[newSize];
        System.arraycopy(signal, 0, paddedSignal, 0, signal.length);
        return paddedSignal;
    }

    private double[] calculateMagnitudes(float[] real, float[] imag) {
        double[] magnitudes = new double[real.length];
        for (int i = 0; i < real.length; i++) {
            magnitudes[i] = Math.sqrt(real[i] * real[i] + imag[i] * imag[i]);
        }
        Log.d(TAG, "Magnitudes: " + Arrays.toString(magnitudes));
        return magnitudes;
    }

    private double[] harmonicProductSpectrum(double[] magnitudes) {
        int length = magnitudes.length;
        int harmonics = 5; // Número de armónicos a considerar
        double[] hps = new double[length];

        for (int i = 0; i < length; i++) {
            hps[i] = magnitudes[i];
            for (int h = 2; h <= harmonics; h++) {
                if (i * h < length) {
                    hps[i] *= magnitudes[i * h];
                }
            }
        }

        // Aplicar sumatoria de magnitudes
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

    private int findPeakIndex(double[] magnitudes) {
        int peakIndex = 0;
        double peakValue = 0;
        for (int i = 1; i < magnitudes.length / 2; i++) {  // Use only the first half of the spectrum
            if (magnitudes[i] > peakValue) {
                peakValue = magnitudes[i];
                peakIndex = i;
            }
        }
        Log.d(TAG, "Peak Index: " + peakIndex);
        return peakIndex;
    }

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
