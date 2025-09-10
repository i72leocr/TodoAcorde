package com.todoacorde.todoacorde.practice.domain;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import com.todoacorde.todoacorde.ChordDetectionListener;
import com.todoacorde.todoacorde.IChordDetector;
import com.todoacorde.todoacorde.achievements.domain.usecase.EvaluatePerfectScoreAchievementUseCase;
import com.todoacorde.todoacorde.achievements.domain.usecase.EvaluateSpeedUnlockAchievementUseCase;
import com.todoacorde.todoacorde.achievements.domain.usecase.EvaluateUniqueChordsAchievementUseCase;
import com.todoacorde.todoacorde.practice.data.PracticeDetail;
import com.todoacorde.todoacorde.practice.data.PracticeRepository;
import com.todoacorde.todoacorde.practice.data.PracticeSession;
import com.todoacorde.todoacorde.songs.data.SongChordWithInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Gestor de sesiones de práctica. Coordina la detección de acordes, el avance
 * de la sesión (libre o sincronizada), el cómputo de métricas y la persistencia
 * de resultados, además de notificar hitos de logros.
 *
 * Implementa {@link ChordDetectionListener} para recibir eventos del detector.
 */
@Singleton
public class PracticeSessionManager implements ChordDetectionListener {

    private static final String TAG = "PracticeSessionManager";

    /** Identificador del usuario en ejecución (placeholder). */
    private static final int USER_ID = 1;

    /** Repositorio de acceso a datos de práctica. */
    private final PracticeRepository repo;
    /** Detector de acordes inyectado. */
    private final IChordDetector chordDetector;
    /** Caso de uso: acordes únicos acertados. */
    private final EvaluateUniqueChordsAchievementUseCase uniqueUc;
    /** Caso de uso: desbloqueo de velocidades. */
    private final EvaluateSpeedUnlockAchievementUseCase speedUc;
    /** Caso de uso: puntuaciones perfectas. */
    private final EvaluatePerfectScoreAchievementUseCase perfectUc;

    /** Callbacks para notificar a la UI. */
    private PracticeSessionCallbacks callbacks;
    /** Flag de sesión en curso. */
    private boolean sessionRunning = false;
    /** Modo actual de práctica. */
    private PracticeViewModel.Mode currentMode;
    /** Id de canción en curso. */
    private int currentSongId;
    /** Factor de velocidad actual (0.5, 0.75, 1.0…). */
    private float currentSpeedFactor;
    /** Índice del acorde esperado en la secuencia. */
    private int currentIndex = 0;
    /** Si ya se detectó un acorde correcto en la ventana sincronizada. */
    private boolean windowDetected = false;

    /** Acumulado de detalles por acorde (clave chordId). */
    private final Map<Integer, PracticeDetail> detailMap = new HashMap<>();
    /** Control de notificaciones de desbloqueo para no duplicar. */
    private final HashSet<String> unlockedSpeedNotifications = new HashSet<>();
    /** Secuencia de nombres de acordes esperados. */
    private final List<String> chordSequence = new ArrayList<>();

    /** Handler sobre hilo principal para temporización de UI. */
    private final Handler uiHandler;

    /** Runnable que cierra la ventana de detección en modo sincronizado. */
    private Runnable windowRunnable;
    /** Runnable que actualiza el progreso de ventana. */
    private Runnable progressRunnable;

    /** Marca temporal de inicio de sesión. */
    private long sessionStartTime;
    /** Marca temporal de fin de sesión. */
    private long sessionEndTime;

    /**
     * Crea el gestor de sesiones con sus dependencias.
     */
    @Inject
    public PracticeSessionManager(@NonNull PracticeRepository repo,
                                  @NonNull IChordDetector chordDetector,
                                  @NonNull EvaluateUniqueChordsAchievementUseCase uniqueUc,
                                  @NonNull EvaluateSpeedUnlockAchievementUseCase speedUc,
                                  @NonNull EvaluatePerfectScoreAchievementUseCase perfectUc) {
        this.repo = repo;
        this.chordDetector = chordDetector;
        this.uniqueUc = uniqueUc;
        this.speedUc = speedUc;
        this.perfectUc = perfectUc;
        this.uiHandler = new Handler(Looper.getMainLooper());
    }

    /**
     * Registra los callbacks de UI.
     *
     * @param callbacks implementación de {@link PracticeSessionCallbacks}.
     */
    public void setCallbacks(@NonNull PracticeSessionCallbacks callbacks) {
        this.callbacks = callbacks;
    }

    /**
     * Inicia una sesión de práctica.
     *
     * @param songId        identificador de canción.
     * @param mode          modo de práctica (FREE o SYNCHRONIZED).
     * @param chordInfoList lista ordenada de acordes con metadatos.
     * @param speedFactor   factor de velocidad de reproducción.
     */
    public void startSession(int songId,
                             @NonNull PracticeViewModel.Mode mode,
                             @NonNull List<SongChordWithInfo> chordInfoList,
                             double speedFactor) {
        if (sessionRunning) return;

        sessionRunning = true;
        currentSongId = songId;
        currentMode = mode;
        currentSpeedFactor = (float) speedFactor;
        currentIndex = 0;
        detailMap.clear();
        windowDetected = false;
        buildChordSequence(chordInfoList);

        if (currentMode == PracticeViewModel.Mode.FREE) {
            Log.d(TAG, "Starting FREE mode detection");
            chordDetector.startDetection(this);
        } else {
            Log.d(TAG, "Starting SYNCHRONIZED mode detection");
            sessionStartTime = System.currentTimeMillis();
            scheduleNextWindow(0);
        }
    }

    /**
     * Detiene la sesión en curso y limpia temporizadores y detector.
     */
    public void stopSession() {
        if (!sessionRunning) return;
        sessionRunning = false;
        chordDetector.stopDetection();
        if (windowRunnable != null) uiHandler.removeCallbacks(windowRunnable);
        if (progressRunnable != null) uiHandler.removeCallbacks(progressRunnable);
    }

    /**
     * Construye la secuencia de nombres de acordes esperados a partir de la lista.
     */
    private void buildChordSequence(@NonNull List<SongChordWithInfo> chordInfoList) {
        chordSequence.clear();
        for (SongChordWithInfo info : chordInfoList) {
            String chordName = info.chord.getName();
            if (chordName != null) {
                chordSequence.add(chordName);
            }
        }
        Log.d(TAG, "Chord sequence initialized: " + chordSequence);
    }

    /**
     * Programa la siguiente ventana de detección en modo sincronizado.
     *
     * @param index índice del acorde que se va a esperar.
     */
    private void scheduleNextWindow(int index) {
        if (!sessionRunning) return;

        if (index >= chordSequence.size()) {
            finishSession();
            return;
        }

        windowDetected = false;
        currentIndex = index;

        // Nota: se accede al valor actual del LiveData, se asume precargado por la UI.
        SongChordWithInfo info = repo.getChordsWithInfoForSong(currentSongId).getValue().get(index);

        long baseMs = info.songChord.getDuration() * 1000L;
        long durationMs = (long) (baseMs / (currentSpeedFactor != 0 ? currentSpeedFactor : 1.0));
        Log.d(TAG, "Window[" + index + "] duration = " + durationMs + "ms (base=" + baseMs + ", speedFactor=" + currentSpeedFactor + ")");

        chordDetector.startDetection(this);
        startProgressTicker(durationMs);

        windowRunnable = () -> {
            Log.d(TAG, "Window expired for index " + index);
            chordDetector.stopDetection();

            if (!windowDetected) {
                int chordId = info.songChord.getChordId();
                recordPracticeDetail(chordId, false);
            }

            if (callbacks != null) {
                String nextChordName = (index + 1 < chordSequence.size()) ? chordSequence.get(index + 1) : "";
                callbacks.onChordIndexChanged(index + 1, nextChordName);
            }
            scheduleNextWindow(index + 1);
        };

        uiHandler.postDelayed(windowRunnable, durationMs);

        if (callbacks != null) {
            String chordName = chordSequence.get(index);
            callbacks.onChordIndexChanged(index, chordName);
        }
    }

    /**
     * Inicia un ticker que actualiza el progreso de la ventana de detección.
     *
     * @param totalMs duración total de la ventana en milisegundos.
     */
    private void startProgressTicker(long totalMs) {
        final long startTime = System.currentTimeMillis();
        progressRunnable = new Runnable() {
            @Override
            public void run() {
                long elapsed = System.currentTimeMillis() - startTime;
                int percent = (int) Math.min(100, (elapsed * 100) / totalMs);

                if (callbacks != null) {
                    callbacks.onProgressUpdated(percent);
                }
                if (percent < 100 && sessionRunning) {
                    uiHandler.postDelayed(this, 50);
                }
            }
        };
        if (callbacks != null) {
            callbacks.onProgressUpdated(0);
        }
        uiHandler.removeCallbacks(progressRunnable);
        uiHandler.post(progressRunnable);
    }

    /**
     * Evento de detección de acorde desde el detector.
     */
    @Override
    public void onChordDetected(String detectedChordName) {
        if (!sessionRunning) return;
        if (currentIndex >= chordSequence.size()) return;

        String expected = chordSequence.get(currentIndex);
        if (!detectedChordName.equals(expected)) {
            return;
        }

        if (currentMode == PracticeViewModel.Mode.SYNCHRONIZED) {
            if (!windowDetected) {
                Log.d(TAG, "SYNC mode: Correct chord detected at index " + currentIndex);
                windowDetected = true;
                if (callbacks != null) {
                    callbacks.onChordDetectedCorrect(currentIndex);
                }
                SongChordWithInfo info = repo.getChordsWithInfoForSong(currentSongId).getValue().get(currentIndex);
                recordPracticeDetail(info.songChord.getChordId(), true);
            }
        } else {
            Log.d(TAG, "FREE mode: Correct chord detected at index " + currentIndex);
            if (callbacks != null) {
                callbacks.onChordDetectedCorrect(currentIndex);
            }
            int nextIndex = currentIndex + 1;
            if (nextIndex < chordSequence.size()) {
                String nextChordName = chordSequence.get(nextIndex);
                if (callbacks != null) {
                    callbacks.onChordIndexChanged(nextIndex, nextChordName);
                }
            }
            currentIndex = nextIndex;
            if (currentIndex >= chordSequence.size()) {
                finishSession();
            }
        }
    }

    /**
     * Registra un intento para un acorde determinado, marcándolo como acierto o fallo.
     */
    private void recordPracticeDetail(int chordId, boolean isCorrect) {
        PracticeDetail detail = detailMap.get(chordId);
        if (detail == null) {
            detail = new PracticeDetail(0, chordId, 0, 0, 0);
            detailMap.put(chordId, detail);
        }
        detail.totalAttempts++;
        if (isCorrect) {
            detail.correctCount++;
        } else {
            detail.incorrectCount++;
        }
    }

    /**
     * Finaliza la sesión: computa puntuación, persiste datos y evalúa logros.
     *
     * Reglas de puntuación:
     * - score = 100 * aciertos / total de acordes en la secuencia.
     * - Un acierto cuenta si se detectó al menos una vez el acorde correcto en su ventana.
     */
    private void finishSession() {
        int totalChords = chordSequence.size();

        int correctCount = 0;
        for (PracticeDetail d : detailMap.values()) {
            if (d.correctCount > 0) correctCount += d.correctCount;
        }

        int score = totalChords == 0 ? 0 : (int) Math.round(100.0 * (correctCount) / totalChords);
        Log.d(TAG, "Practice session finished: score=" + score);

        sessionEndTime = System.currentTimeMillis();
        long duration = sessionEndTime - sessionStartTime;

        PracticeSession session = new PracticeSession();
        session.userId = USER_ID;
        session.songId = currentSongId;
        session.isCompleted = true;
        session.startTime = sessionStartTime;
        session.endTime = sessionEndTime;
        session.duration = duration;
        session.totalScore = score;
        session.speed = currentSpeedFactor;
        session.isLastSession = true;
        session.isBestSession = true;

        List<PracticeDetail> detailsList = new ArrayList<>(detailMap.values());
        repo.saveSessionWithDetails(session, detailsList);

        // Evaluaciones de logros.
        uniqueUc.evaluate();
        if (score == 100) {
            perfectUc.evaluate();
        }

        // Intento de desbloqueo de velocidad si la puntuación es suficiente.
        if (score >= 80) {
            repo.tryUnlockNextSpeed(currentSongId, USER_ID, currentSpeedFactor, unlocked -> {
                if (unlocked) {
                    String nextLabel;
                    if (currentSpeedFactor == 0.5f) {
                        nextLabel = "0.75x";
                    } else if (currentSpeedFactor == 0.75f) {
                        nextLabel = "1x";
                    } else {
                        nextLabel = null;
                    }
                    if (nextLabel != null && unlockedSpeedNotifications.add(nextLabel)) {
                        if (callbacks != null) {
                            callbacks.onSpeedUnlocked(nextLabel);
                        }
                        speedUc.evaluate();
                    }
                }
            });
        }

        if (callbacks != null) {
            callbacks.onSessionFinished(score);
        }
    }

    /**
     * Callbacks para la UI que consume los eventos de la sesión de práctica.
     */
    public interface PracticeSessionCallbacks {
        /**
         * Notifica cambio de índice de acorde esperado.
         *
         * @param index         índice actual.
         * @param nextChordName nombre del acorde esperado.
         */
        void onChordIndexChanged(int index, @NonNull String nextChordName);

        /**
         * Notifica detección correcta del acorde actual.
         *
         * @param index índice del acorde acertado.
         */
        void onChordDetectedCorrect(int index);

        /**
         * Notifica el progreso de la ventana de detección en porcentaje.
         *
         * @param percent valor 0..100.
         */
        void onProgressUpdated(int percent);

        /**
         * Notifica el fin de la sesión con su puntuación final.
         *
         * @param finalScore puntuación 0..100.
         */
        void onSessionFinished(int finalScore);

        /**
         * Notifica que se ha desbloqueado una velocidad nueva.
         *
         * @param speedLabel etiqueta de velocidad (por ejemplo, 0.75x).
         */
        void onSpeedUnlocked(@NonNull String speedLabel);
    }
}
