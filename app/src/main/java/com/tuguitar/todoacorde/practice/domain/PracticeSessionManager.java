package com.tuguitar.todoacorde.practice.domain;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import com.tuguitar.todoacorde.Chord;
import com.tuguitar.todoacorde.ChordDetectionListener;
import com.tuguitar.todoacorde.IChordDetector;
import com.tuguitar.todoacorde.achievements.domain.usecases.EvaluatePerfectScoreAchievementUseCase;
import com.tuguitar.todoacorde.achievements.domain.usecases.EvaluateSpeedUnlockAchievementUseCase;
import com.tuguitar.todoacorde.achievements.domain.usecases.EvaluateUniqueChordsAchievementUseCase;
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

    // Dependencies
    private final PracticeRepository repo;
    private final IChordDetector chordDetector;
    private final EvaluateUniqueChordsAchievementUseCase uniqueUc;
    private final EvaluateSpeedUnlockAchievementUseCase speedUc;
    private final EvaluatePerfectScoreAchievementUseCase perfectUc;
    private PracticeSessionCallbacks callbacks;

    // Internal state
    private boolean sessionRunning = false;
    private PracticeViewModel.Mode currentMode;
    private int currentSongId;
    private float currentSpeedFactor;
    private int currentIndex = 0;
    private boolean windowDetected = false;
    private final Map<Integer, PracticeDetail> detailMap = new HashMap<>();
    private final HashSet<String> unlockedSpeedNotifications = new HashSet<>();

    // Sequence of chord names for detection
    private final List<String> chordSequence = new ArrayList<>();

    // Handler and runnables for synchronized mode scheduling
    private final Handler uiHandler;
    private Runnable windowRunnable;
    private Runnable progressRunnable;

    // Timing for synchronized session
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
            // In free mode, start continuous chord detection
            chordDetector.startDetection(this);
        } else {
            Log.d(TAG, "Starting SYNCHRONIZED mode detection");
            sessionStartTime = System.currentTimeMillis();
            // Begin scheduled detection windows from the first chord
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
            // All chords processed, finish the session
            finishSession();
            return;
        }
        // Set up detection window for chord at the given index
        windowDetected = false;
        currentIndex = index;
        SongChordWithInfo info = repo.getChordsWithInfoForSong(currentSongId).getValue().get(index);
        long baseMs = info.songChord.getDuration() * 1000L;
        long durationMs = (long) (baseMs / (currentSpeedFactor != 0 ? currentSpeedFactor : 1.0));
        Log.d(TAG, "Window[" + index + "] duration = " + durationMs + "ms (base=" + baseMs + ", speedFactor=" + currentSpeedFactor + ")");

        // Start detecting for this chord window
        chordDetector.startDetection(this);
        startProgressTicker(durationMs);

        // Schedule end-of-window behavior
        windowRunnable = () -> {
            Log.d(TAG, "Window expired for index " + index);
            chordDetector.stopDetection();
            // If chord was not detected in time, record an incorrect attempt
            if (!windowDetected) {
                int chordId = info.songChord.getChordId();
                recordPracticeDetail(chordId, false);
            }
            // Move to the next chord in the sequence
            if (callbacks != null) {
                // Notify that we've advanced to the next chord index
                String nextChordName = (index + 1 < chordSequence.size()) ? chordSequence.get(index + 1) : "";
                callbacks.onChordIndexChanged(index + 1, nextChordName);
            }
            scheduleNextWindow(index + 1);
        };
        uiHandler.postDelayed(windowRunnable, durationMs);

        // Notify UI about current index change at the start of the window
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
        // Initialize progress to 0 and start ticking
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

        // When the detected chord matches the expected chord
        if (currentMode == PracticeViewModel.Mode.SYNCHRONIZED) {
            if (!windowDetected) {
                // In synchronized mode, count the first correct detection in the window
                Log.d(TAG, "SYNC mode: Correct chord detected at index " + currentIndex);
                windowDetected = true;
                // Mark chord as correctly detected
                if (callbacks != null) {
                    callbacks.onChordDetectedCorrect(currentIndex);
                }
                // Record successful attempt for this chord
                SongChordWithInfo info = repo.getChordsWithInfoForSong(currentSongId).getValue().get(currentIndex);
                recordPracticeDetail(info.songChord.getChordId(), true);
            }
            // (Do not advance index here; wait for window expiration to move to next chord)
        } else {
            // In free mode, move immediately to the next chord
            Log.d(TAG, "FREE mode: Correct chord detected at index " + currentIndex);
            if (callbacks != null) {
                callbacks.onChordDetectedCorrect(currentIndex);
            }
            int nextIndex = currentIndex + 1;
            if (nextIndex < chordSequence.size()) {
                // Notify index changed to next chord
                String nextChordName = chordSequence.get(nextIndex);
                if (callbacks != null) {
                    callbacks.onChordIndexChanged(nextIndex, nextChordName);
                }
            }
            currentIndex = nextIndex;
            // If reached end of sequence in free mode, we simply stop automatically
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
        // Calculate final score as percentage of correctly detected chords
        int totalChords = chordSequence.size();
        int correctCount = 0;
        // Count how many indices were marked correct
        for (PracticeDetail d : detailMap.values()) {
            if (d.correctCount > 0) correctCount += d.correctCount; // each correctCount corresponds to one chord correctly hit
        }
        // If detailMap missing some chords (not attempted), treat those as incorrect
        // (Note: detailMap contains only attempted chords, so missed chords are those not present)
        int incorrectMissing = totalChords - detailMap.size();
        int score = totalChords == 0 ? 0 : (int) Math.round(100.0 * (correctCount) / totalChords);
        Log.d(TAG, "Practice session finished: score=" + score);

        // Build PracticeSession entity
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
        // Save session and details to the repository (database)
        List<PracticeDetail> detailsList = new ArrayList<>(detailMap.values());
        repo.saveSessionWithDetails(session, detailsList);

        // Evaluate achievements
        uniqueUc.evaluate();
        if (score == 100) {
            perfectUc.evaluate();
        }
        if (score >= 80) {
            // Attempt to unlock the next speed level
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
                        // Notify that a new speed was unlocked and evaluate related achievement
                        if (callbacks != null) {
                            callbacks.onSpeedUnlocked(nextLabel);
                        }
                        speedUc.evaluate();
                    }
                }
            });
        }

        // Notify that session has finished with the final score
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
