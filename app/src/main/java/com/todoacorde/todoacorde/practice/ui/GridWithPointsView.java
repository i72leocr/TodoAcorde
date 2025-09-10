package com.todoacorde.todoacorde.practice.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

/**
 * Vista personalizada que dibuja un diagrama de mástil con:
 * - Rejilla de trastes y cuerdas.
 * - Puntos de digitación por cuerda/traste.
 * - Símbolos superiores por cuerda (p.ej., 'x' / '0').
 * - Número de traste a la izquierda cuando hay desplazamiento.
 *
 * El contenido se configura mediante {@link #setPointsFromHint(String, String)}.
 */
public class GridWithPointsView extends View {

    /* Pinceles de dibujo */
    private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint thickTopLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pointPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint symbolPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fingerNumberPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    /* Número de trastes visibles en el diagrama (filas) */
    private final int frets = 5;

    /* Estado de cada cuerda */
    private final int[] pointPositions = new int[]{-1, -1, -1, -1, -1, -1};   /* traste por cuerda; -1 = sin punto */
    private final String[] stringSymbols = new String[]{"", "", "", "", "", ""}; /* símbolos superiores (x/0) */
    private final String[] fingerNumbers = new String[]{"", "", "", "", "", ""}; /* números de dedo dentro del punto */

    /* Número de traste mostrado a la izquierda (1 si no hay desplazamiento) */
    private int leftNumber = 1;

    /**
     * Constructor programático.
     */
    public GridWithPointsView(Context ctx) {
        super(ctx);
        initPaints();
    }

    /**
     * Constructor XML.
     */
    public GridWithPointsView(Context ctx, AttributeSet attrs) {
        super(ctx, attrs);
        initPaints();
    }

    /**
     * Constructor XML con estilo/defStyle.
     */
    public GridWithPointsView(Context ctx, AttributeSet attrs, int ds) {
        super(ctx, attrs, ds);
        initPaints();
    }

    /**
     * Inicializa estilos de los pinceles.
     */
    private void initPaints() {
        gridPaint.setColor(0xFF000000);
        gridPaint.setStrokeWidth(4f);

        thickTopLinePaint.setColor(0xFF000000);
        thickTopLinePaint.setStrokeWidth(12f);

        pointPaint.setColor(0xFF000000);
        pointPaint.setStyle(Paint.Style.FILL);

        symbolPaint.setColor(0xFF000000);
        symbolPaint.setTextAlign(Paint.Align.CENTER);

        fingerNumberPaint.setColor(0xFFFFFFFF);
        fingerNumberPaint.setTextAlign(Paint.Align.CENTER);
    }

    /**
     * Renderiza la rejilla y elementos dinámicos del diagrama.
     */
    @Override
    protected void onDraw(Canvas c) {
        super.onDraw(c);

        float padL = getPaddingLeft();
        float padR = getPaddingRight();
        float padT = getPaddingTop();
        float padB = getPaddingBottom();

        float usableWidth = getWidth() - padL - padR;
        float usableHeight = getHeight() - padT - padB;

        int strings = pointPositions.length;

        /* Margen proporcional para mantener proporciones cuadradas */
        float margin = Math.min(usableWidth, usableHeight) * 0.20f;

        float left = padL + margin;
        float right = padL + usableWidth - margin * 0.35f;
        float top = padT + margin;
        float bottom = padT + usableHeight - margin * 0.35f;

        float rowH = (bottom - top) / frets;
        float colW = (right - left) / (strings - 1);

        /* Tamaños dinámicos en función del espacio disponible */
        float dynamicRadius = Math.min(colW, rowH) * 0.48f;
        float symbolTextSize = dynamicRadius * 1.7f;
        float fingerTextSize = dynamicRadius * 1.3f;

        fingerNumberPaint.setTextSize(fingerTextSize);
        symbolPaint.setTextSize(symbolTextSize);

        /* Filas (trastes) */
        for (int i = 0; i <= frets; i++) {
            float y = top + i * rowH;
            c.drawLine(left, y, right, y, i == 0 ? thickTopLinePaint : gridPaint);
        }

        /* Columnas (cuerdas) */
        for (int i = 0; i < strings; i++) {
            float x = left + i * colW;
            c.drawLine(x, top, x, bottom, gridPaint);
        }

        /* Puntos de digitación y número de dedo */
        for (int i = 0; i < strings; i++) {
            int fret = pointPositions[i];
            if (fret > 0) {
                float cx = left + i * colW;
                float cy = top + (fret - 0.5f) * rowH;
                c.drawCircle(cx, cy, dynamicRadius, pointPaint);

                String fn = fingerNumbers[i];
                if (!fn.isEmpty()) {
                    c.drawText(fn, cx, cy + (fingerTextSize / 3), fingerNumberPaint);
                }
            }
        }

        /* Símbolos superiores (x / 0) */
        for (int i = 0; i < strings; i++) {
            String sym = stringSymbols[i];
            if (!sym.isEmpty()) {
                float x = left + i * colW;
                float y = top - dynamicRadius * 1.3f;
                c.drawText(sym, x, y, symbolPaint);
            }
        }

        /* Número de traste a la izquierda */
        float x0 = left - dynamicRadius * 2.0f;
        float y0 = top + rowH / 2 + (symbolTextSize / 2);
        c.drawText(String.valueOf(leftNumber), x0, y0, symbolPaint);
    }

    /**
     * Configura el diagrama a partir de dos cadenas:
     * - {@code hint}: por cuerda, 'x' (muda), '0' (al aire) o dígito de traste (1..9).
     * - {@code fingers}: por cuerda, dígito del dedo (opcional) para mostrar dentro del punto.
     *
     * Si algún traste de {@code hint} supera el número de trastes visibles, el diagrama
     * se desplaza y se ajustan los trastes relativos, actualizando {@link #leftNumber}.
     *
     * @param hint    codificación por cuerda (p.ej., "x32010")
     * @param fingers dígitos de dedos por cuerda (p.ej., "x23010") o vacío
     */
    public void setPointsFromHint(String hint, String fingers) {
        boolean needsAdjust = false;
        int minFret = Integer.MAX_VALUE;

        /* Determina si hay que desplazar el diagrama y calcula el menor traste > 0 */
        for (char ch : hint.toCharArray()) {
            if (ch != 'x' && ch != '0') {
                int f = Character.getNumericValue(ch);
                if (f > frets) needsAdjust = true;
                if (f > 0 && f < minFret) minFret = f;
            }
        }
        leftNumber = needsAdjust ? minFret : 1;

        /* Aplica puntos/símbolos por cuerda */
        for (int i = 0; i < hint.length() && i < pointPositions.length; i++) {
            char ch = hint.charAt(i);
            if (ch == 'x' || ch == '0') {
                pointPositions[i] = -1;
                stringSymbols[i] = String.valueOf(ch);
                fingerNumbers[i] = "";
            } else {
                int f = Character.getNumericValue(ch);
                pointPositions[i] = needsAdjust ? f - (minFret - 1) : f;
                stringSymbols[i] = "";
                fingerNumbers[i] = i < fingers.length() ? String.valueOf(fingers.charAt(i)) : "";
            }
        }
        invalidate();
    }

    /**
     * Limpia completamente el diagrama (sin puntos ni símbolos) y
     * restablece el número de traste izquierdo a 1.
     */
    public void clearDiagram() {
        for (int i = 0; i < pointPositions.length; i++) {
            pointPositions[i] = -1;
            stringSymbols[i] = "";
            fingerNumbers[i] = "";
        }
        leftNumber = 1;
        invalidate();
    }
}
