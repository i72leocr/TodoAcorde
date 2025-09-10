package com.todoacorde.todoacorde.metronome.ui;

import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.Dimension;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.switchmaterial.SwitchMaterial;
import com.todoacorde.todoacorde.R;
import com.todoacorde.todoacorde.metronome.domain.MetronomeViewModel;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * Fragmento de UI para el metrónomo.
 *
 * Permite configurar BPM, compás (beats por compás) y acento del primer tiempo.
 * Muestra indicadores visuales de beat y controla la reproducción a través del
 * {@link MetronomeViewModel}.
 */
@AndroidEntryPoint
public class MetronomeFragment extends Fragment {

    /** Texto que muestra el valor actual de BPM. */
    private TextView tvBpm;
    /** Botón principal de inicio/pausa. */
    private ImageButton btnStartStop;
    /** Botones para incrementar/decrementar BPM. */
    private ImageButton btnIncrease, btnDecrease;
    /** Control deslizante para ajustar BPM. */
    private SeekBar seekBarBpm;
    /** Contenedor de indicadores de beat. */
    private LinearLayout beatIndicators;
    /** Interruptor para acentuar el primer beat del compás. */
    private SwitchMaterial switchAccentFirst;
    /** Botones para navegar la ventana del carrusel de beats. */
    private TextView btnBeatPrev, btnBeatNext;
    /** Slots visibles del carrusel de beats. */
    private TextView tvBeatSlot1, tvBeatSlot2, tvBeatSlot3, tvBeatSlot4, tvBeatSlot5;
    /** Arreglo de los cinco slots. */
    private TextView[] slotViews;

    /** ViewModel del metrónomo. */
    private MetronomeViewModel metronomeViewModel;

    /** Total de beats soportados en el carrusel. */
    private static final int TOTAL_BEATS = 12;
    /** Tamaño de la ventana visible en el carrusel. */
    private static final int WINDOW_SIZE = 5;
    /** Máximo valor inicial de ventana permitido. */
    private static final int MAX_WINDOW_START = TOTAL_BEATS - WINDOW_SIZE + 1;

    /** Valor actualmente seleccionado de beats por compás. */
    private int selectedBeats = 4;
    /** Inicio de la ventana del carrusel (1-indexado). */
    private int windowStart = 1;

    /** Color de texto normal para slots. */
    private int colorTextNormal;
    /** Color de texto seleccionado para slots. */
    private int colorTextSelected;

    /**
     * Infla el layout del fragmento y configura los componentes de UI.
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_metronome, container, false);

        metronomeViewModel = new ViewModelProvider(this).get(MetronomeViewModel.class);

        tvBpm = view.findViewById(R.id.tvBpm);
        btnStartStop = view.findViewById(R.id.btnStartStop);
        btnIncrease = view.findViewById(R.id.btnIncrease);
        btnDecrease = view.findViewById(R.id.btnDecrease);
        seekBarBpm = view.findViewById(R.id.seekBarBpm);
        beatIndicators = view.findViewById(R.id.beatIndicators);
        switchAccentFirst = view.findViewById(R.id.switchAccentFirst);
        btnBeatPrev = view.findViewById(R.id.btnBeatPrev);
        btnBeatNext = view.findViewById(R.id.btnBeatNext);
        tvBeatSlot1 = view.findViewById(R.id.tvBeatSlot1);
        tvBeatSlot2 = view.findViewById(R.id.tvBeatSlot2);
        tvBeatSlot3 = view.findViewById(R.id.tvBeatSlot3);
        tvBeatSlot4 = view.findViewById(R.id.tvBeatSlot4);
        tvBeatSlot5 = view.findViewById(R.id.tvBeatSlot5);
        slotViews = new TextView[]{tvBeatSlot1, tvBeatSlot2, tvBeatSlot3, tvBeatSlot4, tvBeatSlot5};

        colorTextNormal = resolveAttrColor(android.R.attr.textColorPrimary);
        colorTextSelected = resolveAttrColor(android.R.attr.textColorPrimaryInverse);

        setupBpmControls();
        setupAccentSwitch();
        setupCarousel();

        btnStartStop.setOnClickListener(v -> {
            Boolean running = metronomeViewModel.isRunning().getValue();
            if (running != null && running) {
                stopMetronome();
            } else {
                startMetronome();
            }
        });

        return view;
    }

    /**
     * Conecta observadores a los {@code LiveData} del ViewModel y sincroniza la UI.
     */
    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        metronomeViewModel.isRunning().observe(getViewLifecycleOwner(), running -> {
            boolean isRunning = running != null && running;
            btnStartStop.setImageResource(isRunning ? R.drawable.ic_pause_24 : R.drawable.ic_play_24);
            seekBarBpm.setEnabled(!isRunning);
            btnIncrease.setEnabled(!isRunning);
            btnDecrease.setEnabled(!isRunning);
            switchAccentFirst.setEnabled(!isRunning);
            btnBeatPrev.setEnabled(!isRunning && windowStart > 1);
            btnBeatNext.setEnabled(!isRunning && windowStart < MAX_WINDOW_START);
            setSlotsEnabled(!isRunning);

            if (!isRunning) {
                clearBeatIndicators();
            }
        });

        metronomeViewModel.getCurrentBeat().observe(getViewLifecycleOwner(), beatIndex -> {
            if (beatIndex != null) {
                highlightCurrentBeat(beatIndex);
            }
        });

        metronomeViewModel.getBeatsPerMeasure().observe(getViewLifecycleOwner(), beats -> {
            if (beats == null) return;
            applySelectionFromVm(beats);
            updateBeatIndicators(beats);
        });
    }

    /**
     * Configura los controles de BPM (seekbar y botones +/-) y vincula eventos.
     */
    private void setupBpmControls() {
        seekBarBpm.setMax(218);
        seekBarBpm.setProgress(100);
        tvBpm.setText("100 BPM");

        seekBarBpm.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int bpmValue = Math.max(20, progress);
                tvBpm.setText(bpmValue + " BPM");
                metronomeViewModel.setBpm(bpmValue);
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) { }
            @Override public void onStopTrackingTouch(SeekBar seekBar) { }
        });

        btnIncrease.setOnClickListener(v -> {
            int current = seekBarBpm.getProgress();
            int newBpm = Math.min(218, current + 1);
            seekBarBpm.setProgress(newBpm);
            tvBpm.setText(newBpm + " BPM");
            metronomeViewModel.setBpm(newBpm);
        });

        btnDecrease.setOnClickListener(v -> {
            int current = seekBarBpm.getProgress();
            int newBpm = Math.max(20, current - 1);
            seekBarBpm.setProgress(newBpm);
            tvBpm.setText(newBpm + " BPM");
            metronomeViewModel.setBpm(newBpm);
        });
    }

    /**
     * Configura el interruptor de acento del primer beat.
     */
    private void setupAccentSwitch() {
        switchAccentFirst.setChecked(true);
        switchAccentFirst.setOnCheckedChangeListener((buttonView, isChecked) ->
                metronomeViewModel.setAccentFirst(isChecked)
        );
    }

    /**
     * Configura el carrusel de selección de beats por compás y sus controles.
     * Inicializa la selección y sincroniza con el ViewModel.
     */
    private void setupCarousel() {
        selectedBeats = 4;
        windowStart = computeWindowStart(selectedBeats);
        repaintCarousel();

        metronomeViewModel.setBeatsPerMeasure(selectedBeats);
        updateBeatIndicators(selectedBeats);

        btnBeatPrev.setOnClickListener(v -> {
            if (isRunning()) return;
            if (windowStart <= 1) return;
            windowStart--;
            repaintCarousel();
        });

        btnBeatNext.setOnClickListener(v -> {
            if (isRunning()) return;
            if (windowStart >= MAX_WINDOW_START) return;
            windowStart++;
            repaintCarousel();
        });

        for (int i = 0; i < slotViews.length; i++) {
            final int index = i;
            slotViews[i].setOnClickListener(v -> {
                if (isRunning()) return;
                int number = windowStart + index;
                if (number < 1 || number > TOTAL_BEATS) return;
                selectedBeats = number;
                repaintCarousel();
                metronomeViewModel.setBeatsPerMeasure(selectedBeats);
                updateBeatIndicators(selectedBeats);
            });
        }
    }

    /**
     * @return true si el metrónomo está ejecutándose según el ViewModel.
     */
    private boolean isRunning() {
        Boolean running = metronomeViewModel.isRunning().getValue();
        return running != null && running;
    }

    /**
     * Calcula el inicio de ventana del carrusel dado un valor seleccionado.
     *
     * @param selected beats por compás seleccionado.
     * @return posición inicial de la ventana (1-indexado).
     */
    private int computeWindowStart(int selected) {
        int desired = selected - 2;
        if (desired < 1) desired = 1;
        if (desired > MAX_WINDOW_START) desired = MAX_WINDOW_START;
        return desired;
    }

    /**
     * Redibuja el carrusel de beats, aplicando estilos y habilitación de controles.
     */
    private void repaintCarousel() {
        for (int i = 0; i < WINDOW_SIZE; i++) {
            int number = windowStart + i;
            TextView slot = slotViews[i];
            slot.setText(String.valueOf(number));

            boolean isSelected = (number == selectedBeats);
            slot.setBackgroundResource(isSelected ? R.drawable.circle_filled : R.drawable.circle_unfilled);
            slot.setTextColor(isSelected ? colorTextSelected : colorTextNormal);
        }

        boolean running = isRunning();
        btnBeatPrev.setEnabled(!running && windowStart > 1);
        btnBeatNext.setEnabled(!running && windowStart < MAX_WINDOW_START);
        setSlotsEnabled(!running);
    }

    /**
     * Habilita o deshabilita todos los slots del carrusel.
     *
     * @param enabled estado a aplicar.
     */
    private void setSlotsEnabled(boolean enabled) {
        for (TextView tv : slotViews) {
            tv.setEnabled(enabled);
        }
    }

    /**
     * Aplica a la UI el valor de beats por compás proveniente del ViewModel.
     *
     * @param beats valor de compás.
     */
    private void applySelectionFromVm(int beats) {
        if (beats < 1 || beats > TOTAL_BEATS) return;
        selectedBeats = beats;
        repaintCarousel();
    }

    /**
     * Inicia el metrónomo vía ViewModel.
     */
    private void startMetronome() {
        metronomeViewModel.startMetronome();
    }

    /**
     * Detiene el metrónomo vía ViewModel y limpia los indicadores.
     */
    private void stopMetronome() {
        metronomeViewModel.stopMetronome();
        clearBeatIndicators();
    }

    /**
     * Renderiza la tira de indicadores de beat según el compás actual.
     *
     * @param beats número de beats por compás.
     */
    private void updateBeatIndicators(int beats) {
        beatIndicators.removeAllViews();

        int size = dpToPx(18);
        int margin = dpToPx(6);
        for (int i = 0; i < beats; i++) {
            View dot = new View(requireContext());
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
            params.setMargins(margin, 0, margin, 0);
            dot.setLayoutParams(params);
            dot.setBackgroundResource(R.drawable.circle_unfilled);
            beatIndicators.addView(dot);
        }
    }

    /**
     * Destaca visualmente el beat actual en la tira de indicadores.
     *
     * @param currentBeatIndex índice del beat activo.
     */
    private void highlightCurrentBeat(int currentBeatIndex) {
        int count = beatIndicators.getChildCount();
        for (int i = 0; i < count; i++) {
            View dot = beatIndicators.getChildAt(i);
            boolean accentFirst = metronomeViewModel.isAccentFirst();

            int res;
            if (accentFirst && i == 0) {
                res = (i == currentBeatIndex) ? R.drawable.circle_filled_strong : R.drawable.circle_unfilled;
            } else {
                res = (i == currentBeatIndex) ? R.drawable.circle_filled : R.drawable.circle_unfilled;
            }
            dot.setBackgroundResource(res);
        }
    }

    /**
     * Limpia la selección en los indicadores de beat.
     */
    private void clearBeatIndicators() {
        int count = beatIndicators.getChildCount();
        for (int i = 0; i < count; i++) {
            beatIndicators.getChildAt(i).setBackgroundResource(R.drawable.circle_unfilled);
        }
    }

    /**
     * Convierte dp a píxeles usando la densidad actual.
     *
     * @param dp valor en dp.
     * @return valor en píxeles.
     */
    private int dpToPx(@Dimension(unit = Dimension.DP) int dp) {
        return Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics()
        ));
    }

    /**
     * Resuelve un color desde un atributo del tema actual.
     *
     * @param attrResId identificador del atributo de color.
     * @return color ARGB resuelto o 0 si no se pudo resolver.
     */
    private int resolveAttrColor(int attrResId) {
        TypedValue tv = new TypedValue();
        boolean ok = requireContext().getTheme().resolveAttribute(attrResId, tv, true);
        if (!ok) return 0;
        if (tv.resourceId != 0) {
            return ContextCompat.getColor(requireContext(), tv.resourceId);
        } else {
            return tv.data;
        }
    }

    /**
     * Detiene el metrónomo al destruirse la vista para evitar fugas y sonido residual.
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        metronomeViewModel.stopMetronome();
    }
}
