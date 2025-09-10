package com.todoacorde.todoacorde.metronome.domain;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

/**
 * ViewModel que orquesta la lógica del metrónomo para la capa de UI.
 *
 * Expone estado observable (ejecución, beat actual, compás) y delega la
 * temporización y reproducción de audio en {@link MetronomeManager}.
 * Permite ajustar BPM, compás y acento inicial respetando las restricciones
 * de cambio en caliente definidas por el gestor.
 */
@HiltViewModel
public class MetronomeViewModel extends ViewModel implements MetronomeManager.TickListener {

    /** Gestor de temporización y reproducción del metrónomo. */
    private final MetronomeManager metronomeManager;

    /** Indica si el metrónomo está corriendo. */
    private final MutableLiveData<Boolean> isRunning = new MutableLiveData<>(false);
    /** Índice del beat actual dentro del compás (comienza en 0). */
    private final MutableLiveData<Integer> currentBeat = new MutableLiveData<>(0);
    /** Beats por compás observable por la UI. */
    private final MutableLiveData<Integer> beatsPerMeasureLive = new MutableLiveData<>(4);

    /** BPM configurado. */
    private int bpm = 100;
    /** beats por compás configurado. */
    private int beatsPerMeasure = 4;
    /** Si se acentúa el primer beat del compás. */
    private boolean accentFirst = true;

    /**
     * Crea el ViewModel inyectando el gestor y sincroniza el compás inicial.
     *
     * @param metronomeManager gestor del metrónomo.
     */
    @Inject
    public MetronomeViewModel(@NonNull MetronomeManager metronomeManager) {
        this.metronomeManager = metronomeManager;
        this.metronomeManager.setBeatsPerMeasure(beatsPerMeasure);
    }

    /** @return estado observable de ejecución del metrónomo. */
    public LiveData<Boolean> isRunning() {
        return isRunning;
    }

    /** @return beat actual observable. */
    public LiveData<Integer> getCurrentBeat() {
        return currentBeat;
    }

    /** @return beats por compás observable. */
    public LiveData<Integer> getBeatsPerMeasure() {
        return beatsPerMeasureLive;
    }

    /**
     * Ajusta el BPM. Si el metrónomo está corriendo, se actualiza en caliente.
     *
     * @param bpm nuevo valor de BPM.
     */
    @MainThread
    public void setBpm(int bpm) {
        int clamped = clamp(bpm, MetronomeManager.MIN_BPM, MetronomeManager.MAX_BPM);
        if (this.bpm == clamped) return;
        this.bpm = clamped;
        if (Boolean.TRUE.equals(isRunning.getValue())) {
            metronomeManager.setBpm(this.bpm);
        }
    }

    /**
     * Ajusta los beats por compás. Solo se permite si el metrónomo está parado.
     * Reinicia el beat actual a 0 y sincroniza con el gestor.
     *
     * @param beats nuevo número de beats por compás.
     */
    @MainThread
    public void setBeatsPerMeasure(int beats) {
        if (Boolean.TRUE.equals(isRunning.getValue())) {
            return;
        }
        int clamped = clamp(beats, 1, MetronomeManager.MAX_BEATS_PER_MEASURE);
        if (this.beatsPerMeasure == clamped) return;

        this.beatsPerMeasure = clamped;
        this.beatsPerMeasureLive.setValue(clamped);
        metronomeManager.setBeatsPerMeasure(clamped);
        currentBeat.setValue(0);
    }

    /**
     * Activa o desactiva el acento en el primer beat del compás.
     *
     * @param accentFirst true para acentuar el primer beat; false en caso contrario.
     */
    @MainThread
    public void setAccentFirst(boolean accentFirst) {
        if (this.accentFirst == accentFirst) return;
        this.accentFirst = accentFirst;
        metronomeManager.setAccentFirst(accentFirst);
    }

    /** @return true si el primer beat del compás se acentúa. */
    public boolean isAccentFirst() {
        return accentFirst;
    }

    /**
     * Inicia el metrónomo con los parámetros actuales.
     * Publica el estado de ejecución y reinicia el beat observable.
     */
    @MainThread
    public void startMetronome() {
        if (Boolean.TRUE.equals(isRunning.getValue())) return;

        metronomeManager.start(
                bpm,
                beatsPerMeasure,
                accentFirst,
                this
        );

        isRunning.setValue(true);
        currentBeat.setValue(0);
    }

    /**
     * Detiene el metrónomo y reinicia el beat observable.
     */
    @MainThread
    public void stopMetronome() {
        if (Boolean.FALSE.equals(isRunning.getValue())) return;

        metronomeManager.stop();
        isRunning.setValue(false);
        currentBeat.setValue(0);
    }

    /**
     * Callback del gestor llamado en cada beat. Actualiza los LiveData
     * en el hilo correspondiente mediante {@code postValue}.
     *
     * @param beatIndex       índice del beat actual.
     * @param beatsPerMeasure compás vigente reportado por el gestor.
     */
    @Override
    public void onBeat(int beatIndex, int beatsPerMeasure) {
        currentBeat.postValue(beatIndex);
        Integer current = beatsPerMeasureLive.getValue();
        if (current == null || current != beatsPerMeasure) {
            beatsPerMeasureLive.postValue(beatsPerMeasure);
        }
    }

    /**
     * Libera recursos de audio al limpiar el ViewModel.
     */
    @Override
    protected void onCleared() {
        super.onCleared();
        metronomeManager.releaseSound();
    }

    /**
     * Restringe un valor a un rango [min, max].
     *
     * @param value valor a limitar.
     * @param min   mínimo permitido.
     * @param max   máximo permitido.
     * @return valor dentro del rango.
     */
    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
