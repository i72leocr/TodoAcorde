package com.tuguitar.todoacorde.practice.ui;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;
import com.tuguitar.todoacorde.ChordDetectionListener;
import com.tuguitar.todoacorde.practice.domain.PracticeViewModel;
import com.tuguitar.todoacorde.R;
import com.tuguitar.todoacorde.SongChordWithInfo;
import com.tuguitar.todoacorde.practice.data.SongUserSpeed;

import java.util.ArrayList;
import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class PracticeChordsOptimizedFragment extends Fragment implements ChordDetectionListener {
    private static final String TAG = "PracticeOptimizedFrag";
    private static final int REQUEST_MIC = 123;
    private static final String ARG_SONG_ID = "song_id";

    private PracticeViewModel viewModel;
    private Button btnStartStop;
    private Switch switchMode;
    private Spinner spinnerSpeed;
    private RecyclerView recycler;
    private LyricChordAdapter adapter;
    private GridWithPointsView gridView;
    private ProgressBar progressBar;
    private TextView tvSongTitle;
    private TextView tvSongAuthor;
    private TextView tvCurrentChordName;
    private TextView tvBest;
    private TextView tvLast;
    private TextView tvCountdown;
    private final Handler countdownHandler = new Handler(Looper.getMainLooper());
    private Runnable countdownRunnable;
    private int countdownRemaining;

    private final List<SongChordWithInfo> chordInfoCache = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_practice_chords_optimized,
                container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 1) Inyecta el ViewModel con Hilt
        viewModel = new ViewModelProvider(this)
                .get(PracticeViewModel.class);

        // 2) Vincula vistas y configura UI
        bindViews(view);
        setupSpeedSpinner();
        setupModeSwitch();
        setupAdapterClicks();
        observeLineItems();
        observeChordInfo();
        observeCurrentIndex();
        observeCurrentLineIndex();
        observeCorrectIndices();
        observeProgress();
        observeScoreEvent();
        observeRunningState();
        setupStartStopButton();

        // 3) Inicializa práctica con el songId
        Bundle args = getArguments();
        if (args != null && args.containsKey(ARG_SONG_ID)) {
            viewModel.initForSong(args.getInt(ARG_SONG_ID));
        } else {
            Log.e(TAG, "No se recibió song_id en los argumentos");
        }

        // 4) Observadores adicionales
        viewModel.songDetails.observe(getViewLifecycleOwner(), details -> {
            if (details != null && details.song != null) {
                tvSongTitle.setText(details.song.getTitle());
                tvSongAuthor.setText(details.song.getAuthor());
            }
        });

        viewModel.bestScore.observe(getViewLifecycleOwner(), best -> {
            tvBest.setText("Mejor Puntaje: " + (best != null ? best : "--"));
        });

        viewModel.lastScore.observe(getViewLifecycleOwner(), last -> {
            tvLast.setText("Último Puntaje: " + (last != null ? last : "--"));
        });

        viewModel.unlockEvent.observe(getViewLifecycleOwner(), ev -> {
            String unlocked = ev.getIfNotHandled();
            if (unlocked != null) {
                Snackbar.make(requireView(),
                        "Has desbloqueado el modo " + unlocked + " 🎉",
                        Snackbar.LENGTH_LONG).show();
            }
        });

        viewModel.getCountdownSeconds().observe(getViewLifecycleOwner(), seconds -> {
            if (seconds != null && seconds > 0) {
                tvCountdown.setText(String.valueOf(seconds));
                tvCountdown.setVisibility(View.VISIBLE);
            } else {
                tvCountdown.setVisibility(View.GONE);
            }
        });

        viewModel.getIsCountingDown().observe(getViewLifecycleOwner(), counting -> {
            btnStartStop.setEnabled(!Boolean.TRUE.equals(counting));
        });

        viewModel.getCountdownFinished().observe(getViewLifecycleOwner(), event -> {
            if (event != null && Boolean.TRUE.equals(event.getIfNotHandled())) {
                toggleDetection(); // arranca después de la cuenta atrás
            }
        });

        viewModel.unlockedSpeeds.observe(getViewLifecycleOwner(), speeds -> {
            if (speeds == null) return;

            int bestUnlocked = speeds.isUnlocked1x ? 2 :
                    speeds.isUnlocked0_75x ? 1 : 0;

            if (spinnerSpeed.getSelectedItemPosition() != bestUnlocked) {
                spinnerSpeed.setOnItemSelectedListener(null);
                spinnerSpeed.setSelection(bestUnlocked, false);
                spinnerSpeed.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override public void onItemSelected(AdapterView<?> parent, View v, int pos, long id) {
                        double f = (pos == 0 ? 0.5 : pos == 1 ? 0.75 : 1.0);
                        SongUserSpeed sus = viewModel.unlockedSpeeds.getValue();
                        boolean isUnlocked =
                                (f == 0.5) ||
                                        (f == 0.75 && sus != null && sus.isUnlocked0_75x) ||
                                        (f == 1.0 && sus != null && sus.isUnlocked1x);

                        if (isUnlocked) {
                            viewModel.setSpeedFactor(f);
                        } else {
                            Toast.makeText(requireContext(),
                                    "Debes obtener al menos 80 puntos en la velocidad anterior para desbloquear esta.",
                                    Toast.LENGTH_SHORT).show();
                            spinnerSpeed.setSelection(bestUnlocked);
                        }
                    }
                    @Override public void onNothingSelected(AdapterView<?> parent) {}
                });
            }

            viewModel.setSpeedFactor(bestUnlocked == 0 ? 0.5 : bestUnlocked == 1 ? 0.75 : 1.0);
        });

        spinnerSpeed.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View v, int pos, long id) {
                double factor = (pos == 0 ? 0.5 : pos == 1 ? 0.75 : 1.0);
                SongUserSpeed sus = viewModel.unlockedSpeeds.getValue();
                boolean isUnlocked =
                        (factor == 0.5) ||
                                (factor == 0.75 && sus != null && sus.isUnlocked0_75x) ||
                                (factor == 1.0 && sus != null && sus.isUnlocked1x);

                if (isUnlocked) {
                    viewModel.setSpeedFactor(factor);
                } else {
                    Toast.makeText(requireContext(),
                            "Debes obtener al menos 80 puntos en la velocidad anterior para desbloquear esta.",
                            Toast.LENGTH_SHORT).show();
                    int restorePos = sus != null
                            ? (sus.getMaxUnlockedSpeed() == 1.0f ? 2 : sus.getMaxUnlockedSpeed() == 0.75f ? 1 : 0)
                            : 0;
                    spinnerSpeed.setSelection(restorePos);
                }
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void bindViews(View root) {
        tvSongTitle        = root.findViewById(R.id.song_title);
        tvSongAuthor       = root.findViewById(R.id.song_author);
        tvBest             = root.findViewById(R.id.tvBestScore);
        tvLast             = root.findViewById(R.id.tvLastScore);
        recycler           = root.findViewById(R.id.recyclerLyricsChords);
        recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter            = new LyricChordAdapter();
        recycler.setAdapter(adapter);
        gridView           = root.findViewById(R.id.gridWithPointsView);
        tvCurrentChordName = root.findViewById(R.id.tvCurrentChordName);
        btnStartStop       = root.findViewById(R.id.btnStartStopPractice);
        switchMode         = root.findViewById(R.id.switchMode);
        spinnerSpeed       = root.findViewById(R.id.spinnerSpeed);
        progressBar        = root.findViewById(R.id.progressBar);
        tvCountdown        = root.findViewById(R.id.tvCountdown);

        progressBar.setIndeterminate(false);
        progressBar.setMax(100);
        progressBar.setProgress(0);
    }

    private void setupSpeedSpinner() {
        ArrayAdapter<CharSequence> spAdapter = ArrayAdapter
                .createFromResource(requireContext(), R.array.speed_options,
                        android.R.layout.simple_spinner_item);
        spAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSpeed.setAdapter(spAdapter);
    }

    private void setupModeSwitch() {
        viewModel.mode.observe(getViewLifecycleOwner(), m -> {
            boolean sync = m == PracticeViewModel.Mode.SYNCHRONIZED;
            switchMode.setChecked(sync);
            spinnerSpeed.setVisibility(sync ? View.VISIBLE : View.GONE);
            progressBar.setVisibility(sync ? View.VISIBLE : View.GONE);
            tvBest.setVisibility(sync ? View.VISIBLE : View.GONE);
            tvLast.setVisibility(sync ? View.VISIBLE : View.GONE);
            progressBar.setProgress(0);
        });
        switchMode.setOnCheckedChangeListener((btn, checked) ->
                viewModel.setMode(checked
                        ? PracticeViewModel.Mode.SYNCHRONIZED
                        : PracticeViewModel.Mode.FREE));
    }

    private void setupAdapterClicks() {
        adapter.setOnChordClickListener(viewModel::setCurrentIndex);
    }

    private void observeLineItems() {
        viewModel.getLineItems()
                .observe(getViewLifecycleOwner(), adapter::submitList);
    }

    private void observeChordInfo() {
        viewModel.getSequenceWithInfo()
                .observe(getViewLifecycleOwner(), list -> {
                    chordInfoCache.clear();
                    if (list != null) chordInfoCache.addAll(list);
                });
    }

    private void observeCurrentIndex() {
        viewModel.currentIndex.observe(getViewLifecycleOwner(), idx -> {
            adapter.setActiveChordGlobalIndex(idx);
            if (idx >= 0 && idx < chordInfoCache.size()) {
                tvCurrentChordName.setText(chordInfoCache.get(idx).chord.getName());
                drawChordDiagram(idx);
            }
        });
    }

    private void observeCurrentLineIndex() {
        viewModel.getCurrentLineIndex()
                .observe(getViewLifecycleOwner(), line -> {
                    if (line != null && line >= 0) recycler.smoothScrollToPosition(line);
                });
    }

    private void observeCorrectIndices() {
        viewModel.getCorrectIndices()
                .observe(getViewLifecycleOwner(), adapter::setCorrectIndices);
    }

    private void observeProgress() {
        viewModel.getProgressPercent()
                .observe(getViewLifecycleOwner(), progressBar::setProgress);
    }

    private void observeScoreEvent() {
        viewModel.scoreEvent.observe(getViewLifecycleOwner(), ev -> {
            Integer score = ev.getIfNotHandled();
            if (score != null) showScoreDialog(score);
        });
    }

    private void showScoreDialog(int score) {
        String msg = score < 50
                ? "Has obtenido " + score + " puntos. Vuelve a intentarlo."
                : "¡Enhorabuena! Has obtenido " + score + " puntos.";

        Snackbar.make(requireView(), msg, Snackbar.LENGTH_LONG).show();
        btnStartStop.setText("Empezar");
    }

    private void observeRunningState() {
        viewModel.isRunning.observe(getViewLifecycleOwner(), running -> {
            switchMode.setEnabled(!running);
            spinnerSpeed.setEnabled(!running);
        });
    }

    private void setupStartStopButton() {
        btnStartStop.setOnClickListener(v -> {
            if (ActivityCompat.checkSelfPermission(
                    requireContext(), Manifest.permission.RECORD_AUDIO)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                        new String[]{Manifest.permission.RECORD_AUDIO},
                        REQUEST_MIC
                );
            } else {
                Boolean running = viewModel.isRunning.getValue();
                if (running == null || !running) {
                    viewModel.startCountdown();
                } else {
                    toggleDetection();
                }
            }
        });
    }

    private void toggleDetection() {
        Boolean running = viewModel.isRunning.getValue();
        if (running == null || !running) {
            viewModel.startDetection();
            btnStartStop.setText("Terminar");
        } else {
            viewModel.stopDetection();
            btnStartStop.setText("Empezar");
        }
    }

    private void drawChordDiagram(int idx) {
        if (idx < 0 || idx >= chordInfoCache.size()) return;
        SongChordWithInfo info = chordInfoCache.get(idx);
        gridView.setPointsFromHint(
                info.chord.getHint(),
                info.chord.getFingerHint()
        );
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_MIC
                && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            toggleDetection();
        }
    }

    @Override
    public void onChordDetected(String chordName) {
        // El ViewModel se encarga de gestionar este evento
    }
}
