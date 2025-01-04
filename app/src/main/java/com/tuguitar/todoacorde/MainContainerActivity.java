package com.tuguitar.todoacorde;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import android.view.MenuItem;

public class MainContainerActivity extends AppCompatActivity implements OnChordClassificationSelectedListener {

    protected BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_container);

        bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                Fragment selectedFragment = null;
                int itemId = item.getItemId();

                if (itemId == R.id.nav_songs) {
                    selectedFragment = new SongFragment();
                } else if (itemId == R.id.nav_tuner) {
                    selectedFragment = new TunerFragment();
                } else if (itemId == R.id.nav_chord) {
                    selectedFragment = new ChordsFragment(); // ChordsFragment will contain the ChordTypeFragment
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
        });

        // Load the initial fragment if none is saved
        if (savedInstanceState == null) {
            bottomNavigationView.setSelectedItemId(R.id.nav_tuner); // Default fragment
        }
    }

    public void onChordClassificationSelected(String classificationType, String classificationValue) {
        // Create a new instance of PracticeChordsFragment
        PracticeChordsFragment practiceChordsFragment = new PracticeChordsFragment();

        // Pass the selected data to the fragment
        Bundle args = new Bundle();
        args.putString("CLASSIFICATION_TYPE", classificationType);
        args.putString("CLASSIFICATION_VALUE", classificationValue);
        practiceChordsFragment.setArguments(args);

        // Replace the current fragment with PracticeChordsFragment
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, practiceChordsFragment)
                .addToBackStack(null)
                .commit();
    }
}
