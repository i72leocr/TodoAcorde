/*package com.tuguitar.todoacorde;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
public class MainActivity extends BaseActivity implements PitchDetectorCallback {

    private TextView frequencyDisplay;
    private boolean isPitchDetectionRunning = false;
    private PitchDetector pitchDetector;
    private Button btnStartStop;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setActivityContent(R.layout.activity_main);

        // Inicializar componentes del afinador
        frequencyDisplay = findViewById(R.id.frequencyDisplay);
        btnStartStop = findViewById(R.id.btnStartStop);

        // Inicializar PitchDetector
        pitchDetector = new PitchDetector(this);

        // Configurar el botón Start/Stop
        btnStartStop.setOnClickListener(v -> {
            if (isPitchDetectionRunning) {
                stopPitchDetection();
                btnStartStop.setText("Start");
            } else {
                startPitchDetection();
                btnStartStop.setText("Stop");
            }
            isPitchDetectionRunning = !isPitchDetectionRunning;
        });
    }

    private void startPitchDetection() {
        pitchDetector.startDetection();
        frequencyDisplay.setText("Detecting...");
    }

    private void stopPitchDetection() {
        pitchDetector.stopDetection();
        frequencyDisplay.setText("0.00 Hz");
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (isPitchDetectionRunning) {
            stopPitchDetection();
            btnStartStop.setText("Start");
            isPitchDetectionRunning = false;
            frequencyDisplay.setText("0.00 Hz");
        }
    }

    @Override
    public void onPitchDetected(double frequency) {
        if (frequency > 0) {
            runOnUiThread(() -> frequencyDisplay.setText(String.format("%.2f Hz", frequency)));
        }
    }
}
*/