package com.tuguitar.todoacorde;

import android.content.Context;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import java.util.Arrays;
import java.util.List;


public class DifficultyFragment extends Fragment {

    private RecyclerView recyclerView;
    private ChordAdapter chordAdapter;
    private OnChordClassificationSelectedListener listener;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof OnChordClassificationSelectedListener) {
            listener = (OnChordClassificationSelectedListener) context;
        } else {
            throw new ClassCastException(context.toString() + " must implement OnChordClassificationSelectedListener");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_difficulty, container, false);

        recyclerView = view.findViewById(R.id.recycler_view_difficulty);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        List<String> difficulties = Arrays.asList("Fácil", "Intermedio", "Avanzado");
        chordAdapter = new ChordAdapter(difficulties);
        recyclerView.setAdapter(chordAdapter);

        chordAdapter.setOnItemClickListener(position -> {
            String selectedDifficulty = difficulties.get(position);
            listener.onChordClassificationSelected("Difficulty", selectedDifficulty);
        });

        return view;
    }
}

