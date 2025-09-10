package com.todoacorde.todoacorde;

import android.app.Dialog;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.todoacorde.todoacorde.practice.ui.GridWithPointsView;

/**
 * Diálogo modal para mostrar un diagrama de acorde (posición de dedos en el mástil).
 * Utiliza un {@link GridWithPointsView} para renderizar el patrón.
 *
 * Uso:
 * ChordDialogFragment.newInstance(hint).show(getSupportFragmentManager(), "tag");
 */
public class ChordDialogFragment extends DialogFragment {

    /** Clave del argumento que transporta el hint/patrón del acorde. */
    private static final String ARG_HINT = "hint";

    /**
     * Crea una nueva instancia del diálogo con el patrón/hint del acorde.
     *
     * @param hint cadena con el patrón del acorde (p. ej. “x06420”, “002310”).
     * @return instancia de {@link ChordDialogFragment}.
     */
    public static ChordDialogFragment newInstance(String hint) {
        ChordDialogFragment fragment = new ChordDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_HINT, hint);
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Ajusta el tamaño y posición de la ventana del diálogo al iniciarse.
     * Se fija WRAP_CONTENT y se centra en pantalla.
     */
    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null && dialog.getWindow() != null) {
            dialog.getWindow().setLayout(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            dialog.getWindow().setGravity(Gravity.CENTER); // centra el diálogo
        }
    }

    /**
     * Infla el layout del diálogo y configura el {@link GridWithPointsView} con el hint recibido.
     * Actualmente se invoca {@code setPointsFromHint} con valores de ejemplo; si existe el argumento
     * {@link #ARG_HINT}, se podría usar para poblar el diagrama según necesidades de la app.
     *
     * @return la vista raíz del diálogo.
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_chord, container, false);

        GridWithPointsView gridWithPointsView = view.findViewById(R.id.gridWithPointsView);
        if (getArguments() != null) {
            String hint = getArguments().getString(ARG_HINT);
            // Ejemplo de configuración del diagrama (actualmente valores fijos)
            gridWithPointsView.setPointsFromHint("x06420", "002310");
            // Si procede, usar 'hint' para personalizar el diagrama.
        }

        return view;
    }

    /**
     * Garantiza el centrado del diálogo también al reanudar (por si cambia el window token/flags).
     */
    @Override
    public void onResume() {
        super.onResume();
        Dialog dialog = getDialog();
        if (dialog != null && dialog.getWindow() != null) {
            dialog.getWindow().setGravity(Gravity.CENTER);
        }
    }
}
