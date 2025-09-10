package com.todoacorde.todoacorde;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.todoacorde.todoacorde.achievements.ui.AchievementFragment;
import com.todoacorde.todoacorde.audio.ui.AudioToolsFragment;
import com.todoacorde.todoacorde.practice.ui.PracticeChordsOptimizedFragment;
import com.todoacorde.todoacorde.scales.ui.ScaleTrainerFragment;
import com.todoacorde.todoacorde.songs.ui.SongFragment;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * Actividad contenedora principal de la app.
 * Gestiona el contenedor de fragmentos y la barra de navegación inferior.
 * Aplica modo de pantalla completa con WindowInsets y controla la navegación
 * cuando hay una práctica activa para solicitar confirmación antes de salir.
 */
@AndroidEntryPoint
public class MainContainerActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;
    /** Indica si hay una sesión de práctica en curso que requiere confirmación al salir. */
    private boolean isPracticeRunning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("MainActivity", "onCreate iniciado");
        enableFullscreen();

        SharedPreferences prefs = getSharedPreferences("todoacorde_prefs", MODE_PRIVATE);

        // Control de versión del seeding de BD. Si cambia, se vuelve a ejecutar la siembra.
        int savedVersion = prefs.getInt("db_populated_version", -1);
        int CURRENT_DB_VERSION = 55;

        if (savedVersion < CURRENT_DB_VERSION) {
            Log.d("MainActivity", "Seeding requerido. Ejecutando...");
            todoAcordeDatabase.databaseWriteExecutor.execute(() -> {
                runOnUiThread(() -> {
                    Log.d("DatabaseSeeder", "Seeding terminado. Guardando versión en prefs");
                    prefs.edit().putInt("db_populated_version", CURRENT_DB_VERSION).apply();
                });
            });
        }

        setContentView(R.layout.activity_main_container);

        final View fragmentContainer = findViewById(R.id.fragment_container);
        bottomNavigationView = findViewById(R.id.bottom_navigation);

        // Ajusta padding del contenedor con insets de sistema para status y nav bar.
        if (fragmentContainer != null) {
            final int ps = fragmentContainer.getPaddingStart();
            final int pt = fragmentContainer.getPaddingTop();
            final int pe = fragmentContainer.getPaddingEnd();
            final int pb = fragmentContainer.getPaddingBottom();
            final int extraTopPx = (int) TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP, 8, getResources().getDisplayMetrics());

            ViewCompat.setOnApplyWindowInsetsListener(fragmentContainer, (v, insets) -> {
                Insets sb = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPaddingRelative(ps, pt + sb.top + extraTopPx, pe, pb + sb.bottom);
                return insets;
            });
            fragmentContainer.requestApplyInsets();
        }

        // Eleva el BottomNavigationView por encima de la nav bar con margen.
        if (bottomNavigationView != null) {
            InsetsUtils.liftAboveNavBarWithMargin(bottomNavigationView);
        }

        // Listener de navegación con chequeo de práctica activa.
        bottomNavigationView.setOnNavigationItemSelectedListener(
                new BottomNavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                        if (isPracticeRunning) {
                            showExitConfirmationDialog(item);
                            return false;
                        } else {
                            return navigateToSelectedFragment(item);
                        }
                    }
                });

        // Pestaña inicial por defecto.
        if (savedInstanceState == null) {
            bottomNavigationView.setSelectedItemId(R.id.nav_tools);
        }
    }

    /**
     * Configura el modo de pantalla completa gestionando WindowInsets manualmente.
     * Se permite mostrar barras del sistema de forma transitoria mediante gesto.
     */
    private void enableFullscreen() {
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        WindowInsetsControllerCompat controller =
                new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
        controller.setSystemBarsBehavior(
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        controller.show(WindowInsetsCompat.Type.systemBars());
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Si la práctica está activa, se levanta el servicio monitor para gestionar regreso.
        if (isPracticeRunning) {
            Intent intent = new Intent(this, AppMonitorService.class);
            startService(intent);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Al volver al foreground, se detiene el servicio monitor si estaba activo.
        Intent intent = new Intent(this, AppMonitorService.class);
        stopService(intent);
    }

    /**
     * Realiza la navegación al fragment asociado al item de menú seleccionado.
     *
     * @param item elemento del BottomNavigationView.
     * @return true si se realizó la navegación, false en caso contrario.
     */
    private boolean navigateToSelectedFragment(@NonNull MenuItem item) {
        Fragment selectedFragment = null;
        int itemId = item.getItemId();

        if (itemId == R.id.nav_songs) {
            selectedFragment = new SongFragment();
        } else if (itemId == R.id.nav_tools) {
            selectedFragment = new AudioToolsFragment();
        } else if (itemId == R.id.nav_achievements) {
            selectedFragment = new AchievementFragment();
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

    /**
     * Muestra diálogo de confirmación cuando el usuario intenta navegar con práctica activa.
     * Si confirma, detiene la práctica y navega al destino solicitado.
     *
     * @param item elemento de menú que originó la navegación.
     */
    private void showExitConfirmationDialog(@NonNull MenuItem item) {
        new AlertDialog.Builder(this)
                .setTitle("Confirmación")
                .setMessage("¿Deseas parar la práctica?")
                .setPositiveButton("Sí", (dialog, which) -> {
                    isPracticeRunning = false;
                    Fragment current = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
                    if (current instanceof PracticeChordsOptimizedFragment) {
                        ((PracticeChordsOptimizedFragment) current).terminatePracticeFromActivity();
                    }
                    navigateToSelectedFragment(item);
                })
                .setNegativeButton("No", null)
                .show();
    }

    /**
     * Muestra un diálogo de error en UI thread.
     *
     * @param message texto del error a mostrar.
     */
    private void showErrorDialog(String message) {
        runOnUiThread(() -> new AlertDialog.Builder(this)
                .setTitle("Error")
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show());
    }

    /**
     * Permite a fragmentos hijos informar a la actividad que hay una práctica en curso.
     *
     * @param isRunning true si la práctica está activa; false en caso contrario.
     */
    public void setPracticeRunning(boolean isRunning) {
        this.isPracticeRunning = isRunning;
    }
}
