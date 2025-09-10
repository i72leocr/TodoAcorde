package com.todoacorde.todoacorde.tuner.domain;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

/**
 * ViewModel para el afinador.
 *
 * Expone un {@link LiveData} con el último {@link TuningResult} calculado y
 * orquesta el ciclo de vida de la detección delegando en {@link TunerManager}.
 *
 * Inyección: gestionado por Hilt con alcance de ViewModel.
 */
@HiltViewModel
public class TunerViewModel extends ViewModel implements TunerManager.TuningUpdateListener {

    /** Coordinador de lógica de afinación (detección, filtrado y cálculo de progreso). */
    private final TunerManager tunerManager;

    /** Estado observable para la UI con el resultado más reciente de afinación. */
    private final MutableLiveData<TuningResult> tuningResult = new MutableLiveData<>();

    /**
     * Crea el ViewModel con el {@link TunerManager} inyectado.
     *
     * @param tunerManager gestor de afinación.
     */
    @Inject
    public TunerViewModel(TunerManager tunerManager) {
        this.tunerManager = tunerManager;
    }

    /**
     * Flujo observable de resultados de afinación.
     *
     * @return LiveData que emite instancias de {@link TuningResult}.
     */
    public LiveData<TuningResult> getTuningResult() {
        return tuningResult;
    }

    /**
     * Inicia la detección de tono y suscribe este ViewModel como receptor de actualizaciones.
     *
     * @param context contexto requerido para inicializar la capa de audio.
     */
    public void startTuning(Context context) {
        tunerManager.start(context, this);
    }

    /**
     * Detiene la detección de tono.
     */
    public void stopTuning() {
        tunerManager.stop();
    }

    /**
     * Define la nota/cuerda objetivo para el cálculo de progreso.
     *
     * @param note etiqueta de la nota objetivo (por ejemplo "E2", "A2").
     */
    public void setTargetNote(String note) {
        tunerManager.setTargetNote(note);
    }

    /**
     * Callback de {@link TunerManager} con un nuevo resultado de afinación.
     * Publica el valor en el {@link LiveData} observado por la UI.
     *
     * @param result resultado de afinación calculado.
     */
    @Override
    public void onTuningUpdate(TuningResult result) {
        tuningResult.postValue(result);
    }
}
