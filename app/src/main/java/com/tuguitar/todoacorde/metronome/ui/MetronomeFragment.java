package com.tuguitar.todoacorde.metronome.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.tuguitar.todoacorde.R;
import com.tuguitar.todoacorde.metronome.domain.MetronomeViewModel;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class MetronomeFragment extends Fragment {

    private TextView tvBpm;
    private ImageButton btnStartStop;
    private ImageButton btnIncrease, btnDecrease;
    private SeekBar seekBarBpm;
    private ChipGroup chipGroupTimeSignature;
    private LinearLayout beatIndicators;
    private SwitchMaterial switchAccentFirst;

    private MetronomeViewModel metronomeViewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_metronome, container, false);

        // ✅ Inicializar ViewModel al inicio
        metronomeViewModel = new ViewModelProvider(this).get(MetronomeViewModel.class);

        // Inicializar vistas
        tvBpm = view.findViewById(R.id.tvBpm);
        btnStartStop = view.findViewById(R.id.btnStartStop);
        btnIncrease = view.findViewById(R.id.btnIncrease);
        btnDecrease = view.findViewById(R.id.btnDecrease);
        seekBarBpm = view.findViewById(R.id.seekBarBpm);
        chipGroupTimeSignature = view.findViewById(R.id.chipGroupTimeSignature);
        beatIndicators = view.findViewById(R.id.beatIndicators);
        switchAccentFirst = view.findViewById(R.id.switchAccentFirst);

        // Setup inicial
        btnStartStop.setImageResource(R.drawable.ic_play_24);
        seekBarBpm.setMax(218);
        seekBarBpm.setProgress(100);
        tvBpm.setText("100 BPM");
        switchAccentFirst.setChecked(true);

        // SeekBar listener
        seekBarBpm.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int bpmValue = Math.max(20, progress);
                tvBpm.setText(bpmValue + " BPM");
                metronomeViewModel.setBpm(bpmValue);
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // Botones + y -
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

        // Selector de compás
        chipGroupTimeSignature.setOnCheckedChangeListener((group, checkedId) -> {
            Chip chip = group.findViewById(checkedId);
            if (chip != null) {
                String text = chip.getText().toString();
                try {
                    int beats = Integer.parseInt(text.split("/")[0]);
                    metronomeViewModel.setBeatsPerMeasure(beats);
                    updateBeatIndicators(beats);
                } catch (NumberFormatException e) {
                    metronomeViewModel.setBeatsPerMeasure(4);
                    updateBeatIndicators(4);
                }
            }
        });

        // ✅ Establecer compás por defecto después de inicializar ViewModel
        chipGroupTimeSignature.check(R.id.chip4_4);
        updateBeatIndicators(4);

        // Switch acento primer tiempo
        switchAccentFirst.setOnCheckedChangeListener((buttonView, isChecked) -> {
            metronomeViewModel.setAccentFirst(isChecked);
        });

        // Botón play/pause
        btnStartStop.setOnClickListener(v -> {
            if (metronomeViewModel.isMetronomeRunning()) {
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

        // Observador del beat actual
        metronomeViewModel.getCurrentBeat().observe(getViewLifecycleOwner(), beatIndex -> {
            if (beatIndex != null) {
                highlightCurrentBeat(beatIndex);
            }
        });
    }

    private void startMetronome() {
        metronomeViewModel.startMetronome();
        btnStartStop.setImageResource(R.drawable.ic_pause_24);
        btnIncrease.setEnabled(false);
        btnDecrease.setEnabled(false);
        seekBarBpm.setEnabled(false);
        chipGroupTimeSignature.setEnabled(false);
        switchAccentFirst.setEnabled(false);
    }

    private void stopMetronome() {
        metronomeViewModel.stopMetronome();
        btnStartStop.setImageResource(R.drawable.ic_play_24);
        btnIncrease.setEnabled(true);
        btnDecrease.setEnabled(true);
        seekBarBpm.setEnabled(true);
        chipGroupTimeSignature.setEnabled(true);
        switchAccentFirst.setEnabled(true);
        clearBeatIndicators();
    }

    private void updateBeatIndicators(int beats) {
        beatIndicators.removeAllViews();
        for (int i = 0; i < beats; i++) {
            View dot = new View(requireContext());
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(60, 60);
            params.setMargins(16, 0, 16, 0);
            dot.setLayoutParams(params);
            dot.setBackgroundResource(R.drawable.circle_unfilled);
            beatIndicators.addView(dot);
        }
    }

    private void highlightCurrentBeat(int currentBeatIndex) {
        int count = beatIndicators.getChildCount();
        for (int i = 0; i < count; i++) {
            View dot = beatIndicators.getChildAt(i);
            int res;
            if (metronomeViewModel.isAccentFirst() && i == 0) {
                res = (i == currentBeatIndex) ? R.drawable.circle_filled_strong : R.drawable.circle_unfilled;
            } else {
                res = (i == currentBeatIndex) ? R.drawable.circle_filled : R.drawable.circle_unfilled;
            }
            dot.setBackgroundResource(res);
        }
    }

    private void clearBeatIndicators() {
        int count = beatIndicators.getChildCount();
        for (int i = 0; i < count; i++) {
            beatIndicators.getChildAt(i).setBackgroundResource(R.drawable.circle_unfilled);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        metronomeViewModel.stopMetronome();
    }
}
