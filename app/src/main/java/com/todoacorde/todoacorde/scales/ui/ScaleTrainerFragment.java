package com.todoacorde.todoacorde.scales.ui;

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

import com.google.android.material.button.MaterialButton;
import com.todoacorde.todoacorde.R;
import com.todoacorde.todoacorde.SessionManager;
import com.todoacorde.todoacorde.scales.data.NoteUtils;
import com.todoacorde.todoacorde.scales.data.ScaleFretNote;
import com.todoacorde.todoacorde.scales.domain.ScaleTrainerViewModel;
import com.todoacorde.todoacorde.scales.ui.controllers.PitchInputController;
import com.todoacorde.todoacorde.scales.ui.helpers.NoteBubblesHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * Fragmento de entrenamiento de escalas.
 *
 * Responsabilidades:
 * - Gestiona la selección de tipo, tonalidad y variante (caja) de la escala.
 * - Inicia y detiene la sesión de práctica.
 * - Muestra la secuencia de notas, progreso y un diapasón resaltando la nota objetivo.
 * - Recibe notas estables del micrófono mediante PitchInputController y actualiza el ViewModel.
 */
@AndroidEntryPoint
public class ScaleTrainerFragment extends Fragment implements PitchInputController.Listener {

    private static final String TAG = "ScaleFrag";

    // VM
    private ScaleTrainerViewModel viewModel;

    // UI principal
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
    @Nullable
    private MaterialButton btnOpenTiers;

    // Helpers UI
    private NoteBubblesHelper bubbles;

    // Entrada de audio
    private PitchInputController pitchController;

    // Adaptadores de spinners
    private ArrayAdapter<String> adapterTypes;
    private ArrayAdapter<String> adapterRoots;
    private ArrayAdapter<String> adapterVariants;

    // Control de estado de UI
    private boolean suppressUiCallbacks = false;
    private int previousIndex = -1;

    // Handler para feedback temporal
    private final Handler uiHandler = new Handler();

    // Auto-play (modo pruebas)
    private final boolean autoPlayEnabled = false;
    private boolean autoFeeding = false;

    public ScaleTrainerFragment() {
        // Constructor vacío por requisito del framework
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView()");
        return inflater.inflate(R.layout.fragment_scale_trainer, container, false);
    }

    /**
     * Inicializa vistas, ViewModel, observadores y controladores.
     */
    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.d(TAG, "onViewCreated()");
        viewModel = new ViewModelProvider(this).get(ScaleTrainerViewModel.class);
        Log.d(TAG, "VM instance @" + System.identityHashCode(viewModel));

        // Referencias de vista
        tvScaleTitle = view.findViewById(R.id.tvScaleTitle);
        scrollScaleNotes = view.findViewById(R.id.hScrollNotes);
        llScaleNotes = view.findViewById(R.id.llScaleNotes);
        tvProgress = view.findViewById(R.id.tvProgress);
        progressBar = view.findViewById(R.id.progressBarScale);
        tvFeedback = view.findViewById(R.id.tvFeedback);
        btnStartScale = view.findViewById(R.id.btnStartScale);
        spinnerRoot = view.findViewById(R.id.spinnerRoot);
        spinnerScaleType = view.findViewById(R.id.spinnerScaleType);
        spinnerVariant = view.findViewById(R.id.spinnerVariant);
        scaleFretboardView = view.findViewById(R.id.scaleFretboard);
        btnNotesLeft = view.findViewById(R.id.btnNotesLeft);
        btnNotesRight = view.findViewById(R.id.btnNotesRight);
        btnOpenTiers = view.findViewById(R.id.btnOpenTiers);

        // Hoja de selección por dificultad
        if (btnOpenTiers != null) {
            btnOpenTiers.setOnClickListener(v ->
                    new ScaleTierPickerDialog().show(getChildFragmentManager(), "scale_tier_picker")
            );
        }

        // El tipo de escala se controla desde el diálogo por niveles; ocultamos spinner si procede
        if (spinnerScaleType != null) spinnerScaleType.setVisibility(View.GONE);

        // Burbujas de notas
        bubbles = new NoteBubblesHelper(
                requireContext(),
                scrollScaleNotes,
                llScaleNotes,
                getColorCompat(android.R.color.black)
        );

        // Controlador de entrada de tono (micrófono o auto-feed)
        pitchController = new PitchInputController(requireContext(), this);

        setupSpinners();
        setupObservers();
        setupArrows();

        btnStartScale.setOnClickListener(v -> onStartOrStopClicked());

        // Usuario actual y arranque
        SessionManager sessionManager = new SessionManager(requireContext());
        long uid = sessionManager.getCurrentUserId();
        Log.d(TAG, "onViewCreated -> currentUserId=" + uid);
        viewModel.setCurrentUserId(uid);

        viewModel.onInit();
    }

    /** Configura flechas de scroll horizontal en la tira de notas. */
    private void setupArrows() {
        if (btnNotesLeft != null) btnNotesLeft.setOnClickListener(v -> bubbles.scrollStep(-1));
        if (btnNotesRight != null) btnNotesRight.setOnClickListener(v -> bubbles.scrollStep(+1));
    }

    /**
     * Configura los spinners y sus listeners delegando la lógica en el ViewModel.
     * Se usan banderas para evitar ciclos de eventos durante actualizaciones de estado.
     */
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

    /**
     * Botón principal. Cambia entre iniciar práctica y detenerla.
     * Controla el micrófono/auto-feed y bloquea la UI durante la sesión.
     */
    private void onStartOrStopClicked() {
        ScaleTrainerViewModel.UiState s = viewModel.getUiState().getValue();
        ScaleTrainerViewModel.PracticeState st = (s != null ? s.state : ScaleTrainerViewModel.PracticeState.IDLE);
        Log.d(TAG, "onStartOrStopClicked state=" + st);

        // Si ya está corriendo o recién completado, detenemos todo.
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

        // Pasamos a RUNNING
        if (s != null && s.title != null) tvScaleTitle.setText(s.title);

        resetScaleDisplay();
        viewModel.onStartClicked();
        setUiEnabled(false);
        btnStartScale.setText("Terminar");

        if (!autoPlayEnabled) {
            pitchController.start();
            updateExpectedGate();
        }
    }

    /** Habilita/inhabilita la UI de selección mientras se practica. */
    private void setUiEnabled(boolean enabled) {
        Log.d(TAG, "setUiEnabled=" + enabled);
        spinnerScaleType.setEnabled(enabled);
        spinnerRoot.setEnabled(enabled);
        spinnerVariant.setEnabled(enabled);
        if (btnOpenTiers != null) btnOpenTiers.setEnabled(enabled);
    }

    /**
     * Observa el UiState y aplica cambios a la vista:
     * listas de spinners, ruta de notas, progreso, estado del botón,
     * feedback y gating de tono esperado.
     */
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
                    + " easy=" + (s.easyItems == null ? -1 : s.easyItems.size())
                    + " medium=" + (s.mediumItems == null ? -1 : s.mediumItems.size())
                    + " hard=" + (s.hardItems == null ? -1 : s.hardItems.size())
            );

            if (s.error != null && !s.error.isEmpty()) {
                Toast.makeText(requireContext(), s.error, Toast.LENGTH_SHORT).show();
            }

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

                // Tonalidades
                if (shouldReplace(adapterRoots, s.roots)) {
                    Log.d(TAG, "apply roots -> " + s.roots.size());
                    applyItems(adapterRoots, s.roots);
                }
                if (s.selectedRootIndex >= 0 && s.selectedRootIndex < adapterRoots.getCount()) {
                    spinnerRoot.setSelection(s.selectedRootIndex, false);
                }

                // Variantes (cajas)
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

                // Resaltado en diapasón
                scaleFretboardView.setHighlightSequence(s.highlightPath != null ? s.highlightPath : new ArrayList<>());
                if (s.currentIndex >= 0 && s.highlightPath != null && s.currentIndex < s.highlightPath.size()) {
                    scaleFretboardView.setHighlightIndex(s.currentIndex);
                } else {
                    scaleFretboardView.setHighlightIndex(-1);
                }
                scaleFretboardView.setScaleNotes(extractNotesForBoard(s.highlightPath));
                scaleFretboardView.invalidate();

                // Burbujas de notas
                List<String> display = new ArrayList<>();
                if (s.scaleNotes != null) {
                    for (String n : s.scaleNotes) display.add(NoteUtils.normalizeToSharp(n));
                }
                bubbles.setNotes(display);
                bubbles.highlight(s.state == ScaleTrainerViewModel.PracticeState.RUNNING ? s.currentIndex : -1);

                // Auto scroll a la nota actual
                if (s.state == ScaleTrainerViewModel.PracticeState.RUNNING && s.currentIndex >= 0) {
                    scrollScaleNotes.post(() -> bubbles.scrollTo(s.currentIndex));
                }

                // Progreso
                tvProgress.setText(String.format(Locale.getDefault(), "Progreso: %.0f%%", s.progressPercent));
                if (progressBar != null) {
                    progressBar.setProgress((int) Math.round(s.progressPercent));
                }

                // Auto-feed de prueba
                if (autoPlayEnabled
                        && s.state == ScaleTrainerViewModel.PracticeState.RUNNING
                        && s.scaleNotes != null && !s.scaleNotes.isEmpty()
                        && !autoFeeding) {
                    autoFeeding = true;
                    pitchController.setExpectedRangeHz(0, 0);
                    pitchController.startAutoFeed(s.scaleNotes, 75);
                }

                // Estado global y gating
                if (s.state == ScaleTrainerViewModel.PracticeState.RUNNING) {
                    btnStartScale.setText("Terminar");
                    setUiEnabled(false);
                    if (!autoPlayEnabled) {
                        updateExpectedGate();
                    }
                } else if (s.state == ScaleTrainerViewModel.PracticeState.COMPLETED) {
                    showFeedback("🎉 Completado", true);
                    pitchController.stop();
                    autoFeeding = false;
                    setUiEnabled(true);
                    btnStartScale.setText("Empezar");
                    pitchController.setExpectedRangeHz(0, 0);
                } else {
                    btnStartScale.setText("Empezar");
                    setUiEnabled(true);
                    pitchController.setExpectedRangeHz(0, 0);
                    if (autoFeeding) {
                        pitchController.stop();
                        autoFeeding = false;
                    }
                }

                // Feedback corto en cada acierto
                if (s.currentIndex > previousIndex && previousIndex != -1) {
                    showFeedback("¡Bien!", true);
                }
                previousIndex = s.currentIndex;

            } finally {
                suppressUiCallbacks = false;
            }
        });

        // Efectos one-shot (snacks/toasts de desbloqueos, etc.)
        viewModel.getEffects().observe(getViewLifecycleOwner(), event -> {
            if (event == null) return;
            String msg = event.consume();
            if (msg != null && !msg.isEmpty()) {
                Log.d(TAG, "Effect -> " + msg);
                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Callback de nota estable por nombre de nota.
     * Reenvía al ViewModel para avanzar la práctica.
     */
    @Override
    public void onStableNote(String noteName, double centsOff) {
        Log.d(TAG, "onStableNote " + noteName + " (" + centsOff + " cents)");
        viewModel.onNoteDetected(noteName, centsOff);
    }

    /**
     * Callback de nota estable con frecuencia aproximada.
     * Se usa el mismo flujo que onStableNote.
     */
    @Override
    public void onStablePitch(String noteName, double frequencyHz, double centsOff) {
        Log.d(TAG, "onStablePitch " + noteName + " " + frequencyHz + "Hz (" + centsOff + " cents)");
        viewModel.onNoteDetected(noteName, centsOff);
    }

    /** Sin permisos de micrófono. */
    @Override
    public void onPermissionDenied() {
        Log.w(TAG, "Micro permission denied");
        Toast.makeText(requireContext(), "Permiso de micrófono requerido", Toast.LENGTH_SHORT).show();
    }

    /**
     * Ajusta un gate de frecuencia esperado alrededor de la nota objetivo
     * para filtrar falsos positivos del detector de tono.
     */
    private void updateExpectedGate() {
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

        // Ventana +/- ~0.6 semitonos alrededor del MIDI aproximado
        double semis = 0.6;
        double min = midiToFreq(midi - semis);
        double max = midiToFreq(midi + semis);

        Log.d(TAG, "updateExpectedGate idx=" + idx + " string=" + target.stringIndex + " fret=" + target.fret
                + " midi~" + midi + " min=" + min + "Hz max=" + max + "Hz");

        pitchController.setExpectedRangeHz(min, max);
    }

    /** Extrae la lista para el diapasón (aquí ya viene lista la ruta). */
    private List<ScaleFretNote> extractNotesForBoard(List<ScaleFretNote> highlightPath) {
        return highlightPath != null ? highlightPath : new ArrayList<>();
    }

    /** Resetea contadores visuales y destaca nada. */
    private void resetScaleDisplay() {
        previousIndex = -1;
        tvFeedback.setVisibility(View.INVISIBLE);
        bubbles.reset();
        if (progressBar != null) progressBar.setProgress(0);
        tvProgress.setText("Progreso: 0%");
        scaleFretboardView.setHighlightIndex(-1);
        scaleFretboardView.invalidate();
    }

    /** Muestra un feedback breve en pantalla. */
    private void showFeedback(String text, boolean positive) {
        tvFeedback.setText(text);
        tvFeedback.setVisibility(View.VISIBLE);
        tvFeedback.setTextColor(getColorCompat(positive ? android.R.color.holo_green_dark : android.R.color.holo_red_dark));
        uiHandler.removeCallbacksAndMessages(null);
        uiHandler.postDelayed(() -> tvFeedback.setVisibility(View.INVISIBLE), 500);
    }

    /** MIDI aproximado por cuerda y traste para acotar la frecuencia. */
    private static int approxMidi(int stringIndex, int fret) {
        final int[] OPEN_MIDI = {40, 45, 50, 55, 59, 64};
        int s = Math.max(0, Math.min(5, stringIndex));
        int f = Math.max(0, fret);
        return OPEN_MIDI[s] + f;
    }

    /** Conversión MIDI (double) a frecuencia en Hz. */
    private static double midiToFreq(double midi) {
        return 440.0 * Math.pow(2.0, (midi - 69.0) / 12.0);
    }

    /** getColor compat. */
    private @ColorInt int getColorCompat(int resId) {
        return ContextCompat.getColor(requireContext(), resId);
    }

    /** Indica si hay diferencias entre el contenido del adapter y la nueva lista. */
    private static boolean shouldReplace(ArrayAdapter<String> adapter, List<String> newItems) {
        if (adapter == null) return false;
        if (newItems == null) newItems = new ArrayList<>();
        if (adapter.getCount() != newItems.size()) return true;
        for (int i = 0; i < newItems.size(); i++) {
            if (!newItems.get(i).equals(adapter.getItem(i))) return true;
        }
        return false;
    }

    /** Aplica elementos a un adapter de forma segura. */
    private void applyItems(ArrayAdapter<String> adapter, @Nullable List<String> items) {
        if (adapter == null) return;
        adapter.clear();
        if (items != null && !items.isEmpty()) adapter.addAll(items);
        adapter.notifyDataSetChanged();
        Log.d(TAG, "applyItems -> count=" + adapter.getCount());
    }

    /**
     * Listener simplificado para spinners; ejecuta un runnable en onItemSelected.
     */
    private static class SimpleOnItemSelectedAdapter implements android.widget.AdapterView.OnItemSelectedListener {
        private final Runnable onSelected;

        SimpleOnItemSelectedAdapter(Runnable onSelected) {
            this.onSelected = onSelected;
        }

        @Override
        public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
            Log.d(TAG, "Spinner onItemSelected pos=" + position);
            if (onSelected != null) onSelected.run();
        }

        @Override
        public void onNothingSelected(android.widget.AdapterView<?> parent) {
            Log.d(TAG, "Spinner onNothingSelected");
        }
    }

    /**
     * Ciclo de vida: detiene el detector al pausar.
     */
    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause -> stop pitch");
        pitchController.stop();
        autoFeeding = false;
    }

    /**
     * Ciclo de vida: limpia recursos al destruir la vista.
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.d(TAG, "onDestroyView()");
        uiHandler.removeCallbacksAndMessages(null);
        pitchController.stop();
        autoFeeding = false;
    }
}
