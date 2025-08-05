package com.tuguitar.todoacorde;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ScaleTrainerFragment extends Fragment implements PitchDetectorCallback {

    private static final String TAG = "ScaleTrainerFragment";
    private static final int REQUEST_RECORD_AUDIO = 1001;

    private ScaleTrainerViewModel viewModel;
    private TextView tvScaleTitle;
    private LinearLayout llScaleNotes;
    private TextView tvStreak;
    private TextView tvProgress;
    private TextView tvAccuracy;
    private TextView tvFeedback;
    private Button btnStartScale;
    private Spinner spinnerRoot;
    private Spinner spinnerScaleType;
    private ScaleFretboardView scaleFretboardView;

    private PitchDetector pitchDetector;

    private int previousIndex = -1;
    private String lastNote = "";
    private long lastChangeTime = 0;
    private final long STABLE_MS = 200;
    private double lastCents = 0;
    private static final long COOLDOWN_AFTER_DETECTION_MS = 100;
    private long nextAllowedDetectionTime = 0;

    private final Handler uiHandler = new Handler();

    public ScaleTrainerFragment() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_scale_trainer, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(ScaleTrainerViewModel.class);

        tvScaleTitle = view.findViewById(R.id.tvScaleTitle);
        llScaleNotes = view.findViewById(R.id.llScaleNotes);
        tvStreak = view.findViewById(R.id.tvStreak);
        tvProgress = view.findViewById(R.id.tvProgress);
        tvAccuracy = view.findViewById(R.id.tvAccuracy);
        tvFeedback = view.findViewById(R.id.tvFeedback);
        btnStartScale = view.findViewById(R.id.btnStartScale);
        spinnerRoot = view.findViewById(R.id.spinnerRoot);
        spinnerScaleType = view.findViewById(R.id.spinnerScaleType);
        scaleFretboardView = view.findViewById(R.id.scaleFretboard);

        setupSpinners();
        setupObservers();

        btnStartScale.setOnClickListener(v -> {
            resetScaleDisplay();
            String rootName = (String) spinnerRoot.getSelectedItem();
            String scaleName = (String) spinnerScaleType.getSelectedItem();
            int rootMidi = rootNameToMidi(rootName);
            ScaleUtils.ScaleType scaleType = mapNameToScaleType(scaleName);
            viewModel.startScale(rootMidi, scaleType);
            tvScaleTitle.setText(rootName + " " + scaleName);
            startPitchDetectionIfAllowed();

            // Intentar cargar patrón desde repositorio
            String repoScaleType = mapToRepositoryScaleType(scaleType);
            List<PatternRepository.ScalePattern> patterns = PatternRepository.findPatterns(repoScaleType, rootName);
            if (!patterns.isEmpty()) {
                PatternRepository.ScalePattern pattern = patterns.get(0);
                Log.d(TAG, "Pattern found: " + pattern.name + " type=" + pattern.scaleType + " root=" + pattern.rootNote + " notes=" + pattern.notes.size());
                scaleFretboardView.setScaleNotes(pattern.notes);
                scaleFretboardView.setHighlightSequence(pattern.notes);

                List<ScaleFretNote> sorted = new ArrayList<>(pattern.notes);
                sorted.sort(Comparator
                        .comparingInt((ScaleFretNote n) -> n.stringIndex)
                        .thenComparingInt(n -> n.fret));
                List<String> bubbleNotes = new ArrayList<>();
                for (ScaleFretNote n : sorted) {
                    bubbleNotes.add(n.degree);
                }
                populateScaleBubbles(bubbleNotes);
            } else {
                Log.d(TAG, "No pattern found for type=" + repoScaleType + " root=" + rootName + ", falling back to hardcoded pentatonic.");
                List<ScaleFretNote> fallback = ScaleFretboardView.generatePentatonicBox();
                scaleFretboardView.setScaleNotes(fallback);
                scaleFretboardView.setHighlightSequence(fallback);

                List<ScaleFretNote> sorted = new ArrayList<>(fallback);
                sorted.sort(Comparator
                        .comparingInt((ScaleFretNote n) -> n.fret)
                        .thenComparingInt(n -> n.stringIndex));
                List<String> bubbleNotes = new ArrayList<>();
                for (ScaleFretNote n : sorted) {
                    bubbleNotes.add(n.degree);
                }
                populateScaleBubbles(bubbleNotes);
            }

            // Reaplicar índice actual si ya existía
            Integer currentIdx = viewModel.getCurrentIndex().getValue();
            if (currentIdx != null && scaleFretboardView.getHighlightSequenceSize() > currentIdx) {
                scaleFretboardView.setHighlightIndex(currentIdx);
            }
        });

        btnStartScale.post(() -> btnStartScale.performClick());
    }

    private void setupSpinners() {
        String[] roots = {"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"};
        ArrayAdapter<String> rootAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, roots);
        rootAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRoot.setAdapter(rootAdapter);
        spinnerRoot.setSelection(0);

        String[] scaleNames = new String[]{
                "Mayor", "Menor natural", "Menor armónica", "Menor melódica",
                "Pentatónica mayor", "Pentatónica menor", "Blues",
                "Dórica", "Mixolidia", "Lidia", "Frigia", "Locria"
        };
        ArrayAdapter<String> scaleAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, scaleNames);
        scaleAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerScaleType.setAdapter(scaleAdapter);
        spinnerScaleType.setSelection(0);
    }

    private void setupObservers() {
        viewModel.getScaleNotes().observe(getViewLifecycleOwner(), notes -> populateScaleBubbles(notes));

        viewModel.getCurrentIndex().observe(getViewLifecycleOwner(), idx -> {
            if (idx == null) return;
            highlightCurrentNote(idx);
            if (scaleFretboardView.getHighlightSequenceSize() > idx) {
                scaleFretboardView.setHighlightIndex(idx);
            } else {
                Log.w(TAG, "Cannot set highlightIndex " + idx + " because sequence size is " + scaleFretboardView.getHighlightSequenceSize());
            }
            if (previousIndex != -1 && idx > previousIndex) {
                showFeedback("¡Bien!", true);
            }
            previousIndex = idx;
        });

        viewModel.getStreak().observe(getViewLifecycleOwner(), s -> tvStreak.setText("Streak: " + s));
        viewModel.getProgressPercent().observe(getViewLifecycleOwner(), p -> tvProgress.setText(String.format("Progreso: %.0f%%", p)));

        viewModel.getState().observe(getViewLifecycleOwner(), st -> {
            if (st == ScaleTrainerViewModel.State.COMPLETED) {
                Toast.makeText(requireContext(), "Escala completada", Toast.LENGTH_SHORT).show();
                showFeedback("🎉 Completado", true);
            }
        });
    }

    private void resetScaleDisplay() {
        lastNote = "";
        lastChangeTime = 0;
        lastCents = 0;
        nextAllowedDetectionTime = 0;
        previousIndex = -1;
        tvFeedback.setVisibility(View.INVISIBLE);
        llScaleNotes.removeAllViews();
    }

    private void populateScaleBubbles(List<String> notes) {
        llScaleNotes.removeAllViews();
        for (String note : notes) {
            TextView bubble = createNoteBubble(note);
            llScaleNotes.addView(bubble);
        }
        highlightCurrentNote(viewModel.getCurrentIndex().getValue() != null ? viewModel.getCurrentIndex().getValue() : 0);
    }

    private TextView createNoteBubble(String note) {
        TextView tv = new TextView(requireContext());
        int pad = dpToPx(12);
        tv.setPadding(pad, pad, pad, pad);
        tv.setText(note);
        tv.setTextSize(18f);
        tv.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        tv.setEllipsize(TextUtils.TruncateAt.END);
        tv.setMaxLines(1);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(dpToPx(4), dpToPx(4), dpToPx(4), dpToPx(4));
        tv.setLayoutParams(lp);
        tv.setBackgroundResource(R.drawable.scale_note_default_background);
        tv.setTextColor(getColorCompat(android.R.color.black));
        return tv;
    }

    private void highlightCurrentNote(int index) {
        tvFeedback.setVisibility(View.INVISIBLE);
        int count = llScaleNotes.getChildCount();
        for (int i = 0; i < count; i++) {
            View v = llScaleNotes.getChildAt(i);
            if (!(v instanceof TextView)) continue;
            TextView tv = (TextView) v;
            if (i == index) {
                tv.setBackgroundResource(R.drawable.scale_note_active_background);
                tv.setScaleX(1.1f);
                tv.setScaleY(1.1f);
            } else if (i < index) {
                tv.setBackgroundResource(R.drawable.scale_note_completed_background);
                tv.setScaleX(1f);
                tv.setScaleY(1f);
            } else {
                tv.setBackgroundResource(R.drawable.scale_note_default_background);
                tv.setScaleX(1f);
                tv.setScaleY(1f);
            }
        }
    }

    private void showFeedback(String text, boolean positive) {
        tvFeedback.setText(text);
        tvFeedback.setVisibility(View.VISIBLE);
        tvFeedback.setTextColor(getColorCompat(positive ? android.R.color.holo_green_dark : android.R.color.holo_red_dark));
        uiHandler.removeCallbacksAndMessages(null);
        uiHandler.postDelayed(() -> tvFeedback.setVisibility(View.INVISIBLE), 500);
    }

    private int dpToPx(int dp) {
        float density = requireContext().getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    private @ColorInt int getColorCompat(int resId) {
        return ContextCompat.getColor(requireContext(), resId);
    }

    private void startPitchDetectionIfAllowed() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_RECORD_AUDIO);
            return;
        }
        if (pitchDetector == null) {
            pitchDetector = new PitchDetector(this, requireContext());
        }
        pitchDetector.startDetection();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_RECORD_AUDIO) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startPitchDetectionIfAllowed();
            } else {
                Toast.makeText(requireContext(), "Permiso de micrófono requerido", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onPitchDetected(double frequency) {
        if (System.currentTimeMillis() < nextAllowedDetectionTime) return;
        handlePitch(frequency);
    }

    private void handlePitch(double frequency) {
        if (viewModel == null) return;

        int nearestMidi = NoteUtils.frequencyToNearestMidi(frequency);
        String note = NoteUtils.midiToNoteName(nearestMidi);
        double centsOff = NoteUtils.centsOff(frequency);
        long now = System.currentTimeMillis();

        if (!note.equals(lastNote) || Math.abs(centsOff - lastCents) > 20) {
            lastNote = note;
            lastChangeTime = now;
            lastCents = centsOff;
            return;
        }

        if (now - lastChangeTime >= STABLE_MS) {
            viewModel.onUserPlayedNote(note, centsOff);
            nextAllowedDetectionTime = now + COOLDOWN_AFTER_DETECTION_MS;
            lastChangeTime = now + COOLDOWN_AFTER_DETECTION_MS;
        }
    }

    private int rootNameToMidi(String rootName) {
        int base = 60;
        switch (rootName) {
            case "C#": return base + 1;
            case "D": return base + 2;
            case "D#": return base + 3;
            case "E": return base + 4;
            case "F": return base + 5;
            case "F#": return base + 6;
            case "G": return base + 7;
            case "G#": return base + 8;
            case "A": return base + 9;
            case "A#": return base + 10;
            case "B": return base + 11;
            case "C":
            default: return base;
        }
    }

    private ScaleUtils.ScaleType mapNameToScaleType(String name) {
        if (name == null) return ScaleUtils.ScaleType.MAJOR;
        switch (name) {
            case "Mayor": return ScaleUtils.ScaleType.MAJOR;
            case "Menor natural": return ScaleUtils.ScaleType.NATURAL_MINOR;
            case "Menor armónica": return ScaleUtils.ScaleType.HARMONIC_MINOR;
            case "Menor melódica": return ScaleUtils.ScaleType.MELODIC_MINOR_ASC;
            case "Pentatónica mayor": return ScaleUtils.ScaleType.PENTATONIC_MAJOR;
            case "Pentatónica menor": return ScaleUtils.ScaleType.PENTATONIC_MINOR;
            case "Blues": return ScaleUtils.ScaleType.BLUES;
            case "Dórica": return ScaleUtils.ScaleType.DORIAN;
            case "Mixolidia": return ScaleUtils.ScaleType.MIXOLYDIAN;
            case "Lidia": return ScaleUtils.ScaleType.LYDIAN;
            case "Frigia": return ScaleUtils.ScaleType.PHRYGIAN;
            case "Locria": return ScaleUtils.ScaleType.LOCRIAN;
            default: return ScaleUtils.ScaleType.MAJOR;
        }
    }

    private String mapToRepositoryScaleType(ScaleUtils.ScaleType scaleType) {
        switch (scaleType) {
            case PENTATONIC_MINOR:
                return "Minor Pentatonic";
            case PENTATONIC_MAJOR:
                return "Major Pentatonic";
            case BLUES:
                return "Blues";
            default:
                return scaleType.name();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (pitchDetector != null) {
            pitchDetector.stopDetection();
        }
    }
}
