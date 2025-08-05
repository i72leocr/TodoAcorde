package com.tuguitar.todoacorde;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TunerFragment extends Fragment implements PitchDetectorCallback {

    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private static final int BUFFER_SIZE = 16;
    private static final double TUNING_THRESHOLD = 0.5;

    private View noteCircle;
    private TextView noteDisplay;
    private SeekBar tuningBar;
    private ImageView tuningMinus, tuningPlus;
    private TextView tensionAction;
    private FloatingActionButton btnMic;

    private ObjectAnimator micAnimatorX, micAnimatorY;

    private ImageButton string1, string2, string3, string4, string5, string6;
    private ImageButton string1_circle, string2_circle, string3_circle, string4_circle, string5_circle, string6_circle;

    private ImageButton selectedStringButton = null;
    private ImageButton selectedButton = null;

    private final Map<ImageButton, String> stringNoteMap = new HashMap<>();
    private PitchDetector pitchDetector;
    private final List<Double> pitchBuffer = new ArrayList<>();
    private static final Map<String, double[]> NOTE_FREQUENCIES = new HashMap<>();

    static {
        NOTE_FREQUENCIES.put("E2", new double[]{80.10, 82.41, 84.86});
        NOTE_FREQUENCIES.put("A2", new double[]{106.88, 110.00, 113.28});
        NOTE_FREQUENCIES.put("D3", new double[]{142.71, 146.83, 151.43});
        NOTE_FREQUENCIES.put("G3", new double[]{191.67, 196.00, 202.83});
        NOTE_FREQUENCIES.put("B3", new double[]{240.24, 246.94, 254.35});
        NOTE_FREQUENCIES.put("E4", new double[]{319.35, 329.63, 339.43});
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_tuner, container, false);

        noteCircle = view.findViewById(R.id.noteCircle);
        noteDisplay = view.findViewById(R.id.noteDisplay);
        tuningBar = view.findViewById(R.id.tuningBar);
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

        stringNoteMap.put(string1, "E4");
        stringNoteMap.put(string2, "B3");
        stringNoteMap.put(string3, "G3");
        stringNoteMap.put(string4, "D3");
        stringNoteMap.put(string5, "A2");
        stringNoteMap.put(string6, "E2");

        hideAllStringButtons();
        selectButton(string6, string6_circle);

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

        tuningBar.setMax(100);
        tuningBar.setProgress(50);
        tuningMinus.setVisibility(View.INVISIBLE);
        tuningPlus.setVisibility(View.INVISIBLE);

        btnMic.setEnabled(false);

        // Inicializamos detector pero NO arrancamos
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(),
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    REQUEST_RECORD_AUDIO_PERMISSION);
        } else if (pitchDetector == null) {
            pitchDetector = new PitchDetector(this, requireContext());
        }

        return view;
    }

    @Override
    public void onPause() {
        super.onPause();
        stopPitchDetection();
    }

    @Override
    public void onResume() {
        super.onResume();
        // No arranca automáticamente
    }

    /**
     * Inicia la detección de pitch. Llamar desde el fragment padre.
     */
    public void startPitchDetection() {
        if (pitchDetector != null) {
            pitchDetector.startDetection();
            btnMic.setImageResource(R.drawable.ic_mic);
            startMicAnimation();
            resetTuningUI();
        }
    }

    /**
     * Detiene la detección y resetea la UI. Llamar desde el fragment padre.
     */
    public void stopPitchDetection() {
        if (pitchDetector != null) {
            pitchDetector.stopDetection();
        }
        stopMicAnimation();
        resetTuningUI();
    }

    private void resetTuningUI() {
        tuningBar.setProgress(50);
        tuningBar.invalidate();
        tuningMinus.setVisibility(View.INVISIBLE);
        tuningPlus.setVisibility(View.INVISIBLE);
        tensionAction.setText("");
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
    }

    private void hideAllStringButtons() {
        ImageButton[] buttons = {string1, string2, string3, string4, string5, string6};
        for (ImageButton b : buttons) {
            b.setAlpha(0f);
            b.setClickable(true);
            b.setBackground(null);
        }
    }

    private void startMicAnimation() {
        micAnimatorX = ObjectAnimator.ofFloat(btnMic, "scaleX", 1f, 1.15f, 1f);
        micAnimatorX.setDuration(1000);
        micAnimatorX.setRepeatCount(ObjectAnimator.INFINITE);
        micAnimatorX.setRepeatMode(ObjectAnimator.RESTART);
        micAnimatorX.setInterpolator(new LinearInterpolator());

        micAnimatorY = ObjectAnimator.ofFloat(btnMic, "scaleY", 1f, 1.15f, 1f);
        micAnimatorY.setDuration(1000);
        micAnimatorY.setRepeatCount(ObjectAnimator.INFINITE);
        micAnimatorY.setRepeatMode(ObjectAnimator.RESTART);
        micAnimatorY.setInterpolator(new LinearInterpolator());

        micAnimatorX.start();
        micAnimatorY.start();
    }

    private void stopMicAnimation() {
        if (micAnimatorX != null) micAnimatorX.cancel();
        if (micAnimatorY != null) micAnimatorY.cancel();
        btnMic.setScaleX(1f);
        btnMic.setScaleY(1f);
    }

    @Override
    public void onPitchDetected(double frequency) {
        if (frequency <= 0) return;
        pitchBuffer.add(frequency);
        if (pitchBuffer.size() > BUFFER_SIZE) pitchBuffer.remove(0);
        if (pitchBuffer.size() == BUFFER_SIZE) {
            double filtered = calculateFilteredFrequency(pitchBuffer);
            requireActivity().runOnUiThread(() -> updateTuningUI(filtered));
        }
    }

    private void updateTuningUI(double freq) {
        if (selectedStringButton == null) return;

        String expectedNote = stringNoteMap.get(selectedStringButton);
        double[] r = NOTE_FREQUENCIES.get(expectedNote);
        int red = ContextCompat.getColor(requireContext(), R.color.red);

        if (r != null) {
            double min = r[0], tgt = r[1], max = r[2];
            int progress = freq <= tgt ? (int) (((freq - min) / (tgt - min)) * 50)
                    : 50 + (int) (((freq - tgt) / (max - tgt)) * 50);
            progress = Math.max(0, Math.min(100, progress));
            tuningBar.setProgress(progress);
            tuningMinus.setVisibility(View.INVISIBLE);
            tuningPlus.setVisibility(View.INVISIBLE);
            tensionAction.setText("");

            if (Math.abs(freq - tgt) <= TUNING_THRESHOLD) {
                noteDisplay.setText(expectedNote);
            } else if (freq < tgt) {
                tuningPlus.setVisibility(View.VISIBLE);
                tuningPlus.setColorFilter(red);
                tensionAction.setText("DESTENSAR");
            } else {
                tuningMinus.setVisibility(View.VISIBLE);
                tuningMinus.setColorFilter(red);
                tensionAction.setText("TENSAR");
            }
        } else {
            resetTuningUI();
            tensionAction.setText("AJUSTAR");
        }
    }

    private double calculateFilteredFrequency(List<Double> buffer) {
        List<Double> tmp = new ArrayList<>(buffer);
        Collections.sort(tmp);
        double median = tmp.get(tmp.size() / 2);
        tmp.removeIf(f -> Math.abs(f - median) > 2.0);
        return tmp.size() < 3 ? median : tmp.stream().mapToDouble(d -> d).average().orElse(median);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION
                && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (pitchDetector == null) {
                pitchDetector = new PitchDetector(this, requireContext());
            }
            startPitchDetection();
        } else {
            Toast.makeText(requireContext(),
                    "Permission required", Toast.LENGTH_SHORT).show();
        }
    }
}
