package com.tuguitar.todoacorde.metronome.domain;

import android.os.Handler;
import android.os.Looper;
import com.tuguitar.todoacorde.metronome.data.MetronomeSoundRepository;
import javax.inject.Inject;

/** Domain layer: controls metronome timing and tick events. */
public class MetronomeManager {
    private final MetronomeSoundRepository soundRepository;
    private final Handler handler;
    private Runnable beatRunnable;
    private boolean isRunning = false;
    private int currentBeat = 0;
    private int beatsPerMeasure;
    private boolean accentFirst;
    private double intervalMs;
    private TickListener tickListener;

    public interface TickListener {
        void onBeat(int beatIndex);
    }

    @Inject
    public MetronomeManager(MetronomeSoundRepository soundRepository) {
        this.soundRepository = soundRepository;
        this.handler = new Handler(Looper.getMainLooper());
    }

    public void start(int bpm, int beatsPerMeasure, boolean accentFirst, TickListener listener) {
        if (isRunning) return;
        isRunning = true;
        this.beatsPerMeasure = beatsPerMeasure;
        this.accentFirst = accentFirst;
        this.currentBeat = 0;
        this.intervalMs = 60000.0 / bpm;
        this.tickListener = listener;
        // Define the metronome ticking task
        beatRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isRunning) return;
                // Play tick sound (accented on first beat if applicable)
                boolean accentTick = accentFirst && currentBeat == 0;
                soundRepository.playTick(accentTick);
                // Notify listener (ViewModel) to update UI for current beat
                if (tickListener != null) {
                    tickListener.onBeat(currentBeat);
                }
                // Advance to next beat
                currentBeat = (currentBeat + 1) % MetronomeManager.this.beatsPerMeasure;
                // Schedule the next tick
                handler.postDelayed(this, (long) intervalMs);
            }
        };
        // Start ticking immediately
        handler.post(beatRunnable);
    }

    public void stop() {
        if (!isRunning) return;
        isRunning = false;
        // Remove any pending tick callbacks
        handler.removeCallbacks(beatRunnable);
        currentBeat = 0;
    }

    public void releaseSound() {
        soundRepository.release();
    }
}
