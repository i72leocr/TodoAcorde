package com.todoacorde.todoacorde.practice.domain;

import android.os.CountDownTimer;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;

import com.todoacorde.todoacorde.SongWithDetails;
import com.todoacorde.todoacorde.metronome.domain.MetronomeManager;
import com.todoacorde.todoacorde.practice.data.LineItem;
import com.todoacorde.todoacorde.practice.data.PracticeRepository;
import com.todoacorde.todoacorde.practice.data.SongUserSpeed;
import com.todoacorde.todoacorde.practice.data.SpanInfo;
import com.todoacorde.todoacorde.songs.data.Song;
import com.todoacorde.todoacorde.songs.data.SongChordWithInfo;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

/**
 * ViewModel que coordina la práctica de una canción:
 * - Orquesta la sesión con {@link PracticeSessionManager}.
 * - Gestiona modo, velocidad y estado del metrónomo.
 * - Expone estado observable (índice, progreso, eventos de puntuación y desbloqueos).
 * - Calcula ayudas de UI como el porcentaje de scroll y la línea activa.
 */
@HiltViewModel
public class PracticeViewModel extends ViewModel implements PracticeSessionManager.PracticeSessionCallbacks {

    private static final String TAG = "PracticeViewModel";

    /** Manejador del metrónomo para sincronización opcional. */
    private final MetronomeManager metronomeManager;

    /** Si el metrónomo está habilitado durante la práctica. */
    private boolean metronomeEnabled = false;

    /** Id de usuario en ejecución (placeholder). */
    private static final int USER_ID = 1;

    /** Porcentaje de scroll sugerido en la UI [0..1]. */
    private final MediatorLiveData<Float> _scrollPercent = new MediatorLiveData<>();
    public LiveData<Float> scrollPercent = _scrollPercent;

    /** Modos de práctica soportados. */
    public enum Mode {FREE, SYNCHRONIZED}

    /** Modo de práctica actual. */
    private final MutableLiveData<Mode> _mode = new MutableLiveData<>(Mode.SYNCHRONIZED);
    public LiveData<Mode> mode = _mode;

    /** Cambia el modo de práctica. */
    public void setMode(Mode m) {
        _mode.setValue(m);
    }

    /** Factor de velocidad actual (0.5, 0.75, 1.0...). */
    private final MutableLiveData<Double> _speedFactor = new MutableLiveData<>(1.0);
    public LiveData<Double> speedFactor = _speedFactor;

    /** Establece el factor de velocidad. */
    public void setSpeedFactor(double f) {
        _speedFactor.setValue(f);
    }

    /** Repositorio de datos de práctica. */
    private final PracticeRepository repo;

    /** Gestor de secuencias (líneas, spans, etc.). */
    private final SequenceManager sequenceManager;

    /** Gestor de sesiones: detección, progreso y persistencia. */
    private final PracticeSessionManager practiceSessionManager;

    /** Detalles de la canción activa. */
    public final LiveData<SongWithDetails> songDetails;

    /** Secuencia de acordes con metadatos para la canción activa. */
    private final LiveData<List<SongChordWithInfo>> sequenceWithInfo;

    /** Flag observable: sesión en ejecución. */
    private final MutableLiveData<Boolean> _isRunning = new MutableLiveData<>(false);
    public LiveData<Boolean> isRunning = _isRunning;

    /** Índice global del acorde esperado. */
    private final MutableLiveData<Integer> _currentIndex = new MutableLiveData<>(-1);
    public LiveData<Integer> currentIndex = _currentIndex;

    /** Nombre del acorde esperado actualmente. */
    private final MutableLiveData<String> _expectedChord = new MutableLiveData<>("");

    /** Observa el acorde esperado. */
    public LiveData<String> observeExpectedChord() {
        return _expectedChord;
    }

    /** Conjunto de índices acertados hasta el momento. */
    private final MutableLiveData<Set<Integer>> _correctIndices = new MutableLiveData<>(new HashSet<>());

    /** Observa los índices acertados. */
    public LiveData<Set<Integer>> getCorrectIndices() {
        return _correctIndices;
    }

    /** Índice de línea visual asociada al índice global actual. */
    private final MutableLiveData<Integer> _currentLineIndex = new MutableLiveData<>(0);

    /** Observa la línea actual. */
    public LiveData<Integer> getCurrentLineIndex() {
        return _currentLineIndex;
    }

    /** Progreso de la ventana de detección (0..100). */
    private final MutableLiveData<Integer> _progressPercent = new MutableLiveData<>(0);

    /** Observa el progreso de la ventana. */
    public LiveData<Integer> getProgressPercent() {
        return _progressPercent;
    }

    /** Evento de puntuación final. */
    private final MutableLiveData<Event<Integer>> _scoreEvent = new MutableLiveData<>();
    public LiveData<Event<Integer>> scoreEvent = _scoreEvent;

    /** Evento de desbloqueo de velocidad. */
    private final MutableLiveData<Event<String>> _unlockEvent = new MutableLiveData<>();
    public LiveData<Event<String>> unlockEvent = _unlockEvent;

    /** Velocidades desbloqueadas para el usuario/canción. */
    private final MediatorLiveData<SongUserSpeed> _unlockedSpeeds = new MediatorLiveData<>();
    public LiveData<SongUserSpeed> unlockedSpeeds = _unlockedSpeeds;

    /** Fuente activa del LiveData de velocidades desbloqueadas. */
    private LiveData<SongUserSpeed> unlockedSpeedsSource;

    /** Cuenta atrás previa al inicio de detección. */
    private final MutableLiveData<Integer> countdownSeconds = new MutableLiveData<>(0);
    private final MutableLiveData<Boolean> isCountingDown = new MutableLiveData<>(false);
    private final MutableLiveData<Event<Boolean>> countdownFinished = new MutableLiveData<>();
    private CountDownTimer countdownTimer;

    /** Mejor y última puntuación para la canción/velocidad. */
    public final LiveData<Integer> bestScore;
    public final LiveData<Integer> lastScore;

    /** Estado “activo” de la pantalla de práctica (corre detección o cuenta atrás). */
    private final MediatorLiveData<Boolean> isActive = new MediatorLiveData<>();
    public LiveData<Boolean> isActive() { return isActive; }

    /** Id de la canción actual. */
    private final MutableLiveData<Integer> _songId = new MutableLiveData<>();

    /**
     * Crea el ViewModel con sus dependencias.
     */
    @Inject
    public PracticeViewModel(@NonNull PracticeRepository repo,
                             @NonNull SequenceManager sequenceManager,
                             @NonNull PracticeSessionManager practiceSessionManager,
                             MetronomeManager metronomeManager) {
        this.repo = repo;
        this.sequenceManager = sequenceManager;
        this.practiceSessionManager = practiceSessionManager;
        this.metronomeManager = metronomeManager;

        practiceSessionManager.setCallbacks(this);

        // Detalles de canción se reevalúan cuando cambian modo, velocidad o canción.
        songDetails = Transformations.switchMap(_mode, m ->
                Transformations.switchMap(_speedFactor, sp ->
                        Transformations.switchMap(_songId, id -> repo.getSongWithDetails(id))
                ));

        // Estado compuesto “activo” (corriendo o en cuenta atrás).
        isActive.addSource(isRunning, value -> {
            Boolean countdown = isCountingDown.getValue();
            isActive.setValue(Boolean.TRUE.equals(value) || Boolean.TRUE.equals(countdown));
        });
        isActive.addSource(isCountingDown, value -> {
            Boolean running = isRunning.getValue();
            isActive.setValue(Boolean.TRUE.equals(value) || Boolean.TRUE.equals(running));
        });

        // Secuencia de acordes con info por canción.
        sequenceWithInfo = Transformations.switchMap(_songId, id -> repo.getChordsWithInfoForSong(id));

        // Recalcular porcentaje de scroll cuando cambie la línea o la lista de líneas.
        _scrollPercent.addSource(_currentLineIndex, line -> recalcScrollPercent(line, sequenceManager.getLineItems().getValue()));
        _scrollPercent.addSource(sequenceManager.getLineItems(), list -> recalcScrollPercent(_currentLineIndex.getValue(), list));

        // Mejor y última puntuación por canción/velocidad.
        bestScore = Transformations.switchMap(_songId, songId ->
                Transformations.switchMap(_speedFactor, sp ->
                        repo.getBestScore(songId, USER_ID, sp.floatValue())));
        lastScore = Transformations.switchMap(_songId, songId ->
                Transformations.switchMap(_speedFactor, sp ->
                        repo.getLastScore(songId, USER_ID, sp.floatValue())));
    }

    /**
     * Recalcula el porcentaje de scroll [0..1] a partir de la línea actual y el total.
     */
    private void recalcScrollPercent(Integer lineIndex, List<LineItem> lines) {
        if (lines == null || lines.size() <= 1 || lineIndex == null || lineIndex < 0) {
            _scrollPercent.setValue(0f);
        } else {
            float pct = lineIndex / (float) (lines.size() - 1);
            _scrollPercent.setValue(pct);
        }
    }

    /**
     * Actualiza el índice global del acorde actual.
     */
    public void setCurrentIndex(int index) {
        _currentIndex.setValue(index);
    }

    /**
     * Devuelve las líneas (acordes/letras) preparadas para la canción.
     */
    public LiveData<List<LineItem>> getLineItems() {
        return sequenceManager.getLineItems();
    }

    /**
     * Devuelve la secuencia de acordes con información adicional.
     */
    public LiveData<List<SongChordWithInfo>> getSequenceWithInfo() {
        return sequenceWithInfo;
    }

    /**
     * Habilita o deshabilita el uso de metrónomo durante la práctica.
     */
    public void setMetronomeEnabled(boolean enabled) {
        metronomeEnabled = enabled;
    }

    /**
     * Inicializa estado de práctica para una canción.
     * Carga secuencia, asegura registro de velocidades y observa desbloqueos.
     */
    public void initForSong(int songId) {
        _songId.setValue(songId);
        sequenceManager.initForSong(songId);
        repo.ensureSpeedRecordExists(songId, USER_ID);

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

    /**
     * Arranca la detección y, si procede, el metrónomo.
     */
    public void startDetection() {
        if (Boolean.TRUE.equals(_isRunning.getValue())) return;

        _isRunning.setValue(true);
        resetState();

        Mode modeValue = _mode.getValue() != null ? _mode.getValue() : Mode.SYNCHRONIZED;
        List<SongChordWithInfo> chordInfoList = sequenceWithInfo.getValue();
        double speed = _speedFactor.getValue() != null ? _speedFactor.getValue() : 1.0;

        practiceSessionManager.startSession(
                _songId.getValue(),
                modeValue,
                chordInfoList,
                speed
        );

        if (metronomeEnabled) {
            SongWithDetails details = songDetails.getValue();
            if (details != null && details.song != null) {
                Song song = details.song;
                int baseBpm = song.getBpm();
                int adjustedBpm = (int) Math.round(baseBpm * speed);
                String measureStr = song.getMeasure();
                int beatsPerMeasure = 4;
                try {
                    if (measureStr != null && measureStr.contains("/")) {
                        beatsPerMeasure = Integer.parseInt(measureStr.split("/")[0]);
                    }
                } catch (NumberFormatException ignored) {
                }
                metronomeManager.start(adjustedBpm, beatsPerMeasure, true, null);
            }
        }
    }

    /**
     * Detiene la detección y el metrónomo y limpia estado de UI.
     */
    public void stopDetection() {
        if (!Boolean.TRUE.equals(_isRunning.getValue())) return;
        Log.d(TAG, "Deteniendo práctica");
        _isRunning.setValue(false);
        practiceSessionManager.stopSession();
        metronomeManager.stop();
        resetState();
        _progressPercent.setValue(0);
    }

    /**
     * Limpia el estado observable a valores iniciales.
     */
    private void resetState() {
        _currentIndex.setValue(-1);
        _expectedChord.setValue("");
        _currentLineIndex.setValue(0);
        _correctIndices.setValue(new HashSet<>());
        _progressPercent.setValue(0);
    }

    /** Segundos restantes de la cuenta atrás. */
    public LiveData<Integer> getCountdownSeconds() {
        return countdownSeconds;
    }

    /** Indica si hay una cuenta atrás activa. */
    public LiveData<Boolean> getIsCountingDown() {
        return isCountingDown;
    }

    /** Evento de finalización de la cuenta atrás. */
    public LiveData<Event<Boolean>> getCountdownFinished() {
        return countdownFinished;
    }

    /**
     * Inicia una cuenta atrás de 3 segundos previa a comenzar.
     */
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

    /**
     * Cancela la cuenta atrás si existe.
     */
    public void cancelCountdown() {
        if (countdownTimer != null) {
            countdownTimer.cancel();
            countdownTimer = null;
        }
        isCountingDown.setValue(false);
        countdownSeconds.setValue(0);
    }

    /** {@inheritDoc} */
    @Override
    public void onChordIndexChanged(int index, @NonNull String nextChordName) {
        _currentIndex.postValue(index);
        _expectedChord.postValue(nextChordName);
        _currentLineIndex.postValue(calculateLineForGlobalIndex(index));
    }

    /** {@inheritDoc} */
    @Override
    public void onChordDetectedCorrect(int index) {
        Set<Integer> current = _correctIndices.getValue() != null ? _correctIndices.getValue() : new HashSet<>();
        Set<Integer> newSet = new HashSet<>(current);
        newSet.add(index);
        _correctIndices.postValue(newSet);
    }

    /**
     * Limpieza final de recursos del metrónomo al destruir el ViewModel.
     */
    @Override
    protected void onCleared() {
        super.onCleared();
        metronomeManager.stop();
        metronomeManager.releaseSound();
    }

    /**
     * Finaliza la práctica por acción del usuario.
     */
    public void endPractice() {
        Log.d(TAG, "Finalizando práctica por salida del usuario.");
        cancelCountdown();
        metronomeManager.stop();
        practiceSessionManager.stopSession();
        sequenceManager.reset();

        _isRunning.setValue(false);
        _progressPercent.setValue(0);
        _currentIndex.setValue(-1);
        _expectedChord.setValue("");
        _correctIndices.setValue(new HashSet<>());
        _currentLineIndex.setValue(0);
    }

    /** {@inheritDoc} */
    @Override
    public void onProgressUpdated(int percent) {
        _progressPercent.postValue(percent);
    }

    /** {@inheritDoc} */
    @Override
    public void onSessionFinished(int finalScore) {
        _scoreEvent.postValue(new Event<>(finalScore));
        practiceSessionManager.stopSession();
        metronomeManager.stop();
        _isRunning.postValue(false);
        _progressPercent.postValue(0);
    }

    /** {@inheritDoc} */
    @Override
    public void onSpeedUnlocked(@NonNull String speedLabel) {
        _unlockEvent.postValue(new Event<>(speedLabel));
    }

    /**
     * Calcula la línea visual que contiene el índice global indicado.
     */
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

    /**
     * Contenedor de evento consumible una sola vez.
     */
    public static class Event<T> {
        private final T content;
        private boolean handled = false;

        public Event(T content) {
            this.content = content;
        }

        /**
         * Devuelve el contenido si aún no fue consumido; en caso contrario, null.
         */
        public T getIfNotHandled() {
            if (handled) return null;
            handled = true;
            return content;
        }
    }
}
