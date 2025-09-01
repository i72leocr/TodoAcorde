package com.tuguitar.todoacorde.practice.domain;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import com.tuguitar.todoacorde.ChordDetectionListener;
import com.tuguitar.todoacorde.IChordDetector;
import com.tuguitar.todoacorde.achievements.domain.usecase.EvaluatePerfectScoreAchievementUseCase;
import com.tuguitar.todoacorde.achievements.domain.usecase.EvaluateSpeedUnlockAchievementUseCase;
import com.tuguitar.todoacorde.achievements.domain.usecase.EvaluateUniqueChordsAchievementUseCase;
import com.tuguitar.todoacorde.songs.data.SongChordWithInfo;
import com.tuguitar.todoacorde.practice.data.PracticeDetail;
import com.tuguitar.todoacorde.practice.data.PracticeRepository;
import com.tuguitar.todoacorde.practice.data.PracticeSession;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class PracticeSessionManager implements ChordDetectionListener {
    private static final String TAG = "PracticeSessionManager";
    private static final int USER_ID = 1;
    private final PracticeRepository repo;
    private final IChordDetector chordDetector;
    private final EvaluateUniqueChordsAchievementUseCase uniqueUc;
    private final EvaluateSpeedUnlockAchievementUseCase speedUc;
    private final EvaluatePerfectScoreAchievementUseCase perfectUc;
    private PracticeSessionCallbacks callbacks;
    private boolean sessionRunning = false;
    private PracticeViewModel.Mode currentMode;
    private int currentSongId;
    private float currentSpeedFactor;
    private int currentIndex = 0;
    private boolean windowDetected = false;
    private final Map<Integer, PracticeDetail> detailMap = new HashMap<>();
    private final HashSet<String> unlockedSpeedNotifications = new HashSet<>();
    private final List<String> chordSequence = new ArrayList<>();
    private final Handler uiHandler;
    private Runnable windowRunnable;
    private Runnable progressRunnable;
    private long sessionStartTime;
    private long sessionEndTime;

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

    /** Set the callback listener for session events (usually the ViewModel). */
    public void setCallbacks(@NonNull PracticeSessionCallbacks callbacks) {
        this.callbacks = callbacks;
    }

    /**
     * Start a practice session for the given song in the specified mode.
     * @param songId The ID of the song to practice.
     * @param mode FREE or SYNCHRONIZED mode.
     * @param chordInfoList The list of SongChordWithInfo for the song (chord sequence with durations).
     * @param speedFactor The speed factor (e.g., 0.5x, 0.75x, 1.0x) to apply for SYNCHRONIZED mode.
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

    /** Stop the practice session and clean up any ongoing detection or callbacks. */
    public void stopSession() {
        if (!sessionRunning) return;
        sessionRunning = false;
        chordDetector.stopDetection();
        if (windowRunnable != null) uiHandler.removeCallbacks(windowRunnable);
        if (progressRunnable != null) uiHandler.removeCallbacks(progressRunnable);
    }

    /** Build the sequence of chord names from the SongChordWithInfo list for detection. */
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

    /** Schedule the next detection window (for SYNCHRONIZED mode) starting at the given sequence index. */
    private void scheduleNextWindow(int index) {
        if (!sessionRunning) return;
        if (index >= chordSequence.size()) {
            finishSession();
            return;
        }
        windowDetected = false;
        currentIndex = index;
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

    /** Start a progress updater that ticks the progress bar from 0 to 100% over the given duration. */
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

    /** Callback from IChordDetector when a chord name is detected from audio input. */
    @Override
    public void onChordDetected(String detectedChordName) {
        if (!sessionRunning) return;
        if (currentIndex >= chordSequence.size()) return;
        String expected = chordSequence.get(currentIndex);
        if (!detectedChordName.equals(expected)) {
            return; // Ignore non-matching chord detections
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

    /** Record an attempt (correct or incorrect) for a chord, accumulating in memory for session details. */
    private void recordPracticeDetail(int chordId, boolean isCorrect) {
        PracticeDetail detail = detailMap.get(chordId);
        if (detail == null) {
            detail = new PracticeDetail(0, chordId, 0, 0, 0);
            detailMap.put(chordId, detail);
        }
        detail.totalAttempts++;
        if (isCorrect) detail.correctCount++;
        else detail.incorrectCount++;
    }

    /** Finish the practice session: calculate score, save results, and evaluate achievements. */
    private void finishSession() {
        int totalChords = chordSequence.size();
        int correctCount = 0;
        for (PracticeDetail d : detailMap.values()) {
            if (d.correctCount > 0) correctCount += d.correctCount; // each correctCount corresponds to one chord correctly hit
        }
        int incorrectMissing = totalChords - detailMap.size();
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
        uniqueUc.evaluate();
        if (score == 100) {
            perfectUc.evaluate();
        }
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

    /** Interface for delivering session events back to the ViewModel (or other UI logic). */
    public interface PracticeSessionCallbacks {
        void onChordIndexChanged(int index, @NonNull String nextChordName);
        void onChordDetectedCorrect(int index);
        void onProgressUpdated(int percent);
        void onSessionFinished(int finalScore);
        void onSpeedUnlocked(@NonNull String speedLabel);
    }
}
