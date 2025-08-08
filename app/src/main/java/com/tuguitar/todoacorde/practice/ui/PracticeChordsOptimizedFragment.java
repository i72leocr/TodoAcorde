package com.tuguitar.todoacorde.practice.ui;

import static androidx.navigation.fragment.FragmentKt.findNavController;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
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

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.tuguitar.todoacorde.MainContainerActivity;
import com.tuguitar.todoacorde.R;
import com.tuguitar.todoacorde.songs.data.SongChordWithInfo;
import com.tuguitar.todoacorde.practice.data.SongUserSpeed;
import com.tuguitar.todoacorde.practice.domain.PracticeViewModel;

import android.animation.ObjectAnimator;
import android.view.animation.LinearInterpolator;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.navigation.fragment.NavHostFragment;



import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class PracticeChordsOptimizedFragment extends Fragment {
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
    private CheckBox cbMetronome;

    private FloatingActionButton fabMic;
    private ObjectAnimator micAnimatorX, micAnimatorY;

    private final List<SongChordWithInfo> chordInfoCache = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_practice_chords_optimized, container, false);
    }

    private OnBackPressedCallback exitCallback;

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Inicializar el OnBackPressedCallback deshabilitado
        exitCallback = new OnBackPressedCallback(false) {
            @Override
            public void handleOnBackPressed() {
                showExitPracticeDialog();
            }
        };
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), exitCallback);

        // 1) Obtain the ViewModel via Hilt
        viewModel = new ViewModelProvider(this).get(PracticeViewModel.class);

        // 2) Bind views and configure UI
        bindViews(view);
        setupSpeedSpinner();
        setupModeSwitch();
        setupAdapterClicks();
        observeLineItems();
        observeChordInfo();
        observeCurrentIndex();
        observeScrollPercent();
        observeCorrectIndices();
        observeProgress();
        observeScoreEvent();
        observeRunningState();  // Aquí dentro se activa/desactiva el callback
        setupStartStopButton();

        // 3) Initialize practice with the provided songId argument
        Bundle args = getArguments();
        if (args != null && args.containsKey(ARG_SONG_ID)) {
            viewModel.initForSong(args.getInt(ARG_SONG_ID));
        } else {
            Log.e(TAG, "No song_id provided in fragment arguments");
        }

        // 4) Additional observers
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

            if (Boolean.TRUE.equals(counting)) {
                if (requireActivity() instanceof MainContainerActivity) {
                    ((MainContainerActivity) requireActivity()).setPracticeRunning(false);
                }
            }
        });

        viewModel.getCountdownFinished().observe(getViewLifecycleOwner(), event -> {
            if (event != null && Boolean.TRUE.equals(event.getIfNotHandled())) {
                toggleDetection(); // start after countdown finishes
            }
        });
        viewModel.unlockedSpeeds.observe(getViewLifecycleOwner(), speeds -> {
            if (speeds == null) return;
            int bestUnlockedPos = speeds.isUnlocked1x ? 2 :
                    speeds.isUnlocked0_75x ? 1 : 0;
            if (spinnerSpeed.getSelectedItemPosition() != bestUnlockedPos) {
                // Update spinner selection without triggering listener
                spinnerSpeed.setOnItemSelectedListener(null);
                spinnerSpeed.setSelection(bestUnlockedPos, false);
                spinnerSpeed.setOnItemSelectedListener(speedSpinnerListener());
            }
            viewModel.setSpeedFactor(bestUnlockedPos == 0 ? 0.5 : bestUnlockedPos == 1 ? 0.75 : 1.0);
        });

        // Set initial spinner listener
        spinnerSpeed.setOnItemSelectedListener(speedSpinnerListener());
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
        // Habilita el fading edge vertical (suave degradado arriba/abajo)
        recycler.setVerticalFadingEdgeEnabled(true);
        int fadePx = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                16,
                getResources().getDisplayMetrics()
        );
        recycler.setFadingEdgeLength(fadePx);
        gridView           = root.findViewById(R.id.gridWithPointsView);
        tvCurrentChordName = root.findViewById(R.id.tvCurrentChordName);
        btnStartStop       = root.findViewById(R.id.btnStartStopPractice);
        switchMode         = root.findViewById(R.id.switchMode);
        spinnerSpeed       = root.findViewById(R.id.spinnerSpeed);
        progressBar        = root.findViewById(R.id.progressBar);
        tvCountdown        = root.findViewById(R.id.tvCountdown);
        cbMetronome  = root.findViewById(R.id.cbMetronome);  // Nuevo: checkbox metrónomo
        fabMic = root.findViewById(R.id.btnMic);
        progressBar.setIndeterminate(false);
        progressBar.setMax(100);
        progressBar.setProgress(0);
    }

    private void setupSpeedSpinner() {
        ArrayAdapter<CharSequence> spAdapter = ArrayAdapter.createFromResource(
                requireContext(), R.array.speed_options, android.R.layout.simple_spinner_item
        );
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
        switchMode.setOnCheckedChangeListener((btn, checked) -> {
            viewModel.setMode(checked ? PracticeViewModel.Mode.SYNCHRONIZED
                    : PracticeViewModel.Mode.FREE);
        });
    }

    private void setupAdapterClicks() {
        adapter.setOnChordClickListener(viewModel::setCurrentIndex);
    }

    private void observeLineItems() {
        viewModel.getLineItems().observe(getViewLifecycleOwner(), adapter::submitList);
    }

    private void observeChordInfo() {
        viewModel.getSequenceWithInfo().observe(getViewLifecycleOwner(), list -> {
            chordInfoCache.clear();
            if (list != null) chordInfoCache.addAll(list);
        });
    }

    private void observeCurrentIndex() {
        viewModel.currentIndex.observe(getViewLifecycleOwner(), idx -> {
            adapter.setActiveChordGlobalIndex(idx);
            if (idx != null && idx >= 0 && idx < chordInfoCache.size()) {
                tvCurrentChordName.setText(chordInfoCache.get(idx).chord.getName());
                drawChordDiagram(idx);
            } else {
                tvCurrentChordName.setText("");        // Limpia el nombre
                gridView.clearDiagram();               // ← AÑADE ESTA LÍNEA (debes implementarla en GridWithPointsView)
            }
        });
    }


    private void observeScrollPercent() {
        viewModel.scrollPercent.observe(getViewLifecycleOwner(), pct -> {
            // 1) total de contenido – lo que cabe en pantalla
            int range = recycler.computeVerticalScrollRange()
                    - recycler.computeVerticalScrollExtent();
            // 2) offset objetivo según porcentaje
            int targetOffset = Math.round(range * pct);
            // 3) offset actual
            int currentOffset = recycler.computeVerticalScrollOffset();
            // 4) desplazar la diferencia
            recycler.scrollBy(0, targetOffset - currentOffset);
        });
    }


    private void observeCorrectIndices() {
        viewModel.getCorrectIndices().observe(getViewLifecycleOwner(), adapter::setCorrectIndices);
    }

    private void observeProgress() {
        viewModel.getProgressPercent().observe(getViewLifecycleOwner(), progressBar::setProgress);
    }

    private void observeScoreEvent() {
        viewModel.scoreEvent.observe(getViewLifecycleOwner(), ev -> {
            Integer score = ev.getIfNotHandled();
            if (score != null) {
             tvCurrentChordName.setText("");             // Quita nombre del acorde
                showScoreResult(score);
                adapter.resetVisualState();         // ← AÑADIR AQUÍ
                gridView.clearDiagram();            // ← Y AQUÍ
            }
        });
    }

    private void showScoreResult(int score) {
        String msg = score < 50
                ? "Has obtenido " + score + " puntos. Vuelve a intentarlo."
                : "¡Enhorabuena! Has obtenido " + score + " puntos.";
        Snackbar.make(requireView(), msg, Snackbar.LENGTH_LONG).show();
        btnStartStop.setText("Empezar");
    }

    private void observeRunningState() {
        viewModel.isRunning.observe(getViewLifecycleOwner(), running -> {

            boolean isActive = Boolean.TRUE.equals(running);

            switchMode.setEnabled(!isActive);
            spinnerSpeed.setEnabled(!isActive);
            cbMetronome.setEnabled(!isActive);
            btnStartStop.setText(isActive ? "Terminar" : "Empezar");

            if (exitCallback != null) {
                exitCallback.setEnabled(isActive);
            }
            if (isActive) {
                fabMic.setVisibility(View.VISIBLE);
                startMicAnimation();
            } else {
                stopMicAnimation();
                fabMic.setVisibility(View.GONE);
            }
            if (requireActivity() instanceof MainContainerActivity) {
                ((MainContainerActivity) requireActivity()).setPracticeRunning(isActive);
            }
        });
    }


    private void setupStartStopButton() {
        btnStartStop.setOnClickListener(v -> {
            // Request microphone permission if not granted
            if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_MIC);
            } else {
                Boolean running = viewModel.isRunning.getValue();
                if (running == null || !running) {
                    viewModel.setMetronomeEnabled(cbMetronome.isChecked());
                    // Start with a countdown if not already running
                    viewModel.startCountdown();
                } else {
                    // If currently running, stop the practice
                    toggleDetection();
                }
            }
        });
    }

    private void toggleDetection() {
        Boolean running = viewModel.isRunning.getValue();
        if (running == null || !running) {
            viewModel.startDetection();
        } else {
            viewModel.stopDetection();
        }
    }

    private void drawChordDiagram(int idx) {
        if (idx < 0 || idx >= chordInfoCache.size()) return;
        SongChordWithInfo info = chordInfoCache.get(idx);
        gridView.setPointsFromHint(info.chord.getHint(), info.chord.getFingerHint());
    }

    @NonNull
    private AdapterView.OnItemSelectedListener speedSpinnerListener() {
        return new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
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
                    // Revert to highest unlocked speed
                    int restorePos = sus != null
                            ? (sus.getMaxUnlockedSpeed() == 1.0f ? 2
                            : sus.getMaxUnlockedSpeed() == 0.75f ? 1 : 0)
                            : 0;
                    spinnerSpeed.setSelection(restorePos);
                }
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        };
    }

    private void showExitPracticeDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("¿Salir de la práctica?")
                .setMessage("Si sales, perderás tu progreso actual. ¿Deseas continuar?")
                .setPositiveButton("Salir", (dialog, which) -> {
                    viewModel.endPractice();
                    adapter.resetVisualState();
                    gridView.clearDiagram();
                    ((MainContainerActivity) requireActivity()).setPracticeRunning(false);
                    requireActivity().getSupportFragmentManager().popBackStack();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }
    public void terminatePracticeFromActivity() {
        if (viewModel != null) {
            viewModel.endPractice();
            adapter.resetVisualState();         // ← AÑADIR AQUÍ
            gridView.clearDiagram();
        }
    }

    private void startMicAnimation() {
        micAnimatorX = ObjectAnimator.ofFloat(fabMic, "scaleX", 1f, 1.15f, 1f);
        micAnimatorX.setDuration(1000);
        micAnimatorX.setRepeatCount(ObjectAnimator.INFINITE);
        micAnimatorX.setInterpolator(new LinearInterpolator());
        micAnimatorX.start();

        micAnimatorY = ObjectAnimator.ofFloat(fabMic, "scaleY", 1f, 1.15f, 1f);
        micAnimatorY.setDuration(1000);
        micAnimatorY.setRepeatCount(ObjectAnimator.INFINITE);
        micAnimatorY.setInterpolator(new LinearInterpolator());
        micAnimatorY.start();
    }

    private void stopMicAnimation() {
        if (micAnimatorX != null) micAnimatorX.cancel();
        if (micAnimatorY != null) micAnimatorY.cancel();
        fabMic.setScaleX(1f);
        fabMic.setScaleY(1f);
    }


    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_MIC && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            toggleDetection();
        }
    }
}
