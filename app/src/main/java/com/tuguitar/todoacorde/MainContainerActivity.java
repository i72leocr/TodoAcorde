package com.tuguitar.todoacorde;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.MenuItem;
import android.view.View;
import android.app.AlertDialog;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.tuguitar.todoacorde.achievements.ui.AchievementFragment;
import com.tuguitar.todoacorde.audio.ui.AudioToolsFragment;
import com.tuguitar.todoacorde.practice.ui.PracticeChordsOptimizedFragment;
import com.tuguitar.todoacorde.scales.ui.ScaleTrainerFragment;
import com.tuguitar.todoacorde.songs.ui.SongFragment;
import com.tuguitar.todoacorde.InsetsUtils;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class MainContainerActivity extends AppCompatActivity  {

    private BottomNavigationView bottomNavigationView;
    private boolean isPracticeRunning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("MainActivity", "onCreate iniciado");
        enableFullscreen();

        SharedPreferences prefs = getSharedPreferences("todoacorde_prefs", MODE_PRIVATE);

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
        if (bottomNavigationView != null) {
            InsetsUtils.liftAboveNavBarWithMargin(bottomNavigationView);
        }
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

        if (savedInstanceState == null) {
            bottomNavigationView.setSelectedItemId(R.id.nav_tools); // fragment por defecto
        }
    }
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
