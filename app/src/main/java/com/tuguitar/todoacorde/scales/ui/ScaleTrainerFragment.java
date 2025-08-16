package com.tuguitar.todoacorde.scales.ui;

import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.tuguitar.todoacorde.R;
import com.tuguitar.todoacorde.SessionManager;
import com.tuguitar.todoacorde.scales.data.NoteUtils;
import com.tuguitar.todoacorde.scales.data.ScaleFretNote;
import com.tuguitar.todoacorde.scales.domain.ScaleTrainerViewModel;
import com.tuguitar.todoacorde.scales.ui.controllers.PitchInputController;
import com.tuguitar.todoacorde.scales.ui.helpers.NoteBubblesHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class ScaleTrainerFragment extends Fragment implements PitchInputController.Listener {

    private static final String TAG = "ScaleFrag";

    private ScaleTrainerViewModel viewModel;

    private TextView tvScaleTitle;
    private HorizontalScrollView scrollScaleNotes;
    private LinearLayout llScaleNotes;
    private TextView tvProgress;
    private ProgressBar progressBar;
    private TextView tvFeedback;
    private Button btnStartScale;
    private Spinner spinnerRoot;
    private Spinner spinnerScaleType;
    private Spinner spinnerVariant;
    private ScaleFretboardView scaleFretboardView;
    private View btnNotesLeft;
    private View btnNotesRight;

    private NoteBubblesHelper bubbles;
    private PitchInputController pitchController;

    private ArrayAdapter<String> adapterTypes;
    private ArrayAdapter<String> adapterRoots;
    private ArrayAdapter<String> adapterVariants;

    // Bloqueo global para evitar bucles de onItemSelected al aplicar UiState
    private boolean suppressUiCallbacks = false;

    private int previousIndex = -1;
    private final Handler uiHandler = new Handler();

    // ====== AUTO-COMPLETADO (simulación de notas) ======
    // Actívalo para que la práctica se complete sola (sin micrófono).
    private boolean autoPlayEnabled = true;
    private boolean autoFeeding = false;

    public ScaleTrainerFragment() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView()");
        return inflater.inflate(R.layout.fragment_scale_trainer, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.d(TAG, "onViewCreated()");

        viewModel = new ViewModelProvider(this).get(ScaleTrainerViewModel.class);

        tvScaleTitle       = view.findViewById(R.id.tvScaleTitle);
        scrollScaleNotes   = view.findViewById(R.id.hScrollNotes);
        llScaleNotes       = view.findViewById(R.id.llScaleNotes);
        tvProgress         = view.findViewById(R.id.tvProgress);
        progressBar        = view.findViewById(R.id.progressBarScale);
        tvFeedback         = view.findViewById(R.id.tvFeedback);
        btnStartScale      = view.findViewById(R.id.btnStartScale);
        spinnerRoot        = view.findViewById(R.id.spinnerRoot);
        spinnerScaleType   = view.findViewById(R.id.spinnerScaleType);
        spinnerVariant     = view.findViewById(R.id.spinnerVariant);
        scaleFretboardView = view.findViewById(R.id.scaleFretboard);
        btnNotesLeft       = view.findViewById(R.id.btnNotesLeft);
        btnNotesRight      = view.findViewById(R.id.btnNotesRight);

        bubbles = new NoteBubblesHelper(
                requireContext(),
                scrollScaleNotes,
                llScaleNotes,
                getColorCompat(android.R.color.black)
        );

        pitchController = new PitchInputController(requireContext(), this);

        setupSpinners();
        setupObservers();
        setupArrows();

        btnStartScale.setOnClickListener(v -> onStartOrStopClicked());

        // Usuario actual → ViewModel
        SessionManager sessionManager = new SessionManager(requireContext());
        long uid = sessionManager.getCurrentUserId();
        Log.d(TAG, "onViewCreated -> currentUserId=" + uid);
        viewModel.setCurrentUserId(uid);

        // Carga inicial
        viewModel.onInit();
    }

    private void setupArrows() {
        if (btnNotesLeft != null)  btnNotesLeft.setOnClickListener(v -> bubbles.scrollStep(-1));
        if (btnNotesRight != null) btnNotesRight.setOnClickListener(v -> bubbles.scrollStep(+1));
    }

    private void setupSpinners() {
        Log.d(TAG, "setupSpinners()");
        adapterTypes = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, new ArrayList<>());
        adapterTypes.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerScaleType.setAdapter(adapterTypes);

        adapterRoots = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, new ArrayList<>());
        adapterRoots.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRoot.setAdapter(adapterRoots);

        adapterVariants = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, new ArrayList<>());
        adapterVariants.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerVariant.setAdapter(adapterVariants);

        spinnerScaleType.setOnItemSelectedListener(new SimpleOnItemSelectedAdapter(() -> {
            if (suppressUiCallbacks) return;
            String typeEn = (String) spinnerScaleType.getSelectedItem();
            Log.d(TAG, "spinnerScaleType -> onSelected: " + typeEn);
            viewModel.onTypeSelected(typeEn);
        }));

        spinnerRoot.setOnItemSelectedListener(new SimpleOnItemSelectedAdapter(() -> {
            if (suppressUiCallbacks) return;
            String root = (String) spinnerRoot.getSelectedItem();
            Log.d(TAG, "spinnerRoot -> onSelected: " + root);
            viewModel.onRootSelected(root);
        }));

        spinnerVariant.setOnItemSelectedListener(new SimpleOnItemSelectedAdapter(() -> {
            if (suppressUiCallbacks) return;
            int idx = spinnerVariant.getSelectedItemPosition();
            Log.d(TAG, "spinnerVariant -> onSelected idx=" + idx);
            viewModel.onVariantSelected(idx);
        }));
    }

    private void onStartOrStopClicked() {
        ScaleTrainerViewModel.UiState s = viewModel.getUiState().getValue();
        ScaleTrainerViewModel.PracticeState st = (s != null ? s.state : ScaleTrainerViewModel.PracticeState.IDLE);
        Log.d(TAG, "onStartOrStopClicked state=" + st);

        if (st == ScaleTrainerViewModel.PracticeState.RUNNING
                || st == ScaleTrainerViewModel.PracticeState.COMPLETED) {
            pitchController.stop();
            autoFeeding = false;
            viewModel.onStopClicked();
            setUiEnabled(true);
            btnStartScale.setText("Empezar");
            resetScaleDisplay();
            pitchController.setExpectedRangeHz(0, 0);
            return;
        }

        if (s != null && s.title != null) tvScaleTitle.setText(s.title);

        resetScaleDisplay();
        viewModel.onStartClicked();
        setUiEnabled(false);
        btnStartScale.setText("Terminar");

        // En modo AUTO no arrancamos el micrófono. El auto-feed se lanza
        // desde el observer cuando el estado cambie a RUNNING y haya notes.
        if (!autoPlayEnabled) {
            pitchController.start();     // mic
            updateExpectedGate();        // gate por la nota esperada
        }
    }

    private void setUiEnabled(boolean enabled) {
        Log.d(TAG, "setUiEnabled=" + enabled);
        spinnerScaleType.setEnabled(enabled);
        spinnerRoot.setEnabled(enabled);
        spinnerVariant.setEnabled(enabled);
    }

    private void setupObservers() {
        Log.d(TAG, "setupObservers()");
        viewModel.getUiState().observe(getViewLifecycleOwner(), s -> {
            if (s == null) return;

            Log.d(TAG, "UiState -> loading=" + s.loading
                    + " types=" + s.typesAllowed.size()
                    + " roots=" + s.roots.size()
                    + " variants=" + s.variantLabels.size()
                    + " selTypeIdx=" + s.selectedTypeIndex
                    + " selRootIdx=" + s.selectedRootIndex
                    + " selVarIdx=" + s.selectedVariantIndex
                    + " state=" + s.state
            );

            if (s.error != null && !s.error.isEmpty()) {
                Toast.makeText(requireContext(), s.error, Toast.LENGTH_SHORT).show();
            }

            // Aplicación atómica de spinners y resto de UI
            suppressUiCallbacks = true;
            try {
                // Tipos
                if (shouldReplace(adapterTypes, s.typesAllowed)) {
                    Log.d(TAG, "apply types -> " + s.typesAllowed.size());
                    applyItems(adapterTypes, s.typesAllowed);
                }
                if (s.selectedTypeIndex >= 0 && s.selectedTypeIndex < adapterTypes.getCount()) {
                    spinnerScaleType.setSelection(s.selectedTypeIndex, false);
                }

                // Raíces
                if (shouldReplace(adapterRoots, s.roots)) {
                    Log.d(TAG, "apply roots -> " + s.roots.size());
                    applyItems(adapterRoots, s.roots);
                }
                if (s.selectedRootIndex >= 0 && s.selectedRootIndex < adapterRoots.getCount()) {
                    spinnerRoot.setSelection(s.selectedRootIndex, false);
                }

                // Variantes
                if (shouldReplace(adapterVariants, s.variantLabels)) {
                    Log.d(TAG, "apply variants -> " + s.variantLabels.size());
                    applyItems(adapterVariants, s.variantLabels);
                }
                if (s.selectedVariantIndex >= 0 && s.selectedVariantIndex < adapterVariants.getCount()) {
                    spinnerVariant.setSelection(s.selectedVariantIndex, false);
                } else if (adapterVariants.getCount() > 0) {
                    spinnerVariant.setSelection(0, false);
                }

                // Título
                tvScaleTitle.setText(s.title != null ? s.title : "");

                // Fretboard
                scaleFretboardView.setHighlightSequence(s.highlightPath != null ? s.highlightPath : new ArrayList<>());
                if (s.currentIndex >= 0 && s.highlightPath != null && s.currentIndex < s.highlightPath.size()) {
                    scaleFretboardView.setHighlightIndex(s.currentIndex);
                } else {
                    scaleFretboardView.setHighlightIndex(-1);
                }
                scaleFretboardView.setScaleNotes(extractNotesForBoard(s.highlightPath));
                scaleFretboardView.invalidate();

                // Notas (burbujas)
                List<String> display = new ArrayList<>();
                if (s.scaleNotes != null) for (String n : s.scaleNotes) display.add(NoteUtils.normalizeToSharp(n));
                bubbles.setNotes(display);
                bubbles.highlight(s.state == ScaleTrainerViewModel.PracticeState.RUNNING ? s.currentIndex : -1);

                // Autodesplazamiento tras layout
                if (s.state == ScaleTrainerViewModel.PracticeState.RUNNING && s.currentIndex >= 0) {
                    scrollScaleNotes.post(() -> bubbles.scrollTo(s.currentIndex));
                }

                // Progreso
                tvProgress.setText(String.format(Locale.getDefault(), "Progreso: %.0f%%", s.progressPercent));
                if (progressBar != null) progressBar.setProgress((int) Math.round(s.progressPercent));

                // ====== LÓGICA AUTO-FEED ======
                if (autoPlayEnabled
                        && s.state == ScaleTrainerViewModel.PracticeState.RUNNING
                        && s.scaleNotes != null && !s.scaleNotes.isEmpty()
                        && !autoFeeding) {
                    autoFeeding = true;
                    // En auto no necesitamos gate; que sea libre
                    pitchController.setExpectedRangeHz(0, 0);
                    // Simula tocar TODA la secuencia
                    pitchController.startAutoFeed(s.scaleNotes, /*delayMs*/ 75);
                }

                // Estado → UI + control de entrada
                if (s.state == ScaleTrainerViewModel.PracticeState.RUNNING) {
                    btnStartScale.setText("Terminar");
                    setUiEnabled(false);
                    if (!autoPlayEnabled) {
                        updateExpectedGate(); // solo mic
                    } else {
                        // auto: sin gate, ya lo forzamos a (0,0)
                    }
                } else if (s.state == ScaleTrainerViewModel.PracticeState.COMPLETED) {
                    showFeedback("🎉 Completado", true);
                    pitchController.stop();
                    autoFeeding = false;
                    setUiEnabled(true);
                    btnStartScale.setText("Empezar");
                    pitchController.setExpectedRangeHz(0, 0);
                } else { // IDLE
                    btnStartScale.setText("Empezar");
                    setUiEnabled(true);
                    pitchController.setExpectedRangeHz(0, 0);
                    // Si volvemos a idle, nos aseguramos de cortar auto
                    if (autoFeeding) {
                        pitchController.stop();
                        autoFeeding = false;
                    }
                }

                // “¡Bien!” si avanzó
                if (s.currentIndex > previousIndex && previousIndex != -1) {
                    showFeedback("¡Bien!", true);
                }
                previousIndex = s.currentIndex;

            } finally {
                suppressUiCallbacks = false;
            }
        });

        viewModel.getEffects().observe(getViewLifecycleOwner(), event -> {
            if (event == null) return;
            String msg = event.consume();
            if (msg != null && !msg.isEmpty()) {
                Log.d(TAG, "Effect -> " + msg);
                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    // =======================
    // PitchInputController.Listener
    // =======================
    @Override
    public void onStableNote(String noteName, double centsOff) {
        Log.d(TAG, "onStableNote " + noteName + " (" + centsOff + " cents)");
        viewModel.onNoteDetected(noteName, centsOff);
    }
    @Override
    public void onStablePitch(String noteName, double frequencyHz, double centsOff) {
        Log.d(TAG, "onStablePitch " + noteName + " " + frequencyHz + "Hz (" + centsOff + " cents)");
        viewModel.onNoteDetected(noteName, centsOff);
    }
    @Override
    public void onPermissionDenied() {
        Log.w(TAG, "Micro permission denied");
        Toast.makeText(requireContext(), "Permiso de micrófono requerido", Toast.LENGTH_SHORT).show();
    }

    // =======================
    // Gate esperado
    // =======================
    private void updateExpectedGate() {
        // En modo AUTO no aplicamos gate (no hay mic)
        if (autoPlayEnabled && autoFeeding) {
            pitchController.setExpectedRangeHz(0, 0);
            return;
        }
        ScaleTrainerViewModel.UiState s = viewModel.getUiState().getValue();
        if (s == null || s.state != ScaleTrainerViewModel.PracticeState.RUNNING) {
            pitchController.setExpectedRangeHz(0, 0);
            return;
        }
        List<ScaleFretNote> path = s.highlightPath;
        int idx = s.currentIndex;
        if (path == null || idx < 0 || idx >= path.size()) {
            pitchController.setExpectedRangeHz(0, 0);
            return;
        }
        ScaleFretNote target = path.get(idx);

        int midi = approxMidi(target.stringIndex, target.fret);
        double semis = 0.6;
        double min = midiToFreq(midi - semis);
        double max = midiToFreq(midi + semis);
        Log.d(TAG, "updateExpectedGate idx=" + idx + " string=" + target.stringIndex + " fret=" + target.fret
                + " midi~" + midi + " min=" + min + "Hz max=" + max + "Hz");
        pitchController.setExpectedRangeHz(min, max);
    }

    // =======================
    // Utilidades
    // =======================
    private List<ScaleFretNote> extractNotesForBoard(List<ScaleFretNote> highlightPath) {
        return highlightPath != null ? highlightPath : new ArrayList<>();
    }

    private void resetScaleDisplay() {
        previousIndex = -1;
        tvFeedback.setVisibility(View.INVISIBLE);
        bubbles.reset();
        if (progressBar != null) progressBar.setProgress(0);
        tvProgress.setText("Progreso: 0%");
        scaleFretboardView.setHighlightIndex(-1);
        scaleFretboardView.invalidate();
    }

    private void showFeedback(String text, boolean positive) {
        tvFeedback.setText(text);
        tvFeedback.setVisibility(View.VISIBLE);
        tvFeedback.setTextColor(getColorCompat(positive ? android.R.color.holo_green_dark : android.R.color.holo_red_dark));
        uiHandler.removeCallbacksAndMessages(null);
        uiHandler.postDelayed(() -> tvFeedback.setVisibility(View.INVISIBLE), 500);
    }

    private static int approxMidi(int stringIndex, int fret) {
        final int[] OPEN_MIDI = {40, 45, 50, 55, 59, 64}; // 6ª→1ª : E2,A2,D3,G3,B3,E4
        int s = Math.max(0, Math.min(5, stringIndex));
        int f = Math.max(0, fret);
        return OPEN_MIDI[s] + f;
    }
    private static double midiToFreq(double midi) {
        return 440.0 * Math.pow(2.0, (midi - 69.0) / 12.0);
    }

    private @ColorInt int getColorCompat(int resId) {
        return ContextCompat.getColor(requireContext(), resId);
    }

    private static boolean shouldReplace(ArrayAdapter<String> adapter, List<String> newItems) {
        if (adapter == null) return false;
        if (newItems == null) newItems = new ArrayList<>();
        if (adapter.getCount() != newItems.size()) return true;
        for (int i = 0; i < newItems.size(); i++) {
            if (!newItems.get(i).equals(adapter.getItem(i))) return true;
        }
        return false;
    }

    private void applyItems(ArrayAdapter<String> adapter, @Nullable List<String> items) {
        if (adapter == null) return;
        adapter.clear();
        if (items != null && !items.isEmpty()) adapter.addAll(items);
        adapter.notifyDataSetChanged();
        Log.d(TAG, "applyItems -> count=" + adapter.getCount());
    }

    private static class SimpleOnItemSelectedAdapter implements android.widget.AdapterView.OnItemSelectedListener {
        private final Runnable onSelected;
        SimpleOnItemSelectedAdapter(Runnable onSelected) { this.onSelected = onSelected; }
        @Override public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
            Log.d(TAG, "Spinner onItemSelected pos=" + position);
            if (onSelected != null) onSelected.run();
        }
        @Override public void onNothingSelected(android.widget.AdapterView<?> parent) {
            Log.d(TAG, "Spinner onNothingSelected");
        }
    }

    @Override public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause -> stop pitch");
        pitchController.stop();
        autoFeeding = false;
    }
    @Override public void onDestroyView() {
        super.onDestroyView();
        Log.d(TAG, "onDestroyView()");
        uiHandler.removeCallbacksAndMessages(null);
        pitchController.stop();
        autoFeeding = false;
    }
}
