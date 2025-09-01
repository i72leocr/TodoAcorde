package com.tuguitar.todoacorde.scales.domain;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.tuguitar.todoacorde.achievements.domain.usecase.EvaluateScaleMasteryAchievementUseCase;
import com.tuguitar.todoacorde.scales.data.NoteUtils;
import com.tuguitar.todoacorde.scales.data.PatternRepository;
import com.tuguitar.todoacorde.scales.data.ScaleFretNote;
import com.tuguitar.todoacorde.scales.data.entity.ScaleEntity;
import com.tuguitar.todoacorde.scales.domain.repository.ProgressionRepository;
import com.tuguitar.todoacorde.scales.domain.usecase.CompleteScaleBoxUseCase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.Executor;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class ScaleTrainerViewModel extends ViewModel {

    private static final String TAG = "ScaleVM";
    private static final boolean DEBUG_BYPASS_PROGRESSION = false;
    private final PatternRepository patternRepo;
    private final ProgressionRepository progressionRepo;
    private final CompleteScaleBoxUseCase completeScaleBox;
    private final Executor ioExecutor;
    private final EvaluateScaleMasteryAchievementUseCase evalScaleAchievements;
    private final ScaleProgressionCalculator progressionCalc;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private void emit(@NonNull UiState state) { mainHandler.post(() -> uiState.setValue(state)); }
    private void emitEffect(@NonNull String msg) { mainHandler.post(() -> effects.setValue(new OneShotEvent<>(msg))); }
    public enum PracticeState { IDLE, RUNNING, COMPLETED }

    public static final class UiState {
        public final boolean loading;
        public final String error;

        public final List<String> typesAllowed;   // EN
        public final List<String> roots;          // "C","C#",...
        public final List<String> variantLabels;  // "Caja x [s–e]"
        public final List<Long> variantIds;

        public final int selectedTypeIndex;
        public final int selectedRootIndex;
        public final int selectedVariantIndex;

        public final String title;
        public final List<String> scaleNotes;           // secuencia objetivo / preview
        public final List<ScaleFretNote> highlightPath; // path de la caja
        public final int currentIndex;
        public final int streak;
        public final double progressPercent;
        public final PracticeState state;
        public final List<ScaleProgressionCalculator.TierItem> easyItems;
        public final List<ScaleProgressionCalculator.TierItem> mediumItems;
        public final List<ScaleProgressionCalculator.TierItem> hardItems;

        public UiState(
                boolean loading, String error,
                List<String> typesAllowed, List<String> roots,
                List<String> variantLabels, List<Long> variantIds,
                int selectedTypeIndex, int selectedRootIndex, int selectedVariantIndex,
                String title,
                List<String> scaleNotes,
                List<ScaleFretNote> highlightPath,
                int currentIndex, int streak, double progressPercent, PracticeState state,
                List<ScaleProgressionCalculator.TierItem> easyItems,
                List<ScaleProgressionCalculator.TierItem> mediumItems,
                List<ScaleProgressionCalculator.TierItem> hardItems
        ) {
            this.loading = loading;
            this.error = error;
            this.typesAllowed = typesAllowed;
            this.roots = roots;
            this.variantLabels = variantLabels;
            this.variantIds = variantIds;
            this.selectedTypeIndex = selectedTypeIndex;
            this.selectedRootIndex = selectedRootIndex;
            this.selectedVariantIndex = selectedVariantIndex;
            this.title = title;
            this.scaleNotes = scaleNotes;
            this.highlightPath = highlightPath;
            this.currentIndex = currentIndex;
            this.streak = streak;
            this.progressPercent = progressPercent;
            this.state = state;
            this.easyItems = easyItems;
            this.mediumItems = mediumItems;
            this.hardItems = hardItems;
        }

        public static UiState empty() {
            return new UiState(
                    false, null,
                    Collections.emptyList(), Collections.emptyList(),
                    Collections.emptyList(), Collections.emptyList(),
                    -1, -1, -1,
                    "",
                    Collections.emptyList(), Collections.emptyList(),
                    -1, 0, 0d, PracticeState.IDLE,
                    Collections.emptyList(), Collections.emptyList(), Collections.emptyList()
            );
        }
    }

    /** One-shot event para toasts/snackbars. */
    public static final class OneShotEvent<T> {
        private final T content;
        private boolean handled = false;
        public OneShotEvent(T content) { this.content = content; }
        @Nullable public T consume() { if (handled) return null; handled = true; return content; }
        public T peek() { return content; }
    }

    private final MutableLiveData<UiState> uiState = new MutableLiveData<>(UiState.empty());
    private final MutableLiveData<OneShotEvent<String>> effects = new MutableLiveData<>();

    public LiveData<UiState> getUiState() { return uiState; }
    public LiveData<OneShotEvent<String>> getEffects() { return effects; }
    private volatile int preferredStartFret = 5, preferredEndFret = 9;
    private long currentUserId = 1L;

    private List<ScaleFretNote> patternNotesCache = new ArrayList<>();
    private List<ScaleFretNote> plannedPath       = new ArrayList<>();
    private String selectedTypeEn = null; // EN
    private String selectedRoot   = null; // "C", "C#", ...
    private int    selectedVariantIdx = -1;
    private volatile long selectionVersion = 0L;
    private List<String> allTypesEn = new ArrayList<>();
    private List<String> allowedTypesEn = new ArrayList<>();
    private List<String> currentRoots = new ArrayList<>();
    private List<PatternRepository.PatternVariant> variantsAll = new ArrayList<>();
    private List<PatternRepository.PatternVariant> variantsAllowed = new ArrayList<>();
    private List<Long> variantsAllowedIds = new ArrayList<>();
    private List<String> variantsAllowedLabels = new ArrayList<>();
    private volatile List<ScaleProgressionCalculator.TierItem> easyItemsCache   = Collections.emptyList();
    private volatile List<ScaleProgressionCalculator.TierItem> mediumItemsCache = Collections.emptyList();
    private volatile List<ScaleProgressionCalculator.TierItem> hardItemsCache   = Collections.emptyList();
    private int dbTierBase = 0;

    @Inject
    public ScaleTrainerViewModel(
            @NonNull PatternRepository patternRepository,
            @NonNull ProgressionRepository progressionRepository,
            @NonNull CompleteScaleBoxUseCase completeScaleBoxUseCase,
            @NonNull Executor ioExecutor,
            @NonNull EvaluateScaleMasteryAchievementUseCase evalScaleAchievements
    ) {
        this.patternRepo = Objects.requireNonNull(patternRepository);
        this.progressionRepo = Objects.requireNonNull(progressionRepository);
        this.completeScaleBox = Objects.requireNonNull(completeScaleBoxUseCase);
        this.ioExecutor = Objects.requireNonNull(ioExecutor);
        this.evalScaleAchievements = Objects.requireNonNull(evalScaleAchievements);
        this.progressionCalc = new ScaleProgressionCalculator(patternRepo, progressionRepo);
    }

    @MainThread
    public void setCurrentUserId(long userId) { this.currentUserId = userId > 0 ? userId : 1L; }

    @MainThread
    public void onInit() {
        Log.d(TAG, "onInit() -> start");
        final long v = ++selectionVersion;
        setLoading(true);
        ioExecutor.execute(() -> {
            try {
                progressionRepo.warmUpCaches();
                detectDbTierBase();

                allTypesEn = nonNull(patternRepo.getAllScaleTypesDistinct());
                if (v != selectionVersion) return;
                Log.d(TAG, "onInit() -> allTypesEn size=" + allTypesEn.size() + " => " + join(allTypesEn));

                if (!allTypesEn.isEmpty()) {
                    selectedTypeEn = allTypesEn.get(0);
                    currentRoots = nonNull(patternRepo.getRootsForType(selectedTypeEn));
                    if (v != selectionVersion) return;
                    Log.d(TAG, "onInit() -> roots for " + selectedTypeEn + " size=" + currentRoots.size() + " => " + join(currentRoots));

                    if (!currentRoots.isEmpty()) {
                        int idx = indexOf(currentRoots, "E");
                        if (idx < 0) idx = indexOf(currentRoots, "A");
                        selectedRoot = currentRoots.get(Math.max(0, idx));
                        Log.d(TAG, "onInit() -> selectedRoot=" + selectedRoot);
                    }

                    recomputeAllowedTypesForCurrentRoot(v);

                    if (!allowedTypesEn.isEmpty() && (selectedTypeEn == null || indexOf(allowedTypesEn, selectedTypeEn) < 0)) {
                        selectedTypeEn = allowedTypesEn.get(0);
                        currentRoots = nonNull(patternRepo.getRootsForType(selectedTypeEn));
                        Log.d(TAG, "onInit() -> adjusted selectedTypeEn=" + selectedTypeEn + " roots=" + join(currentRoots));
                    }

                    loadVariantsForCurrentSelection(v);
                    recomputeTierSectionCaches(v);
                } else {
                    Log.w(TAG, "onInit() -> allTypesEn vacío.");
                    easyItemsCache = mediumItemsCache = hardItemsCache = Collections.emptyList();
                }

                if (v != selectionVersion) return;
                pushUiPreviewIdle();

            } catch (Throwable t) {
                if (v != selectionVersion) return;
                Log.e(TAG, "onInit() error", t);
                setError(safeMessage(t));
            } finally {
                if (v == selectionVersion) setLoading(false);
            }
        });
    }

    @MainThread
    public void onTypeSelected(@Nullable String typeEn) {
        Log.d(TAG, "onTypeSelected(" + typeEn + ")");
        if (typeEn == null || (selectedTypeEn != null && typeEn.equalsIgnoreCase(selectedTypeEn))) return;
        selectedTypeEn = typeEn;
        final long v = ++selectionVersion;
        plannedPath = new ArrayList<>();
        setLoading(true);
        ioExecutor.execute(() -> {
            try {
                currentRoots = nonNull(patternRepo.getRootsForType(selectedTypeEn));
                if (v != selectionVersion) return;
                Log.d(TAG, "onTypeSelected -> roots size=" + currentRoots.size() + " => " + join(currentRoots));

                if (selectedRoot == null || indexOf(currentRoots, selectedRoot) < 0) {
                    selectedRoot = currentRoots.isEmpty() ? null : currentRoots.get(0);
                }
                Log.d(TAG, "onTypeSelected -> selectedRoot=" + selectedRoot);

                recomputeAllowedTypesForCurrentRoot(v);

                if (!allowedTypesEn.isEmpty() && indexOf(allowedTypesEn, selectedTypeEn) < 0) {
                    selectedTypeEn = allowedTypesEn.get(0);
                    currentRoots = nonNull(patternRepo.getRootsForType(selectedTypeEn));
                    Log.d(TAG, "onTypeSelected -> adjusted selectedTypeEn=" + selectedTypeEn + " roots=" + join(currentRoots));
                }

                loadVariantsForCurrentSelection(v);
                recomputeTierSectionCaches(v);

                if (v != selectionVersion) return;
                pushUiPreviewIdle();
            } catch (Throwable t) {
                if (v != selectionVersion) return;
                Log.e(TAG, "onTypeSelected error", t);
                setError(safeMessage(t));
            } finally {
                if (v == selectionVersion) setLoading(false);
            }
        });
    }

    @MainThread
    public void onRootSelected(@Nullable String root) {
        Log.d(TAG, "onRootSelected(" + root + ")");
        if (root == null || (selectedRoot != null && root.equalsIgnoreCase(selectedRoot))) return;
        selectedRoot = root;
        final long v = ++selectionVersion;
        plannedPath = new ArrayList<>();
        setLoading(true);
        ioExecutor.execute(() -> {
            try {
                recomputeAllowedTypesForCurrentRoot(v);
                if (v != selectionVersion) return;

                if (!allowedTypesEn.isEmpty() && indexOf(allowedTypesEn, selectedTypeEn) < 0) {
                    selectedTypeEn = allowedTypesEn.get(0);
                    currentRoots = nonNull(patternRepo.getRootsForType(selectedTypeEn));
                    Log.d(TAG, "onRootSelected -> adjusted selectedTypeEn=" + selectedTypeEn + " roots=" + join(currentRoots));
                }

                loadVariantsForCurrentSelection(v);
                recomputeTierSectionCaches(v);

                if (v != selectionVersion) return;
                pushUiPreviewIdle();
            } catch (Throwable t) {
                if (v != selectionVersion) return;
                Log.e(TAG, "onRootSelected error", t);
                setError(safeMessage(t));
            } finally {
                if (v == selectionVersion) setLoading(false);
            }
        });
    }

    @MainThread
    public void onVariantSelected(int allowedIndex) {
        Log.d(TAG, "onVariantSelected idx=" + allowedIndex + " of " + variantsAllowed.size());
        if (allowedIndex < 0 || allowedIndex >= variantsAllowed.size()) return;
        selectedVariantIdx = allowedIndex;
        applyVariant(variantsAllowed.get(allowedIndex));
        pushUiPreviewIdle();
    }

    @MainThread
    public void onStartClicked() {
        Log.d(TAG, "onStartClicked()");
        if (selectedTypeEn == null || selectedRoot == null || selectedVariantIdx < 0) {
            Log.w(TAG, "onStartClicked abort: selection invalid. type=" + selectedTypeEn + " root=" + selectedRoot + " varIdx=" + selectedVariantIdx);
            return;
        }
        if (plannedPath == null || plannedPath.isEmpty()) {
            plannedPath = planBoxUpDown(patternNotesCache);
            Log.d(TAG, "onStartClicked -> plannedPath built, size=" + plannedPath.size());
        }
        if (plannedPath.isEmpty()) { setError("Patrón vacío"); return; }

        List<String> seq = namesFromPath(plannedPath);
        Log.d(TAG, "onStartClicked -> seq size=" + seq.size() + " => " + join(seq));

        UiState s = uiState.getValue();
        if (s == null) s = UiState.empty();
        emit(new UiState(
                s.loading, null,
                s.typesAllowed, s.roots,
                s.variantLabels, s.variantIds,
                s.selectedTypeIndex, s.selectedRootIndex, s.selectedVariantIndex,
                s.title,
                seq,
                plannedPath,
                0, 0, 0d, PracticeState.RUNNING,
                s.easyItems, s.mediumItems, s.hardItems
        ));
    }

    @MainThread
    public void onStopClicked() {
        Log.d(TAG, "onStopClicked()");
        UiState s = uiState.getValue();
        if (s == null) s = UiState.empty();
        emit(new UiState(
                s.loading, null,
                s.typesAllowed, s.roots,
                s.variantLabels, s.variantIds,
                s.selectedTypeIndex, s.selectedRootIndex, s.selectedVariantIndex,
                s.title,
                Collections.emptyList(),
                plannedPath,
                -1, 0, 0d, PracticeState.IDLE,
                s.easyItems, s.mediumItems, s.hardItems
        ));
    }

    @MainThread
    public void onNoteDetected(@NonNull String playedNote, double centsOff) {
        UiState s = uiState.getValue();
        if (s == null || s.state != PracticeState.RUNNING) return;
        List<String> seq = s.scaleNotes;
        if (seq == null || seq.isEmpty()) return;
        int idx = Math.max(0, s.currentIndex);
        if (idx >= seq.size()) return;

        String expected = NoteUtils.normalizeToSharp(NoteUtils.stripOctave(seq.get(idx)));
        String played = NoteUtils.normalizeToSharp(NoteUtils.stripOctave(playedNote));

        Log.d(TAG, "onNoteDetected expected=" + expected + " played=" + played + " cents=" + centsOff);

        if (NoteUtils.equalsEnharmonic(expected, played)) {
            int newIdx = idx + 1;
            double progress = Math.min(100.0, (100.0 * newIdx) / seq.size());
            UiState updated = new UiState(
                    s.loading, null,
                    s.typesAllowed, s.roots,
                    s.variantLabels, s.variantIds,
                    s.selectedTypeIndex, s.selectedRootIndex, s.selectedVariantIndex,
                    s.title,
                    s.scaleNotes,
                    s.highlightPath,
                    newIdx, s.streak + 1, progress,
                    newIdx >= seq.size() ? PracticeState.COMPLETED : PracticeState.RUNNING,
                    s.easyItems, s.mediumItems, s.hardItems
            );
            emit(updated);

            if (updated.state == PracticeState.COMPLETED) {
                Log.d(TAG, "onNoteDetected -> COMPLETED");
                persistCompletionAndRefresh();
            }
        } else {
            UiState updated = new UiState(
                    s.loading, null,
                    s.typesAllowed, s.roots,
                    s.variantLabels, s.variantIds,
                    s.selectedTypeIndex, s.selectedRootIndex, s.selectedVariantIndex,
                    s.title,
                    s.scaleNotes,
                    s.highlightPath,
                    idx, 0, s.progressPercent,
                    s.state,
                    s.easyItems, s.mediumItems, s.hardItems
            );
            emit(updated);
        }
    }

    @MainThread
    public void setPreferredWindow(int startFret, int endFret) {
        Log.d(TAG, "setPreferredWindow " + startFret + "-" + endFret);
        if (startFret < 0) startFret = 0;
        if (endFret < startFret) endFret = startFret;
        this.preferredStartFret = startFret;
        this.preferredEndFret   = endFret;

        if (selectedTypeEn != null && selectedRoot != null) {
            final long v = ++selectionVersion;
            setLoading(true);
            ioExecutor.execute(() -> {
                try {
                    loadVariantsForCurrentSelection(v);
                    recomputeTierSectionCaches(v);
                    if (v != selectionVersion) return;
                    pushUiPreviewIdle();
                } catch (Throwable t) {
                    if (v != selectionVersion) return;
                    Log.e(TAG, "setPreferredWindow error", t);
                    setError(safeMessage(t));
                } finally {
                    if (v == selectionVersion) setLoading(false);
                }
            });
        }
    }

    private void recomputeAllowedTypesForCurrentRoot(long v) {
        if (selectedRoot == null) {
            allowedTypesEn = new ArrayList<>(allTypesEn);
            Log.w(TAG, "recomputeAllowedTypesForCurrentRoot -> selectedRoot=null, mostrando todos (" + allowedTypesEn.size() + ")");
            return;
        }

        if (DEBUG_BYPASS_PROGRESSION) {
            allowedTypesEn = new ArrayList<>();
            for (String en : allTypesEn) {
                List<String> rootsForType = nonNull(patternRepo.getRootsForType(en));
                if (indexOf(rootsForType, selectedRoot) >= 0) {
                    allowedTypesEn.add(en);
                }
            }
            Log.w(TAG, "recomputeAllowedTypesForCurrentRoot -> BYPASS, types with patterns for " + selectedRoot + " = " + allowedTypesEn.size());
            return;
        }

        long tonalityId = progressionRepo.findTonalityIdByName(selectedRoot);
        if (tonalityId <= 0) {
            Log.w(TAG, "recomputeAllowedTypesForCurrentRoot -> tonalityId not found for root=" + selectedRoot + ". No gating.");
            allowedTypesEn = new ArrayList<>(allTypesEn);
            return;
        }

        int maxUnlockedTier = computeMaxCompletedTierForRoot_OFFSET(currentUserId, tonalityId, selectedRoot);
        Log.d(TAG, "recomputeAllowedTypesForCurrentRoot -> root=" + selectedRoot
                + " tonalityId=" + tonalityId + " maxUnlockedTier(logical)=" + maxUnlockedTier + " dbTierBase=" + dbTierBase);

        HashSet<String> allowedEs = new HashSet<>();
        for (int logical = 0; logical <= maxUnlockedTier; logical++) {
            int physical = logical + dbTierBase;
            List<ScaleEntity> tierScales = progressionRepo.getScalesByTier(physical);
            if (tierScales != null) {
                for (ScaleEntity s : tierScales) {
                    allowedEs.add(s.name);
                }
            }
        }
        Log.d(TAG, "recomputeAllowedTypesForCurrentRoot -> allowed ES size=" + allowedEs.size() + " => " + allowedEs);

        List<String> out = new ArrayList<>();
        for (String enRaw : allTypesEn) {
            String en = ScaleTypeMapper.normalizeEnglishTypeAlias(enRaw);
            String es = ScaleTypeMapper.mapEnglishTypeToDbName(en);
            if (!allowedEs.contains(es)) continue;

            List<String> rootsForType = nonNull(patternRepo.getRootsForType(en));
            if (indexOf(rootsForType, selectedRoot) >= 0) out.add(en);
        }

        allowedTypesEn = out;
        Log.d(TAG, "recomputeAllowedTypesForCurrentRoot -> result size=" + allowedTypesEn.size() + " => " + join(allowedTypesEn));
    }

    /** Versión con guardas por versión para evitar pisados de estado. (IO) */
    private void loadVariantsForCurrentSelection(long v) {
        Log.d(TAG, "loadVariantsForCurrentSelection(type=" + selectedTypeEn + ", root=" + selectedRoot + ", v=" + v + ")");
        List<PatternRepository.PatternVariant> all = (selectedTypeEn != null && selectedRoot != null)
                ? nonNull(patternRepo.getVariantsByTypeAndRoot(ScaleTypeMapper.normalizeEnglishTypeAlias(selectedTypeEn), selectedRoot))
                : Collections.emptyList();
        if (v != selectionVersion) return;

        variantsAll = all;
        Log.d(TAG, "variantsAll size=" + variantsAll.size());

        int permitted = DEBUG_BYPASS_PROGRESSION
                ? variantsAll.size()
                : computeAllowedVariantCount(selectedTypeEn, selectedRoot, variantsAll.size());
        if (permitted < 0) permitted = 0;
        Log.d(TAG, "permitted variants=" + permitted + " (max=" + variantsAll.size() + ")");
        if (v != selectionVersion) return;

        List<PatternRepository.PatternVariant> allowed = new ArrayList<>();
        List<Long> ids = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        for (int i = 0; i < variantsAll.size() && i < permitted; i++) {
            PatternRepository.PatternVariant pv = variantsAll.get(i);
            allowed.add(pv);
            ids.add(pv.id);
            String base = (pv.name != null && !pv.name.trim().isEmpty()) ? pv.name : ("Caja " + (i + 1));
            labels.add(base + "  [" + pv.startFret + "–" + pv.endFret + "]");
        }
        if (v != selectionVersion) return;

        variantsAllowed = allowed;
        variantsAllowedIds = ids;
        variantsAllowedLabels = labels;

        Log.d(TAG, "variantsAllowed size=" + variantsAllowed.size() + " labels=" + join(variantsAllowedLabels));

        PatternRepository.PatternVariant best = null;
        int newIdx = -1;

        if (!variantsAllowed.isEmpty()) {
            PatternRepository.PatternVariant nearest =
                    patternRepo.findNearestWindow(ScaleTypeMapper.normalizeEnglishTypeAlias(selectedTypeEn), selectedRoot, preferredStartFret, preferredEndFret);
            if (v != selectionVersion) return;

            Log.d(TAG, "nearestWindow -> " + (nearest==null ? "null" : ("id="+nearest.id+" ["+nearest.startFret+"-"+nearest.endFret+"]")));
            if (nearest != null) {
                int idx = indexOfVariantId(variantsAllowed, nearest.id);
                if (idx >= 0) {
                    best = variantsAllowed.get(idx);
                    newIdx = idx;
                }
            }
            if (best == null) {
                newIdx = Math.max(0, variantsAllowed.size() - 1);
                best = variantsAllowed.get(newIdx);
            }
        }

        selectedVariantIdx = newIdx;
        if (v != selectionVersion) return;

        applyVariant(best);
    }

    private void applyVariant(@Nullable PatternRepository.PatternVariant v) {
        patternNotesCache = (v != null && v.notes != null) ? new ArrayList<>(v.notes) : new ArrayList<>();
        plannedPath = recomputePreviewFromBox(patternNotesCache);
        Log.d(TAG, "applyVariant -> notesCache=" + patternNotesCache.size() + " plannedPath=" + plannedPath.size());
    }

    private void persistCompletionAndRefresh() {
        final long v = selectionVersion;
        ioExecutor.execute(() -> {
            try {
                if (selectedTypeEn == null || selectedRoot == null || selectedVariantIdx < 0) return;

                String esName = ScaleTypeMapper.mapEnglishTypeToDbName(selectedTypeEn);
                long scaleId = progressionRepo.findScaleIdByName(esName);
                long tonalityId = progressionRepo.findTonalityIdByName(selectedRoot);
                Log.d(TAG, "persistCompletion -> esName=" + esName + " scaleId=" + scaleId + " tonalityId=" + tonalityId);
                if (scaleId <= 0 || tonalityId <= 0) {
                    emitEffect("No se pudo guardar el progreso (mapeo escala/tonalidad).");
                    return;
                }

                final int availableInTonality =
                        nonNull(patternRepo.getVariantsByTypeAndRoot(ScaleTypeMapper.normalizeEnglishTypeAlias(selectedTypeEn), selectedRoot)).size();
                final int beforeMaxCompleted =
                        progressionRepo.getMaxCompletedBoxOrZero(currentUserId, scaleId, tonalityId);
                final int beforeMaxCompletedTier =
                        computeMaxCompletedTierForRoot_OFFSET(currentUserId, tonalityId, selectedRoot);

                int boxOrder = selectedVariantIdx + 1;
                int tier = progressionRepo.findScaleTierById(scaleId); // físico (ya contiene base de DB)
                long now = System.currentTimeMillis();

                CompleteScaleBoxUseCase.Result result =
                        completeScaleBox.execute(currentUserId, scaleId, tonalityId, boxOrder, tier, now);
                Log.d(TAG, "persistCompletion -> result next=" + result.nextBoxOrder
                        + " scaleAll=" + result.scaleCompletedAllTonalities
                        + " tierCompleted=" + result.tierCompleted);

                final int afterMaxCompleted =
                        progressionRepo.getMaxCompletedBoxOrZero(currentUserId, scaleId, tonalityId);
                final int afterMaxCompletedTier =
                        computeMaxCompletedTierForRoot_OFFSET(currentUserId, tonalityId, selectedRoot);

                int disponibles = availableInTonality;

                if (result.nextBoxOrder != null && result.nextBoxOrder <= disponibles) {
                    emitEffect("¡Caja " + result.nextBoxOrder + " desbloqueada!");
                }

                if (result.scaleCompletedAllTonalities) {
                    emitEffect("¡Escala " + esName + " completada en todas las tonalidades!");
                }

                if (afterMaxCompletedTier > beforeMaxCompletedTier) {
                    int unlockedTier = afterMaxCompletedTier + 1;
                    boolean existsNextTier = hasAnyScaleInPhysicalTier(unlockedTier + dbTierBase);
                    if (existsNextTier) {
                        emitEffect("¡Has desbloqueado las escalas de dificultad " + tierNameEs(unlockedTier) + "!");
                    }
                }

                try {
                    boolean justCompletedForTonality = (beforeMaxCompleted < disponibles) && (afterMaxCompleted >= disponibles);
                    boolean justCompletedAllTonalities = result.scaleCompletedAllTonalities;
                    evalScaleAchievements.onScaleCompletionEvaluated(
                            currentUserId, scaleId, tonalityId,
                            justCompletedForTonality, justCompletedAllTonalities
                    );
                } catch (Throwable achEx) {
                    Log.e(TAG, "Error evaluando logros de escalas: " + achEx.getMessage(), achEx);
                }
                recomputeAllowedTypesForCurrentRoot(v);
                if (v != selectionVersion) return;
                loadVariantsForCurrentSelection(v);
                if (v != selectionVersion) return;
                recomputeTierSectionCaches(v);
                if (v != selectionVersion) return;
                UiState s = uiState.getValue();
                if (s == null) s = UiState.empty();
                pushUiWith(PracticeState.COMPLETED, s.currentIndex, s.streak, 100.0);

            } catch (Throwable t) {
                Log.e(TAG, "persistCompletionAndRefresh error", t);
                emitEffect("Error guardando progreso: " + safeMessage(t));
            }
        });
    }

    private int computeAllowedVariantCount(@Nullable String typeEn, @Nullable String root, int availableVariants) {
        if (typeEn == null || root == null || availableVariants <= 0) return 0;

        String esName = ScaleTypeMapper.mapEnglishTypeToDbName(typeEn);
        long scaleId = progressionRepo.findScaleIdByName(esName);
        long tonalityId = progressionRepo.findTonalityIdByName(root);
        Log.d(TAG, "computeAllowedVariantCount -> typeEn=" + typeEn + " (ES=" + esName + ") scaleId=" + scaleId + " tonalityId=" + tonalityId + " available=" + availableVariants);

        if (scaleId <= 0 || tonalityId <= 0) {
            Log.w(TAG, "computeAllowedVariantCount -> IDs no encontrados, devolviendo 1 como mínimo.");
            return Math.min(1, availableVariants);
        }

        int maxCompleted = progressionRepo.getMaxCompletedBoxOrZero(currentUserId, scaleId, tonalityId);
        int next = Math.max(1, maxCompleted + 1);
        int result = Math.min(next, availableVariants);

        Log.d(TAG, "computeAllowedVariantCount -> maxCompleted=" + maxCompleted + " available=" + availableVariants + " next=" + next + " => result=" + result);
        return result;
    }

    private void pushUiPreviewIdle() {
        int typeIdx = indexOf(allowedTypesEn, selectedTypeEn);
        int rootIdx = indexOf(currentRoots, selectedRoot);

        String title = (selectedTypeEn != null && selectedRoot != null)
                ? (selectedRoot + " " + selectedTypeEn) : "";

        List<String> seqPreview = namesFromPath(plannedPath);

        List<ScaleProgressionCalculator.TierItem> easy   = easyItemsCache != null ? new ArrayList<>(easyItemsCache) : Collections.emptyList();
        List<ScaleProgressionCalculator.TierItem> medium = mediumItemsCache != null ? new ArrayList<>(mediumItemsCache) : Collections.emptyList();
        List<ScaleProgressionCalculator.TierItem> hard   = hardItemsCache != null ? new ArrayList<>(hardItemsCache) : Collections.emptyList();

        Log.d(TAG, "pushUiPreviewIdle -> UI lists sizes: easy=" + easy.size() + " medium=" + medium.size() + " hard=" + hard.size());
        dumpTierItems("EASY", easy);
        dumpTierItems("MEDIUM", medium);
        dumpTierItems("HARD", hard);

        emit(new UiState(
                false, null,
                new ArrayList<>(allowedTypesEn),
                new ArrayList<>(currentRoots),
                new ArrayList<>(variantsAllowedLabels),
                new ArrayList<>(variantsAllowedIds),
                typeIdx, rootIdx, selectedVariantIdx,
                title,
                seqPreview,
                new ArrayList<>(plannedPath),
                -1,
                0,
                0d,
                PracticeState.IDLE,
                easy, medium, hard
        ));
    }

    private void pushUiWith(PracticeState state, int currentIndex, int streak, double progress) {
        UiState old = uiState.getValue();
        if (old == null) old = UiState.empty();
        emit(new UiState(
                old.loading, null,
                new ArrayList<>(allowedTypesEn),
                new ArrayList<>(currentRoots),
                new ArrayList<>(variantsAllowedLabels),
                new ArrayList<>(variantsAllowedIds),
                indexOf(allowedTypesEn, selectedTypeEn),
                indexOf(currentRoots, selectedRoot),
                selectedVariantIdx,
                (selectedTypeEn != null && selectedRoot != null) ? (selectedRoot + " " + selectedTypeEn) : "",
                old.scaleNotes,
                new ArrayList<>(plannedPath),
                currentIndex, streak, progress, state,
                easyItemsCache != null ? new ArrayList<>(easyItemsCache) : Collections.emptyList(),
                mediumItemsCache != null ? new ArrayList<>(mediumItemsCache) : Collections.emptyList(),
                hardItemsCache != null ? new ArrayList<>(hardItemsCache) : Collections.emptyList()
        ));
    }

    private void setLoading(boolean val) {
        String title = (selectedTypeEn != null && selectedRoot != null)
                ? (selectedRoot + " " + selectedTypeEn) : "";

        UiState old = uiState.getValue();
        PracticeState state = (old != null) ? old.state : PracticeState.IDLE;

        List<String> seqPreview = namesFromPath(plannedPath);
        List<String> scaleNotes = (old != null && old.state == PracticeState.RUNNING)
                ? old.scaleNotes
                : seqPreview;

        int currentIndex = (old != null) ? old.currentIndex : -1;
        int streak = (old != null) ? old.streak : 0;
        double progress = (old != null) ? old.progressPercent : 0d;

        emit(new UiState(
                val, null,
                new ArrayList<>(allowedTypesEn),
                new ArrayList<>(currentRoots),
                new ArrayList<>(variantsAllowedLabels),
                new ArrayList<>(variantsAllowedIds),
                indexOf(allowedTypesEn, selectedTypeEn),
                indexOf(currentRoots, selectedRoot),
                selectedVariantIdx,
                title,
                scaleNotes,
                new ArrayList<>(plannedPath),
                currentIndex, streak, progress, state,
                easyItemsCache != null ? new ArrayList<>(easyItemsCache) : Collections.emptyList(),
                mediumItemsCache != null ? new ArrayList<>(mediumItemsCache) : Collections.emptyList(),
                hardItemsCache != null ? new ArrayList<>(hardItemsCache) : Collections.emptyList()
        ));
    }

    private void setError(String msg) {
        String title = (selectedTypeEn != null && selectedRoot != null)
                ? (selectedRoot + " " + selectedTypeEn) : "";

        UiState old = uiState.getValue();
        PracticeState state = (old != null) ? old.state : PracticeState.IDLE;

        List<String> seqPreview = namesFromPath(plannedPath);
        List<String> scaleNotes = (old != null && old.state == PracticeState.RUNNING)
                ? old.scaleNotes
                : seqPreview;

        int currentIndex = (old != null) ? old.currentIndex : -1;
        int streak = (old != null) ? old.streak : 0;
        double progress = (old != null) ? old.progressPercent : 0d;

        emit(new UiState(
                false, msg,
                new ArrayList<>(allowedTypesEn),
                new ArrayList<>(currentRoots),
                new ArrayList<>(variantsAllowedLabels),
                new ArrayList<>(variantsAllowedIds),
                indexOf(allowedTypesEn, selectedTypeEn),
                indexOf(currentRoots, selectedRoot),
                selectedVariantIdx,
                title,
                scaleNotes,
                new ArrayList<>(plannedPath),
                currentIndex, streak, progress, state,
                easyItemsCache != null ? new ArrayList<>(easyItemsCache) : Collections.emptyList(),
                mediumItemsCache != null ? new ArrayList<>(mediumItemsCache) : Collections.emptyList(),
                hardItemsCache != null ? new ArrayList<>(hardItemsCache) : Collections.emptyList()
        ));
    }

    private static List<ScaleFretNote> planBoxUpDown(@NonNull List<ScaleFretNote> notesInBox) {
        return recomputePreviewFromBox(notesInBox);
    }

    private static List<ScaleFretNote> recomputePreviewFromBox(List<ScaleFretNote> notesInBox) {
        if (notesInBox == null || notesInBox.isEmpty()) return Collections.emptyList();
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

    /** Nombre legible del tier (el que se DESBLOQUEA). */
    private String tierNameEs(int tier) {
        switch (tier) {
            case 0: return "FÁCIL";
            case 1: return "MEDIA";
            case 2: return "DIFÍCIL";
            default: return "";
        }
    }

    /**
     * Recalcula en IO las listas por tier para la root seleccionada,
     * con fallback cuando no hay tonalidad en BD (muestra listas con progreso=0),
     * y mapeando a la base física de la BD.
     */
    private void recomputeTierSectionCaches(long v) {
        if (v != selectionVersion) return;
        String root = selectedRoot;
        if (root == null) {
            easyItemsCache = mediumItemsCache = hardItemsCache = Collections.emptyList();
            return;
        }
        long tonalityId = progressionRepo.findTonalityIdByName(root);

        int maxCompletedTier;
        if (tonalityId <= 0) {
            Log.w(TAG, "recomputeTierSectionCaches -> tonalityId no encontrado para root=" + root + ". Fallback sin gating (listas visibles, progreso=0).");
            maxCompletedTier = -1; // nada completado
        } else {
            maxCompletedTier = computeMaxCompletedTierForRoot_OFFSET(currentUserId, tonalityId, root);
        }

        boolean easyUnlocked   = true;
        boolean mediumUnlocked = (maxCompletedTier >= 0);
        boolean hardUnlocked   = (maxCompletedTier >= 1);
        easyItemsCache   = progressionCalc.buildTierItemsFor(0 + dbTierBase, root, tonalityId, easyUnlocked,   currentUserId);
        mediumItemsCache = progressionCalc.buildTierItemsFor(1 + dbTierBase, root, tonalityId, mediumUnlocked, currentUserId);
        hardItemsCache   = progressionCalc.buildTierItemsFor(2 + dbTierBase, root, tonalityId, hardUnlocked,   currentUserId);
        int catT0 = safeSize(progressionRepo.getScalesByTier(0 + dbTierBase));
        int catT1 = safeSize(progressionRepo.getScalesByTier(1 + dbTierBase));
        int catT2 = safeSize(progressionRepo.getScalesByTier(2 + dbTierBase));
        Log.d(TAG, "CAT Tier counts (physical) -> base=" + dbTierBase + " T0=" + catT0 + " T1=" + catT1 + " T2=" + catT2);

        int v0 = countVariantsForTierRoot(0 + dbTierBase, root);
        int v1 = countVariantsForTierRoot(1 + dbTierBase, root);
        int v2 = countVariantsForTierRoot(2 + dbTierBase, root);
        Log.d(TAG, "PATTERN variants for root " + root + " (physical tiers) -> T0=" + v0 + " T1=" + v1 + " T2=" + v2);

        dumpTierItems("EASY(build)", easyItemsCache);
        dumpTierItems("MEDIUM(build)", mediumItemsCache);
        dumpTierItems("HARD(build)", hardItemsCache);
    }

    /** Detecta si la BD está 0-based (0/1/2) o 1-based (1/2/3). */
    private void detectDbTierBase() {
        boolean has0 = hasAnyScaleInPhysicalTier(0);
        boolean has1 = hasAnyScaleInPhysicalTier(1);
        if (!has0 && has1) {
            dbTierBase = 1;
        } else {
            dbTierBase = 0;
        }
        Log.d(TAG, "detectDbTierBase -> hasT0=" + has0 + " hasT1=" + has1 + " => dbTierBase=" + dbTierBase);
    }

    private boolean hasAnyScaleInPhysicalTier(int physicalTier) {
        try {
            return progressionRepo.hasAnyScaleInTier(physicalTier);
        } catch (Throwable t) {
            Log.e(TAG, "hasAnyScaleInPhysicalTier(" + physicalTier + ") error: " + t.getMessage(), t);
            return false;
        }
    }

    /** Igual que ScaleProgressionCalculator.computeMaxCompletedTierForRoot pero con offset de BD. */
    private int computeMaxCompletedTierForRoot_OFFSET(long userId, long tonalityId, @NonNull String root) {
        int logical = 0, lastCompleted = -1;
        while (hasAnyScaleInPhysicalTier(logical + dbTierBase) &&
                progressionCalc.isTierCompletedForRoot(userId, logical + dbTierBase, tonalityId, root)) {
            lastCompleted = logical;
            logical++;
        }
        return lastCompleted;
    }
    private static <T> List<T> nonNull(@Nullable List<T> in) { return in == null ? Collections.emptyList() : in; }
    private static int indexOf(List<String> list, @Nullable String value) {
        if (list == null || value == null) return -1;
        for (int i = 0; i < list.size(); i++) if (value.equalsIgnoreCase(list.get(i))) return i;
        return -1;
    }
    private static int indexOfVariantId(List<PatternRepository.PatternVariant> list, long id) {
        if (list == null) return -1;
        for (int i = 0; i < list.size(); i++) if (list.get(i) != null && list.get(i).id == id) return i;
        return -1;
    }
    private static String safeMessage(Throwable t) {
        String m = t.getMessage();
        return (m == null || m.isEmpty()) ? t.getClass().getSimpleName() : m;
    }
    private static String join(List<?> l) {
        if (l == null) return "[]";
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < l.size(); i++) {
            if (i>0) sb.append(", ");
            Object x = l.get(i);
            sb.append(x == null ? "null" : String.valueOf(x));
        }
        sb.append("]");
        return sb.toString();
    }

    private int safeSize(@Nullable List<?> list) { return list == null ? 0 : list.size(); }

    private int countVariantsForTierRoot(int physicalTier, @NonNull String root) {
        int total = 0;
        List<ScaleEntity> scales = progressionRepo.getScalesByTier(physicalTier);
        if (scales != null) {
            for (ScaleEntity s : scales) {
                String en = ScaleTypeMapper.mapDbNameToEnglishType(s.name);
                total += nonNull(patternRepo.getVariantsByTypeAndRoot(en, root)).size();
            }
        }
        return total;
    }

    private void dumpTierItems(String label, @Nullable List<ScaleProgressionCalculator.TierItem> items) {
        if (items == null) {
            Log.d(TAG, "dump " + label + " -> null");
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("dump ").append(label).append(" (").append(items.size()).append(")\n");
        for (ScaleProgressionCalculator.TierItem it : items) {
            sb.append(" • ")
                    .append(it.typeEs).append(" / ").append(it.typeEn)
                    .append(" | unlocked=").append(it.unlocked)
                    .append(" hasPatterns=").append(it.hasPatterns)
                    .append(" boxes=").append(it.completedBoxes).append("/").append(it.totalBoxes)
                    .append(" completed=").append(it.completed)
                    .append("\n");
        }
        Log.d(TAG, sb.toString());
    }
}
