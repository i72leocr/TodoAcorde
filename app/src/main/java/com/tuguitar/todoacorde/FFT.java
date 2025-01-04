package com.tuguitar.todoacorde;

public class FFT {
    static {
        System.loadLibrary("fft_processing");
    }
    public static native void performFFT(float[] signal, float[] real, float[] imag);
}
