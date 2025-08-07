package com.tuguitar.todoacorde.tuner.domain;

import android.content.Context;
import com.tuguitar.todoacorde.tuner.data.TunerRepository;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;

/** Domain layer: manages tuner logic (buffering, filtering, tuning calculation). */
public class TunerManager implements PitchDetectorCallback {
    private final TunerRepository repository;
    private final List<Double> pitchBuffer = new ArrayList<>();
    private String currentNote = "E2";
    private boolean isRunning = false;
    private TuningUpdateListener updateListener;

    private static final int BUFFER_SIZE = 16;
    private static final double TUNING_THRESHOLD = 0.5;
    private static final Map<String, double[]> NOTE_FREQUENCIES = new HashMap<>();
    static {
        NOTE_FREQUENCIES.put("E2", new double[]{80.10, 82.41, 84.86});
        NOTE_FREQUENCIES.put("A2", new double[]{106.88, 110.00, 113.28});
        NOTE_FREQUENCIES.put("D3", new double[]{142.71, 146.83, 151.43});
        NOTE_FREQUENCIES.put("G3", new double[]{191.67, 196.00, 202.83});
        NOTE_FREQUENCIES.put("B3", new double[]{240.24, 246.94, 254.35});
        NOTE_FREQUENCIES.put("E4", new double[]{319.35, 329.63, 339.43});
    }

    /** Listener for publishing tuning results to the presentation layer. */
    public interface TuningUpdateListener {
        void onTuningUpdate(TuningResult result);
    }

    @Inject
    public TunerManager(TunerRepository repository) {
        this.repository = repository;
    }

    public void start(Context context, TuningUpdateListener listener) {
        if (isRunning) return;
        updateListener = listener;
        pitchBuffer.clear();
        repository.startDetection(this, context);
        isRunning = true;
    }

    public void stop() {
        if (!isRunning) return;
        repository.stopDetection();
        isRunning = false;
    }

    public void setTargetNote(String note) {
        currentNote = note;
        pitchBuffer.clear();
    }

    @Override
    public void onPitchDetected(double frequency) {
        if (frequency <= 0) return;

        pitchBuffer.add(frequency);
        if (pitchBuffer.size() > BUFFER_SIZE) {
            pitchBuffer.remove(0);
        }

        if (pitchBuffer.size() == BUFFER_SIZE) {
            double filtered = calculateFilteredFrequency(pitchBuffer);
            TuningResult result;

            double[] range = NOTE_FREQUENCIES.get(currentNote);
            if (range != null) {
                double min = range[0], tgt = range[1], max = range[2];
                double clamped = Math.max(min, Math.min(max, filtered));

                int progress;
                if (clamped <= tgt) {
                    progress = (int) (((clamped - min) / (tgt - min)) * 50);
                } else {
                    progress = 50 + (int) (((clamped - tgt) / (max - tgt)) * 50);
                }
                progress = Math.max(0, Math.min(100, progress));

                boolean showPlus = false;
                boolean showMinus = false;
                String actionText = "";
                if (Math.abs(filtered - tgt) <= TUNING_THRESHOLD) {
                    // Está afinado, no se muestra nada adicional
                } else if (filtered < tgt) {
                    showPlus = true;
                    actionText = "DESTENSAR";
                } else {
                    showMinus = true;
                    actionText = "TENSAR";
                }

                float offset = (float) (filtered - tgt);
                result = new TuningResult(progress, showPlus, showMinus, actionText, offset);

            } else {
                // Valores por defecto seguros si la nota no está definida
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


    /** Median-based filtering to smooth out frequency noise. */
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
