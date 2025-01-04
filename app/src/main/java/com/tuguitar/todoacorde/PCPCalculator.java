package com.tuguitar.todoacorde;

public class PCPCalculator {

    public static float[] calculatePCP(int sampleRate, float referenceFreq, float[] magnitudes) {
        float[] pcp = new float[12];
        int pitchClass = -1;
        float frequency = 0;

        for (int i = 0; i < magnitudes.length && frequency < 5000; i++) {
            frequency = ((float) i / (float) magnitudes.length) * ((float) sampleRate / 2f);

            pitchClass = computePitchClass(i, sampleRate, referenceFreq, magnitudes.length);
            if (pitchClass >= 0 && pitchClass < 12) {
                pcp[pitchClass] += Math.pow(Math.abs(magnitudes[i]), 2);
            }
        }

        normalizePCP(pcp);
        return pcp;
    }

    private static int computePitchClass(int index, int sampleRate, float referenceFreq, int numSamples) {
        if (index == 0) return -1;
        double ratio = ((double) sampleRate * ((double) index / (double) numSamples)) / (double) referenceFreq;
        return (int) Math.round(12f * Math.log(ratio) / Math.log(2)) % 12;
    }

    private static void normalizePCP(float[] pcp) {
        double magnitude = 0;
        for (float value : pcp) {
            magnitude += value * value;
        }
        magnitude = Math.sqrt(magnitude);
        for (int i = 0; i < 12; i++) {
            pcp[i] /= magnitude;
        }
    }
}