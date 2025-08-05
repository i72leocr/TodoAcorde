package com.tuguitar.todoacorde;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class AudioToolsPagerAdapter extends FragmentStateAdapter {

    private final Fragment[] fragments = new Fragment[2];

    public AudioToolsPagerAdapter(@NonNull Fragment parent) {
        super(parent);
    }

    /**
     * Establece las instancias de los fragments que manejará el adapter.
     * @param tuner El fragment Afinador
     * @param metronome El fragment Metrónomo
     */
    public void setFragments(Fragment tuner, Fragment metronome) {
        fragments[0] = tuner;
        fragments[1] = metronome;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        return fragments[position];
    }

    @Override
    public int getItemCount() {
        return fragments.length;
    }
}
