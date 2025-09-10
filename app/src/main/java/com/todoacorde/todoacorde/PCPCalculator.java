package com.todoacorde.todoacorde;

/**
 * Utilidad para calcular el vector PCP (Pitch Class Profile) de un espectro.
 * El PCP agrega la energía espectral por clases de tono (12 clases por octava),
 * normalizando el resultado para facilitar comparaciones (p. ej. detección de acordes).
 */
public class PCPCalculator {

    /**
     * Calcula el PCP a partir de las magnitudes espectrales.
     *
     * @param sampleRate     frecuencia de muestreo en Hz.
     * @param referenceFreq  frecuencia de referencia (por ejemplo, C4 ≈ 261.6 Hz).
     * @param magnitudes     magnitudes del espectro (p. ej. |FFT|), de longitud N/2 típicamente.
     * @return vector PCP de 12 elementos normalizado (suma = 1).
     */
    public static float[] calculatePCP(int sampleRate, float referenceFreq, float[] magnitudes) {
        float[] pcp = new float[12];

        // Recorre el espectro y acumula energía en la clase de tono correspondiente.
        // Se limita el barrido a ≈5 kHz para evitar ruido en altas frecuencias.
        float frequency = 0f;
        for (int i = 0; i < magnitudes.length && frequency < 5000f; i++) {
            // Frecuencia aproximada del bin i (asumiendo espectro de N/2 bins hasta Nyquist).
            frequency = ((float) i / (float) magnitudes.length) * ((float) sampleRate / 2f);

            int pc = computePitchClass(i, sampleRate, referenceFreq, magnitudes.length);
            if (pc >= 0 && pc < 12) {
                // Energía proporcional al cuadrado de la magnitud.
                pcp[pc] += Math.pow(Math.abs(magnitudes[i]), 2);
            }
        }

        normalizePCP(pcp);
        return pcp;
    }

    /**
     * Determina la clase de tono (0–11) para un índice de bin espectral.
     *
     * @param index        índice del bin.
     * @param sampleRate   frecuencia de muestreo.
     * @param referenceFreq frecuencia de referencia para el mapeo logarítmico.
     * @param numSamples   número de bins del espectro de entrada.
     * @return clase de tono en [0,11] o -1 si no aplica (index 0).
     */
    private static int computePitchClass(int index, int sampleRate, float referenceFreq, int numSamples) {
        if (index == 0) return -1; // DC no aporta a una clase de tono
        // f_bin ≈ (sampleRate * (index/numSamples)) / 2  -> se integra en el ratio con referencia
        double ratio = ((double) sampleRate * ((double) index / (double) numSamples)) / (double) referenceFreq;
        int pc = (int) Math.round(12.0 * Math.log(ratio) / Math.log(2.0)) % 12;
        // En Java el % con negativos puede dar negativo; ajustamos a [0,11].
        return pc < 0 ? pc + 12 : pc;
    }

    /**
     * Normaliza el PCP para que la suma de sus componentes sea 1.
     *
     * @param pcp vector PCP de 12 elementos a normalizar.
     */
    private static void normalizePCP(float[] pcp) {
        float sum = 0f;
        for (float value : pcp) {
            sum += value;
        }
        if (sum == 0f) sum = 1f; // evita división por cero si no hubo energía acumulada
        for (int i = 0; i < 12; i++) {
            pcp[i] /= sum;
        }
    }
}
