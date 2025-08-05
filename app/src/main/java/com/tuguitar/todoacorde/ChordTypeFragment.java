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
import java.util.List;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

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

        // Cargar dinámicamente los tipos de acordes desde la base de datos
        loadChordTypesFromDatabase();

        return view;
    }

    private void loadChordTypesFromDatabase() {
        Executors.newSingleThreadExecutor().execute(() -> {
            todoAcordeDatabase db = todoAcordeDatabase.getInstance(requireContext());
            ChordTypeDao chordTypeDao = db.chordTypeDao();
            List<ChordType> chordTypes = chordTypeDao.getAllChordTypes();

            // Actualizar la interfaz de usuario en el hilo principal
            requireActivity().runOnUiThread(() -> {
                chordAdapter = new ChordAdapter(
                        chordTypes.stream()
                                .map(ChordType::getTypeName)
                                .collect(Collectors.toList())); // Cambiado para compatibilidad;
                recyclerView.setAdapter(chordAdapter);

                chordAdapter.setOnItemClickListener(position -> {
                    ChordType selectedChordType = chordTypes.get(position);
                    showConfirmationDialog(
                            "ChordType",
                            selectedChordType.getTypeName(),
                            selectedChordType.getDescription()
                    );
                });
            });
        });
    }

    private void showConfirmationDialog(String classificationType, String classificationValue, String description) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Confirm Selection")
                .setMessage("You selected: " + classificationValue + "\n" + description + "\n\nDo you want to start practicing?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    listener.onChordClassificationSelected(classificationType, classificationValue);
                })
                .setNegativeButton("No", null)
                .show();
    }
}
