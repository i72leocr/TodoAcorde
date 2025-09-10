package com.todoacorde.todoacorde.tuner.domain;

import android.content.Context;

import com.todoacorde.todoacorde.tuner.data.TunerRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

/**
 * Gestor de afinación que coordina la captura de tono y transforma
 * las frecuencias detectadas en información útil para la UI.
 *
 * Responsabilidades:
 * - Iniciar y detener la detección de tono a través del {@link TunerRepository}.
 * - Filtrar/estabilizar la frecuencia recibida del micro.
 * - Convertir frecuencia → progreso/orientación (tensar/destensar) según la cuerda objetivo.
 * - Notificar resultados a la UI mediante {@link TuningUpdateListener}.
 */
public class TunerManager implements PitchDetectorCallback {

    /** Repositorio que encapsula el acceso al detector de tono (audio/micrófono). */
    private final TunerRepository repository;

    /** Buffer circular de últimas frecuencias detectadas para filtrado robusto. */
    private final List<Double> pitchBuffer = new ArrayList<>();

    /** Nota objetivo actual (por defecto, E2). */
    private String currentNote = "E2";

    /** Indica si la captura/detección está activa. */
    private boolean isRunning = false;

    /** Callback hacia la UI con el resultado de afinación. */
    private TuningUpdateListener updateListener;

    /** Tamaño del buffer de estabilización (número de muestras). */
    private static final int BUFFER_SIZE = 16;

    /** Umbral de afinación en Hz para considerar "afinado". */
    private static final double TUNING_THRESHOLD = 0.5;

    /**
     * Rango aceptable por cuerda: [min, target, max] en Hz.
     * Se usa para calcular progreso e indicaciones de tensar/destensar.
     */
    private static final Map<String, double[]> NOTE_FREQUENCIES = new HashMap<>();

    static {
        NOTE_FREQUENCIES.put("E2", new double[]{80.10, 82.41, 84.86});
        NOTE_FREQUENCIES.put("A2", new double[]{106.88, 110.00, 113.28});
        NOTE_FREQUENCIES.put("D3", new double[]{142.71, 146.83, 151.43});
        NOTE_FREQUENCIES.put("G3", new double[]{191.67, 196.00, 202.83});
        NOTE_FREQUENCIES.put("B3", new double[]{240.24, 246.94, 254.35});
        NOTE_FREQUENCIES.put("E4", new double[]{319.35, 329.63, 339.43});
    }

    /**
     * Listener de actualizaciones de afinación para la UI.
     */
    public interface TuningUpdateListener {
        /**
         * Notificación con el estado de afinación calculado tras filtrar.
         *
         * @param result estructura con progreso, indicaciones y desfase.
         */
        void onTuningUpdate(TuningResult result);
    }

    /**
     * Inyección del repositorio de afinación.
     *
     * @param repository fuente de detección de tono.
     */
    @Inject
    public TunerManager(TunerRepository repository) {
        this.repository = repository;
    }

    /**
     * Inicia la detección de tono y comienza a emitir actualizaciones.
     *
     * @param context contexto de Android requerido por la capa de audio.
     * @param listener receptor de resultados de afinación.
     */
    public void start(Context context, TuningUpdateListener listener) {
        if (isRunning) return;
        updateListener = listener;
        pitchBuffer.clear();
        repository.startDetection(this, context);
        isRunning = true;
    }

    /**
     * Detiene la detección de tono y limpia estados transitorios.
     */
    public void stop() {
        if (!isRunning) return;
        repository.stopDetection();
        isRunning = false;
    }

    /**
     * Establece la cuerda/nota objetivo para el cálculo de progreso.
     * Reinicia el buffer de estabilización.
     *
     * @param note etiqueta de nota esperada (p. ej. "E2", "A2").
     */
    public void setTargetNote(String note) {
        currentNote = note;
        pitchBuffer.clear();
    }

    /**
     * Callback desde la capa de audio con una frecuencia detectada.
     * Aplica:
     * - Acumulación en buffer
     * - Filtrado robusto
     * - Cálculo de progreso (0–100) y orientación de ajuste
     * - Emisión de {@link TuningResult} a la UI
     *
     * @param frequency frecuencia en Hz detectada en el último frame.
     */
    @Override
    public void onPitchDetected(double frequency) {
        if (frequency <= 0) return;

        // Acumula y mantiene tamaño fijo del buffer
        pitchBuffer.add(frequency);
        if (pitchBuffer.size() > BUFFER_SIZE) {
            pitchBuffer.remove(0);
        }

        // Solo procesa cuando el buffer está lleno (estabilización)
        if (pitchBuffer.size() == BUFFER_SIZE) {
            double filtered = calculateFilteredFrequency(pitchBuffer);
            TuningResult result;

            double[] range = NOTE_FREQUENCIES.get(currentNote);
            if (range != null) {
                double min = range[0], tgt = range[1], max = range[2];

                // Progreso 0–100 centrado en la frecuencia objetivo
                double clamped = Math.max(min, Math.min(max, filtered));
                int progress;
                if (clamped <= tgt) {
                    progress = (int) (((clamped - min) / (tgt - min)) * 50);
                } else {
                    progress = 50 + (int) (((clamped - tgt) / (max - tgt)) * 50);
                }
                progress = Math.max(0, Math.min(100, progress));

                // Indicaciones de acción: tensar/destensar o afinado
                boolean showPlus = false;   // indicador visual “+”
                boolean showMinus = false;  // indicador visual “-”
                String actionText = "";
                if (Math.abs(filtered - tgt) <= TUNING_THRESHOLD) {
                    // Dentro del umbral: ya afinado
                    actionText = "";
                } else if (filtered < tgt) {
                    // Frecuencia por debajo del objetivo: destensar (subir tono)
                    showPlus = true;
                    actionText = "DESTENSAR";
                } else {
                    // Frecuencia por encima del objetivo: tensar (bajar tono)
                    showMinus = true;
                    actionText = "TENSAR";
                }

                float offset = (float) (filtered - tgt);
                result = new TuningResult(progress, showPlus, showMinus, actionText, offset);

            } else {
                // Nota no configurada en la tabla: valor por defecto
                int defaultProgress = 50;
                boolean showPlus = false;
                boolean showMinus = false;
                String actionText = "AJUSTAR";
                float offset = 0f;

                result = new TuningResult(defaultProgress, showPlus, showMinus, actionText, offset);
            }

            if (updateListener != null) {
                updateListener.onTuningUpdate(result);
            }
        }
    }

    /**
     * Filtrado robusto de frecuencia:
     * - Ordena y toma la mediana
     * - Elimina valores alejados de la mediana (> 2 Hz)
     * - Devuelve promedio de los valores restantes (o mediana si hay pocos)
     *
     * @param buffer lista inmutable de frecuencias recientes.
     * @return frecuencia filtrada y estable.
     */
    private double calculateFilteredFrequency(List<Double> buffer) {
        List<Double> tmp = new ArrayList<>(buffer);
        Collections.sort(tmp);
        double median = tmp.get(tmp.size() / 2);
        tmp.removeIf(f -> Math.abs(f - median) > 2.0);
        return tmp.size() < 3
                ? median
                : tmp.stream().mapToDouble(Double::doubleValue).average().orElse(median);
    }
}
