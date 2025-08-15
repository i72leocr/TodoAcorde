package com.tuguitar.todoacorde.metronome.ui;

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
import com.tuguitar.todoacorde.R;
import com.tuguitar.todoacorde.metronome.domain.MetronomeViewModel;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * # MetronomeFragment (Capa de UI)
 *
 * Fragment responsable de la interacción de usuario del metrónomo:
 * muestra BPM, compás (1..12), acento del primer tiempo, e indicadores visuales
 * de beat sincronizados con el audio.
 *
 * ## Características clave
 * - **Compás hasta 12 tiempos** con **carrusel fijo de 5 slots** y flechas de desplazamiento.
 * - **Las flechas sólo deslizan** la ventana; **la selección cambia al tocar** un círculo.
 * - **Bloqueo de cambio de compás al reproducir** (deshabilita UI + VM/Manager lo ignoran).
 * - **Reconstrucción de indicadores** según el compás seleccionado.
 *
 * ## Observables
 * - `isRunning`: habilita/deshabilita controles y cambia icono play/pause.
 * - `currentBeat`: ilumina el indicador activo.
 * - `beatsPerMeasure`: reconstruye indicadores y sincroniza selección visual si procede.
 */
@AndroidEntryPoint
public class MetronomeFragment extends Fragment {

    // ---------------------------------------------------------------------------------------------
    // Referencias UI
    // ---------------------------------------------------------------------------------------------
    private TextView tvBpm;
    private ImageButton btnStartStop;
    private ImageButton btnIncrease, btnDecrease;
    private SeekBar seekBarBpm;
    private LinearLayout beatIndicators;
    private SwitchMaterial switchAccentFirst;

    // Carrusel (flechas y 5 slots)
    private TextView btnBeatPrev, btnBeatNext;
    private TextView tvBeatSlot1, tvBeatSlot2, tvBeatSlot3, tvBeatSlot4, tvBeatSlot5;
    private TextView[] slotViews;

    // ---------------------------------------------------------------------------------------------
    // ViewModel
    // ---------------------------------------------------------------------------------------------
    private MetronomeViewModel metronomeViewModel;

    // ---------------------------------------------------------------------------------------------
    // Estado del carrusel
    // ---------------------------------------------------------------------------------------------
    private static final int TOTAL_BEATS = 12; // Rango 1..12
    private static final int WINDOW_SIZE = 5;  // Ventana fija de 5 elementos
    private static final int MAX_WINDOW_START = TOTAL_BEATS - WINDOW_SIZE + 1; // 8

    /** Compás seleccionado (1..12). */
    private int selectedBeats = 4;

    /** Inicio de ventana visible (1..8) para mostrar 5 elementos: [windowStart .. windowStart+4]. */
    private int windowStart = 1;

    // Colores de texto (tema Material)
    private int colorTextNormal;   // Para slots NO seleccionados (colorOnSurface)
    private int colorTextSelected; // Para slot seleccionado (colorOnPrimary)

    // ---------------------------------------------------------------------------------------------
    // Ciclo de vida
    // ---------------------------------------------------------------------------------------------
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_metronome, container, false);

        // 1) ViewModel
        metronomeViewModel = new ViewModelProvider(this).get(MetronomeViewModel.class);

        // 2) Referencias a vistas
        tvBpm = view.findViewById(R.id.tvBpm);
        btnStartStop = view.findViewById(R.id.btnStartStop);
        btnIncrease = view.findViewById(R.id.btnIncrease);
        btnDecrease = view.findViewById(R.id.btnDecrease);
        seekBarBpm = view.findViewById(R.id.seekBarBpm);
        beatIndicators = view.findViewById(R.id.beatIndicators);
        switchAccentFirst = view.findViewById(R.id.switchAccentFirst);

        // Carrusel
        btnBeatPrev = view.findViewById(R.id.btnBeatPrev);
        btnBeatNext = view.findViewById(R.id.btnBeatNext);
        tvBeatSlot1 = view.findViewById(R.id.tvBeatSlot1);
        tvBeatSlot2 = view.findViewById(R.id.tvBeatSlot2);
        tvBeatSlot3 = view.findViewById(R.id.tvBeatSlot3);
        tvBeatSlot4 = view.findViewById(R.id.tvBeatSlot4);
        tvBeatSlot5 = view.findViewById(R.id.tvBeatSlot5);
        slotViews = new TextView[]{tvBeatSlot1, tvBeatSlot2, tvBeatSlot3, tvBeatSlot4, tvBeatSlot5};

        // 3) Colores de tema (para asegurar contraste en claro/oscuro)
        colorTextNormal = resolveAttrColor(android.R.attr.textColorPrimary);
        colorTextSelected = resolveAttrColor(android.R.attr.textColorPrimaryInverse);
        // 4) Setup inicial de controles
        setupBpmControls();
        setupAccentSwitch();
        setupCarousel(); // inicializa carrusel (selección=4, ventana centrada)

        // 5) Botón Play/Pause
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

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Estado de ejecución -> habilitar/deshabilitar controles e icono
        metronomeViewModel.isRunning().observe(getViewLifecycleOwner(), running -> {
            boolean isRunning = running != null && running;
            btnStartStop.setImageResource(isRunning ? R.drawable.ic_pause_24 : R.drawable.ic_play_24);

            // Deshabilitar BPM y acento mientras suena
            seekBarBpm.setEnabled(!isRunning);
            btnIncrease.setEnabled(!isRunning);
            btnDecrease.setEnabled(!isRunning);
            switchAccentFirst.setEnabled(!isRunning);

            // Deshabilitar carrusel (flechas y slots) mientras suena
            btnBeatPrev.setEnabled(!isRunning && windowStart > 1);
            btnBeatNext.setEnabled(!isRunning && windowStart < MAX_WINDOW_START);
            setSlotsEnabled(!isRunning);

            if (!isRunning) {
                clearBeatIndicators();
            }
        });

        // Beat actual -> iluminar indicador
        metronomeViewModel.getCurrentBeat().observe(getViewLifecycleOwner(), beatIndex -> {
            if (beatIndex != null) {
                highlightCurrentBeat(beatIndex);
            }
        });

        // Cambios en compás -> reconstruir indicadores y refrescar highlight (no movemos la ventana)
        metronomeViewModel.getBeatsPerMeasure().observe(getViewLifecycleOwner(), beats -> {
            if (beats == null) return;
            applySelectionFromVm(beats); // NO altera windowStart
            updateBeatIndicators(beats);
        });
    }

    // ---------------------------------------------------------------------------------------------
    // Configuración de controles
    // ---------------------------------------------------------------------------------------------

    /** Configura SeekBar y botones + / - para el control de BPM. */
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

    /** Configura el interruptor para activar/desactivar el acento en el primer tiempo. */
    private void setupAccentSwitch() {
        switchAccentFirst.setChecked(true);
        switchAccentFirst.setOnCheckedChangeListener((buttonView, isChecked) -> {
            metronomeViewModel.setAccentFirst(isChecked);
        });
    }

    /**
     * Configura el carrusel de selección de compás (1..12) con 5 slots visibles.
     * - Ventana inicial centrada en la medida de lo posible.
     * - Flechas sólo deslizan; el toque en slot cambia selección.
     */
    private void setupCarousel() {
        selectedBeats = 4;
        windowStart = computeWindowStart(selectedBeats);
        repaintCarousel();

        metronomeViewModel.setBeatsPerMeasure(selectedBeats);
        updateBeatIndicators(selectedBeats);

        // Flecha izquierda: SOLO desplaza ventana (no cambia selección)
        btnBeatPrev.setOnClickListener(v -> {
            if (isRunning()) return;
            if (windowStart <= 1) return;
            windowStart--;
            repaintCarousel();
        });

        // Flecha derecha: SOLO desplaza ventana (no cambia selección)
        btnBeatNext.setOnClickListener(v -> {
            if (isRunning()) return;
            if (windowStart >= MAX_WINDOW_START) return;
            windowStart++;
            repaintCarousel();
        });

        // Click en slots: cambia selección y notifica a la VM
        for (int i = 0; i < slotViews.length; i++) {
            final int index = i; // 0..4
            slotViews[i].setOnClickListener(v -> {
                if (isRunning()) return;
                int number = windowStart + index;
                if (number < 1 || number > TOTAL_BEATS) return;
                selectedBeats = number;
                repaintCarousel(); // Mantiene ventana; cambia highlight y color de texto
                metronomeViewModel.setBeatsPerMeasure(selectedBeats);
                updateBeatIndicators(selectedBeats);
            });
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Lógica del carrusel
    // ---------------------------------------------------------------------------------------------

    /** @return true si el metrónomo está en ejecución. */
    private boolean isRunning() {
        Boolean running = metronomeViewModel.isRunning().getValue();
        return running != null && running;
    }

    /** Calcula el inicio de la ventana (1..8) intentando centrar el seleccionado. */
    private int computeWindowStart(int selected) {
        int desired = selected - 2;
        if (desired < 1) desired = 1;
        if (desired > MAX_WINDOW_START) desired = MAX_WINDOW_START;
        return desired;
    }

    /**
     * Repinta los 5 slots con los números de la ventana activa y aplica estilos
     * de seleccionado/no seleccionado. También ajusta el enable de flechas.
     */
    private void repaintCarousel() {
        for (int i = 0; i < WINDOW_SIZE; i++) {
            int number = windowStart + i; // 1..12
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

    /** Habilita o deshabilita los 5 slots del carrusel. */
    private void setSlotsEnabled(boolean enabled) {
        for (TextView tv : slotViews) {
            tv.setEnabled(enabled);
        }
    }

    /**
     * Aplica una selección llegada desde la VM (por ejemplo, restauración de estado)
     * sin mover la ventana para respetar el comportamiento del carrusel.
     */
    private void applySelectionFromVm(int beats) {
        if (beats < 1 || beats > TOTAL_BEATS) return;
        selectedBeats = beats;
        repaintCarousel(); // NO cambia windowStart
    }

    // ---------------------------------------------------------------------------------------------
    // Reacciones UI
    // ---------------------------------------------------------------------------------------------

    /** Inicia el metrónomo. */
    private void startMetronome() {
        metronomeViewModel.startMetronome();
    }

    /** Detiene el metrónomo y limpia indicadores. */
    private void stopMetronome() {
        metronomeViewModel.stopMetronome();
        clearBeatIndicators();
    }

    /**
     * Reconstruye los indicadores de beat (bolitas) conforme al compás seleccionado.
     *
     * @param beats número de tiempos a mostrar (1..12)
     */
    private void updateBeatIndicators(int beats) {
        beatIndicators.removeAllViews();

        int size = dpToPx(18);  // diámetro de cada círculo
        int margin = dpToPx(6); // margen lateral entre círculos

        for (int i = 0; i < beats; i++) {
            View dot = new View(requireContext());
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
            params.setMargins(margin, 0, margin, 0);
            dot.setLayoutParams(params);
            dot.setBackgroundResource(R.drawable.circle_unfilled);
            beatIndicators.addView(dot);
        }
    }

    /** Ilumina el indicador correspondiente al beat actual. */
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

    /** Limpia la selección visual de los indicadores (todos a estado no activo). */
    private void clearBeatIndicators() {
        int count = beatIndicators.getChildCount();
        for (int i = 0; i < count; i++) {
            beatIndicators.getChildAt(i).setBackgroundResource(R.drawable.circle_unfilled);
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Utilidades
    // ---------------------------------------------------------------------------------------------

    /** Convierte dp a px con los recursos del contexto. */
    private int dpToPx(@Dimension(unit = Dimension.DP) int dp) {
        return Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics()
        ));
    }

    /**
     * Resuelve un color del tema Material (attr) devolviendo su int ARGB.
     * Usa el valor literal si es un color directo, o carga el recurso si es referencia.
     */
    /** Resuelve un atributo de color del tema (android:attr/...). */
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

    // ---------------------------------------------------------------------------------------------
    // Limpieza
    // ---------------------------------------------------------------------------------------------
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Paramos el metrónomo para evitar callbacks contra vistas destruidas
        metronomeViewModel.stopMetronome();
    }
}
