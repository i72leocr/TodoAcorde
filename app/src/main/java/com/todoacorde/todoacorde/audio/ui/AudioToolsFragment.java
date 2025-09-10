package com.todoacorde.todoacorde.audio.ui;

import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.todoacorde.todoacorde.R;
import com.todoacorde.todoacorde.metronome.ui.MetronomeFragment;
import com.todoacorde.todoacorde.tuner.ui.TunerFragment;

/**
 * Fragmento contenedor de utilidades de audio: afinador y metrónomo.
 *
 * Configura un {@link ViewPager2} con dos pestañas (Afinador, Metrónomo) y controla
 * el ciclo de detección de tono del afinador para ahorrar recursos cuando no está visible.
 */
public class AudioToolsFragment extends Fragment {

    /** Pestañas superiores para navegar entre herramientas. */
    private TabLayout tabLayout;
    /** Pager para alojar los fragmentos de herramientas. */
    private ViewPager2 viewPager;
    /** Adaptador del pager. */
    private AudioToolsPagerAdapter adapter;
    /** Títulos de pestañas en orden. */
    private final String[] tabTitles = new String[]{"Afinador", "Metrónomo"};

    /** Fragmento del afinador. */
    private TunerFragment tunerFragment;
    /** Fragmento del metrónomo. */
    private MetronomeFragment metronomeFragment;

    /**
     * Infla el layout del fragmento y configura el ViewPager con sus pestañas.
     *
     * @param inflater  inflador de layouts.
     * @param container contenedor padre.
     * @param savedInstanceState estado previo si existe.
     * @return la vista raíz inflada.
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_audio_tools, container, false);

        tabLayout = root.findViewById(R.id.tabLayout);
        viewPager = root.findViewById(R.id.viewPager);

        // Instancias de herramientas
        tunerFragment = new TunerFragment();
        metronomeFragment = new MetronomeFragment();

        // Configuración del pager y mediador de pestañas
        adapter = new AudioToolsPagerAdapter(this);
        adapter.setFragments(tunerFragment, metronomeFragment);
        viewPager.setAdapter(adapter);

        new TabLayoutMediator(tabLayout, viewPager,
                (tab, position) -> tab.setText(tabTitles[position])
        ).attach();

        // Arrancar/detener detección de tono según la pestaña visible
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                if (position == 0) {
                    safeCallTunerStart();
                } else {
                    safeCallTunerStop();
                }
            }
        });

        // Pequeño retraso para asegurar que el fragmento está montado antes de iniciar el afinador
        new Handler().postDelayed(this::safeCallTunerStart, 300);
        return root;
    }

    /**
     * Llama de forma segura al inicio de la detección de tono del afinador,
     * verificando que el fragmento está añadido.
     */
    private void safeCallTunerStart() {
        if (tunerFragment != null && tunerFragment.isAdded()) {
            tunerFragment.startPitchDetection();
        }
    }

    /**
     * Llama de forma segura a la detención de la detección de tono del afinador,
     * verificando que el fragmento está añadido.
     */
    private void safeCallTunerStop() {
        if (tunerFragment != null && tunerFragment.isAdded()) {
            tunerFragment.stopPitchDetection();
        }
    }
}
