package com.tuguitar.todoacorde.metronome.domain;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import dagger.hilt.android.lifecycle.HiltViewModel;
import javax.inject.Inject;

/** Presentation layer ViewModel for MetronomeFragment. */
@HiltViewModel
public class MetronomeViewModel extends ViewModel implements MetronomeManager.TickListener {
    private final MetronomeManager metronomeManager;
    private final MutableLiveData<Integer> currentBeat = new MutableLiveData<>();
    private int bpm = 100;
    private int beatsPerMeasure = 4;
    private boolean accentFirst = true;
    private boolean isRunning = false;

    @Inject
    public MetronomeViewModel(MetronomeManager metronomeManager) {
        this.metronomeManager = metronomeManager;
    }

    public void setBpm(int bpm) {
        this.bpm = bpm;
    }

    public void setBeatsPerMeasure(int beatsPerMeasure) {
        this.beatsPerMeasure = beatsPerMeasure;
    }

    public void setAccentFirst(boolean accentFirst) {
        this.accentFirst = accentFirst;
    }

    public boolean isMetronomeRunning() {
        return isRunning;
    }

    public LiveData<Integer> getCurrentBeat() {
        return currentBeat;
    }

    public boolean isAccentFirst() {
        return accentFirst;
    }

    public void startMetronome() {
        if (isRunning) return;
        metronomeManager.start(bpm, beatsPerMeasure, accentFirst, this);
        isRunning = true;
    }

    public void stopMetronome() {
        metronomeManager.stop();
        isRunning = false;
    }

    @Override
    public void onBeat(int beatIndex) {
        // Called on the main thread by MetronomeManager
        currentBeat.setValue(beatIndex);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        // Release SoundPool resources when ViewModel is destroyed
        metronomeManager.releaseSound();
    }
}
