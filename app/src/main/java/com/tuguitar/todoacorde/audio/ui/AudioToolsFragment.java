package com.tuguitar.todoacorde.audio.ui;

import android.os.Bundle;
import android.os.Handler;
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

        tunerFragment = new TunerFragment();
        metronomeFragment = new MetronomeFragment();

        adapter = new AudioToolsPagerAdapter(this);
        adapter.setFragments(tunerFragment, metronomeFragment);
        viewPager.setAdapter(adapter);

        new TabLayoutMediator(tabLayout, viewPager,
                (tab, position) -> tab.setText(tabTitles[position])
        ).attach();

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
        new Handler().postDelayed(this::safeCallTunerStart, 300);
        return root;
    }

    private void safeCallTunerStart() {
        if (tunerFragment != null && tunerFragment.isAdded()) {
            tunerFragment.startPitchDetection();
        }
    }

    private void safeCallTunerStop() {
        if (tunerFragment != null && tunerFragment.isAdded()) {
            tunerFragment.stopPitchDetection();
        }
    }
}
