package com.tuguitar.todoacorde.practice.domain;

import android.os.CountDownTimer;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;

import com.tuguitar.todoacorde.practice.data.LineItem;
import com.tuguitar.todoacorde.SongWithDetails;
import com.tuguitar.todoacorde.metronome.domain.MetronomeManager;
import com.tuguitar.todoacorde.practice.data.PracticeRepository;
import com.tuguitar.todoacorde.practice.data.SongUserSpeed;
import com.tuguitar.todoacorde.practice.data.SpanInfo;
import com.tuguitar.todoacorde.songs.data.Song;
import com.tuguitar.todoacorde.songs.data.SongChordWithInfo;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class PracticeViewModel extends ViewModel implements PracticeSessionManager.PracticeSessionCallbacks {
    private static final String TAG = "PracticeViewModel";
    private final MetronomeManager metronomeManager;
    private boolean metronomeEnabled = false;
    private static final int USER_ID = 1;
    // al lado de tus otros LiveData
    private final MediatorLiveData<Float> _scrollPercent = new MediatorLiveData<>();
    public LiveData<Float> scrollPercent = _scrollPercent;
    public enum Mode { FREE, SYNCHRONIZED }

    // LiveData for practice mode and speed factor
    private final MutableLiveData<Mode> _mode = new MutableLiveData<>(Mode.SYNCHRONIZED);
    public LiveData<Mode> mode = _mode;
    public void setMode(Mode m) { _mode.setValue(m); }

    private final MutableLiveData<Double> _speedFactor = new MutableLiveData<>(1.0);
    public LiveData<Double> speedFactor = _speedFactor;
    public void setSpeedFactor(double f) { _speedFactor.setValue(f); }

    // Dependencies (repository and use-case managers)
    private final PracticeRepository repo;
    private final SequenceManager sequenceManager;
    private final PracticeSessionManager practiceSessionManager;

    // LiveData for combining data
    public final LiveData<SongWithDetails> songDetails;
    private final LiveData<List<SongChordWithInfo>> sequenceWithInfo;

    // LiveData for UI state
    private final MutableLiveData<Boolean> _isRunning = new MutableLiveData<>(false);
    public LiveData<Boolean> isRunning = _isRunning;

    private final MutableLiveData<Integer> _currentIndex = new MutableLiveData<>(-1);
    public LiveData<Integer> currentIndex = _currentIndex;

    private final MutableLiveData<String> _expectedChord = new MutableLiveData<>("");
    public LiveData<String> observeExpectedChord() { return _expectedChord; }

    private final MutableLiveData<Set<Integer>> _correctIndices = new MutableLiveData<>(new HashSet<>());
    public LiveData<Set<Integer>> getCorrectIndices() { return _correctIndices; }

    private final MutableLiveData<Integer> _currentLineIndex = new MutableLiveData<>(0);
    public LiveData<Integer> getCurrentLineIndex() { return _currentLineIndex; }

    private final MutableLiveData<Integer> _progressPercent = new MutableLiveData<>(0);
    public LiveData<Integer> getProgressPercent() { return _progressPercent; }

    private final MutableLiveData<Event<Integer>> _scoreEvent = new MutableLiveData<>();
    public LiveData<Event<Integer>> scoreEvent = _scoreEvent;

    private final MutableLiveData<Event<String>> _unlockEvent = new MutableLiveData<>();
    public LiveData<Event<String>> unlockEvent = _unlockEvent;

    // Unlocked speeds tracking
    private final MediatorLiveData<SongUserSpeed> _unlockedSpeeds = new MediatorLiveData<>();
    public LiveData<SongUserSpeed> unlockedSpeeds = _unlockedSpeeds;
    private LiveData<SongUserSpeed> unlockedSpeedsSource;

    // Countdown timer state
    private final MutableLiveData<Integer> countdownSeconds = new MutableLiveData<>(0);
    private final MutableLiveData<Boolean> isCountingDown = new MutableLiveData<>(false);
    private final MutableLiveData<Event<Boolean>> countdownFinished = new MutableLiveData<>();
    private CountDownTimer countdownTimer;

    // Best/last score LiveData
    public final LiveData<Integer> bestScore;
    public final LiveData<Integer> lastScore;
    private final MediatorLiveData<Boolean> isActive = new MediatorLiveData<>();

    public LiveData<Boolean> isActive() {
        return isActive;
    }

    @Inject
    public PracticeViewModel(@NonNull PracticeRepository repo,
                             @NonNull SequenceManager sequenceManager,
                             @NonNull PracticeSessionManager practiceSessionManager, MetronomeManager metronomeManager) {
        this.repo = repo;
        this.sequenceManager = sequenceManager;
        this.practiceSessionManager = practiceSessionManager;
        this.metronomeManager = metronomeManager;

        // Attach this ViewModel as callback listener to the PracticeSessionManager
        practiceSessionManager.setCallbacks(this);

        // Prepare LiveData for song details and chords with info (sequence data)
        songDetails = Transformations.switchMap(_mode, m -> {
            // Ensure we re-fetch scores when mode (speed availability) might change if needed
            return Transformations.switchMap(_speedFactor, sp ->
                    Transformations.switchMap(_songId, id -> repo.getSongWithDetails(id))
            );
        });

        isActive.addSource(isRunning, value -> {
            Boolean countdown = isCountingDown.getValue();
            isActive.setValue(Boolean.TRUE.equals(value) || Boolean.TRUE.equals(countdown));
        });
        isActive.addSource(isCountingDown, value -> {
            Boolean running = isRunning.getValue();
            isActive.setValue(Boolean.TRUE.equals(value) || Boolean.TRUE.equals(running));
        });
        // (We will set _songId in initForSong; using a separate internal MutableLiveData for songId)
        sequenceWithInfo = Transformations.switchMap(_songId, id -> repo.getChordsWithInfoForSong(id));
        // ① Observamos cambios en currentLineIndex
        _scrollPercent.addSource(_currentLineIndex, line -> recalcScrollPercent(line, sequenceManager.getLineItems().getValue()));

        // ② Observamos también cuando cambie la lista de líneas
        _scrollPercent.addSource(sequenceManager.getLineItems(), list -> recalcScrollPercent(_currentLineIndex.getValue(), list));


        // Best and last score for current song and speed
        bestScore = Transformations.switchMap(_songId, songId ->
                Transformations.switchMap(_speedFactor, sp ->
                        repo.getBestScore(songId, USER_ID, sp.floatValue())
                ));
        lastScore = Transformations.switchMap(_songId, songId ->
                Transformations.switchMap(_speedFactor, sp ->
                        repo.getLastScore(songId, USER_ID, sp.floatValue())
                ));


    }

    // Internal MutableLiveData for selected song ID
    private final MutableLiveData<Integer> _songId = new MutableLiveData<>();


    private void recalcScrollPercent(Integer lineIndex, List<LineItem> lines) {
        if (lines == null || lines.size() <= 1 || lineIndex == null || lineIndex < 0) {
            _scrollPercent.setValue(0f);
        } else {
            float pct = lineIndex / (float)(lines.size() - 1);
            _scrollPercent.setValue(pct);
        }
    }


    public void setCurrentIndex(int index) {
        _currentIndex.setValue(index);
    }

    public LiveData<List<LineItem>> getLineItems() {
        return sequenceManager.getLineItems();
    }

    public LiveData<List<SongChordWithInfo>> getSequenceWithInfo() {
        return sequenceWithInfo;
    }
    // Nuevo: setter para la preferencia de metrónomo
    public void setMetronomeEnabled(boolean enabled) {
        metronomeEnabled = enabled;
    }

    /** Initialize the ViewModel for a given song. Should be called from the UI (Fragment) with the songId. */
    public void initForSong(int songId) {
        // Initialize data for the given song
        _songId.setValue(songId);
        sequenceManager.initForSong(songId);
        repo.ensureSpeedRecordExists(songId, USER_ID);

        // Manage unlocked speeds LiveData
        if (unlockedSpeedsSource != null) {
            _unlockedSpeeds.removeSource(unlockedSpeedsSource);
        }
        unlockedSpeedsSource = repo.getSongUserSpeed(songId, USER_ID);
        _unlockedSpeeds.addSource(unlockedSpeedsSource, speed -> {
            _unlockedSpeeds.setValue(speed);
            if (speed != null) {
                setSpeedFactor(speed.getMaxUnlockedSpeed());
            }
        });
    }

    /** Start the practice session (after any countdown). */
    public void startDetection() {
        // Si ya está corriendo, no hacer nada
        if (Boolean.TRUE.equals(_isRunning.getValue())) return;

        // Marcar como en ejecución y resetear estado de UI
        _isRunning.setValue(true);
        resetState();

        // Leer modo y lista de acordes
        Mode modeValue = _mode.getValue() != null ? _mode.getValue() : Mode.SYNCHRONIZED;
        List<SongChordWithInfo> chordInfoList = sequenceWithInfo.getValue();

        // Leer factor de velocidad (0.5, 0.75, 1.0, etc)
        double speed = _speedFactor.getValue() != null ? _speedFactor.getValue() : 1.0;

        // Iniciar la sesión de práctica
        practiceSessionManager.startSession(
                _songId.getValue(),
                modeValue,
                chordInfoList,
                speed
        );

        // Si el metrónomo está activo, iniciarlo a tempo ajustado
        if (metronomeEnabled) {
            SongWithDetails details = songDetails.getValue();
            if (details != null && details.song != null) {
                Song song = details.song;

                // BPM base de la canción
                int baseBpm = song.getBpm();

                // BPM ajustado según speedFactor
                int adjustedBpm = (int) Math.round(baseBpm * speed);

                // Calcular beats por compás (ej. "4/4" → 4)
                String measureStr = song.getMeasure();
                int beatsPerMeasure = 4;
                try {
                    if (measureStr != null && measureStr.contains("/")) {
                        beatsPerMeasure = Integer.parseInt(measureStr.split("/")[0]);
                    }
                } catch (NumberFormatException ignored) {
                    // usar 4 si hay error
                }

                // Arrancar metrónomo, acento en primer tiempo
                metronomeManager.start(
                        adjustedBpm,
                        beatsPerMeasure,
                        true,
                        null
                );
            }
        }
    }

    /** Stop the ongoing practice session. */
    public void stopDetection() {
        if (!Boolean.TRUE.equals(_isRunning.getValue())) return;
        Log.d(TAG, "Deteniendo práctica");
        _isRunning.setValue(false);
        practiceSessionManager.stopSession();
        metronomeManager.stop();
        resetState();
        _progressPercent.setValue(0);
    }

    /** Resets the UI-related state in preparation for a new practice run. */
    private void resetState() {
        // Reset current index and related UI indicators
        _currentIndex.setValue(-1);
        // Set expected chord to the first chord's name if available
        _expectedChord.setValue(""); // No mostrar acorde esperado inicialmente
        _currentLineIndex.setValue(0);
        _correctIndices.setValue(new HashSet<>());
        _progressPercent.setValue(0);
    }

    /** LiveData getters for countdown controls. */
    public LiveData<Integer> getCountdownSeconds() { return countdownSeconds; }
    public LiveData<Boolean> getIsCountingDown() { return isCountingDown; }
    public LiveData<Event<Boolean>> getCountdownFinished() { return countdownFinished; }

    /** Start a 3-second countdown before beginning the practice session. */
    public void startCountdown() {
        cancelCountdown();
        isCountingDown.setValue(true);
        countdownSeconds.setValue(3);
        countdownTimer = new CountDownTimer(3000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                int seconds = (int) Math.ceil(millisUntilFinished / 1000.0);
                countdownSeconds.setValue(seconds);
            }
            @Override
            public void onFinish() {
                isCountingDown.setValue(false);
                countdownSeconds.setValue(0);
                countdownFinished.setValue(new Event<>(true));
            }
        }.start();
    }

    /** Cancel the countdown (if any). */
    public void cancelCountdown() {
        if (countdownTimer != null) {
            countdownTimer.cancel();
            countdownTimer = null;
        }
        isCountingDown.setValue(false);
        countdownSeconds.setValue(0);
    }

    /** Callback from PracticeSessionManager when the current chord index changes. */
    @Override
    public void onChordIndexChanged(int index, @NonNull String nextChordName) {
        _currentIndex.postValue(index);
        _expectedChord.postValue(nextChordName);
        // Update which lyric line is currently active (for scrolling in UI)
        _currentLineIndex.postValue(calculateLineForGlobalIndex(index));
    }

    /** Callback from PracticeSessionManager when a chord is detected correctly. */
    @Override
    public void onChordDetectedCorrect(int index) {
        // Update the set of correctly played chord indices
        Set<Integer> newSet = new HashSet<>(_correctIndices.getValue() != null ? _correctIndices.getValue() : new HashSet<>());
        newSet.add(index);
        _correctIndices.postValue(newSet);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        // Detener metrónomo por seguridad y liberar los sonidos
        metronomeManager.stop();
        metronomeManager.releaseSound();
    }

    /** Finaliza completamente la práctica (sin mostrar puntaje). */
    public void endPractice() {
        Log.d(TAG, "Finalizando práctica por salida del usuario.");
        cancelCountdown();                          // Detiene countdown si está activo
        metronomeManager.stop();                    // Detiene metrónomo
        practiceSessionManager.stopSession();       // Detiene la sesión real
        sequenceManager.reset();                    // Limpia la secuencia en curso

        _isRunning.setValue(false);                 // Actualiza estado
        _progressPercent.setValue(0);
        _currentIndex.setValue(-1); // No hay acorde seleccionado inicialmente
        _expectedChord.setValue(""); // Quita cualquier nombre de acorde mostrado
        _correctIndices.setValue(new HashSet<>());
        _expectedChord.setValue("");
        _currentLineIndex.setValue(0);
    }



    /** Callback from PracticeSessionManager to update the progress percentage for synchronized mode. */
    @Override
    public void onProgressUpdated(int percent) {
        _progressPercent.postValue(percent);
    }

    /** Callback from PracticeSessionManager when the practice session has finished with a final score. */
    @Override
    public void onSessionFinished(int finalScore) {
        // Notify UI of the final score
        _scoreEvent.postValue(new Event<>(finalScore));
        // Stop session and reset running state
        practiceSessionManager.stopSession();
        metronomeManager.stop();            // Nuevo: detener metrónomo al finalizar sesión
        _isRunning.postValue(false);
        _progressPercent.postValue(0);
    }

    /** Callback from PracticeSessionManager when a new speed level is unlocked. */
    @Override
    public void onSpeedUnlocked(@NonNull String speedLabel) {
        // Notify UI that a new speed mode has been unlocked (one-time event)
        _unlockEvent.postValue(new Event<>(speedLabel));
    }

    /** Calculate which lyric line (index) corresponds to the given global chord index in the sequence. */
    private int calculateLineForGlobalIndex(int globalIdx) {
        List<LineItem> lines = sequenceManager.getLineItems().getValue();
        if (lines == null) return -1;
        for (int i = 0; i < lines.size(); i++) {
            for (SpanInfo span : lines.get(i).spans) {
                if (span.globalIndex == globalIdx) {
                    return i;
                }
            }
        }
        return -1;
    }

    /** Simple one-time consumable event wrapper. */
    public static class Event<T> {
        private final T content;
        private boolean handled = false;
        public Event(T content) { this.content = content; }
        public T getIfNotHandled() {
            if (handled) return null;
            handled = true;
            return content;
        }
    }
}
