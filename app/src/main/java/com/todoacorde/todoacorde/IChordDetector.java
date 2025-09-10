package com.todoacorde.todoacorde;

import java.util.List;

/**
 * Contrato para un detector de acordes basado en audio.
 * Implementaciones típicas:
 * - Cargar perfiles de acordes (vectores/prototipos) contra los que comparar.
 * - Iniciar la captura y análisis del micrófono, notificando resultados al listener.
 * - Detener la detección y liberar recursos de audio subyacentes.
 */
public interface IChordDetector {

    /**
     * Establece el conjunto de perfiles de acordes a utilizar en la detección.
     * Cada perfil suele representar la huella espectral/PCP esperada de un acorde.
     *
     * @param chordProfiles lista de perfiles; si es null se tratará como lista vacía.
     */
    void setChordProfiles(List<Chord> chordProfiles);

    /**
     * Comienza la detección a partir del audio de entrada y notifica los acordes detectados
     * mediante el {@link ChordDetectionListener}. Debe ser idempotente ante llamadas repetidas.
     *
     * @param listener receptor de eventos de detección (no debe ser null).
     */
    void startDetection(ChordDetectionListener listener);

    /**
     * Detiene la detección en curso y libera cualquier recurso asociado
     * (hebras, {@code AudioRecord}, buffers, etc.).
     * Debe ser segura ante múltiples invocaciones.
     */
    void stopDetection();
}
