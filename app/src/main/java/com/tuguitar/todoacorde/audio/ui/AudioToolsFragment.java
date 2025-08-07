package com.tuguitar.todoacorde.audio.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.tuguitar.todoacorde.R;
import com.tuguitar.todoacorde.metronome.ui.MetronomeFragment;
import com.tuguitar.todoacorde.tuner.ui.TunerFragment;

import androidx.viewpager2.widget.ViewPager2;

public class AudioToolsFragment extends Fragment {

    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private AudioToolsPagerAdapter adapter;
    private final String[] tabTitles = new String[]{"Afinador", "Metrónomo"};

    // Instancias de los fragments hijos
    private TunerFragment tunerFragment;
    private MetronomeFragment metronomeFragment;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_audio_tools, container, false);

        tabLayout = root.findViewById(R.id.tabLayout);
        viewPager = root.findViewById(R.id.viewPager);

        // Crear instancias
        tunerFragment = new TunerFragment();
        metronomeFragment = new MetronomeFragment();

        // Configuramos el adapter y le pasamos los fragments
        adapter = new AudioToolsPagerAdapter(this);
        adapter.setFragments(tunerFragment, metronomeFragment);
        viewPager.setAdapter(adapter);

        // Conectamos TabLayout y ViewPager2
        new TabLayoutMediator(tabLayout, viewPager,
                (tab, position) -> tab.setText(tabTitles[position])
        ).attach();

        // Controlar start/stop de detección según pestaña activa
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                if (position == 0) {
                    tunerFragment.startPitchDetection();
                    metronomeFragment.onPause(); // parar metrónomo si hace algo
                } else {
                    tunerFragment.stopPitchDetection();
                    metronomeFragment.onResume(); // iniciar metrónomo si es necesario
                }
            }
        });

        // Arrancar detección por defecto en la pestaña Afinador
        tunerFragment.startPitchDetection();

        return root;
    }
}
