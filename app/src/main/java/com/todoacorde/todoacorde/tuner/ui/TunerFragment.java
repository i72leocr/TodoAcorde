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
 * Fragmento del afinador de guitarra.
 * 
 * Responsabilidades principales:
 * 
 *   Gestionar la UI del afinador (nota objetivo, barra de afinación, indicadores de tensar/destensar).</li>
 *   Orquestar el inicio/parada de la detección de tono a través de {@link TunerViewModel}.</li>
 *   Manejar permisos de micrófono y el estado de animación del botón de grabación.</li>
 *   Permitir la selección de cuerda objetivo y reflejarla en el {@link TunerViewModel}.</li>
 * 
 *
 * Notas de diseño:
 * 
 *   Se utiliza Hilt para la inyección de dependencias (@AndroidEntryPoint).</li>
 *   La observación de {@link TuningResult} actualiza la UI únicamente en el hilo principal.</li>
 *   La animación del micrófono se gestiona con {@link ObjectAnimator} y se cancela en onPause() para evitar fugas.</li>
 * 
 */
@AndroidEntryPoint
public class TunerFragment extends Fragment {

    /** Botón flotante para activar/desactivar la detección por micrófono. */
    private FloatingActionButton btnMic;
    /** Texto que muestra la nota objetivo seleccionada (E2, A2, etc.). */
    private TextView noteDisplay;
    /** Barra de progreso que representa el desvío respecto a la afinación. */
    private ProgressBar tuningBar;
    /** Icono que indica que hay que destensar (ir hacia abajo). */
    private ImageView tuningMinus;
    /** Icono que indica que hay que tensar (ir hacia arriba). */
    private ImageView tuningPlus;
    /** Texto que muestra la acción recomendada (TENSAR/DESTENSAR). */
    private TextView tensionAction;
    /** Etiqueta "AFINADO" que aparece cuando se está en tono. */
    private TextView tunedStatus;

    /** Botones de clavijas (representan la selección de cuerda por clavija). */
    private ImageButton string1, string2, string3, string4, string5, string6;
    /** Botones circulares (indicadores visuales de la cuerda seleccionada). */
    private ImageButton string1_circle, string2_circle, string3_circle, string4_circle, string5_circle, string6_circle;

    /** Referencia a la clavija actualmente seleccionada. */
    private ImageButton selectedStringButton = null;
    /** Referencia al círculo actualmente seleccionado (para resaltar). */
    private ImageButton selectedButton = null;

    /**
     * Mapa de clavija a nota objetivo. Permite traducir la selección de UI
     * a la nota que consumirá el {@link TunerViewModel}.
     */
    private final Map<ImageButton, String> stringNoteMap = new HashMap<>();

    /** ViewModel que encapsula la lógica de detección de tono y afinado. */
    private TunerViewModel tunerViewModel;

    /** Animadores para el efecto de latido en el botón del micrófono (escala X/Y). */
    private ObjectAnimator micAnimatorX;
    private ObjectAnimator micAnimatorY;

    /** Handler asociado al hilo principal, útil para publicar cambios de UI. */
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    /** Código de solicitud de permiso para RECORD_AUDIO. */
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 2001;

    /** Bandera para diferir el inicio de la detección si el Fragment aún no está añadido. */
    private boolean pendingStartDetection = false;

    /** {@inheritDoc} */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_tuner, container, false);
    }

    /** {@inheritDoc} */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        /* === Referencias de UI === */
        noteDisplay   = view.findViewById(R.id.noteDisplay);
        tuningMinus   = view.findViewById(R.id.tuningMinus);
        tuningPlus    = view.findViewById(R.id.tuningPlus);
        tensionAction = view.findViewById(R.id.tensionAction);
        tunedStatus   = view.findViewById(R.id.tunedStatus);
        btnMic        = view.findViewById(R.id.btnMic);
        tuningBar     = view.findViewById(R.id.tuningBar);

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

        /* Estado inicial de visibilidad/estilo */
        tuningPlus.setVisibility(View.INVISIBLE);
        tuningMinus.setVisibility(View.INVISIBLE);
        tensionAction.setText("");
        tunedStatus.setVisibility(View.GONE);

        /* ViewModel y observación de resultados de afinado */
        tunerViewModel = new ViewModelProvider(this).get(TunerViewModel.class);
        tunerViewModel.getTuningResult().observe(getViewLifecycleOwner(), this::updateTuningUI);

        /* Mapeo de clavijas a notas estándar (EADGBE) */
        stringNoteMap.put(string1, "E4");
        stringNoteMap.put(string2, "B3");
        stringNoteMap.put(string3, "G3");
        stringNoteMap.put(string4, "D3");
        stringNoteMap.put(string5, "A2");
        stringNoteMap.put(string6, "E2");

        /* Listeners de selección de cuerda */
        setUpStringSelectionListeners();

        /* Selección por defecto: 6ª cuerda (E2) */
        selectButton(string6, string6_circle);

        /* Click del micrófono: alterna inicio/parada de la detección */
        btnMic.setOnClickListener(v -> {
            if (micAnimatorX != null && micAnimatorX.isRunning()) {
                stopPitchDetection();
            } else {
                startPitchDetection();
            }
        });

        /* Permiso de audio: deshabilita botón hasta que se conceda */
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            btnMic.setEnabled(false);
            ActivityCompat.requestPermissions(requireActivity(),
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    REQUEST_RECORD_AUDIO_PERMISSION);
        } else {
            btnMic.setEnabled(true);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void onResume() {
        super.onResume();
        /* Si se solicitó iniciar detección antes de estar añadido, hazlo ahora. */
        if (pendingStartDetection) {
            pendingStartDetection = false;
            startPitchDetection();
        }
    }

    /** {@inheritDoc} */
    @Override
    public void onPause() {
        super.onPause();
        /* Al pausar el fragmento se detiene la detección y la animación. */
        stopPitchDetection();
    }

    /**
     * Lanza de forma segura la detección de tono.
     * 
     * - Si el Fragment aún no está añadido, pospone el inicio hasta onResume().
     * - Gestiona la solicitud del permiso de audio si aún no está concedido.
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
     * Detiene la detección de tono y restaura la UI relacionada con el micrófono.
     */
    public void stopPitchDetection() {
        tunerViewModel.stopTuning();
        stopMicAnimation();
        resetTuningUI();
    }

    /**
     * Actualiza la UI con el resultado del afinado.
     * 
     * Comportamiento:
     * 
     *   Actualiza la barra de progreso con {@link TuningResult#progress}.</li>
     *   Muestra el icono correspondiente a tensar/destensar según {@link TuningResult#showPlus} / {@link TuningResult#showMinus}.</li>
     *   Si está en tono, muestra "AFINADO" y oculta el texto de acción; en caso contrario, muestra la acción recomendada.</li>
     * 
     *
     * @param result objeto con el estado de afinación calculado por el ViewModel.
     */
    private void updateTuningUI(TuningResult result) {
        /* Progreso de la afinación */
        tuningBar.setProgress(result.progress);

        /* Indicadores de dirección de ajuste */
        if (result.showPlus) {
            tuningPlus.setVisibility(View.VISIBLE);
            tuningMinus.setVisibility(View.INVISIBLE);
            tuningPlus.setColorFilter(ContextCompat.getColor(requireContext(), R.color.red));
        } else if (result.showMinus) {
            tuningMinus.setVisibility(View.VISIBLE);
            tuningPlus.setVisibility(View.INVISIBLE);
            tuningMinus.setColorFilter(ContextCompat.getColor(requireContext(), R.color.red));
        } else {
            tuningPlus.setVisibility(View.INVISIBLE);
            tuningMinus.setVisibility(View.INVISIBLE);
        }

        /* Estado "en tono" */
        boolean inTune = !result.showPlus && !result.showMinus && (result.actionText == null || result.actionText.isEmpty());
        if (inTune) {
            tunedStatus.setVisibility(View.VISIBLE);
            tensionAction.setVisibility(View.GONE);
        } else {
            tunedStatus.setVisibility(View.GONE);
            tensionAction.setVisibility(View.VISIBLE);
            tensionAction.setText(result.actionText != null ? result.actionText : "");
        }
    }

    /**
     * Selecciona la cuerda objetivo y actualiza los estilos de la UI.
     * 
     * - Resalta la clavija seleccionada y el círculo asociado.
     * - Actualiza la nota objetivo en el {@link TunerViewModel} y en el texto de nota.
     * - Resetea los indicadores de afinación.
     *
     * @param pegButton    botón de la clavija a seleccionar.
     * @param circleButton botón circular asociado a la cuerda.
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
     * Configura los listeners de selección para cada combinación de clavija y círculo.
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
     * Inicia la animación de latido del botón de micrófono (escala X/Y).
     * 
     * Configura animaciones cíclicas sin fin con {@link LinearInterpolator}.
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
     * Detiene la animación de latido del botón de micrófono y restaura la escala por defecto.
     */
    private void stopMicAnimation() {
        if (micAnimatorX != null) micAnimatorX.cancel();
        if (micAnimatorY != null) micAnimatorY.cancel();
        btnMic.setScaleX(1f);
        btnMic.setScaleY(1f);
    }

    /**
     * Restaura el estado visual de los indicadores de afinación a su configuración base.
     */
    private void resetTuningUI() {
        tuningPlus.setVisibility(View.INVISIBLE);
        tuningMinus.setVisibility(View.INVISIBLE);
        tensionAction.setText("");
        tensionAction.setVisibility(View.VISIBLE);
        tunedStatus.setVisibility(View.GONE);
    }

    /** {@inheritDoc} */
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
