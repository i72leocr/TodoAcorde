/*
package com.tuguitar.todoacorde;

import android.graphics.Typeface;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ClickableSpan;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ImageButton;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import androidx.core.widget.TextViewCompat;
import android.util.TypedValue;


import android.graphics.Paint;

public class SongDetailsFragment extends Fragment {

    private TextView songTitle;
    private TextView songAuthor;
    private LinearLayout lyricsChordsContainer;
    private Button practiceChordsButton;

    private Song currentsong;
    private ArrayList<String> chordLines; // Cadenas de texto procesadas de acordes
    private ArrayList<String> lyricLines; // Cadenas de texto procesadas de letras
    private ArrayList<Integer> chordDurations; // Duraciones en ms de los acordes



    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        // Inflar el diseño del fragmento de detalles de la canción
        View view = inflater.inflate(R.layout.fragment_song_details, container, false);

        // Referencias a las vistas
        songTitle = view.findViewById(R.id.song_title);
        songAuthor = view.findViewById(R.id.song_author); // TextView para el autor
        lyricsChordsContainer = view.findViewById(R.id.lyrics_chords_container);
        practiceChordsButton = view.findViewById(R.id.btn_practice_chords);

        // Botón para retroceder al fragmento anterior
        ImageButton backButton = view.findViewById(R.id.back_button);

        backButton.setOnClickListener(v -> {
            Bundle result = new Bundle();
            result.putInt("current_page", getArguments().getInt("current_page", 0));
            getParentFragmentManager().setFragmentResult("requestKey", result);
            getParentFragmentManager().popBackStack();
        });

        // Obtener el ID de la canción desde los argumentos
        int songId = getArguments().getInt("song_id", -1);

        if (songId == -1) {
            Log.e("Error", "No se pasó un ID de canción válido");
            return view;
        }

        // Instancia de la base de datos y DAOs
        todoAcordeDatabase db = todoAcordeDatabase.getInstance(requireContext());
        SongDao songDao = db.songDao();
        SongLyricDao songLyricDao = db.songLyricDao();
        SongChordDao songChordDao = db.songChordDao();
        ChordDao chordDao = db.chordDao();

// Cargar datos de la base de datos en un hilo secundario
        Executors.newSingleThreadExecutor().execute(() -> {
            // Obtener los datos principales de la canción
            Song song = songDao.getSongById(songId);
            List<SongLyric> lyrics = songLyricDao.getLyricsBySongId(songId);
            List<SongChord> songChords = songChordDao.getChordsBySongId(songId);

            // Precargar todos los acordes en un Map
            Map<Integer, Chord> chordMap = new HashMap<>();
            for (SongChord songChord : songChords) {
                if (!chordMap.containsKey(songChord.chordId)) {
                    Chord chord = chordDao.getChordById(songChord.chordId);
                    chordMap.put(songChord.chordId, chord);
                }
            }

            // Actualizar la UI en el hilo principal
            requireActivity().runOnUiThread(() -> {
                if (song != null) {
                    currentsong = song;
                    songTitle.setText(currentsong.getTitle());
                    songAuthor.setText(currentsong.getAuthor());

                }

                // Mostrar letra con acordes usando el Map precargado
                displayLyricsWithChords(lyrics, songChords, chordMap);
            });
        });

        // Listener para el botón de practicar acordes
        practiceChordsButton.setOnClickListener(v -> showPracticeDialog());

        return view;
    }

    /**
     * Muestra la letra de la canción con los acordes superpuestos.
     *
     * @param lyrics       Lista de objetos SongLyric con las líneas de letra.
     * @param songChords   Lista de SongChord que representan los acordes asociados.

    */
/*
    /**
     * Muestra la letra de la canción con los acordes superpuestos y clickeables.
     *
     * @param lyrics       Lista de objetos SongLyric con las líneas de letra.
     * @param songChords   Lista de SongChord que representan los acordes asociados.
     * @param chordMap     Mapa precargado de acordes por su ID.
     */
    /*
       // Limpiar las vistas anteriores
    private void displayLyricsWithChords(List<SongLyric> lyrics, List<SongChord> songChords, Map<Integer, Chord> chordMap) {
        // Limpiar las vistas anteriores
        lyricsChordsContainer.removeAllViews();

        chordLines = new ArrayList<>();
        lyricLines = new ArrayList<>();
        chordDurations = new ArrayList<>();

        for (SongLyric lyric : lyrics) {
            int lyricId = lyric.id;
            String lyricText = lyric.line;

            // Crear la línea de acordes como un StringBuilder
            StringBuilder chordLineBuilder = new StringBuilder();

            // Determinar la longitud máxima necesaria para la línea de acordes
            int maxLength = Math.max(lyricText.length(), songChords.stream()
                    .filter(chord -> chord.lyricId == lyricId)
                    .mapToInt(chord -> chord.positionInVerse + (chordMap.containsKey(chord.chordId) ? chordMap.get(chord.chordId).getName().length() : 0))
                    .max()
                    .orElse(0));

            // Inicializar la línea de acordes con espacios
            for (int i = 0; i < maxLength; i++) {
                chordLineBuilder.append(" ");
            }

            // Insertar los acordes en sus posiciones
            for (SongChord songChord : songChords) {
                if (songChord.lyricId == lyricId) {
                    Chord chord = chordMap.get(songChord.chordId);
                    if (chord != null) {
                        int position = songChord.positionInVerse;
                        String chordName = chord.getName();

                        // Insertar el nombre del acorde en la posición correspondiente
                        for (int i = 0; i < chordName.length(); i++) {
                            if (position + i < chordLineBuilder.length()) {
                                chordLineBuilder.setCharAt(position + i, chordName.charAt(i));
                            }
                        }
                        // Almacenar la duración del acorde
                        chordDurations.add(songChord.duration);
                    }
                }
            }

            // Convertir la línea de acordes en SpannableString
            SpannableString spannableChordLine = new SpannableString(chordLineBuilder.toString());

            // Hacer los acordes clickeables
            for (SongChord songChord : songChords) {
                if (songChord.lyricId == lyricId) {
                    Chord chord = chordMap.get(songChord.chordId);
                    if (chord != null) {
                        int position = songChord.positionInVerse;
                        int end = position + chord.getName().length();

                        if (position >= 0 && end <= spannableChordLine.length()) {
                            spannableChordLine.setSpan(new ClickableSpan() {
                                @Override
                                public void onClick(@NonNull View widget) {
                                    showChordDialog(chord.getName());
                                }
                            }, position, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                        }
                    }
                }
            }


            // Crear y configurar la vista para la línea de acordes
            TextView chordTextView = new TextView(getContext());
            chordTextView.setText(spannableChordLine);
            chordTextView.setMovementMethod(LinkMovementMethod.getInstance());
            chordTextView.setTypeface(Typeface.MONOSPACE);
            chordTextView.setTextColor(getResources().getColor(android.R.color.holo_orange_light));
            chordTextView.setMaxLines(1); // Importante para el ajuste horizontal
            TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(chordTextView, 6, 16, 1, TypedValue.COMPLEX_UNIT_SP);
            lyricsChordsContainer.addView(chordTextView);

// Crear y configurar la vista para la línea de la letra
            TextView lyricTextView = new TextView(getContext());
            lyricTextView.setText(lyricText);
            lyricTextView.setTypeface(Typeface.MONOSPACE);
            lyricTextView.setTextColor(getResources().getColor(android.R.color.black));
            lyricTextView.setMaxLines(1); // También en la letra
            TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(lyricTextView, 6, 16, 1, TypedValue.COMPLEX_UNIT_SP);
            lyricsChordsContainer.addView(lyricTextView);


            chordLines.add(chordLineBuilder.toString());
            lyricLines.add(lyricText);

        }
    }



    /**
     * Reemplaza un carácter en un SpannableString sin sobrescribir los Spans.
     *
     * @param spannableString El SpannableString original.
     * @param position        La posición del carácter a reemplazar.
     * @param newChar         El nuevo carácter.
     * @return Un nuevo SpannableString con el carácter reemplazado.
     */
/*
    private SpannableString replaceChar(SpannableString spannableString, int position, char newChar) {
        char[] chars = spannableString.toString().toCharArray();
        chars[position] = newChar;
        return new SpannableString(new String(chars));
    }



    /**
     * Muestra un cuadro de diálogo con información del acorde seleccionado.
     *
     * @param chordName Nombre del acorde.
     */

/*
    private void showChordDialog(String chordName) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle(chordName);

        // Cargar la vista personalizada para el acorde
        View chordView = getLayoutInflater().inflate(R.layout.dialog_chord, null);
        GridWithPointsView gridWithPointsView = chordView.findViewById(R.id.gridWithPointsView);

        // Obtener el hint del acorde desde la base de datos
        todoAcordeDatabase db = todoAcordeDatabase.getInstance(requireContext());
        ChordDao chordDao = db.chordDao();
        Executors.newSingleThreadExecutor().execute(() -> {
            Chord chord = chordDao.getChordByName(chordName);
            if (chord != null) {
                String hint = chord.hint;
                String fingerHint = chord.fingerHint;

                requireActivity().runOnUiThread(() -> {
                    gridWithPointsView.setPointsFromHint(hint, fingerHint);
                });
            }
        });

        builder.setView(chordView);
        builder.setPositiveButton("OK", (dialog, which) -> dialog.dismiss());
        builder.show();
    }



    private void showPracticeDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Practice Chords");
        builder.setMessage("Are you ready to practice the chords for this song?");
        builder.setPositiveButton("Yes", (dialog, which) -> navigateToPracticeChords());
        builder.setNegativeButton("No", null);
        builder.show();

        // Log para las líneas de acordes
        Log.d("Debug", "Chord Lines: " + chordLines.toString());

        // Log para las líneas de letras
        Log.d("Debug", "Lyric Lines: " + lyricLines.toString());

        // Log para las duraciones de acordes
        Log.d("Debug", "Chord Durations: " + chordDurations.toString());
    }

    private void navigateToPracticeChords() {
       PracticeChordsFragment practiceChordsFragment = new PracticeChordsFragment();
        Bundle bundle = new Bundle();



        bundle.putStringArrayList("chord_lines", chordLines); // Pasar líneas de acordes
        bundle.putStringArrayList("lyric_lines", lyricLines); // Pasar líneas de letras
        bundle.putIntegerArrayList("chord_durations", chordDurations); // Pasar duraciones de acordes
        bundle.putString("song_title", currentsong.getTitle());
        bundle.putString("song_author", currentsong.getAuthor());
        bundle.putInt("song_bpm", currentsong.getBpm());
        bundle.putInt("song_id", currentsong.getId());
        bundle.putString("song_measure",currentsong.getMeasure());

        bundle.putInt("current_page", getArguments().getInt("current_page", 0));  // Pass the current page

        practiceChordsFragment.setArguments(bundle);
        // Navigate to PracticeChordsFragment
        getParentFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, practiceChordsFragment)
                .addToBackStack(null)
                .commit();
    }

}


 */


