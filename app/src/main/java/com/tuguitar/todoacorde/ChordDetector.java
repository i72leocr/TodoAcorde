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
    private static final float SILENCE_THRESHOLD = 3000f; // RMS threshold for detecting silence

    private List<Chord> chordProfiles;
    private AudioRecord audioRecord;
    private Thread recordingThread;
    private boolean isRunning = false;
    private float[] pcp1 = new float[12];
    private float[] pcp2 = new float[12];
    private float[] pcp3 = new float[12];
    private float[] avgPcp = new float[12];
    private int currentPcpIndex = 0; // Index to track which PCP array to update
    private ChordDetectionListener listener;


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

                        float rms = calculateRMS(floatSamples);

                        if (rms < SILENCE_THRESHOLD) {
                            Log.d(TAG, "No chord detected (silence).");
                            listener.onChordDetected("No Chord");
                            continue;
                        }

                        float[] magnitudes = performFFT(floatSamples);
                        updatePcpArrays(magnitudes);
                        String detectedChord = detectChord(avgPcp);
                        Log.d(TAG, "Detected Chord: " + detectedChord);
                        listener.onChordDetected(detectedChord);
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

    private float[] performFFT(float[] samples) {
        int n = samples.length;
        float[] real = new float[n];
        float[] imag = new float[n];

        // Realiza la FFT sobre samples
        FFT.performFFT(samples, real, imag);  // Aquí obtenemos las partes reales e imaginarias

        // Calcula las magnitudes usando solo la parte real
        float[] magnitudes = new float[n / 2];
        for (int i = 0; i < magnitudes.length; i++) {
            magnitudes[i] = Math.abs(real[i]);  // Usamos solo la parte real, ignorando la parte imaginaria
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

    private String detectChord(float[] pcp) {
        String bestChord = "No Chord";
        double maxScore = 0.0;  // Inicializa con 0 en lugar de Double.MIN_VALUE
        double threshold = 0.5;  // Ajusta el umbral de coincidencia

        for (Chord chord : chordProfiles) {
            double score = computeChordScore(chord.pcp, pcp);
            Log.d(TAG, "Chord: " + chord.getName() + " Score: " + score);
            if (score > maxScore && score > threshold) {
                maxScore = score;
                bestChord = chord.name;
            }
        }

        return bestChord;
    }


    private double computeChordScore(double[] chordPcp, float[] pcp) {
        double score = 0.0;
        for (int i = 0; i < chordPcp.length; i++) {
            score += chordPcp[i] * pcp[i];
        }
        return score;
    }

    private float calculateRMS(float[] samples) {
        float sum = 0.0f;
        for (float sample : samples) {
            sum += sample * sample;
        }
        return (float) Math.sqrt(sum / samples.length);
    }

    private String arrayToString(float[] array) {
        StringBuilder sb = new StringBuilder();
        for (float value : array) {
            sb.append(value).append(", ");
        }
        return sb.toString();
    }
}
