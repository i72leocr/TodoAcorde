package com.todoacorde.todoacorde.practice.ui;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.todoacorde.todoacorde.MainContainerActivity;
import com.todoacorde.todoacorde.R;
import com.todoacorde.todoacorde.practice.data.SongUserSpeed;
import com.todoacorde.todoacorde.practice.domain.PracticeViewModel;
import com.todoacorde.todoacorde.songs.data.SongChordWithInfo;

import java.util.ArrayList;
import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * Fragmento de práctica de acordes (optimizado).
 * Gestiona modos Libre/Sincronizado, selector de velocidad, progreso,
 * conteo regresivo, permisos de micrófono y visualización de diagramas.
 */
@AndroidEntryPoint
public class PracticeChordsOptimizedFragment extends Fragment {

    /* Constantes */
    private static final String TAG = "PracticeOptimizedFrag";
    private static final int REQUEST_MIC = 123;
    private static final String ARG_SONG_ID = "song_id";

    /* VM y vistas */
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

    /* Animaciones FAB micrófono */
    private ObjectAnimator micAnimatorX, micAnimatorY;

    /* Cache de acordes de la canción */
    private final List<SongChordWithInfo> chordInfoCache = new ArrayList<>();

    /* Callback para interceptar “atrás” durante práctica */
    private OnBackPressedCallback exitCallback;

    /**
     * Infla el layout del fragmento.
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_practice_chords_optimized, container, false);
    }

    /**
     * Inicializa vistas, observadores y estado al crear la vista.
     */
    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        /* Manejo de “atrás” con confirmación */
        exitCallback = new OnBackPressedCallback(false) {
            @Override
            public void handleOnBackPressed() {
                showExitPracticeDialog();
            }
        };
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), exitCallback);

        /* ViewModel */
        viewModel = new ViewModelProvider(this).get(PracticeViewModel.class);

        /* UI */
        bindViews(view);
        setupSpeedSpinner();
        setupModeSwitch();
        setupAdapterClicks();

        /* Observadores */
        observeLineItems();
        observeChordInfo();
        observeCurrentIndex();
        observeScrollPercent();
        observeCorrectIndices();
        observeProgress();
        observeScoreEvent();
        observeRunningState();
        setupStartStopButton();

        /* Argumentos: canción objetivo */
        Bundle args = getArguments();
        if (args != null && args.containsKey(ARG_SONG_ID)) {
            viewModel.initForSong(args.getInt(ARG_SONG_ID));
        } else {
            Log.e(TAG, "No song_id provided in fragment arguments");
        }

        /* Metadatos de la canción y puntuaciones */
        viewModel.songDetails.observe(getViewLifecycleOwner(), details -> {
            if (details != null && details.song != null) {
                tvSongTitle.setText(details.song.getTitle());
                tvSongAuthor.setText(details.song.getAuthor());
            }
        });
        viewModel.bestScore.observe(getViewLifecycleOwner(), best ->
                tvBest.setText("Mejor Puntaje: " + (best != null ? best : "--"))
        );
        viewModel.lastScore.observe(getViewLifecycleOwner(), last ->
                tvLast.setText("Último Puntaje: " + (last != null ? last : "--"))
        );

        /* Desbloqueos de velocidad */
        viewModel.unlockEvent.observe(getViewLifecycleOwner(), ev -> {
            String unlocked = ev.getIfNotHandled();
            if (unlocked != null) {
                Snackbar.make(requireView(),
                        "Has desbloqueado el modo " + unlocked + " 🎉",
                        Snackbar.LENGTH_LONG).show();
            }
        });

        /* Conteo regresivo */
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
            if (Boolean.TRUE.equals(counting) && requireActivity() instanceof MainContainerActivity) {
                ((MainContainerActivity) requireActivity()).setPracticeRunning(false);
            }
        });
        viewModel.getCountdownFinished().observe(getViewLifecycleOwner(), event -> {
            if (event != null && Boolean.TRUE.equals(event.getIfNotHandled())) {
                toggleDetection();
            }
        });

        /* Sincroniza spinner con velocidades desbloqueadas */
        viewModel.unlockedSpeeds.observe(getViewLifecycleOwner(), speeds -> {
            if (speeds == null) return;
            int bestUnlockedPos = speeds.isUnlocked1x ? 2 : (speeds.isUnlocked0_75x ? 1 : 0);
            if (spinnerSpeed.getSelectedItemPosition() != bestUnlockedPos) {
                spinnerSpeed.setOnItemSelectedListener(null);
                spinnerSpeed.setSelection(bestUnlockedPos, false);
                spinnerSpeed.setOnItemSelectedListener(speedSpinnerListener());
            }
            viewModel.setSpeedFactor(bestUnlockedPos == 0 ? 0.5 : (bestUnlockedPos == 1 ? 0.75 : 1.0));
        });

        /* Importante: set listener después de la primera observación */
        spinnerSpeed.setOnItemSelectedListener(speedSpinnerListener());
    }

    /**
     * Vincula referencias a vistas y aplica configuración básica.
     */
    private void bindViews(View root) {
        tvSongTitle = root.findViewById(R.id.song_title);
        tvSongAuthor = root.findViewById(R.id.song_author);
        tvBest = root.findViewById(R.id.tvBestScore);
        tvLast = root.findViewById(R.id.tvLastScore);

        recycler = root.findViewById(R.id.recyclerLyricsChords);
        recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new LyricChordAdapter();
        recycler.setAdapter(adapter);
        recycler.setVerticalFadingEdgeEnabled(true);

        int fadePx = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 16, getResources().getDisplayMetrics()
        );
        recycler.setFadingEdgeLength(fadePx);

        gridView = root.findViewById(R.id.gridWithPointsView);
        tvCurrentChordName = root.findViewById(R.id.tvCurrentChordName);
        btnStartStop = root.findViewById(R.id.btnStartStopPractice);
        switchMode = root.findViewById(R.id.switchMode);
        spinnerSpeed = root.findViewById(R.id.spinnerSpeed);
        progressBar = root.findViewById(R.id.progressBar);
        tvCountdown = root.findViewById(R.id.tvCountdown);
        cbMetronome = root.findViewById(R.id.cbMetronome);
        fabMic = root.findViewById(R.id.btnMic);

        progressBar.setIndeterminate(false);
        progressBar.setMax(100);
        progressBar.setProgress(0);
    }

    /**
     * Rellena el spinner con las opciones de velocidad.
     */
    private void setupSpeedSpinner() {
        ArrayAdapter<CharSequence> spAdapter = ArrayAdapter.createFromResource(
                requireContext(), R.array.speed_options, android.R.layout.simple_spinner_item
        );
        spAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSpeed.setAdapter(spAdapter);
    }

    /**
     * Conecta el switch de modo con el VM y ajusta visibilidad de UI.
     */
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
                viewModel.setMode(checked ? PracticeViewModel.Mode.SYNCHRONIZED : PracticeViewModel.Mode.FREE)
        );
    }

    /**
     * Clicks sobre acordes en el RecyclerView.
     */
    private void setupAdapterClicks() {
        adapter.setOnChordClickListener(viewModel::setCurrentIndex);
    }

    /**
     * Observa líneas letra/acorde y las pasa al adaptador.
     */
    private void observeLineItems() {
        viewModel.getLineItems().observe(getViewLifecycleOwner(), adapter::submitList);
    }

    /**
     * Mantiene cache local de acordes enriquecidos para dibujar el diagrama.
     */
    private void observeChordInfo() {
        viewModel.getSequenceWithInfo().observe(getViewLifecycleOwner(), list -> {
            chordInfoCache.clear();
            if (list != null) chordInfoCache.addAll(list);
        });
    }

    /**
     * Sincroniza acorde activo, nombre visible y diagrama.
     */
    private void observeCurrentIndex() {
        viewModel.currentIndex.observe(getViewLifecycleOwner(), idx -> {
            adapter.setActiveChordGlobalIndex(idx);
            if (idx != null && idx >= 0 && idx < chordInfoCache.size()) {
                tvCurrentChordName.setText(chordInfoCache.get(idx).chord.getName());
                drawChordDiagram(idx);
            } else {
                tvCurrentChordName.setText("");
                gridView.clearDiagram();
            }
        });
    }

    /**
     * Desplaza automáticamente la lista según el porcentaje calculado.
     */
    private void observeScrollPercent() {
        viewModel.scrollPercent.observe(getViewLifecycleOwner(), pct -> {
            int range = recycler.computeVerticalScrollRange() - recycler.computeVerticalScrollExtent();
            int targetOffset = Math.round(range * pct);
            int currentOffset = recycler.computeVerticalScrollOffset();
            recycler.scrollBy(0, targetOffset - currentOffset);
        });
    }

    /**
     * Pinta los índices correctos en el adaptador.
     */
    private void observeCorrectIndices() {
        viewModel.getCorrectIndices().observe(getViewLifecycleOwner(), adapter::setCorrectIndices);
    }

    /**
     * Barra de progreso para modo sincronizado.
     */
    private void observeProgress() {
        viewModel.getProgressPercent().observe(getViewLifecycleOwner(), progressBar::setProgress);
    }

    /**
     * Muestra resultado de puntuación y resetea visuales.
     */
    private void observeScoreEvent() {
        viewModel.scoreEvent.observe(getViewLifecycleOwner(), ev -> {
            Integer score = ev.getIfNotHandled();
            if (score != null) {
                tvCurrentChordName.setText("");
                showScoreResult(score);
                adapter.resetVisualState();
                gridView.clearDiagram();
            }
        });
    }

    /**
     * Bloquea/desbloquea controles según ejecución y anima el FAB.
     */
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

    /**
     * Configura botón principal: permisos + inicio/fin.
     */
    private void setupStartStopButton() {
        btnStartStop.setOnClickListener(v -> {
            if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_MIC);
            } else {
                Boolean running = viewModel.isRunning.getValue();
                if (running == null || !running) {
                    viewModel.setMetronomeEnabled(cbMetronome.isChecked());
                    viewModel.startCountdown();
                } else {
                    toggleDetection();
                }
            }
        });
    }

    /**
     * Alterna entre iniciar o detener la detección.
     */
    private void toggleDetection() {
        Boolean running = viewModel.isRunning.getValue();
        if (running == null || !running) {
            viewModel.startDetection();
        } else {
            viewModel.stopDetection();
        }
    }

    /**
     * Dibuja el diagrama del acorde activo.
     */
    private void drawChordDiagram(int idx) {
        if (idx < 0 || idx >= chordInfoCache.size()) return;
        SongChordWithInfo info = chordInfoCache.get(idx);
        gridView.setPointsFromHint(info.chord.getHint(), info.chord.getFingerHint());
    }

    /**
     * Listener del spinner de velocidad con validación de desbloqueos.
     */
    @NonNull
    private AdapterView.OnItemSelectedListener speedSpinnerListener() {
        return new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                double factor = (pos == 0 ? 0.5 : (pos == 1 ? 0.75 : 1.0));
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
                            ? (sus.getMaxUnlockedSpeed() == 1.0f ? 2
                            : (sus.getMaxUnlockedSpeed() == 0.75f ? 1 : 0))
                            : 0;
                    spinnerSpeed.setSelection(restorePos);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                /* No-op */
            }
        };
    }

    /**
     * Diálogo de salida de práctica en curso.
     */
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

    /**
     * Llamable desde la Activity para terminar la práctica externamente.
     */
    public void terminatePracticeFromActivity() {
        if (viewModel != null) {
            viewModel.endPractice();
            adapter.resetVisualState();
            gridView.clearDiagram();
        }
    }

    /**
     * Muestra un Snackbar con la puntuación obtenida y resetea el botón.
     */
    private void showScoreResult(int score) {
        String msg = score < 50
                ? "Has obtenido " + score + " puntos. Vuelve a intentarlo."
                : "¡Enhorabuena! Has obtenido " + score + " puntos.";
        Snackbar.make(requireView(), msg, Snackbar.LENGTH_LONG).show();
        btnStartStop.setText("Empezar");
    }

    /**
     * Inicia animación de “latido” del FAB de micrófono.
     */
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

    /**
     * Detiene la animación del FAB y restablece la escala.
     */
    private void stopMicAnimation() {
        if (micAnimatorX != null) micAnimatorX.cancel();
        if (micAnimatorY != null) micAnimatorY.cancel();
        fabMic.setScaleX(1f);
        fabMic.setScaleY(1f);
    }

    /**
     * Resultado de la solicitud de permisos de micrófono.
     */
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
}
