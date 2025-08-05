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

public class ProgressionsFragment extends Fragment {

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
        View view = inflater.inflate(R.layout.fragment_progressions, container, false);

        recyclerView = view.findViewById(R.id.recycler_view_progressions);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Cargar progresiones desde la base de datos
        loadProgressionsFromDatabase();

        return view;
    }

    private void loadProgressionsFromDatabase() {
        Executors.newSingleThreadExecutor().execute(() -> {
            todoAcordeDatabase db = todoAcordeDatabase.getInstance(requireContext());
            ProgressionDao progressionDao = db.progressionDao();

            // Obtener todas las progresiones estáticas
            List<Progression> progressions = progressionDao.getStaticProgressions();

            requireActivity().runOnUiThread(() -> {
                if (progressions != null && !progressions.isEmpty()) {
                    // Convertir las progresiones a una lista de nombres
                    List<String> progressionNames = progressions.stream()
                            .map(Progression::getName)
                            .collect(Collectors.toList());

                    // Configurar el adaptador
                    chordAdapter = new ChordAdapter(progressionNames);
                    recyclerView.setAdapter(chordAdapter);

                    // Manejar clics en las progresiones
                    chordAdapter.setOnItemClickListener(position -> {
                        String selectedProgression = progressionNames.get(position);
                        listener.onChordClassificationSelected("Progression", selectedProgression);
                    });
                } else {
                    // Manejar el caso en el que no haya progresiones disponibles
                    // Opcional: Mostrar un mensaje al usuario
                }
            });
        });
    }
}
