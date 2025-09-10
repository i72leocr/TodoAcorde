package com.todoacorde.todoacorde.tuner.domain;

/**
 * Interfaz de callback para recibir notificaciones de frecuencia detectada
 * por el {@link com.todoacorde.todoacorde.tuner.data.PitchDetector}.
 *
 * Permite a las capas superiores (por ejemplo, ViewModels o Fragments)
 * reaccionar en tiempo real a los tonos captados por el micrófono.
 */
public interface PitchDetectorCallback {

    /**
     * Método invocado cada vez que se detecta un tono válido en la señal de audio.
     *
     * @param frequency frecuencia detectada en Hertz (Hz).
     */
    void onPitchDetected(double frequency);
}
