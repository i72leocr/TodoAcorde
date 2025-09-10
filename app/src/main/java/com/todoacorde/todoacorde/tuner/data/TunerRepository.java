package com.todoacorde.todoacorde.tuner.data;

import android.content.Context;

import com.todoacorde.todoacorde.tuner.domain.PitchDetectorCallback;

import javax.inject.Inject;

/**
 * Repositorio para la funcionalidad del afinador.
 * Gestiona el ciclo de vida de un {@link PitchDetector} y expone
 * métodos de alto nivel para iniciar y detener la detección de tono.
 *
 * El repositorio crea perezosamente una instancia de {@link PitchDetector}
 * y la reutiliza entre llamadas, evitando reconstrucciones innecesarias.
 */
public class TunerRepository {

    /**
     * Instancia única del detector de tono administrada por el repositorio.
     * Se inicializa al invocar {@link #startDetection(PitchDetectorCallback, Context)}.
     */
    private PitchDetector pitchDetector;

    /**
     * Constructor inyectable para Hilt/Dagger.
     * No requiere dependencias explícitas en este nivel.
     */
    @Inject
    public TunerRepository() {
    }

    /**
     * Inicia la detección de tono. Si el detector aún no existe,
     * se crea con el {@link PitchDetectorCallback} y el {@link Context} proporcionados.
     * En invocaciones posteriores, reutiliza el detector ya creado.
     *
     * @param callback receptor de eventos de tono detectado.
     * @param context  contexto de la aplicación o actividad, necesario para inicializar el detector.
     */
    public void startDetection(PitchDetectorCallback callback, Context context) {
        if (pitchDetector == null) {
            pitchDetector = new PitchDetector(callback, context);
        }
        pitchDetector.startDetection();
    }

    /**
     * Detiene la detección de tono si el detector está activo.
     * Libera los recursos asociados a la captura de audio.
     */
    public void stopDetection() {
        if (pitchDetector != null) {
            pitchDetector.stopDetection();
        }
    }
}
