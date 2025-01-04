package com.tuguitar.todoacorde;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import android.widget.SeekBar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

import android.widget.ImageButton;


public class TunerFragment extends Fragment implements PitchDetectorCallback {

    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private static final int BUFFER_SIZE = 16; // Tamaño del buffer de valores de frecuencia
    private static final double TUNING_THRESHOLD = 0.5; // Umbral de afinación
    private static final int CONSISTENCY_THRESHOLD = 3;
    private static final float MAX_FREQ_OFFSET = 2.6f; // Número de decisiones consecutivas necesarias para un estado

    private TextView frequencyDisplay, noteDisplay, tuningIndicator, tensionAction;
    private boolean isPitchDetectionRunning = false;
    private PitchDetector pitchDetector;
    private SeekBar tuningBar;
    private ImageButton btnMic;
    private boolean isMicActive = false;

    private static final String TAG = "TunerFragment";

    private List<Double> pitchBuffer = new ArrayList<>();


    // Mapa de notas con sus frecuencias correspondientes
    private static final Map<String, double[]> NOTE_FREQUENCIES = new HashMap<>();
    static {
        // El formato del array es [frecuencia inferior, frecuencia afinada, frecuencia superior]

        NOTE_FREQUENCIES.put("C2", new double[]{63.36, 65.41, 67.36}); // C2
        NOTE_FREQUENCIES.put("C#2", new double[]{67.36, 69.30, 71.36}); // C#2/Db2
        NOTE_FREQUENCIES.put("D2", new double[]{71.36, 73.42, 75.60}); // D2
        NOTE_FREQUENCIES.put("D#2", new double[]{75.60, 77.78, 80.10}); // D#2/Eb2
        NOTE_FREQUENCIES.put("E2", new double[]{80.10, 82.41, 84.86}); // E2
        NOTE_FREQUENCIES.put("F2", new double[]{84.86, 87.31, 89.90}); // F2
        NOTE_FREQUENCIES.put("F#2", new double[]{89.90, 92.50, 95.25}); // F#2/Gb2
        NOTE_FREQUENCIES.put("G2", new double[]{95.25, 98.00, 100.92}); // G2
        NOTE_FREQUENCIES.put("G#2", new double[]{100.92, 103.83, 106.88}); // G#2/Ab2
        NOTE_FREQUENCIES.put("A2", new double[]{106.88, 110.00, 113.28}); // A2
        NOTE_FREQUENCIES.put("A#2", new double[]{113.28, 116.54, 120.77}); // A#2/Bb2
        NOTE_FREQUENCIES.put("B2", new double[]{120.77, 123.47, 127.14}); // B2

        NOTE_FREQUENCIES.put("C3", new double[]{127.14, 130.81, 134.70}); // C3
        NOTE_FREQUENCIES.put("C#3", new double[]{134.70, 138.59, 142.71}); // C#3/Db3
        NOTE_FREQUENCIES.put("D3", new double[]{142.71, 146.83, 151.43}); // D3
        NOTE_FREQUENCIES.put("D#3", new double[]{151.43, 155.56, 159.90}); // D#3/Eb3
        NOTE_FREQUENCIES.put("E3", new double[]{159.90, 164.25, 169.90}); // E3
        NOTE_FREQUENCIES.put("F3", new double[]{169.90, 174.61, 179.81}); // F3
        NOTE_FREQUENCIES.put("F#3", new double[]{179.81, 185.00, 191.67}); // F#3/Gb3
        NOTE_FREQUENCIES.put("G3", new double[]{191.67, 196.00, 202.83}); // G3
        NOTE_FREQUENCIES.put("G#3", new double[]{202.83, 207.65, 213.50}); // G#3/Ab3
        NOTE_FREQUENCIES.put("A3", new double[]{213.50, 220.66, 227.00}); // A3
        NOTE_FREQUENCIES.put("A#3", new double[]{227.00, 233.54, 240.24}); // A#3/Bb3
        NOTE_FREQUENCIES.put("B3", new double[]{240.24, 246.94, 254.35}); // B3

        NOTE_FREQUENCIES.put("C4", new double[]{254.35, 261.63, 268.90}); // C4
        NOTE_FREQUENCIES.put("C#4", new double[]{268.90, 277.18, 285.29}); // C#4/Db4
        NOTE_FREQUENCIES.put("D4", new double[]{285.29, 293.82, 302.46}); // D4
        NOTE_FREQUENCIES.put("D#4", new double[]{302.46, 311.13, 319.35}); // D#4/Eb4
        NOTE_FREQUENCIES.put("E4", new double[]{319.35, 329.63, 339.43}); // E4
        NOTE_FREQUENCIES.put("F4", new double[]{339.43, 349.23, 358.91}); // F4
        NOTE_FREQUENCIES.put("F#4", new double[]{358.91, 370.00, 380.64}); // F#4/Gb4
        NOTE_FREQUENCIES.put("G4", new double[]{380.64, 392.00, 404.64}); // G4
        NOTE_FREQUENCIES.put("G#4", new double[]{404.64, 415.30, 428.34}); // G#4/Ab4
        NOTE_FREQUENCIES.put("A4", new double[]{428.34, 440.00, 455.12}); // A4
        NOTE_FREQUENCIES.put("A#4", new double[]{455.12, 466.98, 480.60}); // A#4/Bb4
        NOTE_FREQUENCIES.put("B4", new double[]{480.60, 494.21, 509.28}); // B4

        NOTE_FREQUENCIES.put("C5", new double[]{509.28, 523.24, 538.80}); // C5
        NOTE_FREQUENCIES.put("C#5", new double[]{538.80, 554.37, 570.01}); // C#5/Db5
        NOTE_FREQUENCIES.put("D5", new double[]{570.01, 587.64, 604.91}); // D5
        NOTE_FREQUENCIES.put("D#5", new double[]{604.91, 622.19, 640.12}); // D#5/Eb5
        NOTE_FREQUENCIES.put("E5", new double[]{640.12, 657.20, 675.90}); // E5
    }



    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_tuner, container, false);

        // Inicialización de componentes
        frequencyDisplay = view.findViewById(R.id.frequencyDisplay);  // Agregar esta línea
        noteDisplay = view.findViewById(R.id.noteDisplay);
        tuningIndicator = view.findViewById(R.id.tuningIndicator);
        tensionAction = view.findViewById(R.id.tensionAction);
        btnMic = view.findViewById(R.id.btnMic);
        tuningBar = view.findViewById(R.id.tuningBar);

        tuningBar.setMax(100);
        tuningBar.setProgress(50); // Barra inicia en el centro

        // Comprobar permisos y solicitar si es necesario
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), new String[]{Manifest.permission.RECORD_AUDIO},
                    REQUEST_RECORD_AUDIO_PERMISSION);
        } else {
            // Inicializar PitchDetector una vez concedidos los permisos
            pitchDetector = new PitchDetector(this, requireContext());
        }

        // Configurar botón de Start/Stop
        // Configurar la acción del botón del micrófono
        btnMic.setOnClickListener(v -> {
            if (isMicActive) {
                startPitchDetection();
            } else {
                stopPitchDetection();
            }
            isMicActive = !isMicActive;
        });

        return view;
    }

    private void startPitchDetection() {
        if (pitchDetector != null) {
            btnMic.setImageResource(android.R.drawable.ic_btn_speak_now);
            pitchDetector.startDetection();
            frequencyDisplay.setText("Detecting...");
            noteDisplay.setText("-");
            tuningIndicator.setText("");
            tensionAction.setText("");
        } else {
            Toast.makeText(requireContext(), "Cannot start pitch detection. Permission not granted.", Toast.LENGTH_SHORT).show();
        }
    }

    private void stopPitchDetection() {
        if (pitchDetector != null) {
            btnMic.setImageResource(android.R.drawable.ic_lock_silent_mode);
            pitchDetector.stopDetection();
        }
        frequencyDisplay.setText("0.00 Hz");
        noteDisplay.setText("-");
        tuningIndicator.setText("");
        tensionAction.setText("");
    }

    @Override
    public void onPause() {
        super.onPause();
        if (isPitchDetectionRunning) {
            stopPitchDetection();
            isPitchDetectionRunning = false;
            frequencyDisplay.setText("0.00 Hz");
            noteDisplay.setText("-");
            tuningIndicator.setText("");
            tensionAction.setText("");
        }
    }

    @Override
    public void onPitchDetected(double frequency) {
        if (frequency > 0) {
            Log.d(TAG, "Detected frequency: " + frequency);
            pitchBuffer.add(frequency);

            if (pitchBuffer.size() > BUFFER_SIZE) {
                pitchBuffer.remove(0); // Mantenemos solo los últimos 16 valores
            }

            // Si tenemos suficientes valores en el buffer, aplicamos el filtro y calculamos la mediana
            if (pitchBuffer.size() == BUFFER_SIZE) {
                double filteredFrequency = calculateFilteredFrequency(pitchBuffer);
                Log.d(TAG, "Filtered frequency: " + filteredFrequency);

                // Mostrar la frecuencia detectada
                requireActivity().runOnUiThread(() -> {
                    frequencyDisplay.setText(String.format("%.2f Hz", filteredFrequency));
                    updateTuningBarAndIndicator(filteredFrequency);
                });
            }
        }
    }

    private void updateTuningBarAndIndicator(double frequency) {
        String detectedNote = null;
        double[] frequencyData = null;

        // Encontrar la nota y su rango basado en la frecuencia detectada
        for (Map.Entry<String, double[]> entry : NOTE_FREQUENCIES.entrySet()) {
            double[] range = entry.getValue();
            if (frequency >= range[0] && frequency <= range[2]) {
                detectedNote = entry.getKey();
                frequencyData = range;
                break;
            }
        }

        if (detectedNote != null && frequencyData != null) {
            noteDisplay.setText(detectedNote);

            double minFreq = frequencyData[0];
            double targetFreq = frequencyData[1];
            double maxFreq = frequencyData[2];

            int progress;

            // Si la frecuencia detectada está por debajo o igual a la frecuencia afinada
            if (frequency <= targetFreq) {
                // Calcular la posición normalizada para la mitad inferior (0-50%)
                double normalizedPosition = (frequency - minFreq) / (targetFreq - minFreq);
                progress = (int) (normalizedPosition * 50);  // Escalar a 0-50%
            } else {
                // Calcular la posición normalizada para la mitad superior (50-100%)
                double normalizedPosition = (frequency - targetFreq) / (maxFreq - targetFreq);
                progress = (int) (50 + (normalizedPosition * 50));  // Escalar a 50-100%
            }

            // Actualizar la barra de progreso
            tuningBar.setProgress(progress);

            // Actualizar el thumb basado en si está afinado o no
            if (Math.abs(frequency - targetFreq) <= TUNING_THRESHOLD) {
                tuningIndicator.setText("Afinado");
                tuningIndicator.setTextColor(getResources().getColor(R.color.green));
                tensionAction.setText("");
                tuningBar.setThumb(getResources().getDrawable(R.drawable.thumb_green_circle));  // Indicador afinado
            } else if (frequency < targetFreq) {
                tuningIndicator.setText("-");
                tuningIndicator.setTextColor(getResources().getColor(R.color.red));
                tensionAction.setText("DESTENSAR");
                tuningBar.setThumb(getResources().getDrawable(R.drawable.thumb_red_circle));  // Indicador desafinado
            } else {
                tuningIndicator.setText("+");
                tuningIndicator.setTextColor(getResources().getColor(R.color.red));
                tensionAction.setText("TENSAR");
                tuningBar.setThumb(getResources().getDrawable(R.drawable.thumb_red_circle));  // Indicador desafinado
            }
        } else {
            tuningBar.setProgress(50);  // Mantener la barra en el centro si no se detecta una nota
            tuningIndicator.setText("Desconocido");
            tuningIndicator.setTextColor(getResources().getColor(R.color.red));
            tensionAction.setText("Ajustar");
        }
    }





    // Calcular la mediana después de filtrar los valores distantes en el buffer
    private double calculateFilteredFrequency(List<Double> buffer) {
        List<Double> filteredBuffer = new ArrayList<>(buffer);

        // Calcular la mediana del buffer
        Collections.sort(filteredBuffer);
        double median = filteredBuffer.get(filteredBuffer.size() / 2);

        // Filtrar los valores que se alejan demasiado de la mediana (más de un umbral de 2 Hz, por ejemplo)
        filteredBuffer.removeIf(frequency -> Math.abs(frequency - median) > 2);

        // Si no quedan suficientes valores después del filtrado, devolver la mediana original
        if (filteredBuffer.size() < 3) {
            return median;
        }

        // Calcular la media de los valores filtrados
        return filteredBuffer.stream().mapToDouble(Double::doubleValue).average().orElse(median);
    }



    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                pitchDetector = new PitchDetector(this, requireContext());
            } else {
                Toast.makeText(requireContext(), "Microphone permission is required for pitch detection.", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
