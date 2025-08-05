package com.tuguitar.todoacorde;

import java.util.List;

/**
 * Interfaz pura para detección de acordes: API independiente de Android y de cualquier capa superior.
 */
public interface IChordDetector {
    /**
     * Configura los perfiles de acordes sobre los que se detectará.
     */
    void setChordProfiles(List<Chord> chordProfiles);

    /**
     * Inicia la detección continua de acordes.
     * @param listener callback que recibe el nombre del acorde detectado.
     */
    void startDetection(ChordDetectionListener listener);

    /**
     * Detiene la detección y libera recursos.
     */
    void stopDetection();
}
