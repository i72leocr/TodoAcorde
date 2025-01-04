package com.tuguitar.todoacorde;
import android.graphics.Typeface;

import android.content.DialogInterface;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ClickableSpan;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ImageButton;
import android.widget.ScrollView;
import java.util.ArrayList;
import java.util.List;

public class SongDetailsFragment extends Fragment {

    private TextView songTitle;
    private TextView songAuthor;
    private LinearLayout lyricsChordsContainer;
    private Button practiceChordsButton;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_song_details, container, false);

        songTitle = view.findViewById(R.id.song_title);
        songAuthor = view.findViewById(R.id.song_author); // New TextView for the author
        lyricsChordsContainer = view.findViewById(R.id.lyrics_chords_container);
        practiceChordsButton = view.findViewById(R.id.btn_practice_chords);

        ImageButton backButton = view.findViewById(R.id.back_button);  // Add this back button
        backButton.setOnClickListener(v -> {
            Bundle result = new Bundle();
            result.putInt("current_page", getArguments().getInt("current_page", 0));
            getParentFragmentManager().setFragmentResult("requestKey", result);
            getParentFragmentManager().popBackStack();
        });

        // Get song data from arguments
        Bundle args = getArguments();
        if (args != null) {
            String title = args.getString("song_title");
            String author = args.getString("song_author");  // Get the author
            List<String> lyrics = args.getStringArrayList("song_lyrics");
            List<String> chords = args.getStringArrayList("song_chords");

            songTitle.setText(title);
            songAuthor.setText(author);  // Set the author text
            displayLyricsWithChords(lyrics, chords);
        }

        practiceChordsButton.setOnClickListener(v -> showPracticeDialog());

        return view;
    }

    private void displayLyricsWithChords(List<String> lyrics, List<String> chords) {
        // Clear the previous views from the container
        lyricsChordsContainer.removeAllViews();

        // Ensure that each chord line is displayed first, followed by its respective lyric line
        int totalLines = Math.max(chords.size(), lyrics.size());

        for (int i = 0; i < totalLines; i++) {
            if (i < chords.size()) {
                String chordLine = chords.get(i);

                SpannableString spannableString = new SpannableString(chordLine);

                int start = 0;
                while (start < chordLine.length()) {
                    if (chordLine.charAt(start) != ' ') {
                        int end = start;
                        while (end < chordLine.length() && chordLine.charAt(end) != ' ') {
                            end++;
                        }

                        final String chord = chordLine.substring(start, end);
                        spannableString.setSpan(new ClickableSpan() {
                            @Override
                            public void onClick(@NonNull View widget) {
                                showChordDialog(chord);
                            }
                        }, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

                        start = end;
                    } else {
                        start++;
                    }
                }

                TextView chordTextView = new TextView(getContext());
                chordTextView.setText(spannableString);
                chordTextView.setTextSize(16);  // Reduced text size
                chordTextView.setTextColor(getResources().getColor(android.R.color.holo_orange_light));
                chordTextView.setTypeface(Typeface.MONOSPACE);
                chordTextView.setMovementMethod(LinkMovementMethod.getInstance());

                lyricsChordsContainer.addView(chordTextView);
            }

            if (i < lyrics.size()) {
                String lyricLine = lyrics.get(i);

                TextView lyricTextView = new TextView(getContext());
                lyricTextView.setText(lyricLine);
                lyricTextView.setTextSize(16);  // Reduced text size
                lyricTextView.setTextColor(getResources().getColor(android.R.color.black));
                lyricTextView.setTypeface(Typeface.MONOSPACE);

                lyricsChordsContainer.addView(lyricTextView);
            }
        }
    }






    private void showChordDialog(String chord) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle(chord);

        // Load the custom view for the dialog
        View chordView = getLayoutInflater().inflate(R.layout.dialog_chord, null);
        GridWithPointsView gridWithPointsView = chordView.findViewById(R.id.gridWithPointsView);

        // Example hint for testing; replace with actual logic to fetch hint from the database
        String hint = getChordHint(chord); // Use the method to get the correct hint
        gridWithPointsView.setPointsFromHint("x02210","002310");

        builder.setView(chordView);
        builder.setPositiveButton("OK", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private String getChordHint(String chordName) {
        // This method should return the chord hint based on the chord name
        // Replace this switch-case with actual database logic
        switch (chordName) {
            case "Am":
                return "0642xx";
            case "C":
                return "032010";
            case "D":
                return "xx0232";
            case "F":
                return "133211";
            case "E":
                return "022100";
            default:
                return "";
        }
    }

    private void showPracticeDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Practice Chords");
        builder.setMessage("Are you ready to practice the chords for this song?");
        builder.setPositiveButton("Yes", (dialog, which) -> navigateToPracticeChords());
        builder.setNegativeButton("No", null);
        builder.show();
    }

    private void navigateToPracticeChords() {
        PracticeChordsFragment practiceChordsFragment = new PracticeChordsFragment();
        Bundle bundle = new Bundle();

        // Pass song data to the practice fragment
        bundle.putString("song_title", songTitle.getText().toString());
        bundle.putString("song_author", songAuthor.getText().toString());
        bundle.putStringArrayList("song_lyrics", getArguments().getStringArrayList("song_lyrics"));
        bundle.putStringArrayList("song_chords", getArguments().getStringArrayList("song_chords"));
        bundle.putInt("current_page", getArguments().getInt("current_page", 0));  // Pass the current page        practiceChordsFragment.setArguments(bundle);

        practiceChordsFragment.setArguments(bundle);
        // Navigate to PracticeChordsFragment
        getParentFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, practiceChordsFragment)
                .addToBackStack(null)
                .commit();
    }
}
