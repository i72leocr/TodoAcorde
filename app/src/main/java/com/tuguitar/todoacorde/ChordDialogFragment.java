package com.tuguitar.todoacorde;

import android.app.Dialog;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Gravity;

public class ChordDialogFragment extends DialogFragment {

    private static final String ARG_HINT = "hint";

    public static ChordDialogFragment newInstance(String hint) {
        ChordDialogFragment fragment = new ChordDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_HINT, hint);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null && dialog.getWindow() != null) {
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            dialog.getWindow().setGravity(Gravity.CENTER); // Center the dialog
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_chord, container, false);

        GridWithPointsView gridWithPointsView = view.findViewById(R.id.gridWithPointsView);
        if (getArguments() != null) {
            String hint = getArguments().getString(ARG_HINT);
            gridWithPointsView.setPointsFromHint("x06420", "002310");
        }

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        Dialog dialog = getDialog();
        if (dialog != null && dialog.getWindow() != null) {
            // Ensure the dialog stays centered
            dialog.getWindow().setGravity(Gravity.CENTER);
        }
    }
}
