package com.tuguitar.todoacorde.scales.domain;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.tuguitar.todoacorde.scales.data.NoteUtils;
import com.tuguitar.todoacorde.scales.data.PatternRepository;
import com.tuguitar.todoacorde.scales.data.ScaleFretNote;
import com.tuguitar.todoacorde.scales.data.ScaleUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.Executor;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class ScaleTrainerViewModel extends ViewModel {

    private final PatternRepository repository;
    private final Executor ioExecutor;

    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<String>  error   = new MutableLiveData<>(null);

    private final MutableLiveData<List<String>> scaleTypes     = new MutableLiveData<>(Collections.emptyList());
    private final MutableLiveData<List<String>> roots          = new MutableLiveData<>(Collections.emptyList());

    private final MutableLiveData<List<PatternRepository.PatternVariant>> variants =
            new MutableLiveData<>(Collections.emptyList());
    private final MutableLiveData<PatternRepository.PatternVariant> selectedVariant =
            new MutableLiveData<>(null);

    private final MutableLiveData<List<String>>        scaleNotes     = new MutableLiveData<>(Collections.emptyList());
    private final MutableLiveData<List<ScaleFretNote>> highlightPath  = new MutableLiveData<>(Collections.emptyList());
    private final MutableLiveData<Integer> currentIndex   = new MutableLiveData<>(-1);
    private final MutableLiveData<Integer> streak         = new MutableLiveData<>(0);
    private final MutableLiveData<Double>  progressPercent= new MutableLiveData<>(0d);

    public enum State { IDLE, RUNNING, COMPLETED }
    private final MutableLiveData<State> state = new MutableLiveData<>(State.IDLE);

    private volatile int preferredStartFret = 5, preferredEndFret = 9;

    private List<ScaleFretNote> patternNotesCache = new ArrayList<>();
    private List<String> expectedNoteSequence     = new ArrayList<>();
    private List<ScaleFretNote> plannedPath       = new ArrayList<>();

    @Inject
    public ScaleTrainerViewModel(@NonNull PatternRepository repository,
                                 @NonNull Executor ioExecutor) {
        this.repository = Objects.requireNonNull(repository);
        this.ioExecutor = Objects.requireNonNull(ioExecutor);
    }

    public LiveData<Boolean> getLoading() { return loading; }
    public LiveData<String>  getError()   { return error; }
    public LiveData<List<String>> getScaleTypes() { return scaleTypes; }
    public LiveData<List<String>> getRoots()      { return roots; }
    public LiveData<List<PatternRepository.PatternVariant>> getVariants() { return variants; }
    public LiveData<PatternRepository.PatternVariant> getSelectedVariant() { return selectedVariant; }
    public LiveData<List<String>> getScaleNotes()  { return scaleNotes; }
    public LiveData<List<ScaleFretNote>> getHighlightPath() { return highlightPath; }
    public LiveData<Integer> getCurrentIndex() { return currentIndex; }
    public LiveData<Integer> getStreak()       { return streak; }
    public LiveData<Double>  getProgressPercent() { return progressPercent; }
    public LiveData<State>   getState() { return state; }

    @MainThread
    public void setPreferredWindow(int startFret, int endFret) {
        if (startFret < 0) startFret = 0;
        if (endFret < startFret) endFret = startFret;
        this.preferredStartFret = startFret;
        this.preferredEndFret   = endFret;

        PatternRepository.PatternVariant current = selectedVariant.getValue();
        if (current != null) pickVariant(current.scaleType, current.rootNote);
    }

    @MainThread
    public void loadScaleTypes() {
        loading.setValue(true); error.setValue(null);
        ioExecutor.execute(() -> {
            try {
                List<String> types = repository.getAllScaleTypesDistinct();
                post(scaleTypes, nonNull(types));
            } catch (Throwable t) {
                post(error, safeMessage(t));
            } finally {
                post(loading, false);
            }
        });
    }

    @MainThread
    public void loadRootsForType(@Nullable String scaleType) {
        loading.setValue(true); error.setValue(null);
        final String st = trimOrNull(scaleType);
        ioExecutor.execute(() -> {
            try {
                List<String> r = (st == null) ? Collections.emptyList() : repository.getRootsForType(st);
                post(roots, nonNull(r));
            } catch (Throwable t) {
                post(error, safeMessage(t));
            } finally {
                post(loading, false);
            }
        });
    }

    @MainThread
    public void pickVariant(@Nullable String scaleType, @Nullable String rootNote) {
        loading.setValue(true); error.setValue(null);

        final String st = trimOrNull(scaleType);
        final String rn = normalizeRoot(rootNote);

        ioExecutor.execute(() -> {
            try {
                List<PatternRepository.PatternVariant> all = (st != null && rn != null)
                        ? repository.getVariantsByTypeAndRoot(st, rn)
                        : Collections.emptyList();
                post(variants, nonNull(all));

                PatternRepository.PatternVariant best = null;
                if (st != null && rn != null) {
                    best = repository.findNearestWindow(st, rn, preferredStartFret, preferredEndFret);
                    if (best == null && !all.isEmpty()) best = all.get(0);
                }
                applyVariant(best);

            } catch (Throwable t) {
                post(error, safeMessage(t));
                post(variants, Collections.emptyList());
                applyVariant(null);
            } finally {
                post(loading, false);
            }
        });
    }

    @MainThread
    public void loadVariants(@Nullable String scaleType, @Nullable String rootNote) {
        final String st = trimOrNull(scaleType);
        final String rn = normalizeRoot(rootNote);
        ioExecutor.execute(() -> {
            try {
                List<PatternRepository.PatternVariant> all = (st != null && rn != null)
                        ? repository.getVariantsByTypeAndRoot(st, rn)
                        : Collections.emptyList();
                post(variants, nonNull(all));
            } catch (Throwable t) {
                post(error, safeMessage(t));
            }
        });
    }

    @MainThread
    public void selectVariantByIndex(int index) {
        List<PatternRepository.PatternVariant> list = variants.getValue();
        if (list == null || list.isEmpty()) return;
        if (index < 0 || index >= list.size()) return;
        applyVariant(list.get(index));
    }

    @MainThread
    public void selectVariantById(long id) {
        List<PatternRepository.PatternVariant> list = variants.getValue();
        if (list == null || list.isEmpty()) return;
        for (PatternRepository.PatternVariant v : list) {
            if (v != null && v.id == id) {
                applyVariant(v);
                return;
            }
        }
    }

    @MainThread
    private void applyVariant(@Nullable PatternRepository.PatternVariant v) {
        post(selectedVariant, v);
        List<ScaleFretNote> notes = (v != null && v.notes != null) ? v.notes : Collections.emptyList();
        this.patternNotesCache = new ArrayList<>(notes);
        recomputePreviewFromBox();

        post(currentIndex, -1);
        post(streak, 0);
        post(progressPercent, 0d);
        post(state, State.IDLE);
    }

    @MainThread
    public void startScale(int rootMidi, @NonNull ScaleUtils.ScaleType type) {
        if (plannedPath == null || plannedPath.isEmpty()) {
            plannedPath = planBoxUpDown(patternNotesCache);
        }
        List<String> seq = namesFromPath(plannedPath);
        if (seq.isEmpty()) { post(error, "Patrón vacío"); return; }

        expectedNoteSequence = seq;
        post(scaleNotes, seq);
        post(currentIndex, 0);
        post(streak, 0);
        post(progressPercent, 0d);
        post(state, State.RUNNING);
        post(highlightPath, plannedPath);
    }

    @MainThread
    public void stopPractice() {
        post(state, State.IDLE);
        post(currentIndex, -1);
        post(streak, 0);
        post(progressPercent, 0d);
        recomputePreviewFromBox();
    }

    @MainThread
    public void onUserPlayedNote(@NonNull String playedNote, double centsOff) {
        if (state.getValue() != State.RUNNING) return;
        List<String> seq = scaleNotes.getValue();
        Integer idx = currentIndex.getValue();
        if (seq == null || seq.isEmpty() || idx == null || idx < 0) return;

        String expected = seq.get(idx);
        String played = NoteUtils.normalizeToSharp(NoteUtils.stripOctave(playedNote));

        if (NoteUtils.equalsEnharmonic(expected, played)) {
            int newIdx = idx + 1;
            post(currentIndex, newIdx);
            post(streak, (streak.getValue() == null ? 0 : streak.getValue()) + 1);
            double progress = (100.0 * newIdx) / seq.size();
            if (progress > 100.0) progress = 100.0;
            post(progressPercent, progress);
            if (newIdx >= seq.size()) post(state, State.COMPLETED);
        } else {
            post(streak, 0);
        }
    }

    // ---------- Preview desde la caja ----------
    private void recomputePreviewFromBox() {
        if (patternNotesCache == null || patternNotesCache.isEmpty()) {
            plannedPath = Collections.emptyList();
            post(highlightPath, plannedPath);
            post(scaleNotes, Collections.emptyList());
            post(currentIndex, -1);
            return;
        }
        plannedPath = planBoxUpDown(patternNotesCache);
        post(highlightPath, plannedPath);
        List<String> names = namesFromPath(plannedPath);
        expectedNoteSequence = names;
        post(scaleNotes, names);
        if (state.getValue() != State.RUNNING) post(currentIndex, -1);
    }

    @NonNull
    private static List<ScaleFretNote> planBoxUpDown(@NonNull List<ScaleFretNote> notesInBox) {
        if (notesInBox.isEmpty()) return Collections.emptyList();
        List<ScaleFretNote> up = new ArrayList<>(notesInBox);
        up.sort(Comparator.comparingInt(ScaleTrainerViewModel::approxMidi));
        List<ScaleFretNote> path = new ArrayList<>(up.size() * 2 - 1);
        path.addAll(up);
        for (int i = up.size() - 2; i >= 0; i--) path.add(up.get(i));
        return path;
    }

    private static int approxMidi(@NonNull ScaleFretNote n) {
        final int[] OPEN_MIDI = {40, 45, 50, 55, 59, 64}; // E2,A2,D3,G3,B3,E4
        int s = clamp(n.stringIndex, 0, 5);
        int f = Math.max(0, n.fret);
        return OPEN_MIDI[s] + f;
    }
    private static int clamp(int v, int lo, int hi) { return Math.max(lo, Math.min(hi, v)); }

    private static List<String> namesFromPath(@NonNull List<ScaleFretNote> path) {
        List<String> out = new ArrayList<>(path.size());
        for (ScaleFretNote n : path) {
            String name = (n != null && n.noteName != null) ? n.noteName : "";
            out.add(NoteUtils.normalizeToSharp(NoteUtils.stripOctave(name)));
        }
        return out;
    }

    // Utils
    private static String trimOrNull(@Nullable String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
    @Nullable
    private static String normalizeRoot(@Nullable String root) {
        if (root == null) return null;
        String t = root.trim();
        if (t.isEmpty()) return null;
        return t.toUpperCase(Locale.ROOT);
    }
    private static <T> List<T> nonNull(@Nullable List<T> in) {
        return in == null ? Collections.emptyList() : in;
    }
    private static <T> void post(MutableLiveData<T> live, T value) {
        if (live != null) live.postValue(value);
    }
    private static String safeMessage(Throwable t) {
        String m = t.getMessage();
        return (m == null || m.isEmpty()) ? t.getClass().getSimpleName() : m;
    }
}
