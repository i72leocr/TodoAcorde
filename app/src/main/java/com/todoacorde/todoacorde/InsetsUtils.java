package com.todoacorde.todoacorde;

import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/**
 * Utilidades para aplicar paddings y márgenes en función de los
 * WindowInsets (status bar, navigation bar, gestural insets, etc.).
 *
 * Todas las funciones registran un OnApplyWindowInsetsListener sobre la vista
 * y llaman a requestApplyInsets() para forzar una primera aplicación.
 */
public final class InsetsUtils {
    private InsetsUtils() {
    }

    /**
     * Aplica padding adicional en la vista para acomodar las system bars.
     *
     * Conserva el padding original de la vista y suma el in-set superior
     * y/o inferior en función de los parámetros.
     *
     * @param v vista objetivo.
     * @param top si es true, añade el in-set superior (status bar) al paddingTop.
     * @param bottom si es true, añade el in-set inferior (navigation bar/gestos) al paddingBottom.
     */
    public static void applySystemBarsPadding(View v, boolean top, boolean bottom) {
        final int start0 = v.getPaddingStart();
        final int top0 = v.getPaddingTop();
        final int end0 = v.getPaddingEnd();
        final int bottom0 = v.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(v, (view, insets) -> {
            Insets sb = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            int t = top ? top0 + sb.top : top0;
            int b = bottom ? bottom0 + sb.bottom : bottom0;
            view.setPaddingRelative(start0, t, end0, b);
            return insets;
        });
        v.requestApplyInsets();
    }

    /**
     * Aplica el in-set superior (status bar) como paddingTop y añade una
     * cantidad extra fija en dp. Conserva el padding original.
     *
     * @param v vista objetivo.
     * @param extraDp cantidad adicional en dp a sumar al in-set superior.
     */
    public static void applyTopInsetPaddingWithExtra(View v, int extraDp) {
        final int start0 = v.getPaddingStart();
        final int top0 = v.getPaddingTop();
        final int end0 = v.getPaddingEnd();
        final int bottom0 = v.getPaddingBottom();
        final int extraPx = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, extraDp, v.getResources().getDisplayMetrics());

        ViewCompat.setOnApplyWindowInsetsListener(v, (view, insets) -> {
            Insets sb = insets.getInsets(WindowInsetsCompat.Type.statusBars());
            view.setPaddingRelative(start0, top0 + sb.top + extraPx, end0, bottom0);
            return insets;
        });
        v.requestApplyInsets();
    }

    /**
     * Eleva visualmente una vista por encima de la barra de navegación.
     *
     * Si la vista tiene LayoutParams de tipo MarginLayoutParams, ajusta el
     * bottomMargin al in-set inferior. En caso contrario, suma el in-set inferior
     * al paddingBottom. Útil para botones flotantes o contenedores en la parte baja.
     *
     * @param v vista objetivo.
     */
    public static void liftAboveNavBarWithMargin(View v) {
        ViewCompat.setOnApplyWindowInsetsListener(v, (view, insets) -> {
            Insets sb = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            ViewGroup.LayoutParams p = view.getLayoutParams();
            if (p instanceof ViewGroup.MarginLayoutParams) {
                ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) p;
                lp.bottomMargin = sb.bottom;
                view.setLayoutParams(lp);
            } else {
                view.setPadding(
                        view.getPaddingLeft(),
                        view.getPaddingTop(),
                        view.getPaddingRight(),
                        view.getPaddingBottom() + sb.bottom
                );
            }
            return insets;
        });
        v.requestApplyInsets();
    }
}
