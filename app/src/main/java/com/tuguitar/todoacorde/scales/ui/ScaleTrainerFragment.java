package com.tuguitar.todoacorde.scales.ui;

import android.os.Bundle;
import android.os.Handler;
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

import com.tuguitar.todoacorde.scales.data.NoteUtils;
import com.tuguitar.todoacorde.R;
import com.tuguitar.todoacorde.scales.data.PatternRepository;
import com.tuguitar.todoacorde.scales.data.ScaleFretNote;
import com.tuguitar.todoacorde.scales.data.ScaleUtils;
import com.tuguitar.todoacorde.scales.domain.ScaleTrainerViewModel;
import com.tuguitar.todoacorde.scales.ui.controllers.PitchInputController;
import com.tuguitar.todoacorde.scales.ui.helpers.NoteBubblesHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class ScaleTrainerFragment extends Fragment implements PitchInputController.Listener {

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

    private final List<Long> variantIds = new ArrayList<>();

    private int previousIndex = -1;
    private final Handler uiHandler = new Handler();

    private List<PatternRepository.PatternVariant> lastVariants = new ArrayList<>();
    private PatternRepository.PatternVariant lastSelectedVariant = null;

    private boolean progType = false;
    private boolean progRoot = false;
    private boolean progVariant = false;

    public ScaleTrainerFragment() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_scale_trainer, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
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

        viewModel.loadScaleTypes();
    }

    private void setupArrows() {
        if (btnNotesLeft != null)  btnNotesLeft.setOnClickListener(v -> bubbles.scrollStep(-1));
        if (btnNotesRight != null) btnNotesRight.setOnClickListener(v -> bubbles.scrollStep(+1));
    }

    private void setupSpinners() {
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
            if (progType) { progType = false; return; }
            String type = (String) spinnerScaleType.getSelectedItem();
            viewModel.loadRootsForType(type);
        }));

        spinnerRoot.setOnItemSelectedListener(new SimpleOnItemSelectedAdapter(() -> {
            if (progRoot) { progRoot = false; return; }
            String type = (String) spinnerScaleType.getSelectedItem();
            String root = (String) spinnerRoot.getSelectedItem();
            viewModel.pickVariant(type, root);
            if (type != null && root != null) tvScaleTitle.setText(root + " " + type);
        }));

        spinnerVariant.setOnItemSelectedListener(new SimpleOnItemSelectedAdapter(() -> {
            if (progVariant) { progVariant = false; return; }
            int idx = spinnerVariant.getSelectedItemPosition();
            if (idx >= 0 && idx < variantIds.size()) {
                Long id = variantIds.get(idx);
                if (id != null) viewModel.selectVariantById(id);
            }
        }));
    }

    private void onStartOrStopClicked() {
        ScaleTrainerViewModel.State s = viewModel.getState().getValue();
        if (s == ScaleTrainerViewModel.State.RUNNING || s == ScaleTrainerViewModel.State.COMPLETED) {
            pitchController.stop();
            viewModel.stopPractice();
            setUiEnabled(true);
            btnStartScale.setText("Empezar");
            resetScaleDisplay();
            // desactiva gating
            pitchController.setExpectedRangeHz(0, 0);
            return;
        }

        final String rootName = (String) spinnerRoot.getSelectedItem();
        final String typeName = (String) spinnerScaleType.getSelectedItem();
        if (rootName == null || typeName == null) {
            Toast.makeText(requireContext(), "Selecciona tipo y tónica", Toast.LENGTH_SHORT).show();
            return;
        }
        tvScaleTitle.setText(rootName + " " + typeName);

        int rootMidi = rootNameToMidi(rootName);
        ScaleUtils.ScaleType mapped = ScaleUtils.fromDbTypeName(typeName);
        if (mapped == null) mapped = ScaleUtils.ScaleType.MAJOR;

        resetScaleDisplay();
        viewModel.startScale(rootMidi, mapped);
        setUiEnabled(false);
        btnStartScale.setText("Terminar");
        pitchController.start();

        // Configura el gate para el primer objetivo
        updateExpectedGate();
    }

    private void setUiEnabled(boolean enabled) {
        spinnerScaleType.setEnabled(enabled);
        spinnerRoot.setEnabled(enabled);
        spinnerVariant.setEnabled(enabled);
    }

    private void setupObservers() {
        viewModel.getScaleTypes().observe(getViewLifecycleOwner(), types -> {
            applyItems(adapterTypes, types);
            if (types != null && !types.isEmpty()) {
                int idx = indexOfIgnoreCase(types, "Phrygian");
                if (idx < 0) idx = indexOfIgnoreCase(types, "Phrygian Dominant");
                if (idx < 0) idx = 0;
                progType = true;
                spinnerScaleType.setSelection(idx);
                viewModel.loadRootsForType(types.get(idx));
            } else {
                applyItems(adapterRoots, new ArrayList<>());
            }
        });

        viewModel.getRoots().observe(getViewLifecycleOwner(), roots -> {
            applyItems(adapterRoots, roots);
            if (roots != null && !roots.isEmpty()) {
                int idx = roots.indexOf("E");
                if (idx < 0) idx = roots.indexOf("A");
                if (idx < 0) idx = 0;
                progRoot = true;
                spinnerRoot.setSelection(idx);
                String type = (String) spinnerScaleType.getSelectedItem();
                viewModel.pickVariant(type, roots.get(idx));
                if (type != null) tvScaleTitle.setText(roots.get(idx) + " " + type);
            }
        });

        viewModel.getVariants().observe(getViewLifecycleOwner(), list -> {
            List<PatternRepository.PatternVariant> variants = (list != null) ? list : new ArrayList<>();
            lastVariants = variants;
            List<String> labels = new ArrayList<>();
            variantIds.clear();
            for (PatternRepository.PatternVariant v : variants) {
                String base = (v.name != null && !v.name.trim().isEmpty()) ? v.name : "Caja";
                labels.add(base + "  [" + v.startFret + "–" + v.endFret + "]");
                variantIds.add(v.id);
            }
            adapterVariants.clear();
            adapterVariants.addAll(labels);
            adapterVariants.notifyDataSetChanged();
        });

        viewModel.getSelectedVariant().observe(getViewLifecycleOwner(), variant -> {
            lastSelectedVariant = variant;

            List<ScaleFretNote> notes = (variant != null && variant.notes != null)
                    ? variant.notes : new ArrayList<>();
            scaleFretboardView.setScaleNotes(notes);
            scaleFretboardView.setHighlightIndex(-1);
            scaleFretboardView.invalidate();

            if (variant != null) {
                int idx = indexOfVariantId(variant.id);
                if (idx < 0) idx = 0;
                if (idx >= 0 && idx < adapterVariants.getCount()) {
                    progVariant = true;
                    spinnerVariant.setSelection(idx);
                }
            }
        });

        viewModel.getScaleNotes().observe(getViewLifecycleOwner(), notes -> {
            List<String> display = new ArrayList<>();
            if (notes != null) for (String n : notes) display.add(NoteUtils.normalizeToSharp(n));
            bubbles.setNotes(display);
            ScaleTrainerViewModel.State s = viewModel.getState().getValue();
            bubbles.highlight(s == ScaleTrainerViewModel.State.RUNNING ? 0 : -1);
        });

        viewModel.getHighlightPath().observe(getViewLifecycleOwner(), path -> {
            scaleFretboardView.setHighlightSequence(path != null ? path : new ArrayList<>());
            Integer idx = viewModel.getCurrentIndex().getValue();
            if (idx != null && idx >= 0 && idx < scaleFretboardView.getHighlightSequenceSize()) {
                scaleFretboardView.setHighlightIndex(idx);
            } else {
                scaleFretboardView.setHighlightIndex(-1);
            }
            scaleFretboardView.invalidate();

            // Si estamos practicando, actualiza “gate” por la nueva nota objetivo
            if (viewModel.getState().getValue() == ScaleTrainerViewModel.State.RUNNING) {
                updateExpectedGate();
            }
        });

        viewModel.getCurrentIndex().observe(getViewLifecycleOwner(), idx -> {
            if (idx == null) return;
            bubbles.highlight(idx);
            if (idx >= 0) bubbles.scrollTo(idx);

            if (idx >= 0 && scaleFretboardView.getHighlightSequenceSize() > idx) {
                scaleFretboardView.setHighlightIndex(idx);
            } else {
                scaleFretboardView.setHighlightIndex(-1);
            }
            scaleFretboardView.invalidate();

            if (previousIndex != -1 && idx > previousIndex) showFeedback("¡Bien!", true);
            previousIndex = (idx != null ? idx : -1);

            // Recalcula gate al avanzar a la siguiente nota
            if (viewModel.getState().getValue() == ScaleTrainerViewModel.State.RUNNING) {
                updateExpectedGate();
            }
        });

        viewModel.getProgressPercent().observe(getViewLifecycleOwner(), p -> {
            double val = (p != null ? p : 0);
            tvProgress.setText(String.format(Locale.getDefault(), "Progreso: %.0f%%", val));
            if (progressBar != null) progressBar.setProgress((int) Math.round(val));
        });

        viewModel.getState().observe(getViewLifecycleOwner(), st -> {
            if (st == ScaleTrainerViewModel.State.RUNNING) {
                btnStartScale.setText("Terminar");
                setUiEnabled(false);
                updateExpectedGate();
            } else {
                if (st == ScaleTrainerViewModel.State.COMPLETED) {
                    Toast.makeText(requireContext(), "Escala completada", Toast.LENGTH_SHORT).show();
                    showFeedback("🎉 Completado", true);
                    btnStartScale.setText("Terminar");
                    setUiEnabled(false);
                } else {
                    btnStartScale.setText("Empezar");
                    setUiEnabled(true);
                    pitchController.setExpectedRangeHz(0, 0);
                }
            }
        });
    }

    // =======================
    // PitchInputController.Listener
    // =======================

    @Override
    public void onStableNote(String noteName, double centsOff) {
        // Compat: si tu VM solo usa el nombre, seguimos notificando
        viewModel.onUserPlayedNote(noteName, centsOff);
    }

    @Override
    public void onStablePitch(String noteName, double frequencyHz, double centsOff) {
        // Si quieres lógica específica por frecuencia real, puedes añadir un método en VM.
        // De momento seguimos con onUserPlayedNote (la mejora clave ya la hace el gating del micro).
        viewModel.onUserPlayedNote(noteName, centsOff);
    }

    @Override
    public void onPermissionDenied() {
        Toast.makeText(requireContext(), "Permiso de micrófono requerido", Toast.LENGTH_SHORT).show();
    }

    // =======================
    // Gate de frecuencia esperada (string-aware)
    // =======================
    private void updateExpectedGate() {
        if (viewModel.getState().getValue() != ScaleTrainerViewModel.State.RUNNING) {
            pitchController.setExpectedRangeHz(0, 0);
            return;
        }
        List<ScaleFretNote> path = viewModel.getHighlightPath().getValue();
        Integer idx = viewModel.getCurrentIndex().getValue();
        if (path == null || idx == null || idx < 0 || idx >= path.size()) {
            pitchController.setExpectedRangeHz(0, 0);
            return;
        }
        ScaleFretNote target = path.get(idx);

        // MIDI esperado desde cuerda+traste (sin octava real, lo aproximamos)
        int midi = approxMidi(target.stringIndex, target.fret);
        double f0 = midiToFreq(midi);

        // margen: ±0.6 semitonos (algo más ancho que 50 cents para tolerar bendings ligeros)
        double semis = 0.6;
        double min = midiToFreq(midi - semis);
        double max = midiToFreq(midi + semis);
        pitchController.setExpectedRangeHz(min, max);
    }

    // =======================
    // Utilidades
    // =======================
    private int indexOfVariantId(long id) {
        for (int i = 0; i < variantIds.size(); i++) {
            Long v = variantIds.get(i);
            if (v != null && v == id) return i;
        }
        return -1;
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

    private int rootNameToMidi(String rootName) {
        int base = 60; // C4
        switch (rootName) {
            case "C#": return base + 1;
            case "D":  return base + 2;
            case "D#": return base + 3;
            case "E":  return base + 4;
            case "F":  return base + 5;
            case "F#": return base + 6;
            case "G":  return base + 7;
            case "G#": return base + 8;
            case "A":  return base + 9;
            case "A#": return base + 10;
            case "B":  return base + 11;
            case "C":
            default:   return base;
        }
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

    private static int indexOfIgnoreCase(List<String> list, String needle) {
        if (needle == null) return -1;
        for (int i = 0; i < list.size(); i++) if (needle.equalsIgnoreCase(list.get(i))) return i;
        return -1;
    }

    private void applyItems(ArrayAdapter<String> adapter, @Nullable List<String> items) {
        if (adapter == null) return;
        adapter.clear();
        if (items != null && !items.isEmpty()) adapter.addAll(items);
        adapter.notifyDataSetChanged();
    }

    private static class SimpleOnItemSelectedAdapter implements android.widget.AdapterView.OnItemSelectedListener {
        private final Runnable onSelected;
        SimpleOnItemSelectedAdapter(Runnable onSelected) { this.onSelected = onSelected; }
        @Override public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) { if (onSelected != null) onSelected.run(); }
        @Override public void onNothingSelected(android.widget.AdapterView<?> parent) {}
    }

    @Override public void onPause() { super.onPause(); pitchController.stop(); }
    @Override public void onDestroyView() { super.onDestroyView(); uiHandler.removeCallbacksAndMessages(null); }
}
