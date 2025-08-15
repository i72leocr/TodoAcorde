package com.tuguitar.todoacorde.scales.ui.helpers;

import android.content.Context;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.ColorInt;

import com.tuguitar.todoacorde.R;

import java.util.List;

public class NoteBubblesHelper {

    private static final int MAX_VISIBLE = 5;   // máx. burbujas visibles
    private static final int FALLBACK_DP = 44;  // tamaño si aún no hay medidas
    private static final int MARGIN_DP   = 6;   // margen de cada círculo

    private final Context context;
    private final HorizontalScrollView scroll;
    private final LinearLayout container;
    private final @ColorInt int textColor;

    private int scrollIndex = 0;
    private int bubblePx = -1;

    public NoteBubblesHelper(Context context,
                             HorizontalScrollView scroll,
                             LinearLayout container,
                             @ColorInt int textColor) {
        this.context = context;
        this.scroll = scroll;
        this.container = container;
        this.textColor = textColor;
    }

    public void reset() {
        container.removeAllViews();
        scrollIndex = 0;
    }

    /** Crea las burbujas (no hace highlight automático). */
    public void setNotes(List<String> notes) {
        container.removeAllViews();
        if (notes != null) {
            for (String note : notes) {
                container.addView(createNoteBubble(note));
            }
        }
        scrollIndex = 0;
        scroll.post(() -> {
            adjustBubbleSizeToViewport();
            scrollTo(0);
        });
    }

    /** i==-1 ⇒ ninguna activa (todo gris). */
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

    /** Centra visualmente la burbuja. Ignora índices fuera de rango. */
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

    /** Paso de flechas: ±1. */
    public void scrollStep(int delta) {
        int count = container.getChildCount();
        if (count == 0) return;
        int next = Math.max(0, Math.min(count - 1, scrollIndex + delta));
        scrollTo(next);
    }

    // ---------- privados ----------
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

    private int dp(int dp) {
        float density = context.getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}
