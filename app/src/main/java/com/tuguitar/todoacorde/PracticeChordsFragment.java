package com.tuguitar.todoacorde;

import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.widget.Button;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.ProgressBar;

import android.util.Log; // Para usar Log
import java.util.Arrays; // Para usar Arrays.toString()



import com.google.android.material.navigation.NavigationView;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.widget.RelativeLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.widget.ImageButton;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.HashMap;
import java.util.Map;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.Manifest;
import android.content.pm.PackageManager;
import android.widget.Toast;
import androidx.core.content.ContextCompat;
public class PracticeChordsFragment extends Fragment implements ChordDetectionListener {

    private List<String> risingSunChords;
    private Set<String> playedChords = new HashSet<>();
    private int currentChordIndex = 0;
    private boolean isRandomMode = false;
    private boolean isRealMode = false;
    private boolean isPracticeRunning = false;
    private List<Chord> chordProfiles;
    private List<String> uniqueChords;  // Lista para guardar acordes sin repetición para modos aleatorio y en orden
    private List<String> chordBuffer = new ArrayList<>();
    private static final int BUFFER_SIZE = 3;
    private static final long DEBOUNCE_DELAY = 200;
    private GridWithPointsView gridWithPointsView;
    private static final int REQUEST_MIC_PERMISSION = 200;
    private TextView tvCurrentChord;
    private TextView tvScore;
    private TextView tvDetectedChord;
    private Button btnStartStopPractice;
    private RadioGroup radioGroupMode, radioGroupSpeed;
    private RadioButton radioOrder, radioRandom, radioReal, radioSpeed1x, radioSpeed075x, radioSpeed05x;
    private ProgressBar progressBar; // Referencia a la barra de progreso

    private ChordDetector chordDetector;
    private int score = 0;
    private long startTime;
    private Handler handler = new Handler();
    private boolean isCooldown = false;
    private int currentPulse = 0;
    private long pulseIntervalMillis = 0;
    private float currentSpeed = 1.0f;
    private boolean isErrorShown = false;

    private Song currentSong;


    private String getRandomChord() {
        // Asegúrate de que aún haya acordes disponibles
        if (uniqueChords.isEmpty()) {
            stopPractice();
            return null;
        }

        // Selecciona un acorde aleatorio
        int randomIndex = (int) (Math.random() * uniqueChords.size());
        return uniqueChords.remove(randomIndex); // Elimina el acorde seleccionado de la lista
    }



    private void adjustSpeedForPractice() {
        pulseIntervalMillis = (long) ((60000 / currentSong.getBpm()) / currentSpeed);
    }


    private void onIncorrectChordDetected() {
        tvCurrentChord.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
    }

    // Métodos auxiliares para la puntuación y los cálculos
    private int calculateScore(long elapsedTime) {
        int basePoints = 10;
        int penalty = (int) (elapsedTime / 3000);
        return Math.max(basePoints - penalty, 0);
    }


    private List<String> getRisingSunChords() {
        List<String> chords = new ArrayList<>();
        chords.add("Am");
        chords.add("C");
        chords.add("D");
        chords.add("F");
        chords.add("E");
        chords.add("Am");
        chords.add("C");
        chords.add("E");
        return chords;
    }

    private List<Chord> loadChordsFromCSV() {
        List<Chord> chords = new ArrayList<>();
        InputStream is = getResources().openRawResource(R.raw.chord_pcp_dataset);
        BufferedReader br = null;

        try {
            br = new BufferedReader(new InputStreamReader(is));
            String line = br.readLine(); // Ignorar la fila de encabezados

            while ((line = br.readLine()) != null) {
                String[] data = line.split(",");

                if (data.length < 14) {
                    Log.e("loadChordsFromCSV", "Skipping invalid line: " + line);
                    continue; // Asegurarse de que la fila tenga al menos 14 columnas (Nombre + 12 PCP + Hint)
                }

                String name = data[0];
                double[] pcp = new double[12];

                // Leer los 12 valores de PCP
                for (int i = 0; i < 12; i++) {
                    try {
                        pcp[i] = Double.parseDouble(data[i + 1]);
                    } catch (NumberFormatException e) {
                        pcp[i] = 0.0; // Asignar un valor por defecto de 0.0 en caso de error
                        Log.e("loadChordsFromCSV", "Invalid PCP value at index " + (i + 1) + " for chord " + name);
                    }
                }

                // Leer el hint de la columna 13
                String hint = data[13];

                // Crear el objeto Chord y añadirlo a la lista
                Chord chord = new Chord(name, pcp, hint);
                chords.add(chord);

                Log.d("loadChordsFromCSV", "Added Chord: " + name + ", PCP: " + Arrays.toString(pcp) + ", Hint: " + hint);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
                if (br != null) {
                    br.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return chords;
    }


    private void selectSong(String songName) {
        if (songName.equals("House of the Rising Sun")) {
            List<String> chords = List.of("Am", "C", "D", "F", "Am", "C", "E", "E", "Am", "C", "D", "F", "Am", "E", "Am", "E");
            List<Integer> durations = List.of(12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12);
            List<String> lyrics = List.of(
                    "There is a house in New Orleans",
                    "They call the rising sun",
                    "And it's been the ruin",
                    "of many a poor boy",
                    "And God, I know, I'm one"
            );

            currentSong = new Song("House of the Rising Sun", "Traditional", 3, lyrics, chords, durations, 120, false);
        }
    }

    private String getChordHint(String chordName) {
        for (Chord chord : chordProfiles) {
            if (chord.getName().equals(chordName)) {
                return chord.getHint();
            }
        }
        return "";
    }

    private void updateGuitarDiagram(String hint) {
        gridWithPointsView.setPointsFromHint(hint, "002310"); // Cambia el segundo argumento si es necesario
    }


    private void beginPractice() {
        disableModeSelection();  // Deshabilitar la selección de modo durante la práctica
        score = 0;
        playedChords.clear();

        // Restablecer la lista de acordes dependiendo del modo
        if (isRandomMode) {
            uniqueChords = new ArrayList<>(new HashSet<>(risingSunChords));  // En modo aleatorio, sin repetición
        } else {
            uniqueChords = new ArrayList<>(risingSunChords);  // En modo orden, respetar el orden de la canción
            currentChordIndex = 0;  // Empezar desde el primer acorde
        }

        displayNextChord();  // Mostrar el siguiente acorde
        chordDetector.startDetection();  // Iniciar la detección de acordes
    }

    private void disableModeSelection() {
        radioGroupMode.setEnabled(false);
        radioOrder.setEnabled(false);
        radioRandom.setEnabled(false);
        radioReal.setEnabled(false);
    }

    private void enableModeSelection() {
        radioGroupMode.setEnabled(true);
        radioOrder.setEnabled(true);
        radioRandom.setEnabled(true);
        radioReal.setEnabled(true);
    }

    private void disableSpeedButtons() {
        radioSpeed1x.setEnabled(false);
        radioSpeed075x.setEnabled(false);
        radioSpeed05x.setEnabled(false);
    }

    private void enableSpeedButtons() {
        radioSpeed1x.setEnabled(true);
        radioSpeed075x.setEnabled(true);
        radioSpeed05x.setEnabled(true);
    }

    @Override
    public void onPause() {
        super.onPause();
        stopPractice();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        stopPractice();
    }








    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_practice_chords, container, false);

        // Inicializar UI
        gridWithPointsView = view.findViewById(R.id.gridWithPointsView);
        tvCurrentChord = view.findViewById(R.id.tvCurrentChord);
        tvScore = view.findViewById(R.id.tvScore);
        tvDetectedChord = view.findViewById(R.id.tvDetectedChord);
        btnStartStopPractice = view.findViewById(R.id.btnStartStopPractice);
        radioGroupMode = view.findViewById(R.id.radioGroupMode);
        radioOrder = view.findViewById(R.id.radioOrder);
        radioRandom = view.findViewById(R.id.radioRandom);
        radioReal = view.findViewById(R.id.radioReal);
        radioGroupSpeed = view.findViewById(R.id.radioGroupSpeed);
        radioSpeed1x = view.findViewById(R.id.radioSpeed1x);
        radioSpeed075x = view.findViewById(R.id.radioSpeed075x);
        radioSpeed05x = view.findViewById(R.id.radioSpeed05x);
        progressBar = view.findViewById(R.id.progressBar);


        // Ocultar elementos que no deben mostrarse al inicio
        gridWithPointsView.setVisibility(View.INVISIBLE);
        tvCurrentChord.setVisibility(View.INVISIBLE);
        tvDetectedChord.setVisibility(View.INVISIBLE);
        radioGroupSpeed.setVisibility(View.GONE); // Ocultar el selector de velocidad inicialmente

        // Seleccionar velocidad x1 por defecto en el modo real
        radioSpeed1x.setChecked(true);

        // Inicializar las listas de acordes
        risingSunChords = getRisingSunChords();
        uniqueChords = new ArrayList<>(new HashSet<>(risingSunChords));  // Acordes únicos para modos aleatorio y en orden
        chordProfiles = loadChordsFromCSV();


        // Inicializar el detector de acordes
        chordDetector = new ChordDetector(chordProfiles, this);


        // Manejar la selección de modo de práctica
        radioGroupMode.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radioReal) {
                isRealMode = true;
                isRandomMode = false;
                radioGroupSpeed.setVisibility(View.VISIBLE);
            } else {
                isRealMode = false;
                isRandomMode = (checkedId == R.id.radioRandom);
                radioGroupSpeed.setVisibility(View.GONE);
            }
        });

        // Manejar la selección de velocidad en modo real
        radioGroupSpeed.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radioSpeed1x) {
                currentSpeed = 1.0f;
            } else if (checkedId == R.id.radioSpeed075x) {
                currentSpeed = 0.75f;
            } else if (checkedId == R.id.radioSpeed05x) {
                currentSpeed = 0.5f;
            }
        });

        // Manejar el botón de iniciar/detener la práctica
        btnStartStopPractice.setOnClickListener(v -> {
            if (isPracticeRunning) {
                stopPractice();
            } else {
                startPractice();
            }
            isPracticeRunning = !isPracticeRunning;
        });

        // Inicializar el texto y el botón
        tvCurrentChord.setText("");
        btnStartStopPractice.setText("Empezar");

        return view;
    }

    private void startPractice() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_MIC_PERMISSION);
        } else {
            // Mostrar los elementos cuando comience la práctica
            gridWithPointsView.setVisibility(View.INVISIBLE);
            tvCurrentChord.setVisibility(View.INVISIBLE);
            tvDetectedChord.setVisibility(View.INVISIBLE);

            if (isRealMode) {
                selectSong("House of the Rising Sun"); // Selecciona la canción
                startPracticeWithSong(currentSong); // Inicia la práctica en el modo real
            } else {
                beginPractice(); // Inicia la práctica en otro modo
            }

            // Cambiar el texto del botón a "Terminar"
            btnStartStopPractice.setText("Terminar");

            // Deshabilitar la selección de modo si se activa la práctica
            disableModeSelection();

            // Si es el modo real, deshabilitar botones de velocidad
            if (isRealMode) {
                disableSpeedButtons();
            }
        }
    }

    private void stopPractice() {
        // Volver el botón a su estado original "Empezar"
        resetPracticeState();  // Asegurarse de que todo se reinicie adecuadamente
        btnStartStopPractice.setText("Empezar");

        // Habilitar la selección de modo cuando la práctica termina
        enableModeSelection();

        chordDetector.stopDetection();
        handler.removeCallbacks(realModeRunnable);

        // Ocultar acorde, diagrama y acorde detectado al terminar
        tvCurrentChord.setVisibility(View.INVISIBLE);
        gridWithPointsView.setVisibility(View.INVISIBLE);
        tvDetectedChord.setVisibility(View.INVISIBLE); // Ocultar acorde detectado al terminar

        enableSpeedButtons();
    }




    @Override

    public void onChordDetected(String chord) {
        if (isPracticeRunning) {
            requireActivity().runOnUiThread(() -> {
                tvDetectedChord.setText(chord); // Muestra el acorde detectado en la UI
                evaluateChord(chord); // Evalúa si el acorde detectado es correcto
            });
        }
    }



    private void resetPracticeState() {
        isPracticeRunning = false;
        btnStartStopPractice.setText("Empezar");

        // Habilitar la selección de modo y botones de velocidad
        enableModeSelection();
        enableSpeedButtons();

        // Reiniciar el índice de acordes
        currentChordIndex = 0;

        // Reinicia la lista de acordes únicos en caso de que se vuelva a iniciar la práctica
        uniqueChords = isRandomMode ? new ArrayList<>(new HashSet<>(risingSunChords)) : new ArrayList<>(risingSunChords);
    }




    private void evaluateChord(String detectedChord) {
        // Verifica si el acorde detectado es correcto
        if (detectedChord.equals(tvCurrentChord.getText().toString())) {
            onCorrectChordDetected(); // Cambia el color a verde

            // Solo avanzar al siguiente acorde en modos de "Orden" y "Aleatorio"
            if (!isRealMode) {
                // Evita que se acumulen múltiples llamados al handler y provoquen saltos
                handler.removeCallbacksAndMessages(null);
                handler.postDelayed(() -> {
                    resetFeedback(); // Resetea el color antes de avanzar
                    displayNextChord(); // Avanza al siguiente acorde después del delay
                }, 500); // Delay antes de cambiar al siguiente acorde
            }
        }
    }

    private void displayNextChord() {
        // Verifica que haya acordes restantes en la lista
        if (currentChordIndex >= uniqueChords.size()) {
            stopPractice(); // Si se han terminado los acordes, detener la práctica
            return;
        }

        // Obtener el siguiente acorde basado en el modo seleccionado
        String nextChord;
        if (isRandomMode) {
            nextChord = getRandomChord();
        } else {
            nextChord = uniqueChords.get(currentChordIndex);
            currentChordIndex++;
        }

        // Actualiza el acorde y el diagrama de la guitarra
        String hint = getChordHint(nextChord);
        updateGuitarDiagram(hint);

        // Actualiza la UI
        tvCurrentChord.setText(nextChord);
        tvCurrentChord.setVisibility(View.VISIBLE);
        gridWithPointsView.setVisibility(View.VISIBLE);
        tvDetectedChord.setVisibility(View.VISIBLE);

        // Restablece el color del acorde
        tvCurrentChord.setTextColor(getResources().getColor(android.R.color.darker_gray));

        // Reinicia el temporizador
        startTime = SystemClock.elapsedRealtime();
    }


    private void onCorrectChordDetected() {
        tvCurrentChord.setTextColor(getResources().getColor(android.R.color.holo_green_dark)); // Cambia el color a verde
    }

    private void startPracticeWithSong(Song song) {
        currentSong = song;
        currentChordIndex = 0;
        currentPulse = 0;

        chordDetector.startDetection(); // Asegúrate de que el detector esté activo
        handler.removeCallbacks(realModeRunnable);

        // Restablece el progreso de la barra de progreso
        progressBar.setProgress(0);

        adjustSpeedForPractice();
        handler.postDelayed(realModeRunnable, pulseIntervalMillis);
    }


    private Runnable realModeRunnable = new Runnable() {
        @Override
        public void run() {
            if (currentChordIndex >= currentSong.getChords().size()) {
                stopPractice(); // Detener la práctica si se acaban los acordes
                return;
            }

            int chordDuration = currentSong.getChordDurations().get(currentChordIndex); // Duración del acorde actual

            // Calcular el progreso actual en la barra de progreso
            int progress = (int) ((currentPulse % chordDuration) / (float) chordDuration * 100);
            progressBar.setProgress(progress);

            // Mostrar el acorde al comienzo del ciclo
            if (currentPulse % chordDuration == 0) {
                String nextChord = currentSong.getChords().get(currentChordIndex);
                tvCurrentChord.setText(nextChord); // Actualiza el acorde en pantalla
                tvCurrentChord.setVisibility(View.VISIBLE);
                gridWithPointsView.setVisibility(View.VISIBLE);
                tvDetectedChord.setVisibility(View.VISIBLE);

                tvCurrentChord.setTextColor(getResources().getColor(android.R.color.darker_gray)); // Reinicia el color del acorde
            }

            if (currentPulse % chordDuration == chordDuration - 1) {
                // Avanza al siguiente acorde al final del ciclo
                if (tvCurrentChord.getCurrentTextColor() != getResources().getColor(android.R.color.holo_green_dark)) {
                    onIncorrectChordDetected(); // Marca como incorrecto si no se detectó el acorde correcto
                }
                currentChordIndex++;
                currentPulse++;
                handler.postDelayed(realModeRunnable, pulseIntervalMillis);
            } else {
                currentPulse++;
                handler.postDelayed(this, pulseIntervalMillis);
            }
        }
    };




    private void resetFeedback() {
        tvCurrentChord.setTextColor(getResources().getColor(android.R.color.darker_gray));
    }






    // Otros métodos como getRisingSunChords, getChordHint, updateGuitarDiagram, loadChordsFromCSV, etc.
}

