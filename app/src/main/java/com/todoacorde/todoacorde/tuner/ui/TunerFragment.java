package com.todoacorde.todoacorde.tuner.ui;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.todoacorde.todoacorde.R;
import com.todoacorde.todoacorde.tuner.domain.TunerViewModel;
import com.todoacorde.todoacorde.tuner.domain.TuningResult;

import java.util.HashMap;
import java.util.Map;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * Fragmento de interfaz para el afinador.
 * Gestiona la selección de cuerda objetivo, el permiso de micrófono,
 * el inicio/parada de la detección de tono y la representación visual del resultado.
 */
@AndroidEntryPoint
public class TunerFragment extends Fragment {

    /** Código de solicitud para el permiso RECORD_AUDIO. */
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;

    // Referencias UI principales
    private View noteCircle;
    private TextView noteDisplay;
    private ImageView tuningMinus, tuningPlus;
    private TextView tensionAction;
    private FloatingActionButton btnMic;

    // Controles de selección de cuerda (clavija y círculo de resaltado)
    private ImageButton string1, string2, string3, string4, string5, string6;
    private ImageButton string1_circle, string2_circle, string3_circle, string4_circle, string5_circle, string6_circle;

    // Estado de selección
    private ImageButton selectedStringButton = null;
    private ImageButton selectedButton = null;

    // Animaciones del botón de micrófono
    private ObjectAnimator micAnimatorX, micAnimatorY;

    /** Mapa botón→nota objetivo (afinación estándar EADGBE). */
    private final Map<ImageButton, String> stringNoteMap = new HashMap<>();

    /** ViewModel que orquesta la lógica de afinación. */
    private TunerViewModel tunerViewModel;

    /** Barra de progreso de afinación (0–100). */
    private ProgressBar tuningBar;

    /** Marca de inicio diferido si el Fragment aún no está agregado cuando se solicita empezar. */
    private boolean pendingStartDetection = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_tuner, container, false);

        // Binding de vistas
        noteCircle = view.findViewById(R.id.noteCircle);
        noteDisplay = view.findViewById(R.id.noteDisplay);
        tuningMinus = view.findViewById(R.id.tuningMinus);
        tuningPlus = view.findViewById(R.id.tuningPlus);
        tensionAction = view.findViewById(R.id.tensionAction);
        btnMic = view.findViewById(R.id.btnMic);

        string1 = view.findViewById(R.id.string1_clavija);
        string2 = view.findViewById(R.id.string2_clavija);
        string3 = view.findViewById(R.id.string3_clavija);
        string4 = view.findViewById(R.id.string4_clavija);
        string5 = view.findViewById(R.id.string5_clavija);
        string6 = view.findViewById(R.id.string6_clavija);

        string1_circle = view.findViewById(R.id.string1);
        string2_circle = view.findViewById(R.id.string2);
        string3_circle = view.findViewById(R.id.string3);
        string4_circle = view.findViewById(R.id.string4);
        string5_circle = view.findViewById(R.id.string5);
        string6_circle = view.findViewById(R.id.string6);

        // Mapeo de cuerdas a su nota objetivo
        stringNoteMap.put(string1, "E4");
        stringNoteMap.put(string2, "B3");
        stringNoteMap.put(string3, "G3");
        stringNoteMap.put(string4, "D3");
        stringNoteMap.put(string5, "A2");
        stringNoteMap.put(string6, "E2");

        tuningBar = view.findViewById(R.id.tuningBar);

        // Estado inicial de UI
        hideAllStringButtons();
        selectButton(string6, string6_circle); // Selección inicial: sexta cuerda E2
        tuningPlus.setVisibility(View.INVISIBLE);
        tuningMinus.setVisibility(View.INVISIBLE);
        tensionAction.setText("");
        btnMic.setEnabled(false);

        // Listeners para selección de cuerda
        setUpStringSelectionListeners();

        // Solicitud de permiso de micrófono si es necesario
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(),
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    REQUEST_RECORD_AUDIO_PERMISSION);
        } else {
            btnMic.setEnabled(true);
        }

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Configuración del ViewModel y observación del resultado de afinación
        tunerViewModel = new ViewModelProvider(this).get(TunerViewModel.class);
        tunerViewModel.getTuningResult().observe(getViewLifecycleOwner(), this::updateTuningUI);

        // Si se solicitó empezar antes de estar añadido, ejecuta inicio ahora
        if (pendingStartDetection) {
            new Handler(Looper.getMainLooper()).post(this::startPitchDetection);
            pendingStartDetection = false;
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        stopPitchDetection();
    }

    /**
     * Inicia la detección de tono.
     * Comprueba permiso de micrófono y, si procede, arranca el flujo en el ViewModel.
     * Si el Fragment aún no está agregado, difiere el inicio.
     */
    public void startPitchDetection() {
        if (!isAdded()) {
            pendingStartDetection = true;
            return;
        }

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(),
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    REQUEST_RECORD_AUDIO_PERMISSION);
        } else {
            tunerViewModel.startTuning(requireContext());
            btnMic.setImageResource(R.drawable.ic_mic);
            startMicAnimation();
            resetTuningUI();
        }
    }

    /**
     * Detiene la detección de tono y restablece la UI relacionada con el micrófono.
     */
    public void stopPitchDetection() {
        tunerViewModel.stopTuning();
        stopMicAnimation();
        resetTuningUI();
    }

    /**
     * Actualiza los elementos de UI en función del {@link TuningResult} recibido.
     * Muestra indicadores de acción, actualiza texto de instrucción y
     * ajusta la barra de progreso de afinación.
     */
    private void updateTuningUI(TuningResult result) {
        noteCircle.setAlpha(1f);

        if (result.showPlus) {
            tuningPlus.setVisibility(View.VISIBLE);
            tuningPlus.setColorFilter(ContextCompat.getColor(requireContext(), R.color.red));
        } else {
            tuningPlus.setVisibility(View.INVISIBLE);
        }

        if (result.showMinus) {
            tuningMinus.setVisibility(View.VISIBLE);
            tuningMinus.setColorFilter(ContextCompat.getColor(requireContext(), R.color.red));
        } else {
            tuningMinus.setVisibility(View.INVISIBLE);
        }

        tensionAction.setText(result.actionText != null ? result.actionText : "");

        // Conversión simple de offset a progreso relativo centrado en 50
        float offset = result.getOffset();
        int center = 50;
        int progress = (int) (center + offset);
        progress = Math.max(0, Math.min(100, progress));
        tuningBar.setProgress(progress);
    }

    /**
     * Marca visualmente la cuerda seleccionada y comunica la nota objetivo al ViewModel.
     * Además, reinicia la UI de indicaciones.
     */
    private void selectButton(ImageButton pegButton, ImageButton circleButton) {
        if (selectedStringButton != null && selectedStringButton != pegButton) {
            selectedStringButton.setAlpha(0f);
            selectedStringButton.setBackground(null);
        }
        pegButton.setAlpha(1f);
        pegButton.setBackgroundResource(R.drawable.circle_translucent_selected);
        selectedStringButton = pegButton;

        if (selectedButton != null && selectedButton != circleButton) {
            selectedButton.clearColorFilter();
        }
        int highlightColor = ContextCompat.getColor(requireContext(), R.color.colorPrimary);
        circleButton.setColorFilter(highlightColor);
        selectedButton = circleButton;

        String note = stringNoteMap.get(pegButton);
        noteDisplay.setText(note);
        if (tunerViewModel != null) {
            tunerViewModel.setTargetNote(note);
        }

        resetTuningUI();
    }

    /**
     * Oculta y limpia el estado visual de todas las clavijas al inicializar.
     */
    private void hideAllStringButtons() {
        ImageButton[] buttons = {string1, string2, string3, string4, string5, string6};
        for (ImageButton b : buttons) {
            b.setAlpha(0f);
            b.setClickable(true);
            b.setBackground(null);
        }
    }

    /**
     * Configura listeners para seleccionar cuerda tanto desde la clavija como desde el círculo.
     */
    private void setUpStringSelectionListeners() {
        string1.setOnClickListener(v -> selectButton(string1, string1_circle));
        string1_circle.setOnClickListener(v -> selectButton(string1, string1_circle));

        string2.setOnClickListener(v -> selectButton(string2, string2_circle));
        string2_circle.setOnClickListener(v -> selectButton(string2, string2_circle));

        string3.setOnClickListener(v -> selectButton(string3, string3_circle));
        string3_circle.setOnClickListener(v -> selectButton(string3, string3_circle));

        string4.setOnClickListener(v -> selectButton(string4, string4_circle));
        string4_circle.setOnClickListener(v -> selectButton(string4, string4_circle));

        string5.setOnClickListener(v -> selectButton(string5, string5_circle));
        string5_circle.setOnClickListener(v -> selectButton(string5, string5_circle));

        string6.setOnClickListener(v -> selectButton(string6, string6_circle));
        string6_circle.setOnClickListener(v -> selectButton(string6, string6_circle));
    }

    /**
     * Inicia una animación de “latido” sobre el botón del micrófono mientras graba.
     */
    private void startMicAnimation() {
        micAnimatorX = ObjectAnimator.ofFloat(btnMic, "scaleX", 1f, 1.15f, 1f);
        micAnimatorX.setDuration(1000);
        micAnimatorX.setRepeatCount(ObjectAnimator.INFINITE);
        micAnimatorX.setInterpolator(new LinearInterpolator());
        micAnimatorX.start();

        micAnimatorY = ObjectAnimator.ofFloat(btnMic, "scaleY", 1f, 1.15f, 1f);
        micAnimatorY.setDuration(1000);
        micAnimatorY.setRepeatCount(ObjectAnimator.INFINITE);
        micAnimatorY.setInterpolator(new LinearInterpolator());
        micAnimatorY.start();
    }

    /**
     * Detiene y limpia la animación del botón de micrófono.
     */
    private void stopMicAnimation() {
        if (micAnimatorX != null) micAnimatorX.cancel();
        if (micAnimatorY != null) micAnimatorY.cancel();
        btnMic.setScaleX(1f);
        btnMic.setScaleY(1f);
    }

    /**
     * Restaura los indicadores de acción a estado neutro.
     */
    private void resetTuningUI() {
        tuningPlus.setVisibility(View.INVISIBLE);
        tuningMinus.setVisibility(View.INVISIBLE);
        tensionAction.setText("");
    }

    /**
     * Callback del resultado de la petición de permisos.
     * Si se concede, habilita y arranca la detección de tono.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                btnMic.setEnabled(true);
                startPitchDetection();
            } else {
                Toast.makeText(requireContext(), "Se necesita permiso de micrófono", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
