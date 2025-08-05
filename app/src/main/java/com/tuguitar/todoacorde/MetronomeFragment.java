package com.tuguitar.todoacorde;

import android.media.AudioAttributes;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.switchmaterial.SwitchMaterial;

public class MetronomeFragment extends Fragment {

    private TextView tvBpm;
    private ImageButton btnStartStop, btnIncrease, btnDecrease;
    private SeekBar seekBarBpm;
    private ChipGroup chipGroupTimeSignature;
    private LinearLayout beatIndicators;
    private SwitchMaterial switchAccentFirst;

    private int bpm = 100;
    private int beatsPerMeasure = 4;
    private boolean isRunning = false;
    private boolean accentFirstBeat = true;

    private Handler handler;
    private Runnable beatRunnable;
    private int currentBeat = 0;

    private SoundPool soundPool;
    private int tickSoundId;
    private int tickUpSoundId;
    private boolean soundLoaded = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_metronome, container, false);

        tvBpm = view.findViewById(R.id.tvBpm);
        btnStartStop = view.findViewById(R.id.btnStartStop);
        btnIncrease = view.findViewById(R.id.btnIncrease);
        btnDecrease = view.findViewById(R.id.btnDecrease);
        seekBarBpm = view.findViewById(R.id.seekBarBpm);
        chipGroupTimeSignature = view.findViewById(R.id.chipGroupTimeSignature);
        beatIndicators = view.findViewById(R.id.beatIndicators);
        switchAccentFirst = view.findViewById(R.id.switchAccentFirst);
        btnStartStop.setImageResource(R.drawable.ic_play_24);

        tvBpm.setText(bpm + " BPM");
        seekBarBpm.setMax(218);
        seekBarBpm.setProgress(bpm);

        handler = new Handler(Looper.getMainLooper());
        initSoundPool();

        seekBarBpm.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                bpm = Math.max(20, progress);
                tvBpm.setText(bpm + " BPM");
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        btnIncrease.setOnClickListener(v -> {
            bpm = Math.min(218, bpm + 1);
            seekBarBpm.setProgress(bpm);
            tvBpm.setText(bpm + " BPM");
        });

        btnDecrease.setOnClickListener(v -> {
            bpm = Math.max(20, bpm - 1);
            seekBarBpm.setProgress(bpm);
            tvBpm.setText(bpm + " BPM");
        });

        chipGroupTimeSignature.setOnCheckedChangeListener((group, checkedId) -> {
            Chip chip = group.findViewById(checkedId);
            if (chip != null && chip.getText() != null) {
                try {
                    beatsPerMeasure = Integer.parseInt(chip.getText().toString().split("/")[0]);
                } catch (NumberFormatException e) {
                    beatsPerMeasure = 4;
                }
                updateBeatIndicators();
            }
        });
        chipGroupTimeSignature.check(R.id.chip4_4);

        switchAccentFirst.setChecked(true);
        switchAccentFirst.setOnCheckedChangeListener((button, isChecked) -> accentFirstBeat = isChecked);

        btnStartStop.setOnClickListener(v -> {
            if (isRunning) stopMetronome();
            else startMetronome();
        });

        updateBeatIndicators();
        return view;
    }

    private void initSoundPool() {
        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build();

        soundPool = new SoundPool.Builder()
                .setMaxStreams(1)
                .setAudioAttributes(audioAttributes)
                .build();

        tickSoundId = soundPool.load(requireContext(), R.raw.tick, 1);
        tickUpSoundId = soundPool.load(requireContext(), R.raw.tick_up, 1);

        soundPool.setOnLoadCompleteListener((pool, id, status) -> soundLoaded = (status == 0));
    }

    private void updateBeatIndicators() {
        beatIndicators.removeAllViews();
        for (int i = 0; i < beatsPerMeasure; i++) {
            View dot = new View(requireContext());
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(60, 60);
            params.setMargins(16, 0, 16, 0);
            dot.setLayoutParams(params);
            dot.setBackgroundResource(R.drawable.circle_unfilled);
            beatIndicators.addView(dot);
        }
    }

    private void startMetronome() {
        isRunning = true;
        btnStartStop.setImageResource(R.drawable.ic_pause_24);

        // Bloquear controles sin ocultar
        btnIncrease.setEnabled(false);
        btnDecrease.setEnabled(false);
        seekBarBpm.setEnabled(false);
        chipGroupTimeSignature.setEnabled(false);
        switchAccentFirst.setEnabled(false);

        currentBeat = 0;
        double intervalMs = 60000.0 / bpm;

        beatRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isRunning) return;
                if (soundLoaded) {
                    int soundId = (accentFirstBeat && currentBeat == 0) ? tickUpSoundId : tickSoundId;
                    soundPool.play(soundId, 1, 1, 0, 0, 1f);
                }
                highlightCurrentBeat();
                currentBeat = (currentBeat + 1) % beatsPerMeasure;
                handler.postDelayed(this, (long) intervalMs);
            }
        };
        handler.post(beatRunnable);
    }

    private void stopMetronome() {
        isRunning = false;
        btnStartStop.setImageResource(R.drawable.ic_play_24);

        // Desbloquear controles
        btnIncrease.setEnabled(true);
        btnDecrease.setEnabled(true);
        seekBarBpm.setEnabled(true);
        chipGroupTimeSignature.setEnabled(true);
        switchAccentFirst.setEnabled(true);

        handler.removeCallbacks(beatRunnable);
        clearBeatIndicators();
        currentBeat = 0;
    }

    private void highlightCurrentBeat() {
        for (int i = 0; i < beatIndicators.getChildCount(); i++) {
            View dot = beatIndicators.getChildAt(i);
            int res = (accentFirstBeat && i == 0)
                    ? (i == currentBeat ? R.drawable.circle_filled_strong : R.drawable.circle_unfilled)
                    : (i == currentBeat ? R.drawable.circle_filled : R.drawable.circle_unfilled);
            dot.setBackgroundResource(res);
        }
    }

    private void clearBeatIndicators() {
        for (int i = 0; i < beatIndicators.getChildCount(); i++) {
            beatIndicators.getChildAt(i).setBackgroundResource(R.drawable.circle_unfilled);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        stopMetronome();
        if (soundPool != null) {
            soundPool.release();
            soundPool = null;
        }
    }
}
