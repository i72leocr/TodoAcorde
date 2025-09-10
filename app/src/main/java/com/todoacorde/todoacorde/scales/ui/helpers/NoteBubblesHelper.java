package com.todoacorde.todoacorde.scales.ui.helpers;

import android.content.Context;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.ColorInt;

import com.todoacorde.todoacorde.R;

import java.util.List;

/**
 * Helper para renderizar una tira horizontal de “burbujas” (notas) dentro de un {@link HorizontalScrollView}
 * con un {@link LinearLayout} como contenedor.
 *
 * Características:
 * - Tamaño de burbuja adaptable al ancho visible (máx. ~5 burbujas visibles).
 * - Resalta la nota activa y marca las anteriores como completadas.
 * - Desplazamiento al índice indicado, centrando visualmente la burbuja objetivo.
 */
public class NoteBubblesHelper {

    /* Configuración de layout y fallback. */
    private static final int MAX_VISIBLE = 5;   /* Número objetivo de burbujas visibles. */
    private static final int FALLBACK_DP = 44;  /* Tamaño por defecto si aún no hay viewport. */
    private static final int MARGIN_DP = 6;     /* Margen alrededor de cada burbuja. */

    private final Context context;
    private final HorizontalScrollView scroll;
    private final LinearLayout container;
    private final @ColorInt int textColor;

    private int scrollIndex = 0;
    private int bubblePx = -1;

    /**
     * @param context   contexto
     * @param scroll    scroll horizontal que contiene el contenedor
     * @param container contenedor lineal (debe tener orientación horizontal)
     * @param textColor color de texto para las burbujas
     */
    public NoteBubblesHelper(Context context,
                             HorizontalScrollView scroll,
                             LinearLayout container,
                             @ColorInt int textColor) {
        this.context = context;
        this.scroll = scroll;
        this.container = container;
        this.textColor = textColor;
    }

    /**
     * Limpia el contenedor y reinicia el índice de scroll.
     */
    public void reset() {
        container.removeAllViews();
        scrollIndex = 0;
    }

    /**
     * Establece la secuencia de notas a mostrar como burbujas, resetea el scroll
     * y ajusta tamaños una vez disponible el viewport.
     *
     * @param notes lista de nombres de nota (se muestran tal cual)
     */
    public void setNotes(List<String> notes) {
        container.removeAllViews();
        if (notes != null) {
            for (String note : notes) {
                container.addView(createNoteBubble(note));
            }
        }
        scrollIndex = 0;
        /* Post para asegurar que el ancho del scroll esté medido antes de ajustar. */
        scroll.post(() -> {
            adjustBubbleSizeToViewport();
            scrollTo(0);
        });
    }

    /**
     * Actualiza el estilo de las burbujas para reflejar el índice activo y los ya completados.
     * Activa: fondo “active”; completados: fondo “completed”; futuros: fondo “default”.
     *
     * @param index índice de la nota activa (base 0); -1 para ninguna activa
     */
    public void highlight(int index) {
        int count = container.getChildCount();
        for (int i = 0; i < count; i++) {
            View v = container.getChildAt(i);
            if (!(v instanceof TextView)) continue;
            TextView tv = (TextView) v;

            tv.setScaleX(1f);
            tv.setScaleY(1f);

            if (i == index) {
                tv.setBackgroundResource(R.drawable.scale_note_active_background);
            } else if (index >= 0 && i < index) {
                tv.setBackgroundResource(R.drawable.scale_note_completed_background);
            } else {
                tv.setBackgroundResource(R.drawable.scale_note_default_background);
            }
        }
    }

    /**
     * Desplaza el scroll para centrar (en lo posible) la burbuja indicada.
     *
     * @param index índice de destino
     */
    public void scrollTo(int index) {
        if (index < 0 || index >= container.getChildCount()) return;
        scrollIndex = index;
        View child = container.getChildAt(index);

        scroll.post(() -> {
            int childWidth = child.getWidth();
            int viewport = scroll.getWidth();
            int target = child.getLeft() - (viewport - childWidth) / 2;

            int max = Math.max(0, container.getWidth() - viewport);
            if (target < 0) target = 0;
            if (target > max) target = max;

            scroll.smoothScrollTo(target, 0);
        });
    }

    /**
     * Desplaza el scroll en pasos relativos (por ejemplo, +1 o -1).
     *
     * @param delta desplazamiento relativo del índice (negativo para atrás)
     */
    public void scrollStep(int delta) {
        int count = container.getChildCount();
        if (count == 0) return;
        int next = Math.max(0, Math.min(count - 1, scrollIndex + delta));
        scrollTo(next);
    }

    /**
     * Ajusta el tamaño de cada burbuja en función del ancho visible del {@link HorizontalScrollView}.
     * Limita el tamaño entre 32dp y 64dp y reparte para ~MAX_VISIBLE elementos.
     */
    private void adjustBubbleSizeToViewport() {
        int viewport = scroll.getWidth();
        if (viewport <= 0) {
            bubblePx = dp(FALLBACK_DP);
        } else {
            int m = dp(MARGIN_DP);
            int candidate = (viewport / MAX_VISIBLE) - (2 * m);
            int min = dp(32), max = dp(64);
            if (candidate < min) candidate = min;
            if (candidate > max) candidate = max;
            bubblePx = candidate;
        }
        int count = container.getChildCount();
        for (int i = 0; i < count; i++) {
            View v = container.getChildAt(i);
            LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) v.getLayoutParams();
            lp.width = bubblePx;
            lp.height = bubblePx;
            int m = dp(MARGIN_DP);
            lp.setMargins(m, m, m, m);
            v.setLayoutParams(lp);
        }
        container.requestLayout();
    }

    /**
     * Crea una vista de tipo “burbuja” para una nota concreta.
     *
     * @param note texto a mostrar en la burbuja
     * @return TextView configurado
     */
    private TextView createNoteBubble(String note) {
        TextView tv = new TextView(context);
        int size = (bubblePx > 0) ? bubblePx : dp(FALLBACK_DP);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(size, size);
        int m = dp(MARGIN_DP);
        lp.setMargins(m, m, m, m);
        tv.setLayoutParams(lp);

        tv.setText(note);
        tv.setGravity(Gravity.CENTER);
        tv.setIncludeFontPadding(false);
        tv.setTextSize(16f);
        tv.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        tv.setEllipsize(TextUtils.TruncateAt.END);
        tv.setMaxLines(1);
        tv.setBackgroundResource(R.drawable.scale_note_default_background);
        tv.setTextColor(textColor);
        return tv;
    }

    /**
     * Conversión dp → px.
     */
    private int dp(int dp) {
        float density = context.getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}
