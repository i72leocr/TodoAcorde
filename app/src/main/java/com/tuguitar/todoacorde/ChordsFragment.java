package com.tuguitar.todoacorde;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.ViewPager;
import com.google.android.material.tabs.TabLayout;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class ChordsFragment extends Fragment {

    private TabLayout tabLayout;
    private ViewPager viewPager;
    private ChordsPagerAdapter chordsPagerAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chords, container, false);

        tabLayout = view.findViewById(R.id.tab_layout);
        viewPager = view.findViewById(R.id.view_pager);

        setupViewPager(viewPager);
        tabLayout.setupWithViewPager(viewPager);

        return view;
    }


    private void setupViewPager(ViewPager viewPager) {
        chordsPagerAdapter = new ChordsPagerAdapter(getChildFragmentManager());

        // Añadir los fragmentos para cada categoría
        chordsPagerAdapter.addFragment(new ChordTypeFragment(), "Tipo de Acorde");
        chordsPagerAdapter.addFragment(new ProgressionsFragment(), "Progresiones Comunes");
//        chordsPagerAdapter.addFragment(new DifficultyFragment(), "Dificultad Técnica");

        viewPager.setAdapter(chordsPagerAdapter);
    }
}
