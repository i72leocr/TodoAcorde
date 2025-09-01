package com.tuguitar.todoacorde;

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
 * Implementación profesional de IChordDetector, que aísla toda la lógica de AudioRecord y DSP.
 */
@Singleton
public class ChordDetectorImpl implements IChordDetector {
    private static final String TAG = "ChordDetector";
    private static final int SAMPLE_RATE = 44100;
    private static final int BUFFER_SIZE = 8192;
    private static final float SILENCE_THRESHOLD = 3000f;

    private List<Chord> chordProfiles = new ArrayList<>();
    private AudioRecord audioRecord;
    private Thread recordingThread;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final float[][] pcpBuffers = new float[3][12];
    private final float[] avgPcp = new float[12];
    private int currentIndex = 0;

    @Inject
    public ChordDetectorImpl() {
    }

    @Override
    public void setChordProfiles(List<Chord> profiles) {
        this.chordProfiles = profiles != null ? profiles : new ArrayList<>();
        Log.d(TAG, "Set chordProfiles: " + chordProfiles.size() + " items");
    }

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
                    float[] samples = new float[read];
                    for (int i = 0; i < read; i++) {
                        samples[i] = rawBuffer[i];
                    }

                    float rms = calculateRMS(samples);
                    if (rms < SILENCE_THRESHOLD || chordProfiles.isEmpty()) {
                        listener.onChordDetected("No Chord");
                        continue;
                    }

                    float[] magnitudes = computeMagnitudes(samples);
                    updatePcpBuffers(magnitudes);
                    String chord = detectChord(avgPcp);
                    listener.onChordDetected(chord);
                }
            }
        }, "ChordDetectorThread");
        recordingThread.start();
    }

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

    private float calculateRMS(float[] samples) {
        float sum = 0f;
        for (float s : samples) {
            sum += s * s;
        }
        return (float) Math.sqrt(sum / samples.length);
    }

    private float[] computeMagnitudes(float[] samples) {
        int n = samples.length;
        float[] real = new float[n];
        float[] imag = new float[n];
        FFT.performFFT(samples, real, imag);

        float[] mags = new float[n / 2];
        for (int i = 0; i < mags.length; i++) {
            mags[i] = Math.abs(real[i]);
        }
        return mags;
    }

    private void updatePcpBuffers(float[] magnitudes) {
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

    private String detectChord(float[] pcp) {
        String best = "No Chord";
        double maxScore = 0;
        double threshold = 0.5;
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

    private double computeScore(double[] profile, float[] pcp) {
        double num = 0, den1 = 0, den2 = 0;
        for (int i = 0; i < profile.length; i++) {
            num  += profile[i] * pcp[i];
            den1 += profile[i] * profile[i];
            den2 += pcp[i] * pcp[i];
        }
        if (den1 == 0 || den2 == 0) return 0;
        return num / (Math.sqrt(den1) * Math.sqrt(den2));
    }

}
