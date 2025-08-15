package com.tuguitar.todoacorde;

import android.content.res.Resources;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public final class InsetsUtils {
    private InsetsUtils() {}

    /** Aplica padding con los systemBars (status + nav) solo en los lados indicados. */
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

    /** Igual que arriba, pero añade además un extra fijo en dp al TOP. */
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

    /** Coloca una vista (p.ej. BottomNavigationView) justo por encima del gesto/nav bar usando MARGEN. */
    public static void liftAboveNavBarWithMargin(View v) {
        ViewCompat.setOnApplyWindowInsetsListener(v, (view, insets) -> {
            Insets sb = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            ViewGroup.LayoutParams p = view.getLayoutParams();
            if (p instanceof ViewGroup.MarginLayoutParams) {
                ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) p;
                lp.bottomMargin = sb.bottom; // deja el nav view “pegado” al contenido, no flotando demasiado abajo
                view.setLayoutParams(lp);
            } else {
                // fallback: usa padding si no hay márgenes
                view.setPadding(view.getPaddingLeft(), view.getPaddingTop(),
                        view.getPaddingRight(), view.getPaddingBottom() + sb.bottom);
            }
            return insets;
        });
        v.requestApplyInsets();
    }
}
