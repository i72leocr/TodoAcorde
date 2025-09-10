package com.todoacorde.todoacorde;

/**
 * Listener para recibir notificaciones de acordes detectados.
 * Implementa esta interfaz para reaccionar cuando el detector
 * identifique un acorde a partir de la señal de audio de entrada.
 */
public interface ChordDetectionListener {
    /**
     * Callback invocado cuando se detecta un acorde.
     *
     * @param detectedChord nombre del acorde detectado. Puede ser un valor
     *                      como "C", "Gmaj7", "Am", o "No Chord" si no se
     *                      alcanza umbral suficiente de detección.
     */
    void onChordDetected(String detectedChord);
}
