package com.tuguitar.todoacorde;
/*
import android.annotation.SuppressLint;
import android.media.AudioFormat;
import android.media.MediaRecorder;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class AudioRecord extends Thread {
    private static final int SAMPLE_RATE = 44100;
    private static final int BUFFER_SIZE = 16384;
    private static final int DETECTION_PERIOD_MS = 250;
    private static final double FREQUENCY_THRESHOLD = 5.0;
    private static final double[] GUITAR_FREQUENCIES = {81.41, 110.00, 146.83, 196.00, 246.94, 329.63};
    private static final double GUITAR_FREQUENCY_RANGE = 50.0;
    private static final double SILENCE_THRESHOLD = 150.0;

    private boolean isRunning;
    private final Pitch pitchDetector;
    private final PitchListener pitchListener;
    private final ChordDetector chordDetector;
    private double referenceFrequency = -1;
    private long lastDetectionTime = 0;

    private double[] pcp1 = new double[12];
    private double[] pcp2 = new double[12];
    private double[] pcp3 = new double[12];
    private double[] avgpcp = new double[12];
    private int currentpcp = 0;

    public interface PitchListener {
        void onPitchDetected(double pitch);
        void onChordDetected(String chord);
    }

    public AudioRecord(Pitch pitchDetector, PitchListener pitchListener, List<Chord> chordProfiles) {
        this.pitchDetector = pitchDetector;
        this.pitchListener = pitchListener;
        this.chordDetector = new ChordDetector(chordProfiles);
        this.isRunning = false;
    }

    @Override
    public void run() {
        @SuppressLint("MissingPermission") android.media.AudioRecord audioRecord = new android.media.AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                BUFFER_SIZE
        );

        int state = audioRecord.getState();
        if (state != android.media.AudioRecord.STATE_INITIALIZED) {
            Log.e("AudioRecord", "AudioRecord initialization failed with state: " + state);
            return;
        }

        audioRecord.startRecording();
        short[] buffer = new short[BUFFER_SIZE];
        List<Double> pitchBuffer = new ArrayList<>();

        while (isRunning) {
            int read = audioRecord.read(buffer, 0, BUFFER_SIZE);
            if (read > 0) {
                double pitch = pitchDetector.detectPitch(buffer);

               ) if (pitch > 0) {
                    long currentTime = System.currentTimeMillis();

                    if (referenceFrequency == -1 || isSignificantFrequencyChange(pitch)) {
                        if (isWithinGuitarFrequencyRange(pitch)) {
                            referenceFrequency = pitch;
                            lastDetectionTime = currentTime;
                            pitchBuffer.clear();
                        }
                    }

                    if (currentTime - lastDetectionTime <= DETECTION_PERIOD_MS) {
                        pitchBuffer.add(pitch);
                    } else {
                        double averagePitch = calculateAverage(pitchBuffer);
                        pitchListener.onPitchDetected(averagePitch);
                        Log.d("AudioRecord", "Detected pitch: " + averagePitch;

                        double[] chromagram = chordDetector.computeChromagram(buffer);

                        switch (currentpcp) {
                            case 0: pcp1 = chromagram; break;
                            case 1: pcp2 = chromagram; break;
                            case 2: pcp3 = chromagram; break;
                        }
                        currentpcp = (currentpcp + 1) % 3;
                        for (int i = 0; i < 12; i++) avgpcp[i] = (pcp1[i] + pcp2[i] + pcp3[i]) / 3f;

                        double loudness = calculateLoudness(chromagram);
                        final String guessedChord = chordDetector.detectChord(avgpcp);

                        pitchListener.onChordDetected(guessedChord);
                        Log.d("AudioRecord", "Detected chord: " + guessedChord);

                        referenceFrequency = -1;
                        pitchBuffer.clear();
                    }
                }
            }
        }

        audioRecord.stop();
        audioRecord.release();
    }

    private boolean isSignificantFrequencyChange(double pitch) {
        return Math.abs(pitch - referenceFrequency) > FREQUENCY_THRESHOLD;
    }

    private boolean isWithinGuitarFrequencyRange(double pitch) {
        for (double guitarFrequency : GUITAR_FREQUENCIES) {
            if (Math.abs(pitch - guitarFrequency) <= GUITAR_FREQUENCY_RANGE) {
                return true;
            }
        }
        return false;
    }

    private double calculateAverage(List<Double> pitches) {
        double sum = 0.0;
        for (double pitch : pitches) {
            sum += pitch;
        }
        return pitches.size() > 0 ? sum / pitches.size() : 0.0;
    }

    private double calculateLoudness(double[] magnitudes) {
        double loudness = 0;
        for (double magnitude : magnitudes) {
            loudness += magnitude;
        }
        return Math.abs(loudness / magnitudes.length);
    }

    public void startCapture() {
        isRunning = true;
        start();
    }

    public void stopCapture() {
        isRunning = false;
    }
}*/
