package com.tuguitar.todoacorde.metronome.domain;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import dagger.hilt.android.lifecycle.HiltViewModel;

import javax.inject.Inject;

/**
 * # MetronomeViewModel (Capa de Presentación)
 *
 * Coordina la lógica de presentación del metrónomo para la UI (Fragment).
 * Expone estado observable mediante LiveData y orquesta las llamadas al
 * {@link MetronomeManager} (capa de dominio).
 *
 * ## Responsabilidades
 * - Mantener el estado de configuración (BPM, compás, acento).
 * - Exponer cambios de estado a la UI: ejecución, beat actual y compás.
 * - Aplicar reglas de negocio de la vista:
 *   - **No permitir cambiar el compás mientras el metrónomo está en marcha.**
 *   - Restringir BPM a [20, 218] y compás a [1, 12].
 *
 * ## Flujo de datos
 * - La UI observa `isRunning`, `currentBeat` y `beatsPerMeasure`.
 * - Al iniciar, la VM delega en el Manager y resetea `currentBeat`.
 * - En cada tick, el Manager notifica y la VM publica el beat actual.
 *
 * ## Notas
 * - `onCleared()` libera recursos de audio (SoundPool) del repositorio de datos.
 */
@HiltViewModel
public class MetronomeViewModel extends ViewModel implements MetronomeManager.TickListener {
    private final MetronomeManager metronomeManager;
    private final MutableLiveData<Boolean> isRunning = new MutableLiveData<>(false);
    private final MutableLiveData<Integer> currentBeat = new MutableLiveData<>(0);
    private final MutableLiveData<Integer> beatsPerMeasureLive = new MutableLiveData<>(4);
    private int bpm = 100;
    private int beatsPerMeasure = 4;
    private boolean accentFirst = true;
    @Inject
    public MetronomeViewModel(@NonNull MetronomeManager metronomeManager) {
        this.metronomeManager = metronomeManager;
        this.metronomeManager.setBeatsPerMeasure(beatsPerMeasure);
    }

    /** Estado de ejecución del metrónomo. */
    public LiveData<Boolean> isRunning() {
        return isRunning;
    }

    /** Índice del beat actual dentro del compás (base 0). */
    public LiveData<Integer> getCurrentBeat() {
        return currentBeat;
    }

    /** Número de tiempos por compás actual. */
    public LiveData<Integer> getBeatsPerMeasure() {
        return beatsPerMeasureLive;
    }

    /**
     * Establece el BPM. Si el metrónomo está en marcha, el cambio se aplica
     * inmediatamente reprogramando el intervalo de ticks en el Manager.
     *
     * @param bpm nuevo BPM, se clamp a [MetronomeManager.MIN_BPM, MetronomeManager.MAX_BPM]
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
     * Establece el número de tiempos por compás.
     * <p>
     * **Regla:** Si está sonando, se ignora la petición (bloqueo de compás en marcha).
     * La UI debe deshabilitar el control de compás observando `isRunning()`.
     *
     * @param beats nuevo compás (1..12).
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
     * Activa/desactiva el acento en el primer tiempo del compás.
     * Puede aplicarse en caliente.
     */
    @MainThread
    public void setAccentFirst(boolean accentFirst) {
        if (this.accentFirst == accentFirst) return;
        this.accentFirst = accentFirst;
        metronomeManager.setAccentFirst(accentFirst);
    }

    /** Devuelve el valor actual del flag de acento. */
    public boolean isAccentFirst() {
        return accentFirst;
    }

    /**
     * Inicia el metrónomo con la configuración actual.
     * Publica estado y resetea índice de beat en la UI.
     */
    @MainThread
    public void startMetronome() {
        if (Boolean.TRUE.equals(isRunning.getValue())) return;

        metronomeManager.start(
                bpm,
                beatsPerMeasure,
                accentFirst,
                this // TickListener
        );

        isRunning.setValue(true);
        currentBeat.setValue(0); // Primer tiempo visible a la UI
    }

    /**
     * Detiene el metrónomo y notifica a la UI.
     */
    @MainThread
    public void stopMetronome() {
        if (Boolean.FALSE.equals(isRunning.getValue())) return;

        metronomeManager.stop();
        isRunning.setValue(false);
        currentBeat.setValue(0);
    }

    /**
     * Notificación de beat desde el motor de dominio.
     *
     * @param beatIndex        índice de beat (0..beatsPerMeasure-1)
     * @param beatsPerMeasure  compás activo que reporta el Manager (para coherencia)
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
     * Libera recursos de audio cuando el ViewModel es destruido.
     * Evita fugas y mantiene estable el subsistema de sonido.
     */
    @Override
    protected void onCleared() {
        super.onCleared();
        metronomeManager.releaseSound();
    }

    /** Clamp genérico para enteros. */
    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
