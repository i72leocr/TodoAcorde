package com.tuguitar.todoacorde;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

public class GridWithPointsView extends View {
    private final Paint gridPaint         = new Paint();
    private final Paint thickTopLinePaint = new Paint();
    private final Paint pointPaint        = new Paint();
    private final Paint symbolPaint       = new Paint();
    private final Paint fingerNumberPaint = new Paint();
    private int frets   = 5;

    private final float pointRadius       = 15f;  // radio de los puntos

    private int[]    pointPositions = new int[]{-1, -1, -1, -1, -1, -1};
    private String[] stringSymbols  = new String[]{"", "", "", "", "", ""};
    private String[] fingerNumbers  = new String[]{"", "", "", "", "", ""};
    private int      leftNumber     = 1;

    public GridWithPointsView(Context ctx)                     { super(ctx); initPaints(); }
    public GridWithPointsView(Context ctx, AttributeSet attrs) { super(ctx, attrs); initPaints(); }
    public GridWithPointsView(Context ctx, AttributeSet attrs, int ds) {
        super(ctx, attrs, ds); initPaints();
    }

    private void initPaints() {
        gridPaint.setColor(0xFF000000);
        gridPaint.setStrokeWidth(4);

        thickTopLinePaint.setColor(0xFF000000);
        thickTopLinePaint.setStrokeWidth(12);

        pointPaint.setColor(0xFF000000);
        pointPaint.setStyle(Paint.Style.FILL);

        symbolPaint.setColor(0xFF000000);
        symbolPaint.setTextAlign(Paint.Align.CENTER);

        fingerNumberPaint.setColor(0xFFFFFFFF);
        fingerNumberPaint.setTextAlign(Paint.Align.CENTER);
    }

    @Override
    protected void onDraw(Canvas c) {
        super.onDraw(c);

        // 1) Respetar paddings
        float padL = getPaddingLeft();
        float padR = getPaddingRight();
        float padT = getPaddingTop();
        float padB = getPaddingBottom();

        // 2) Márgenes internos (dejamos espacio de pointRadius dentro de paddings)
        float left   = padL + pointRadius;
        float right  = getWidth()  - padR - pointRadius;
        float top    = padT + pointRadius;
        float bottom = getHeight() - padB - pointRadius;

        // 3) Parámetros fijos
        int strings = pointPositions.length; // 6

        // 4) Cálculo de espaciados
        float rowH = (bottom - top) / frets;
        float colW = (right - left) / (strings - 1);

        // 5) Dibujar líneas horizontales
        for (int i = 0; i <= frets; i++) {
            float y = top + i * rowH;
            c.drawLine(left, y, right, y, i == 0 ? thickTopLinePaint : gridPaint);
        }
        // 6) Dibujar líneas verticales
        for (int i = 0; i < strings; i++) {
            float x = left + i * colW;
            c.drawLine(x, top, x, bottom, gridPaint);
        }

        // 7) Dibujar puntos y fingerNumbers
        for (int i = 0; i < strings; i++) {
            int fret = pointPositions[i];
            if (fret > 0) {
                float cx = left + i * colW;
                float cy = top + (fret - 0.5f) * rowH;
                c.drawCircle(cx, cy, pointRadius, pointPaint);
                String fn = fingerNumbers[i];
                if (!fn.isEmpty()) {
                    // centramos ligeramente hacia abajo
                    c.drawText(fn, cx, cy + (fingerNumberPaint.getTextSize() / 3), fingerNumberPaint);
                }
            }
        }

        // 8) Dibujar símbolos 'X' y '0' encima de la rejilla (respetando padding)
        for (int i = 0; i < strings; i++) {
            String sym = stringSymbols[i];
            if (!sym.isEmpty()) {
                float x = left + i * colW;
                float y = top - pointRadius; // suficiente espacio por paddingTop
                symbolPaint.setTextSize(sym.equals("0") ? 40 : 60);
                c.drawText(sym, x, y, symbolPaint);
            }
        }

        // 9) Número de traste en la izquierda
        float x0 = left - pointRadius * 1.2f;
        float y0 = top + rowH / 2 + (symbolPaint.getTextSize() / 2 - 6);
        c.drawText(String.valueOf(leftNumber), x0, y0, symbolPaint);
    }

    /**
     * hint:   e.g. "0232x0"
     * fingers: e.g. "012300"
     */
    public void setPointsFromHint(String hint, String fingers) {
        // primer pase: detectar si hay que ajustar
        boolean needsAdjust = false;
        int minFret = Integer.MAX_VALUE;
        for (char ch : hint.toCharArray()) {
            if (ch != 'x' && ch != '0') {
                int f = Character.getNumericValue(ch);
                if (f > frets) needsAdjust = true;
                if (f > 0 && f < minFret) minFret = f;
            }
        }
        leftNumber = needsAdjust ? minFret : 1;

        // segundo pase: rellenar arrays
        for (int i = 0; i < hint.length() && i < pointPositions.length; i++) {
            char ch = hint.charAt(i);
            if (ch == 'x' || ch == '0') {
                pointPositions[i] = -1;
                stringSymbols[i]  = String.valueOf(ch);
                fingerNumbers[i]  = "";
            } else {
                int f = Character.getNumericValue(ch);
                pointPositions[i] = needsAdjust ? f - (minFret - 1) : f;
                stringSymbols[i]  = "";
                fingerNumbers[i]  = i < fingers.length() ? String.valueOf(fingers.charAt(i)) : "";
            }
        }
        invalidate();
    }
}
