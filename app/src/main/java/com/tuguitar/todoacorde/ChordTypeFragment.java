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
import android.app.AlertDialog;

public class ChordTypeFragment extends Fragment {

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
        View view = inflater.inflate(R.layout.fragment_chord_type, container, false);

        recyclerView = view.findViewById(R.id.recycler_view_type);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        List<String> chordTypes = Arrays.asList("Acordes Mayores", "Acordes Menores", "Acordes Dominantes", "Acordes Mayores Séptimos", "Acordes Menores Séptimos");
        chordAdapter = new ChordAdapter(chordTypes);
        recyclerView.setAdapter(chordAdapter);

        chordAdapter.setOnItemClickListener(position -> {
            String selectedChordType = chordTypes.get(position);
            String description = getDescriptionForChordType(selectedChordType);
            showConfirmationDialog("ChordType", selectedChordType, description);
        });

        return view;
    }

    private String getDescriptionForChordType(String chordType) {
        switch (chordType) {
            case "Acordes Mayores":
                return "Acordes como A, E, D... que tienen un sonido brillante y completo.";
            case "Acordes Menores":
                return "Acordes como Am, Em, Dm... conocidos por su tono melancólico.";
            // Add cases for other chord types with their descriptions
            default:
                return "Descripción no disponible.";
        }
    }

    private void showConfirmationDialog(String classificationType, String classificationValue, String description) {
        new AlertDialog.Builder(getContext())
                .setTitle("Confirm Selection")
                .setMessage("You selected: " + classificationValue + "\n" + description + "\n\nDo you want to start practicing?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    listener.onChordClassificationSelected(classificationType, classificationValue);
                })
                .setNegativeButton("No", null)
                .show();
    }
}


