package com.tuguitar.todoacorde;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.tuguitar.todoacorde.achievements.ui.AchievementFragment;
import com.tuguitar.todoacorde.audio.ui.AudioToolsFragment;
import com.tuguitar.todoacorde.practice.ui.PracticeChordsOptimizedFragment;
import com.tuguitar.todoacorde.scales.ui.ScaleTrainerFragment;
import com.tuguitar.todoacorde.songs.ui.SongFragment;

import android.util.Log;
import android.view.MenuItem;
import android.app.AlertDialog;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

// Import de Hilt
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class MainContainerActivity extends AppCompatActivity implements OnChordClassificationSelectedListener {

    protected BottomNavigationView bottomNavigationView;
    private boolean isPracticeRunning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("MainActivity", "onCreate iniciado");

        SharedPreferences prefs = getSharedPreferences("todoacorde_prefs", MODE_PRIVATE);
        // prefs.edit().clear().apply();

        int savedVersion = prefs.getInt("db_populated_version", -1);
        int CURRENT_DB_VERSION = 55;

        if (savedVersion < CURRENT_DB_VERSION) {
            Log.d("MainActivity", "Seeding requerido. Ejecutando...");
            todoAcordeDatabase.databaseWriteExecutor.execute(() -> {
                // DatabaseSeeder.seed(getApplicationContext());

                // Confirmar desde hilo principal que ya se ha poblado
                runOnUiThread(() -> {
                    Log.d("DatabaseSeeder", "Seeding terminado. Guardando versión en prefs");
                    prefs.edit().putInt("db_populated_version", CURRENT_DB_VERSION).apply();
                });
            });
        }

        setContentView(R.layout.activity_main_container);

        bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                if (isPracticeRunning) {
                    showExitConfirmationDialog(item);
                    return false; // Prevent immediate navigation
                } else {
                    return navigateToSelectedFragment(item);
                }
            }
        });

        // Load the initial fragment if none is saved
        if (savedInstanceState == null) {
            bottomNavigationView.setSelectedItemId(R.id.nav_tools); // Default fragment
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (isPracticeRunning) {
            Intent intent = new Intent(this, AppMonitorService.class);
            startService(intent);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Intent intent = new Intent(this, AppMonitorService.class);
        stopService(intent);
    }

    private boolean navigateToSelectedFragment(@NonNull MenuItem item) {
        Fragment selectedFragment = null;
        int itemId = item.getItemId();

        if (itemId == R.id.nav_songs) {
            selectedFragment = new SongFragment();
        } else if (itemId == R.id.nav_tools) {
            selectedFragment = new AudioToolsFragment();
        } else if (itemId == R.id.nav_achievements) {   // <--- NUEVA LÍNEA
            selectedFragment = new AchievementFragment(); // <--- TU FRAGMENTO DE LOGROS
        } else if (itemId == R.id.nav_userdetails) {
            selectedFragment = new ScaleTrainerFragment();
        }

        if (selectedFragment != null) {
            getSupportFragmentManager().beginTransaction()
                    .setCustomAnimations(R.anim.fragment_fade_in, R.anim.fragment_fade_out)
                    .replace(R.id.fragment_container, selectedFragment)
                    .commit();
            return true;
        }
        return false;
    }

    private void showExitConfirmationDialog(@NonNull MenuItem item) {
        new AlertDialog.Builder(this)
                .setTitle("Confirmación")
                .setMessage("¿Deseas parar la práctica?")
                .setPositiveButton("Sí", (dialog, which) -> {
                    isPracticeRunning = false;

                    // Buscar el fragmento activo y llamar a endPractice si aplica
                    Fragment current = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
                    if (current instanceof PracticeChordsOptimizedFragment) {
                        ((PracticeChordsOptimizedFragment) current).terminatePracticeFromActivity();
                    }

                    navigateToSelectedFragment(item);
                })
                .setNegativeButton("No", null)
                .show();
    }


    @Override
    public void onChordClassificationSelected(String classificationType, String classificationValue) {
        // Verifica si la clasificación seleccionada es una progresión
        if (classificationType.equalsIgnoreCase("Progresión")) {
            // Consultar la base de datos para obtener los acordes de la progresión
            Executors.newSingleThreadExecutor().execute(() -> {
                todoAcordeDatabase db = todoAcordeDatabase.getInstance(this);
                ProgressionDao progressionDao = db.progressionDao();
                ProgressionChordDao progressionChordDao = db.progressionChordDao();

                // Busca la progresión seleccionada
                Progression progression = progressionDao.getProgressionByName(classificationValue);

                if (progression != null) {
                    List<String> chordNames = new ArrayList<>();

                    if (progression.isDynamic) {
                        // Progresión dinámica: Generar acordes en base a tipo y dificultad
                        if (progression.typeFilter != 0 && progression.difficultyFilter != 0) {
                            // Código para cargar acordes dinámicos...
                        } else {
                            showErrorDialog("La progresión dinámica no tiene filtros definidos.");
                            return;
                        }
                    } else {
                        // Progresión estática: Usa la tabla de relaciones
                        List<ProgressionChord> progressionChords = progressionChordDao.getChordsForProgression(progression.id);
                        // Código para cargar acordes estáticos...
                    }

                    if (!chordNames.isEmpty()) {
                        // Preparar los argumentos para PracticeChordsFragment
                        Bundle args = new Bundle();
                        args.putStringArrayList("chord_lines", new ArrayList<>(chordNames));
                        args.putString("progression_name", progression.name);
                        args.putBoolean("isProgressionPractice", true);

                        // Inicia el PracticeChordsFragment con los datos de la progresión
                        runOnUiThread(() -> {
                            getSupportFragmentManager().beginTransaction()
                                    .addToBackStack(null)
                                    .commit();
                        });
                    } else {
                        showErrorDialog("No se encontraron acordes para la progresión seleccionada.");
                    }
                } else {
                    showErrorDialog("No se encontró la progresión seleccionada.");
                }
            });
        } else {
            // Lógica para canciones u otros tipos de clasificación
            Bundle args = new Bundle();
            args.putString("classification_type", classificationType);
            args.putString("classification_value", classificationValue);

            getSupportFragmentManager().beginTransaction()
                    .addToBackStack(null)
                    .commit();
        }
    }

    private void showErrorDialog(String message) {
        runOnUiThread(() -> new AlertDialog.Builder(this)
                .setTitle("Error")
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show());
    }

    public void setPracticeRunning(boolean isRunning) {
        this.isPracticeRunning = isRunning;
    }
}
