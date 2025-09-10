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

@AndroidEntryPoint
public class TunerFragment extends Fragment {

    
    private FloatingActionButton btnMic;
    private TextView noteDisplay;
    private ProgressBar tuningBar;
    private ImageView tuningMinus, tuningPlus;
    private TextView tensionAction;
    private TextView tunedStatus; 

    
    private ImageButton string1, string2, string3, string4, string5, string6;
    private ImageButton string1_circle, string2_circle, string3_circle, string4_circle, string5_circle, string6_circle;

    
    private ImageButton selectedStringButton = null;
    private ImageButton selectedButton = null;

    
    private final Map<ImageButton, String> stringNoteMap = new HashMap<>();

    
    private TunerViewModel tunerViewModel;

    
    private ObjectAnimator micAnimatorX;
    private ObjectAnimator micAnimatorY;

    
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 2001;

    
    private boolean pendingStartDetection = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_tuner, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        
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

        
        tuningPlus.setVisibility(View.INVISIBLE);
        tuningMinus.setVisibility(View.INVISIBLE);
        tensionAction.setText("");
        tunedStatus.setVisibility(View.GONE); 

        
        tunerViewModel = new ViewModelProvider(this).get(TunerViewModel.class);
        tunerViewModel.getTuningResult().observe(getViewLifecycleOwner(), this::updateTuningUI);

        
        stringNoteMap.put(string1, "E4");
        stringNoteMap.put(string2, "B3");
        stringNoteMap.put(string3, "G3");
        stringNoteMap.put(string4, "D3");
        stringNoteMap.put(string5, "A2");
        stringNoteMap.put(string6, "E2");

        
        setUpStringSelectionListeners();

        
        selectButton(string6, string6_circle);

        
        btnMic.setOnClickListener(v -> {
            
            if (micAnimatorX != null && micAnimatorX.isRunning()) {
                stopPitchDetection();
            } else {
                startPitchDetection();
            }
        });

        
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

    @Override
    public void onResume() {
        super.onResume();
        if (pendingStartDetection) {
            pendingStartDetection = false;
            startPitchDetection();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        stopPitchDetection();
    }

    /** Método seguro para lanzar detección desde fuera del fragmento (post-onViewCreated). */
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

    public void stopPitchDetection() {
        tunerViewModel.stopTuning();
        stopMicAnimation();
        resetTuningUI();
    }

    /**
     * Actualiza la UI con el resultado del afinado.
     * - Muestra "AFINADO" (verde) justo debajo de la barra cuando está en tono.
     * - Si no, muestra TENSAR/DESTENSAR y los iconos correspondientes.
     */
    private void updateTuningUI(TuningResult result) {
        
        tuningBar.setProgress(result.progress);

        
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

        
        boolean inTune = !result.showPlus && !result.showMinus && (result.actionText == null || result.actionText.isEmpty());
        if (inTune) {
            tunedStatus.setVisibility(View.VISIBLE);   
            tensionAction.setVisibility(View.GONE);
        } else {
            tunedStatus.setVisibility(View.GONE);      
            tensionAction.setVisibility(View.VISIBLE);
        }
    }

    
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

    private void stopMicAnimation() {
        if (micAnimatorX != null) micAnimatorX.cancel();
        if (micAnimatorY != null) micAnimatorY.cancel();
        btnMic.setScaleX(1f);
        btnMic.setScaleY(1f);
    }

    
    private void resetTuningUI() {
        tuningPlus.setVisibility(View.INVISIBLE);
        tuningMinus.setVisibility(View.INVISIBLE);
        tensionAction.setText("");
        tensionAction.setVisibility(View.VISIBLE);
        tunedStatus.setVisibility(View.GONE); 
        
    }

    
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
