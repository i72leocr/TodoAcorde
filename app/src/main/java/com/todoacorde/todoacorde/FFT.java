package com.todoacorde.todoacorde;

/**
 * Envoltorio Java para la ejecución de la Transformada Rápida de Fourier (FFT)
 * mediante código nativo cargado con JNI.
 *
 * <p>La librería nativa {@code fft_processing} debe estar disponible en el
 * proyecto (por ejemplo, a través de NDK). El método expuesto permite
 * transformar una señal en dominio temporal a sus componentes en
 * dominio frecuencial.</p>
 */
public class FFT {

    /** Carga la librería nativa que contiene la implementación de la FFT. */
    static {
        System.loadLibrary("fft_processing");
    }

    /**
     * Ejecuta la FFT sobre una señal de entrada y devuelve las componentes
     * en las partes real e imaginaria.
     *
     * @param signal vector de entrada con la señal en dominio temporal.
     * @param real   vector de salida para las componentes reales.
     * @param imag   vector de salida para las componentes imaginarias.
     */
    public static native void performFFT(float[] signal, float[] real, float[] imag);
}
