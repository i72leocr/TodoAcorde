package com.todoacorde.todoacorde.audio.ui;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

/**
 * Adaptador para el {@link androidx.viewpager2.widget.ViewPager2} de herramientas de audio.
 *
 * Mantiene dos fragmentos: afinador y metrónomo. Los fragmentos deben establecerse
 * previamente mediante {@link #setFragments(Fragment, Fragment)} antes de usarse.
 */
public class AudioToolsPagerAdapter extends FragmentStateAdapter {

    /** Arreglo fijo con los dos fragmentos (posición 0: afinador, 1: metrónomo). */
    private final Fragment[] fragments = new Fragment[2];

    /**
     * Crea el adaptador asociado a un fragmento padre.
     *
     * @param parent fragmento padre que actuará como {@code FragmentActivity}/host.
     */
    public AudioToolsPagerAdapter(@NonNull Fragment parent) {
        super(parent);
    }

    /**
     * Establece los fragmentos del afinador y del metrónomo.
     * Debe llamarse antes de que el ViewPager solicite elementos.
     *
     * @param tuner      fragmento para el afinador.
     * @param metronome  fragmento para el metrónomo.
     */
    public void setFragments(Fragment tuner, Fragment metronome) {
        fragments[0] = tuner;
        fragments[1] = metronome;
        notifyDataSetChanged();
    }

    /**
     * Devuelve el fragmento correspondiente a la posición solicitada.
     *
     * @param position índice del elemento (0 = afinador, 1 = metrónomo).
     * @return el fragmento asociado a la posición.
     * @throws ArrayIndexOutOfBoundsException si la posición no es válida.
     */
    @NonNull
    @Override
    public Fragment createFragment(int position) {
        return fragments[position];
    }

    /**
     * Número total de páginas gestionadas por el adaptador.
     *
     * @return 2, correspondiente a afinador y metrónomo.
     */
    @Override
    public int getItemCount() {
        return fragments.length;
    }
}
