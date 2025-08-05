package com.tuguitar.todoacorde;

import android.annotation.SuppressLint;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import java.util.Arrays;
import java.util.List;

public class ChordDetector {
    private static final int SAMPLE_RATE = 44100;
    private static final int BUFFER_SIZE_PCP = 8192;
    private static final String TAG = "ChordDetector";
    private static final float INITIAL_SILENCE_THRESHOLD = 4000f; // subido un poco

    private List<Chord> chordProfiles;
    private AudioRecord audioRecord;
    private Thread recordingThread;
    private boolean isRunning = false;
    private float[] pcp1 = new float[12];
    private float[] pcp2 = new float[12];
    private float[] pcp3 = new float[12];
    private float[] avgPcp = new float[12];
    private int currentPcpIndex = 0;
    private ChordDetectionListener listener;
    private float minRms = Float.MAX_VALUE;
    private String lastChord = "No Chord";
    private int chordStableCount = 0;
    private static final int DEBOUNCE_LIMIT = 2;

    public ChordDetector(List<Chord> chordProfiles, ChordDetectionListener listener) {
        this.chordProfiles = chordProfiles;
        this.listener = listener;
        Log.d("ChordDetector", "Number of chords loaded: " + chordProfiles.size());
        for (Chord chord : chordProfiles) {
            Log.d("ChordDetector", "Loaded Chord: " + chord.getName() + " PCP: " + Arrays.toString(chord.getPcp()));
        }
    }

    @SuppressLint("MissingPermission")
    public void startDetection() {
        if (isRunning) return;

        if (BUFFER_SIZE_PCP == AudioRecord.ERROR || BUFFER_SIZE_PCP == AudioRecord.ERROR_BAD_VALUE) {
            Log.e(TAG, "Invalid buffer size. Cannot initialize AudioRecord.");
            return;
        }
        try {
            audioRecord = new AudioRecord(MediaRecorder.AudioSource.DEFAULT, SAMPLE_RATE,
                    AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, BUFFER_SIZE_PCP);

            if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "AudioRecord initialization failed.");
                return;
            }
            audioRecord.startRecording();
            isRunning = true;

            recordingThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    short[] buffer = new short[BUFFER_SIZE_PCP / 2];

                    while (isRunning) {
                        int readResult = audioRecord.read(buffer, 0, BUFFER_SIZE_PCP / 2);
                        if (readResult > 0) {
                            float[] floatSamples = convertToFloatArray(buffer);

                            // Ventana de Hann para reducir leakage
                            applyHannWindow(floatSamples);

                            float rms = calculateRMS(floatSamples);

                            // Umbral adaptativo (rms debe fluctuar si hay sonido real)
                            minRms = Math.min(minRms, rms);
                            float dynamicThreshold = Math.max(INITIAL_SILENCE_THRESHOLD, minRms * 3f);

                            if (rms < dynamicThreshold) {
                                Log.d(TAG, "No chord detected (silence). RMS=" + rms + " threshold=" + dynamicThreshold);
                                processChordResult("No Chord");
                                continue;
                            }

                            float[] magnitudes = performFFT(floatSamples);
                            updatePcpArrays(magnitudes);

                            // Filtro: si el PCP está "difuso" es ruido, no acorde
                            if (!isPcpConcentrated(avgPcp)) {
                                Log.d(TAG, "PCP difuso, descartando como ruido: " + Arrays.toString(avgPcp));
                                processChordResult("No Chord");
                                continue;
                            }

                            String detectedChord = detectChord(avgPcp);
                            Log.d(TAG, "Detected Chord: " + detectedChord);
                            processChordResult(detectedChord);
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

    private float[] convertToFloatArray(short[] buffer) {
        float[] floatArray = new float[buffer.length];
        for (int i = 0; i < buffer.length; i++) {
            floatArray[i] = buffer[i];
        }
        return floatArray;
    }

    // Aplica ventana de Hann sobre las muestras antes de la FFT
    private void applyHannWindow(float[] samples) {
        int n = samples.length;
        for (int i = 0; i < n; i++) {
            samples[i] *= 0.5f * (1f - (float) Math.cos(2f * Math.PI * i / (n - 1)));
        }
    }

    // Calcula correctamente la magnitud FFT combinando real+imaginaria y filtra < 80 Hz
    private float[] performFFT(float[] samples) {
        int n = samples.length;
        float[] real = new float[n];
        float[] imag = new float[n];

        FFT.performFFT(samples, real, imag);

        float[] magnitudes = new float[n / 2];
        float freqRes = SAMPLE_RATE / (float) n;
        for (int i = 0; i < magnitudes.length; i++) {
            float freq = i * freqRes;
            magnitudes[i] = (float) Math.sqrt(real[i] * real[i] + imag[i] * imag[i]);
            if (freq < 80f) { // Filtra < 80 Hz (ruido ambiente, ventiladores)
                magnitudes[i] = 0f;
            }
        }
        return magnitudes;
    }

    private void updatePcpArrays(float[] magnitudes) {
        switch (currentPcpIndex) {
            case 0:
                pcp1 = PCPCalculator.calculatePCP(SAMPLE_RATE, 261.6f, magnitudes);
                break;
            case 1:
                pcp2 = PCPCalculator.calculatePCP(SAMPLE_RATE, 261.6f, magnitudes);
                break;
            case 2:
                pcp3 = PCPCalculator.calculatePCP(SAMPLE_RATE, 261.6f, magnitudes);
                break;
        }

        currentPcpIndex = (currentPcpIndex + 1) % 3;
        for (int i = 0; i < 12; i++) {
            avgPcp[i] = (pcp1[i] + pcp2[i] + pcp3[i]) / 3f;
        }
        Log.d(TAG, "avgPcp: " + arrayToString(avgPcp));
    }

    // Chequea que el PCP tenga concentración en pocas notas (máx 4), descarta acordes "difusos"
    private boolean isPcpConcentrated(float[] pcp) {
        int count = 0;
        for (float v : pcp) if (v > 0.2f) count++;
        return count <= 4;
    }

    private String detectChord(float[] pcp) {
        String bestChord = "No Chord";
        double maxScore = 0.0;
        double threshold = 0.7;  // Threshold más alto, ajusta si es necesario

        for (Chord chord : chordProfiles) {
            double score = computeChordScoreWithPenalty(chord.pcp, pcp);
            Log.d(TAG, "Chord: " + chord.getName() + " Score: " + score);
            if (score > maxScore && score > threshold) {
                maxScore = score;
                bestChord = chord.name;
            }
        }
        return bestChord;
    }

    private double computeChordScoreWithPenalty(double[] chordPcp, float[] pcp) {
        double score = 0.0;
        double totalEnergy = 0.0;
        double noise = 0.0;
        for (int i = 0; i < chordPcp.length; i++) {
            score += chordPcp[i] * pcp[i];
            totalEnergy += pcp[i];
            if (chordPcp[i] < 0.05) noise += pcp[i];
        }
        double noisePenalty = noise / (totalEnergy + 1e-8);
        return score * (1.0 - noisePenalty); // Penaliza score si hay notas fuera del acorde
    }

    private float calculateRMS(float[] samples) {
        float sum = 0.0f;
        for (float sample : samples) {
            sum += sample * sample;
        }
        return (float) Math.sqrt(sum / samples.length);
    }

    // Debouncing para mostrar solo si se repite varias veces seguidas
    private void processChordResult(String detectedChord) {
        if (detectedChord.equals(lastChord)) {
            chordStableCount++;
        } else {
            chordStableCount = 1;
            lastChord = detectedChord;
        }

        if (chordStableCount >= DEBOUNCE_LIMIT) {
            listener.onChordDetected(detectedChord);
        }
    }

    private String arrayToString(float[] array) {
        StringBuilder sb = new StringBuilder();
        for (float value : array) {
            sb.append(value).append(", ");
        }
        return sb.toString();
    }
}
