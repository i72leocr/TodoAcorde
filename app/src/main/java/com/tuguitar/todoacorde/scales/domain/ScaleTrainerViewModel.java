package com.tuguitar.todoacorde.scales.domain;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.tuguitar.todoacorde.scales.data.ScaleUtils;

import java.util.ArrayList;
import java.util.List;

public class ScaleTrainerViewModel extends ViewModel {

    public enum State { IDLE, PLAYING, COMPLETED }

    private final MutableLiveData<List<String>> scaleNotes = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Integer> currentIndex = new MutableLiveData<>(0);
    private final MutableLiveData<Integer> correctCount = new MutableLiveData<>(0);
    private final MutableLiveData<Integer> streak = new MutableLiveData<>(0);
    private final MutableLiveData<Float> progressPercent = new MutableLiveData<>(0f);
    private final MutableLiveData<State> state = new MutableLiveData<>(State.IDLE);

    private final double CENTS_TOLERANCE = 50.0;

    public LiveData<List<String>> getScaleNotes() { return scaleNotes; }
    public LiveData<Integer> getCurrentIndex() { return currentIndex; }
    public LiveData<Integer> getCorrectCount() { return correctCount; }
    public LiveData<Integer> getStreak() { return streak; }
    public LiveData<Float> getProgressPercent() { return progressPercent; }
    public LiveData<State> getState() { return state; }

    public void startScale(int rootMidiNote, ScaleUtils.ScaleType type) {
        List<String> notes = ScaleUtils.buildScale(rootMidiNote, type);
        scaleNotes.setValue(notes);
        currentIndex.setValue(0);
        correctCount.setValue(0);
        streak.setValue(0);
        progressPercent.setValue(0f);
        state.setValue(State.PLAYING);
    }

    /**
     * Solo avanza cuando la nota es correcta. No penaliza por errores.
     * Puede llamarse desde hilos en background, por eso usa postValue.
     */
    public void onUserPlayedNote(String playedNoteName, double centsOff) {
        if (state.getValue() != State.PLAYING) return;

        List<String> notes = scaleNotes.getValue();
        Integer idxObj = currentIndex.getValue();
        int idx = idxObj != null ? idxObj : 0;
        if (notes == null || idx >= notes.size()) return;

        String expected = notes.get(idx);
        boolean match = expected.equalsIgnoreCase(playedNoteName) && Math.abs(centsOff) < CENTS_TOLERANCE;

        if (!match) {
            return; // solo feedback positivo: ignoramos errores
        }

        int nextIndex = idx + 1;

        int prevCorrect = correctCount.getValue() != null ? correctCount.getValue() : 0;
        correctCount.postValue(prevCorrect + 1);

        int prevStreak = streak.getValue() != null ? streak.getValue() : 0;
        streak.postValue(prevStreak + 1);

        currentIndex.postValue(nextIndex);

        int total = notes.size();
        float progress = total == 0 ? 0f : ((float) nextIndex / total) * 100f;
        progressPercent.postValue(progress);

        if (nextIndex >= total) {
            state.postValue(State.COMPLETED);
        }
    }

    public void reset() {
        scaleNotes.setValue(new ArrayList<>());
        currentIndex.setValue(0);
        correctCount.setValue(0);
        streak.setValue(0);
        progressPercent.setValue(0f);
        state.setValue(State.IDLE);
    }
}
