
/*
package com.tuguitar.todoacorde;

import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;

import android.widget.Button;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import com.google.android.material.navigation.NavigationView;
import android.app.AlertDialog;
import android.content.DialogInterface;

import android.widget.RelativeLayout;

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


public class PracticeChordsActivity extends BaseActivity implements ChordDetectionListener {

    private List<String> risingSunChords; // Lista de acordes para "House of the Rising Sun"
    private Set<String> playedChords = new HashSet<>();
    private int currentChordIndex = 0;
    private boolean isRandomMode = false;
    private boolean isPracticeRunning = false;
    private List<Chord> chordProfiles;
    private List<String> chordBuffer = new ArrayList<>();
    private static final int BUFFER_SIZE = 3; // Buffer size for debounce
    private static final long DEBOUNCE_DELAY = 200; // Debounce delay in milliseconds
    private RelativeLayout guitarDiagram;


    private TextView tvCurrentChord;
    private TextView tvFeedback;
    private TextView tvScore;
    private TextView tvDetectedChord; // Nuevo TextView para mostrar el acorde detectado
    private Button btnStartStopPractice;
    private RadioGroup radioGroupMode;
    private RadioButton radioOrder, radioRandom;

    private ChordDetector chordDetector;
    private int score = 0;
    private long startTime;
    private Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setActivityContent(R.layout.activity_practice_chords);


        gridWithPointsView = findViewById(R.id.gridWithPointsView);
        ImageButton btnInfo = findViewById(R.id.btnInfo);
        btnInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showInfoDialog();
            }
        });

        // Inicializar la UI
        tvCurrentChord = findViewById(R.id.tvCurrentChord);
        tvFeedback = findViewById(R.id.tvFeedback);
        tvScore = findViewById(R.id.tvScore);
        tvDetectedChord = findViewById(R.id.tvDetectedChord); // Inicializar el TextView para el acorde detectado
        btnStartStopPractice = findViewById(R.id.btnStartStopPractice);
        radioGroupMode = findViewById(R.id.radioGroupMode);
        radioOrder = findViewById(R.id.radioOrder);
        radioRandom = findViewById(R.id.radioRandom);

        // Inicializar la lista de acordes de "House of the Rising Sun"
        risingSunChords = getRisingSunChords();
        chordProfiles = loadChordsFromCSV();

        // Inicializar el detector de acordes con los perfiles de acorde cargados
        chordDetector = new ChordDetector(chordProfiles, this);

        // Configuración del RadioGroup
        radioGroupMode.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radioOrder) {
                isRandomMode = false;
            } else if (checkedId == R.id.radioRandom) {
                isRandomMode = true;
            }
        });

        // Configuración del botón de Empezar/Terminar
        btnStartStopPractice.setOnClickListener(v -> {
            if (isPracticeRunning) {
                stopPractice();
            } else {
                startPractice();
            }
            isPracticeRunning = !isPracticeRunning;
        });

        // Inicialmente, no mostrar ningún acorde
        tvCurrentChord.setText("");
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopPractice(); // Detener la práctica cuando la actividad se pausa
    }

    private void startPractice() {
        // Iniciar la práctica
        btnStartStopPractice.setText("Terminar");
        radioGroupMode.setEnabled(false); // Desactivar cambios de modo durante la práctica
        radioOrder.setEnabled(false);
        radioRandom.setEnabled(false);
        currentChordIndex = 0; // Reiniciar el índice al inicio
        score = 0; // Reiniciar la puntuación
        playedChords.clear(); // Limpiar los acordes tocados

        // Mostrar el acorde inicial Am con su diagrama correspondiente
        displayNextChord(); // Mostrar el primer acorde
        chordDetector.startDetection();
    }

    private void stopPractice() {
        // Terminar la práctica
        btnStartStopPractice.setText("Empezar");
        radioGroupMode.setEnabled(true); // Rehabilitar cambios de modo
        radioOrder.setEnabled(true);
        radioRandom.setEnabled(true);
        tvFeedback.setText(""); // Limpiar el feedback
        tvCurrentChord.setText(""); // Limpiar el acorde mostrado
        tvScore.setText("Puntuación: " + score); // Mostrar la puntuación final
        currentChordIndex = 0; // Reiniciar el índice
        chordDetector.stopDetection();
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

    private void displayNextChord() {
        if (playedChords.size() == risingSunChords.size()) {
            stopPractice();
            tvFeedback.setText("¡Buen trabajo! Tu puntuación es: " + score);
            return;
        }

        // Asegúrate de que el acorde inicial sea "Am"
        String nextChord = "Am";

        // Llamar al método para actualizar el diagrama con el hint específico de Am
        String hint = "3642xx";
        updateGuitarDiagram(hint);

        // Mostrar el acorde en pantalla
        tvCurrentChord.setText(nextChord);
        startTime = SystemClock.elapsedRealtime();
        tvCurrentChord.setTextColor(getResources().getColor(android.R.color.darker_gray));
    }


    private String getChordHint(String chordName) {
        for (Chord chord : chordProfiles) {
            if (chord.getName().equals(chordName)) {
                return chord.getHint();
            }
        }
        return "";
    }
    private void hideAllFretPoints() {
        // Ocultar todos los puntos antes de actualizar para un nuevo acorde
        for (int i = 1; i <= 6; i++) {
            for (int j = 1; j <= 5; j++) { // Supongamos que 5 es el número máximo de trastes que puedes tener
                String viewIdName = "point" + i + "_" + j;
                int viewId = getResources().getIdentifier(viewIdName, "id", getPackageName());
                if (viewId != 0) {
                    View pointView = findViewById(viewId);
                    if (pointView != null) {
                        pointView.setVisibility(View.INVISIBLE);
                    }
                }
            }

            // Ocultar puntos de cuerdas al aire y cuerdas no tocadas
            String viewIdName = "point_x" + i;
            int viewId = getResources().getIdentifier(viewIdName, "id", getPackageName());
            if (viewId != 0) {
                View pointView = findViewById(viewId);
                if (pointView != null) {
                    pointView.setVisibility(View.INVISIBLE);
                }
            }

            viewIdName = "point_0" + i;
            viewId = getResources().getIdentifier(viewIdName, "id", getPackageName());
            if (viewId != 0) {
                View pointView = findViewById(viewId);
                if (pointView != null) {
                    pointView.setVisibility(View.INVISIBLE);
                }
            }
        }
    }
    private GridWithPointsView gridWithPointsView;


    private static final String TAG = "PracticeChordsActivity";
    private void updateGuitarDiagram(String hint) {
        gridWithPointsView.setPointsFromHint(hint);
    }


    private void showInfoDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("How to Use This Screen");
        builder.setMessage("Here you can explain how the practice mode works, what the buttons do, and how to read the chord diagram. For example:\n\n" +
                "1. Select 'Order' to practice in a sequence or 'Aleatorio' for random chords.\n" +
                "2. Press 'Empezar' to begin.\n" +
                "3. Play the chord displayed, and the app will detect it.\n" +
                "4. The score updates based on your performance.\n" +
                "5. The grid shows the chord diagram based on the hint.");
        builder.setPositiveButton("OK", null);
        builder.show();
    }




    private void evaluateChord(String detectedChord) {
        if (detectedChord.equals("No Chord")) {
            // Do nothing, maintain the gray color
            tvDetectedChord.setTextColor(getResources().getColor(android.R.color.darker_gray));
            return;
        }

        // Add the detected chord to the buffer
        chordBuffer.add(detectedChord);
        if (chordBuffer.size() > BUFFER_SIZE) {
            chordBuffer.remove(0); // Keep the buffer at the defined size
        }

        // Use a handler to introduce a debounce delay
        handler.postDelayed(() -> {
            String modeChord = findModeChord(chordBuffer); // Find the most frequent chord in the buffer

            if (modeChord == null) {
                // No clear mode, exit the method
                return;
            }

            if (modeChord.equals(tvCurrentChord.getText().toString())) {
                onCorrectChordDetected();
            } else {
                onIncorrectChordDetected();
            }
        }, DEBOUNCE_DELAY);
    }
    @Override
    public void onChordDetected(String detectedChord) {
        runOnUiThread(() -> evaluateChord(detectedChord));
    }
    private boolean isCooldown = false; // New flag to manage cooldown state

    private void onCorrectChordDetected() {
        if (isCooldown) return; // If still in cooldown, don't process

        // Cambiar el color del texto a verde y mostrar "Correcto"
        tvCurrentChord.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
        tvFeedback.setText("Correcto");
        tvFeedback.setTextColor(getResources().getColor(android.R.color.holo_green_dark));

        long elapsedTime = SystemClock.elapsedRealtime() - startTime;
        int points = calculateScore(elapsedTime);
        score += points;
        tvScore.setText("Puntuación: " + score);

        playedChords.add(tvCurrentChord.getText().toString()); // Marcar este acorde como tocado

        isCooldown = true; // Set the cooldown state
        handler.postDelayed(() -> {
            isCooldown = false; // Clear the cooldown state after delay
            displayNextChord();
            resetFeedback();
        }, 1500); // 1 second delay
    }

    private void onIncorrectChordDetected() {
        // Cambiar el color del texto a rojo y mostrar "Fallo"
        tvCurrentChord.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
        tvFeedback.setText("Fallo");
        tvFeedback.setTextColor(getResources().getColor(android.R.color.holo_red_dark));

        // Reset to gray after a short delay
        handler.postDelayed(() -> resetFeedback(), 1000); // 1 second delay
    }

    private int calculateScore(long elapsedTime) {
        int basePoints = 10;
        int penalty = (int) (elapsedTime / 3000); // Descuento de 1 punto por cada 3 segundos adicionales
        int points = Math.max(basePoints - penalty, 0); // Asegurar que no se reste por debajo de 0
        return points;
    }

    private String findModeChord(List<String> chords) {
        Map<String, Integer> frequencyMap = new HashMap<>();
        for (String chord : chords) {
            frequencyMap.put(chord, frequencyMap.getOrDefault(chord, 0) + 1);
        }

        String modeChord = null;
        int maxCount = 0;
        int secondMaxCount = 0;

        for (Map.Entry<String, Integer> entry : frequencyMap.entrySet()) {
            int count = entry.getValue();
            if (count > maxCount) {
                secondMaxCount = maxCount;
                maxCount = count;
                modeChord = entry.getKey();
            } else if (count > secondMaxCount) {
                secondMaxCount = count;
            }
        }

        // Only return the modeChord if its count is significantly higher than the second most common chord
        if (maxCount >= secondMaxCount + 2) {
            return modeChord;
        } else {
            return null; // No clear mode
        }
    }

    private void resetFeedback() {
        tvCurrentChord.setTextColor(getResources().getColor(android.R.color.darker_gray));
        tvFeedback.setText("");
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

                if (data.length < 15) {
                    continue; // Asegurarse de que la fila tenga al menos 15 columnas (Nombre + 12 PCP + Hint)
                }

                String name = data[0];
                double[] pcp = new double[12];

                for (int i = 0; i < 12; i++) {
                    try {
                        pcp[i] = Double.parseDouble(data[i + 1]);
                    } catch (NumberFormatException e) {
                        pcp[i] = 0.0; // Asignar un valor por defecto de 0.0 en caso de error
                    }
                }

                String hint = data[14]; // El hint está en la 15ª columna

                Chord chord = new Chord(name, pcp, hint); // Asignar el hint al acorde
                chords.add(chord);
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
}
 */
