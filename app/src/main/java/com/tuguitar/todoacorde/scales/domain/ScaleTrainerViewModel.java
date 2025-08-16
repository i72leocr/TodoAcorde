// ScaleTrainerViewModel.java
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
import com.tuguitar.todoacorde.scales.domain.repository.ProgressionRepository;
import com.tuguitar.todoacorde.scales.domain.usecase.CompleteScaleBoxUseCase;
import com.tuguitar.todoacorde.scales.data.entity.ScaleEntity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
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

    /** Progresión real (NO bypass). */
    private static final boolean DEBUG_BYPASS_PROGRESSION = false;

    // ===== Inyectados =====
    private final PatternRepository patternRepo;
    private final ProgressionRepository progressionRepo;
    private final CompleteScaleBoxUseCase completeScaleBox;
    private final Executor ioExecutor;
    // NUEVO: use case de logros de maestría de escalas
    private final EvaluateScaleMasteryAchievementUseCase evalScaleAchievements;

    // ===== Main thread handler y emisión segura =====
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private void emit(@NonNull UiState state) { mainHandler.post(() -> uiState.setValue(state)); }
    private void emitEffect(@NonNull String msg) { mainHandler.post(() -> effects.setValue(new OneShotEvent<>(msg))); }

    // ===== Estado UI =====
    public enum PracticeState { IDLE, RUNNING, COMPLETED }

    public static final class UiState {
        public final boolean loading;
        public final String error;

        public final List<String> typesAllowed;   // EN (PatternRepository)
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

        public UiState(
                boolean loading, String error,
                List<String> typesAllowed, List<String> roots,
                List<String> variantLabels, List<Long> variantIds,
                int selectedTypeIndex, int selectedRootIndex, int selectedVariantIndex,
                String title,
                List<String> scaleNotes,
                List<ScaleFretNote> highlightPath,
                int currentIndex, int streak, double progressPercent, PracticeState state
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
        }

        public static UiState empty() {
            return new UiState(
                    false, null,
                    Collections.emptyList(), Collections.emptyList(),
                    Collections.emptyList(), Collections.emptyList(),
                    -1, -1, -1,
                    "",
                    Collections.emptyList(), Collections.emptyList(),
                    -1, 0, 0d, PracticeState.IDLE
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

    // ===== Campos internos =====
    private volatile int preferredStartFret = 5, preferredEndFret = 9;
    private long currentUserId = 1L;

    private List<ScaleFretNote> patternNotesCache = new ArrayList<>();
    private List<ScaleFretNote> plannedPath       = new ArrayList<>();

    // selección actual (fuente de verdad)
    private String selectedTypeEn = null; // EN
    private String selectedRoot   = null; // "C", "C#", ...
    private int    selectedVariantIdx = -1;

    // versionado para cancelar resultados obsoletos
    private volatile long selectionVersion = 0L;

    // listas en memoria
    private List<String> allTypesEn = new ArrayList<>();
    private List<String> allowedTypesEn = new ArrayList<>();
    private List<String> currentRoots = new ArrayList<>();
    private List<PatternRepository.PatternVariant> variantsAll = new ArrayList<>();
    private List<PatternRepository.PatternVariant> variantsAllowed = new ArrayList<>();
    private List<Long> variantsAllowedIds = new ArrayList<>();
    private List<String> variantsAllowedLabels = new ArrayList<>();

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
    }

    // ===== API desde la vista =====

    @MainThread
    public void setCurrentUserId(long userId) { this.currentUserId = userId > 0 ? userId : 1L; }

    @MainThread
    public void onInit() {
        Log.d(TAG, "onInit() -> start");
        final long v = ++selectionVersion;
        setLoading(true);
        ioExecutor.execute(() -> {
            try {
                allTypesEn = nonNull(patternRepo.getAllScaleTypesDistinct());
                if (v != selectionVersion) return;
                Log.d(TAG, "onInit() -> getAllScaleTypesDistinct size=" + allTypesEn.size() + " => " + join(allTypesEn));

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
                } else {
                    Log.w(TAG, "onInit() -> allTypesEn vacío.");
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
                0, 0, 0d, PracticeState.RUNNING
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
                -1, 0, 0d, PracticeState.IDLE
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
                    newIdx >= seq.size() ? PracticeState.COMPLETED : PracticeState.RUNNING
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
                    s.state
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

    // ===== Internos: cálculos de progresión / variantes / guardado =====

    /**
     * Calcula tipos permitidos para la tonalidad actual **ignorando** las escalas
     * del tier que no tienen patrones para esta tonalidad. Usa el nº de variantes reales
     * de (type+root) para decidir si una escala está completa.
     */
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

        // 1) Último tier desbloqueado para ESTA tonalidad, considerando solo escalas con patrones en esta tonalidad
        int maxUnlockedTier = 0;
        while (isTierCompletedForRoot_LOCAL(currentUserId, maxUnlockedTier, tonalityId, selectedRoot)
                && progressionRepo.hasAnyScaleInTier(maxUnlockedTier + 1)) {
            maxUnlockedTier++;
        }
        Log.d(TAG, "recomputeAllowedTypesForCurrentRoot -> root=" + selectedRoot
                + " tonalityId=" + tonalityId + " maxUnlockedTier=" + maxUnlockedTier);

        // 2) Nombres ES permitidos de tiers 0..maxUnlockedTier
        HashSet<String> allowedEs = new HashSet<>();
        for (int t = 0; t <= maxUnlockedTier; t++) {
            List<ScaleEntity> tierScales = progressionRepo.getScalesByTier(t);
            if (tierScales != null) for (ScaleEntity s : tierScales) allowedEs.add(s.name);
        }
        Log.d(TAG, "recomputeAllowedTypesForCurrentRoot -> allowed ES size=" + allowedEs.size() + " => " + allowedEs);

        // 3) Filtrado EN por desbloqueo + existencia de patrones en la root
        List<String> out = new ArrayList<>();
        for (String en : allTypesEn) {
            String es = mapEnglishTypeToDbName(en);
            if (!allowedEs.contains(es)) continue;

            List<String> rootsForType = nonNull(patternRepo.getRootsForType(en));
            if (indexOf(rootsForType, selectedRoot) >= 0) {
                out.add(en);
            }
        }

        allowedTypesEn = out;
        Log.d(TAG, "recomputeAllowedTypesForCurrentRoot -> result size=" + allowedTypesEn.size() + " => " + join(allowedTypesEn));
    }

    /**
     * Tier completo para ESTA tonalidad si TODAS las escalas del tier que
     * SÍ tienen variantes para la tonalidad están completas, entendiendo por completo:
     * maxCompletedBoxes(user, escala, tonalidad) >= variantesDisponibles(type+root).
     */
    private boolean isTierCompletedForRoot_LOCAL(long userId, int tier, long tonalityId, @NonNull String root) {
        List<ScaleEntity> tierScales = progressionRepo.getScalesByTier(tier);
        if (tierScales == null || tierScales.isEmpty()) {
            Log.d(TAG, "isTierCompletedForRoot_LOCAL tier=" + tier + " -> no hay escalas, devolvemos false");
            return false;
        }

        int relevantes = 0;
        List<String> debug = new ArrayList<>();

        for (ScaleEntity s : tierScales) {
            String en = mapDbNameToEnglishType(s.name);
            List<PatternRepository.PatternVariant> variants = nonNull(patternRepo.getVariantsByTypeAndRoot(en, root));
            int disponibles = variants.size();

            if (disponibles <= 0) {
                debug.add(" - " + s.name + " [sin patrones para " + root + "] -> IGNORAR");
                continue;
            }

            relevantes++;
            int maxHechas = progressionRepo.getMaxCompletedBoxOrZero(userId, s.id, tonalityId);
            boolean done = maxHechas >= disponibles;

            debug.add(" - " + s.name + " [patrones " + root + ": " + disponibles + "] -> "
                    + (done ? "COMPLETA" : "PENDIENTE") + " (" + maxHechas + "/" + disponibles + ")");

            if (!done) {
                Log.d(TAG, "isTierCompletedForRoot_LOCAL tier=" + tier + " -> NO completo. Detalle:\n" + join(debug));
                return false;
            }
        }

        // 🔧 FIX: si no hay escalas relevantes (ninguna con patrones para esta tonalidad),
        // NO consideramos el tier como completo (evita desbloqueos fantasma).
        if (relevantes == 0) {
            Log.d(TAG, "isTierCompletedForRoot_LOCAL tier=" + tier + " -> sin escalas relevantes para " + root + ", NO completo.");
            return false;
        }

        Log.d(TAG, "isTierCompletedForRoot_LOCAL tier=" + tier + " -> COMPLETO. Detalle:\n" + join(debug));
        return true;
    }

    /** Versión con guardas por versión para evitar pisados de estado. */
    private void loadVariantsForCurrentSelection(long v) {
        Log.d(TAG, "loadVariantsForCurrentSelection(type=" + selectedTypeEn + ", root=" + selectedRoot + ", v=" + v + ")");
        List<PatternRepository.PatternVariant> all = (selectedTypeEn != null && selectedRoot != null)
                ? nonNull(patternRepo.getVariantsByTypeAndRoot(selectedTypeEn, selectedRoot))
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
                    patternRepo.findNearestWindow(selectedTypeEn, selectedRoot, preferredStartFret, preferredEndFret);
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

                String esName = mapEnglishTypeToDbName(selectedTypeEn);
                long scaleId = progressionRepo.findScaleIdByName(esName);
                long tonalityId = progressionRepo.findTonalityIdByName(selectedRoot);
                Log.d(TAG, "persistCompletion -> esName=" + esName + " scaleId=" + scaleId + " tonalityId=" + tonalityId);
                if (scaleId <= 0 || tonalityId <= 0) {
                    emitEffect("No se pudo guardar el progreso (mapeo escala/tonalidad).");
                    return;
                }

                // Tomamos estado ANTES
                final int availableInTonality =
                        nonNull(patternRepo.getVariantsByTypeAndRoot(selectedTypeEn, selectedRoot)).size();
                final int beforeMaxCompleted =
                        progressionRepo.getMaxCompletedBoxOrZero(currentUserId, scaleId, tonalityId);

                int boxOrder = selectedVariantIdx + 1;
                int tier = progressionRepo.findScaleTierById(scaleId);
                long now = System.currentTimeMillis();

                CompleteScaleBoxUseCase.Result result =
                        completeScaleBox.execute(currentUserId, scaleId, tonalityId, boxOrder, tier, now);
                Log.d(TAG, "persistCompletion -> result next=" + result.nextBoxOrder
                        + " scaleAll=" + result.scaleCompletedAllTonalities
                        + " tierCompleted=" + result.tierCompleted);

                // Estado DESPUÉS
                final int afterMaxCompleted =
                        progressionRepo.getMaxCompletedBoxOrZero(currentUserId, scaleId, tonalityId);

                int disponibles = availableInTonality;

                // Mensaje “siguiente caja” solo si existe en esta tonalidad
                if (result.nextBoxOrder != null && result.nextBoxOrder <= disponibles) {
                    emitEffect("¡Caja " + result.nextBoxOrder + " desbloqueada!");
                }

                if (result.scaleCompletedAllTonalities) {
                    emitEffect("¡Escala " + esName + " completada en todas las tonalidades!");
                }
                if (result.tierCompleted) {
                    emitEffect("¡Nuevas escalas desbloqueadas!");
                }

                // === LOGROS: maestría de escalas ===
                boolean justCompletedForTonality =
                        (beforeMaxCompleted < disponibles) && (afterMaxCompleted >= disponibles);
                boolean justCompletedAllTonalities = result.scaleCompletedAllTonalities;

                try {
                    evalScaleAchievements.onScaleCompletionEvaluated(
                            currentUserId,
                            scaleId,
                            tonalityId,
                            justCompletedForTonality,
                            justCompletedAllTonalities
                    );
                } catch (Throwable achEx) {
                    Log.e(TAG, "Error evaluando logros de escalas: " + achEx.getMessage(), achEx);
                }

                // Refrescos
                recomputeAllowedTypesForCurrentRoot(v);
                if (v != selectionVersion) return;
                loadVariantsForCurrentSelection(v);
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

        String esName = mapEnglishTypeToDbName(typeEn);
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

    // ===== Helpers de UI state =====

    private void pushUiPreviewIdle() {
        int typeIdx = indexOf(allowedTypesEn, selectedTypeEn);
        int rootIdx = indexOf(currentRoots, selectedRoot);

        String title = (selectedTypeEn != null && selectedRoot != null)
                ? (selectedRoot + " " + selectedTypeEn) : "";

        List<String> seqPreview = namesFromPath(plannedPath);

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
                PracticeState.IDLE
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
                currentIndex, streak, progress, state
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
                currentIndex, streak, progress, state
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
                currentIndex, streak, progress, state
        ));
    }

    // ===== Notas / Path =====

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

    // ===== Mapeo EN ↔ ES =====
    private String mapEnglishTypeToDbName(String english) {
        if (english == null) return "";
        String e = english.trim().toLowerCase(Locale.ROOT);
        HashMap<String,String> m = new HashMap<>();

        m.put("major pentatonic", "Pentatónica mayor");
        m.put("minor pentatonic", "Pentatónica menor");
        m.put("blues",            "Blues");

        m.put("ionian",           "Mayor");
        m.put("major",            "Mayor");

        m.put("aeolian",          "Menor natural");
        m.put("natural minor",    "Menor natural");
        m.put("minor",            "Menor natural");

        m.put("dorian",           "Dórica");
        m.put("mixolydian",       "Mixolidia");
        m.put("lydian",           "Lidia");
        m.put("phrygian",         "Frigia");
        m.put("locrian",          "Locria");

        m.put("phrygian dominant","Frigia dominante");
        m.put("double harmonic major", "Doble armónica mayor");

        m.put("harmonic minor",   "Menor armónica");
        m.put("melodic minor (asc)", "Menor melódica");
        m.put("melodic minor",    "Menor melódica");

        String db = m.get(e);
        return db != null ? db : english;
    }

    /** Inverso ES → EN para chequear patrones en PatternRepository. */
    private String mapDbNameToEnglishType(String es) {
        if (es == null) return "";
        String s = es.trim().toLowerCase(Locale.ROOT);
        HashMap<String,String> inv = new HashMap<>();

        inv.put("pentatónica mayor", "major pentatonic");
        inv.put("pentatónica menor", "minor pentatonic");
        inv.put("blues",             "blues");

        inv.put("mayor",             "major");
        inv.put("menor natural",     "natural minor");

        inv.put("dórica",            "dorian");
        inv.put("mixolidia",         "mixolydian");
        inv.put("lidia",             "lydian");
        inv.put("frigia",            "phrygian");
        inv.put("locria",            "locrian");

        inv.put("frigia dominante",  "phrygian dominant");
        inv.put("doble armónica mayor", "double harmonic major");

        inv.put("menor armónica",    "harmonic minor");
        inv.put("menor melódica",    "melodic minor");

        String en = inv.get(s);
        return en != null ? en : es;
    }

    // ===== Utils =====
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
}
