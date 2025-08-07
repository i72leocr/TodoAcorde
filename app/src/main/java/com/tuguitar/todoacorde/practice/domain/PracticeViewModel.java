package com.tuguitar.todoacorde.practice.domain;

import android.os.CountDownTimer;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;

import com.tuguitar.todoacorde.Chord;
import com.tuguitar.todoacorde.ChordDetectionListener;
import com.tuguitar.todoacorde.achievements.domain.usecases.EvaluatePerfectScoreAchievementUseCase;
import com.tuguitar.todoacorde.achievements.domain.usecases.EvaluateSpeedUnlockAchievementUseCase;
import com.tuguitar.todoacorde.achievements.domain.usecases.EvaluateUniqueChordsAchievementUseCase;
import com.tuguitar.todoacorde.IChordDetector;
import com.tuguitar.todoacorde.LineItem;
import com.tuguitar.todoacorde.practice.data.PracticeDetail;
import com.tuguitar.todoacorde.practice.data.PracticeRepository;
import com.tuguitar.todoacorde.practice.data.PracticeSession;
import com.tuguitar.todoacorde.SongChordWithInfo;
import com.tuguitar.todoacorde.practice.data.SongUserSpeed;
import com.tuguitar.todoacorde.SongWithDetails;
import com.tuguitar.todoacorde.SpanInfo;
import com.tuguitar.todoacorde.songs.data.SongChord;
import com.tuguitar.todoacorde.songs.data.SongLyric;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

/**
 * ViewModel para la práctica de acordes de una canción.
 * Soporta modo libre y sincronizado (por duración BPM/measure),
 * marca solo el primer acierto por ventana, expone progreso (%),
 * calcula un score final en % al terminar la práctica sincronizada
 * y ajusta la duración de cada acorde según un speedFactor (0.5×, 0.75×, 1×).
 */
@HiltViewModel
public class PracticeViewModel extends ViewModel implements ChordDetectionListener {
    private static final String TAG = "PracticeViewModel";

    // Para evitar eventos duplicados al desbloquear velocidad
    private final Set<String> unlockedSpeedNotifications = new HashSet<>();

    private final Map<Integer, PracticeDetail> detailMap = new HashMap<>();

    public enum Mode { FREE, SYNCHRONIZED }

    private final MutableLiveData<Mode> _mode = new MutableLiveData<>(Mode.SYNCHRONIZED);
    public LiveData<Mode> mode = _mode;
    public void setMode(Mode m) { _mode.setValue(m); }

    /** Velocidad de reproducción: 0.5×, 0.75× o 1.0× */
    private final MutableLiveData<Double> _speedFactor = new MutableLiveData<>(1.0);
    public LiveData<Double> speedFactor = _speedFactor;
    public void setSpeedFactor(double f) { _speedFactor.setValue(f); }

    private boolean windowDetected = false;
    private int bpm;
    private String measure;
    private long sessionStartTime;
    private long sessionEndTime;

    private final PracticeRepository repo;

    private final EvaluateUniqueChordsAchievementUseCase uniqueUc;
    private final EvaluateSpeedUnlockAchievementUseCase speedUc;
    private final EvaluatePerfectScoreAchievementUseCase perfectUc;

    private final IChordDetector chordDetector;
    private final Handler uiHandler = new Handler();

    private final MutableLiveData<Integer> _songId = new MutableLiveData<>();
    private LiveData<List<Chord>> chordProfiles = null;
    private final LiveData<List<SongChord>> songChords;
    public final LiveData<SongWithDetails> songDetails;
    private final LiveData<List<SongChordWithInfo>> sequenceWithInfo;
    private final MediatorLiveData<SongUserSpeed> _unlockedSpeeds = new MediatorLiveData<>();
    public LiveData<SongUserSpeed> unlockedSpeeds = _unlockedSpeeds;
    private final MediatorLiveData<Pair<List<Chord>,List<SongChord>>> seqSource = new MediatorLiveData<>();
    private final MediatorLiveData<List<LineItem>> lineItems = new MediatorLiveData<>();

    private final List<String> sequence = new ArrayList<>();
    private final Map<Integer,Integer> chordGlobalIndexByPosition = new HashMap<>();

    private final MutableLiveData<Boolean> _isRunning = new MutableLiveData<>(false);
    public final LiveData<Boolean> isRunning = _isRunning;
    private final MutableLiveData<Integer> _currentIndex = new MutableLiveData<>(0);
    public final LiveData<Integer> currentIndex = _currentIndex;
    private final MutableLiveData<String> _expectedChord = new MutableLiveData<>("");
    public final LiveData<String> expectedChord = _expectedChord;

    private final MutableLiveData<Integer> _currentLineIndex = new MutableLiveData<>(0);
    public LiveData<Integer> getCurrentLineIndex() { return _currentLineIndex; }

    private final MutableLiveData<Set<Integer>> _correctIndices = new MutableLiveData<>(new HashSet<>());
    public LiveData<Set<Integer>> getCorrectIndices() { return _correctIndices; }

    /** Porcentaje de progreso de la ventana sincronizada (0–100) */
    private final MutableLiveData<Integer> _progressPercent = new MutableLiveData<>(0);
    public LiveData<Integer> getProgressPercent() { return _progressPercent; }

    /** Evento de score final tras práctica sincronizada */
    private final MutableLiveData<Event<Integer>> _scoreEvent = new MutableLiveData<>();
    public LiveData<Event<Integer>> scoreEvent = _scoreEvent;

    private Runnable windowRunnable;
    private Runnable progressRunnable;

    public LiveData<Integer> bestScore  = new MutableLiveData<>(0);
    public LiveData<Integer> lastScore  = new MutableLiveData<>(0);

    private static final int USER_ID = 1; // o tu variable de user
    private final MutableLiveData<Event<String>> _unlockEvent = new MutableLiveData<>();
    public LiveData<Event<String>> unlockEvent = _unlockEvent;

    @Inject
    public PracticeViewModel(@NonNull PracticeRepository repo,
                             @NonNull IChordDetector chordDetector,
                             @NonNull EvaluateUniqueChordsAchievementUseCase uniqueUc,
                             @NonNull EvaluateSpeedUnlockAchievementUseCase speedUc,
                             @NonNull EvaluatePerfectScoreAchievementUseCase perfectUc) {
        this.repo = repo;
        this.chordDetector = chordDetector;
        this.uniqueUc   = uniqueUc;
        this.speedUc    = speedUc;
        this.perfectUc  = perfectUc;

        chordProfiles    = repo.getAllChords();
        chordProfiles.observeForever(profiles -> {
            chordDetector.setChordProfiles(profiles);
            Log.d(TAG, "Chord profiles set: " + (profiles != null ? profiles.size() : 0));
        });
        songChords       = Transformations.switchMap(_songId, repo::getChordsForSong);
        songDetails      = Transformations.switchMap(_songId, repo::getSongWithDetails);
        sequenceWithInfo = Transformations.switchMap(_songId, repo::getChordsWithInfoForSong);

        seqSource.addSource(chordProfiles, p -> seqSource.setValue(Pair.create(p, songChords.getValue())));
        seqSource.addSource(songChords, sc -> seqSource.setValue(Pair.create(chordProfiles.getValue(), sc)));

        seqSource.observeForever(pair -> {
            if (pair.first != null && pair.second != null) {
                buildSequenceAndIndexMap(pair.first, pair.second);
            }
        });

        bestScore = Transformations.switchMap(_songId, songId ->
                Transformations.switchMap(_speedFactor, speed ->
                        repo.getBestScore(songId, USER_ID, speed.floatValue())
                )
        );

        lastScore = Transformations.switchMap(_songId, songId ->
                Transformations.switchMap(_speedFactor, speed ->
                        repo.getLastScore(songId, USER_ID, speed.floatValue())
                )
        );

        lineItems.addSource(seqSource, __ -> buildLineItems());
        lineItems.addSource(songDetails, __ -> {
            SongWithDetails d = songDetails.getValue();
            if (d != null) {
                bpm = d.song.getBpm();
                measure = d.song.getMeasure();
                Log.d(TAG, "Song loaded: BPM=" + bpm +
                        " measure=" + measure);
            }
            buildLineItems();
        });

        currentIndex.observeForever(this::onIndexChanged);
    }

    /** Configura la canción a practicar */
    private LiveData<SongUserSpeed> unlockedSpeedsSource;

    public void initForSong(int songId) {
        _songId.setValue(songId);
        repo.ensureSpeedRecordExists(songId, USER_ID);

        if (unlockedSpeedsSource != null) {
            _unlockedSpeeds.removeSource(unlockedSpeedsSource); // ⬅️ LIMPIAR ANTERIOR
        }

        unlockedSpeedsSource = repo.getSongUserSpeed(songId, USER_ID);
        _unlockedSpeeds.addSource(unlockedSpeedsSource, speed -> {
            _unlockedSpeeds.setValue(speed);

            if (speed != null) {
                Log.d(TAG, "Velocidad desbloqueada desde BD: " + speed.getMaxUnlockedSpeed());
                setSpeedFactor(speed.getMaxUnlockedSpeed());
            }
        });
    }



    /** Inicia detección según el modo */
    public void startDetection() {
        if (Boolean.TRUE.equals(_isRunning.getValue())) return;
        _isRunning.setValue(true);
        resetState();
        if (_mode.getValue() == Mode.FREE) {
            Log.d(TAG, "Arrancando modo FREE");
            chordDetector.startDetection(this);
        } else {
            Log.d(TAG, "Arrancando modo SYNCHRONIZED");
            sessionStartTime = System.currentTimeMillis();
            detailMap.clear();
            scheduleWindow(0);
        }
    }

    /** Detiene la práctica y limpia callbacks */
    public void stopDetection() {
        if (!Boolean.TRUE.equals(_isRunning.getValue())) return;
        Log.d(TAG, "Deteniendo práctica");
        _isRunning.setValue(false);
        chordDetector.stopDetection();
        if (windowRunnable != null) uiHandler.removeCallbacks(windowRunnable);
        if (progressRunnable != null) uiHandler.removeCallbacks(progressRunnable);
        _progressPercent.postValue(0);
    }

    private void scheduleWindow(int idx) {
        List<SongChordWithInfo> seq = sequenceWithInfo.getValue();
        if (!_isRunning.getValue() || seq == null) {
            stopDetection();
            return;
        }
        if (idx >= seq.size()) {
            finishSynchronizedPractice();
            return;
        }
        SongChordWithInfo info = seq.get(idx);
        long baseMs = info.songChord.getDuration() * 1000L;
        double factor = speedFactor.getValue() != null ? speedFactor.getValue() : 1.0;
        long durationMs = (long)(baseMs / factor);
        Log.d(TAG, "Window[" + idx + "] base=" + baseMs +
                "ms factor=" + factor + " → dur=" + durationMs + "ms");

        windowDetected = false;
        chordDetector.startDetection(this);
        startProgressTicker(durationMs);

        windowRunnable = () -> {
            Log.d(TAG, "Window expired[" + idx + "]");
            chordDetector.stopDetection();

            // 👇 Nuevo: registrar intento fallido si no hubo acierto
            if (!windowDetected && _mode.getValue() == Mode.SYNCHRONIZED) {
                if (seq != null && idx < seq.size()) {
                    int chordId = seq.get(idx).songChord.getChordId();
                    recordPracticeDetail(chordId, false);
                }
            }

            _currentIndex.postValue(idx + 1);
            scheduleWindow(idx + 1);
        };
        uiHandler.postDelayed(windowRunnable, durationMs);
    }

    private void finishSynchronizedPractice() {
        int total   = sequence.size();
        int correct = _correctIndices.getValue().size();
        int score   = total == 0 ? 0 : (int)Math.round(100.0 * correct / total);
        Log.d(TAG, "Práctica sincronizada finalizada: score=" + score);
        _scoreEvent.postValue(new Event<>(score));

        List<SongChordWithInfo> infoSeq = sequenceWithInfo.getValue();
        if (infoSeq == null) return;

        // — marcamos el fin y calculamos duración —
        sessionEndTime = System.currentTimeMillis();
        long duration = sessionEndTime - sessionStartTime;

        // — montamos la entidad PracticeSession —
        PracticeSession ps = new PracticeSession();
        ps.userId        = USER_ID;
        ps.songId        = _songId.getValue();
        ps.isCompleted   = true;
        ps.startTime     = sessionStartTime;
        ps.endTime       = sessionEndTime;
        ps.duration      = duration;
        ps.totalScore    = score;
        ps.speed         = speedFactor.getValue().floatValue();
        ps.isLastSession = true;
        ps.isBestSession = true;

        List<PracticeDetail> details = new ArrayList<>(detailMap.values());
        repo.saveSessionWithDetails(ps, details);
        //  ——————————— Evaluar los logros ———————————
        uniqueUc.evaluate();
        if (score == 100){
            perfectUc.evaluate();
        }
        // — intentar desbloquear siguiente nivel si aplica —
        if (score >= 80) {
            float currentSpeed = ps.speed;
            repo.tryUnlockNextSpeed(ps.songId, ps.userId, currentSpeed, unlocked -> {
                if (unlocked) {
                    String nextLabel = currentSpeed == 0.5f ? "0.75x" :
                            currentSpeed == 0.75f ? "1x" : null;
                    if (nextLabel != null && unlockedSpeedNotifications.add(nextLabel)) {
                        _unlockEvent.postValue(new Event<>(nextLabel));
                        speedUc.evaluate();
                    }
                }
            });
        }

        stopDetection();
    }

    private void startProgressTicker(long totalMs) {
        final long startTime = SystemClock.elapsedRealtime();
        progressRunnable = new Runnable() {
            @Override public void run() {
                long elapsed = SystemClock.elapsedRealtime() - startTime;
                int pct     = (int)Math.min(100, (elapsed * 100) / totalMs);
                _progressPercent.postValue(pct);
                if (pct < 100) uiHandler.postDelayed(this, 50);
            }
        };
        uiHandler.removeCallbacks(progressRunnable);
        _progressPercent.postValue(0);
        uiHandler.post(progressRunnable);
    }

    @Override
    public void onChordDetected(String detected) {
        if (!Boolean.TRUE.equals(_isRunning.getValue())) return;
        Integer idx = _currentIndex.getValue();
        if (idx == null || idx >= sequence.size()) return;
        if (!detected.equals(sequence.get(idx))) return;

        if (_mode.getValue() == Mode.SYNCHRONIZED) {
            if (!windowDetected) {
                Log.d(TAG, "SYNC acertado idx=" + idx);
                markCorrect(idx);
                windowDetected = true;
            }
        } else {
            Log.d(TAG, "FREE acertado idx=" + idx);
            markCorrect(idx);
            _currentIndex.postValue(idx + 1);
        }
    }

    private void markCorrect(int idx) {
        Set<Integer> s = new HashSet<>(_correctIndices.getValue());
        s.add(idx);
        _correctIndices.postValue(s);
        // ✅ Solo acumular intento si estamos en modo sincronizado
        if (_mode.getValue() == Mode.SYNCHRONIZED) {
            List<SongChordWithInfo> infoSeq = sequenceWithInfo.getValue();
            if (infoSeq != null && idx < infoSeq.size()) {
                int chordId = infoSeq.get(idx).songChord.getChordId();
                recordPracticeDetail(chordId, true);
            }
        }
    }

    /** Acumula un intento en memoria para el acorde actual */
    public void recordPracticeDetail(int chordId, boolean isCorrect) {
        PracticeDetail d = detailMap.get(chordId);
        if (d == null) {
            d = new PracticeDetail(0, chordId, 0, 0, 0);
            detailMap.put(chordId, d);
        }
        d.totalAttempts++;
        if (isCorrect) d.correctCount++;
        else d.incorrectCount++;
    }

    private void onIndexChanged(int idx) {
        if (idx < sequence.size()) {
            _expectedChord.postValue(sequence.get(idx));
        }
        _currentLineIndex.postValue(calculateLineForGlobalIndex(idx));
    }

    private void buildSequenceAndIndexMap(List<Chord> profiles,
                                          List<SongChord> scList) {
        Map<Integer,String> nameById = new HashMap<>();
        for (Chord c : profiles) nameById.put(c.id, c.getName());
        sequence.clear();
        chordGlobalIndexByPosition.clear();
        for (int i = 0; i < scList.size(); i++) {
            SongChord sc = scList.get(i);
            String nm  = nameById.get(sc.chordId);
            if (nm != null) {
                chordGlobalIndexByPosition.put(i, sequence.size());
                sequence.add(nm);
            }
        }
        resetState();
        Log.d(TAG, "Secuencia inicializada: " + sequence);
    }

    private void buildLineItems() {
        Pair<List<Chord>,List<SongChord>> data = seqSource.getValue();
        SongWithDetails details = songDetails.getValue();
        if (data==null || details==null
                || data.first==null || data.second==null
                || details.lyricLines==null) {
            lineItems.setValue(Collections.emptyList());
            return;
        }
        Map<Integer,String> nameById = new HashMap<>();
        for (Chord c: data.first) nameById.put(c.id, c.getName());
        List<SongChord> scList = new ArrayList<>(data.second);
        scList.sort(Comparator
                .comparingInt((SongChord sc)->sc.lyricId)
                .thenComparingInt(sc->sc.positionInVerse)
        );
        List<LineItem> items = new ArrayList<>();
        for (SongLyric lyric: details.lyricLines) {
            String txt = lyric.line != null ? lyric.line : "";
            int maxEnd = txt.length();
            for (SongChord sc: scList) {
                if (sc.lyricId!=lyric.id) continue;
                String nm = nameById.get(sc.chordId);
                if (nm!=null) maxEnd = Math.max(maxEnd,
                        sc.positionInVerse+nm.length());
            }
            char[] buf = new char[maxEnd];
            Arrays.fill(buf,' ');
            List<SpanInfo> spans = new ArrayList<>();
            for (int i=0; i<scList.size(); i++){
                SongChord sc = scList.get(i);
                if (sc.lyricId!=lyric.id) continue;
                String nm = nameById.get(sc.chordId);
                int p = sc.positionInVerse;
                if (nm!=null && p+nm.length()<=buf.length){
                    for (int k=0; k<nm.length(); k++){
                        buf[p+k] = nm.charAt(k);
                    }
                    spans.add(new SpanInfo(
                            chordGlobalIndexByPosition.getOrDefault(i,-1),
                            p, p+nm.length()
                    ));
                }
            }
            items.add(new LineItem(new String(buf), txt, spans));
        }
        lineItems.setValue(items);
    }

    private void resetState() {
        _currentIndex.postValue(0);
        _expectedChord.postValue(sequence.isEmpty() ? "" : sequence.get(0));
        _currentLineIndex.postValue(0);
        _correctIndices.postValue(new HashSet<>());
        _progressPercent.postValue(0);
        if (windowRunnable!=null) uiHandler.removeCallbacks(windowRunnable);
        if (progressRunnable!=null) uiHandler.removeCallbacks(progressRunnable);
    }

    private int calculateLineForGlobalIndex(int globalIdx) {
        List<LineItem> lines = lineItems.getValue();
        if (lines==null) return -1;
        for (int i=0; i<lines.size(); i++){
            for (SpanInfo span: lines.get(i).spans){
                if (span.globalIndex==globalIdx) return i;
            }
        }
        return -1;
    }

    public LiveData<List<LineItem>> getLineItems() { return lineItems; }
    public LiveData<List<SongChordWithInfo>> getSequenceWithInfo() {
        return sequenceWithInfo;
    }
    public void setCurrentIndex(int idx) {
        Boolean running = _isRunning.getValue();
        if (running!=null && running) return;
        if (idx>=0 && idx<sequence.size()){
            _currentIndex.setValue(idx);
            _expectedChord.setValue(sequence.get(idx));
        }
    }

    /** Evento de un solo consumo. */
    public static class Event<T> {
        private final T content;
        private boolean handled = false;
        public Event(T content){ this.content = content; }
        public T getIfNotHandled(){
            if (handled) return null;
            handled = true;
            return content;
        }
    }


    // En el ViewModel:
    private final MutableLiveData<Integer> countdownSeconds = new MutableLiveData<>(0);
    private final MutableLiveData<Boolean> isCountingDown = new MutableLiveData<>(false);
    private final MutableLiveData<Event<Boolean>> countdownFinished = new MutableLiveData<>();
    private CountDownTimer countdownTimer;

    public LiveData<Integer> getCountdownSeconds() { return countdownSeconds; }
    public LiveData<Boolean> getIsCountingDown() { return isCountingDown; }
    public LiveData<Event<Boolean>> getCountdownFinished() { return countdownFinished; }

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

    public void cancelCountdown() {
        if (countdownTimer != null) {
            countdownTimer.cancel();
            countdownTimer = null;
        }
        isCountingDown.setValue(false);
        countdownSeconds.setValue(0);
    }
}
